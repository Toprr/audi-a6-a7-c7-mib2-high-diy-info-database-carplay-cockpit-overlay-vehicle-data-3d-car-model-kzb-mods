#!/bin/sh
#
# VehLog 0.2: read-only logger for oil temperature, coolant temperature and speed
# Audi MHI2, firmware MHI2_ER_AU57x_K3663_1 (MU1425) only.
#
# Usage: vehlog.sh <command>
#   install     copy VEHLOG.jar to the unit and add the boot guard block to lsd.sh
#               (also re-arms the logger after the boot guard disabled it)
#   uninstall   remove the lsd.sh block, the jar and the flags (the log stays)
#   status      show what is installed
#   collect     copy the log from the unit to the SD card (vehlog/logs/)
#   update      collect, then install if the jar on the unit differs from the SD card
#
# 0.2 also re-arms MIB2-Patcher's TOUCHCURSOR after a false boot guard stop
# (at most 3 times in a row), see VehLog.java.
#
# Started from the M.I.B. launcher (mod/command.sh). Reboot the MMI after
# install and uninstall. Everything it does is written to vehlog/vehlog_install.log.
#
# For tests on a PC: VEHLOG_TEST_ROOT=<folder> runs it against copies, no mounts.

VERSION="0.2"
MARKER="#VEHLOG"
JAR_NAME="VEHLOG.jar"
SUPPORTED_JXE_SHA1="2be4758b1e9f3068173c8df6dac9be541113a3e6"

PKG=$(dirname "$0")
case "$PKG" in
	/*) ;;
	*) PKG="$(pwd)/$PKG" ;;
esac
SD_ROOT="${PKG%/*}"
LOG="$PKG/vehlog_install.log"

T="${VEHLOG_TEST_ROOT:-}"
if [ -z "$T" ]; then
	export PATH=:/proc/boot:/sbin:/bin:/usr/bin:/usr/sbin:/net/mmx/bin:/net/mmx/usr/bin:/net/mmx/usr/sbin:/net/mmx/sbin:/net/mmx/mnt/app/armle/bin:/net/mmx/mnt/app/armle/sbin:/net/mmx/mnt/app/armle/usr/bin:/net/mmx/mnt/app/armle/usr/sbin
	export LD_LIBRARY_PATH=/net/mmx/mnt/app/root/lib-target:/net/mmx/mnt/eso/lib:/net/mmx/eso/lib:/net/mmx/mnt/app/usr/lib:/net/mmx/mnt/app/armle/lib:/net/mmx/mnt/app/armle/lib/dll:/net/mmx/mnt/app/armle/usr/lib
	mount -uw "$SD_ROOT" 2> /dev/null
fi

APP_MOUNT="$T/net/mmx/mnt/app"
LSD_SH="$APP_MOUNT/eso/hmi/lsd/lsd.sh"
LSD_BAK="$LSD_SH.vehlog.bak"
LSD_JXE="$T/net/mmx/ifs/lsd.jxe"
JARS="$APP_MOUNT/eso/hmi/lsd/jars"
PERSIST="$T/net/mmx/mnt/persist"
FLAG_PENDING="$PERSIST/vehlog_pending"
FLAG_LOADED="$PERSIST/vehlog_loaded"
FLAG_DISABLED="$PERSIST/vehlog_disabled"
UNIT_LOG="$PERSIST/vehlog.log"
SHMEM="$T/net/rcc/dev/shmem"

if [ "$(print -r -- '-x\t' 2> /dev/null)" = '-x\t' ]; then
	out() { print -r -- "$1"; }
else
	out() { printf '%s\n' "$1"; }
fi

log() {
	echo "$*"
	{ echo "$*" >> "$LOG"; } 2> /dev/null
}

die() {
	log "ERROR: $*"
	log "nothing more was changed"
	exit 1
}

mount_rw() {
	[ -n "$T" ] && return 0
	mount -uw "$APP_MOUNT" 2> /dev/null || log "WARNING: could not remount $APP_MOUNT read-write"
}

mount_ro() {
	[ -n "$T" ] && return 0
	mount -ur "$APP_MOUNT" 2> /dev/null
}

sha1_of() {
	for tool in sha1sum /net/rcc/usr/bin/sha1sum /usr/bin/sha1sum "$SD_ROOT/apps/sbin/sha1sum"; do
		sum=$("$tool" "$1" 2> /dev/null) || continue
		set -- $sum
		[ -n "$1" ] || continue
		echo "$1"
		return 0
	done
	return 1
}

# MU1425 starts J9 with: on -p 12 /ifs/jre/bin/j9 $VMOPTIONS -Xbootclasspath:$BOOTCLASSPATH ...
is_j9_line() {
	case "$1" in
		'#'*) return 1 ;;
		'$J9'*|*'/j9 '*'$BOOTCLASSPATH'*) return 0 ;;
	esac
	return 1
}

count_j9_lines() {
	n=0
	while IFS= read -r l || [ -n "$l" ]; do
		is_j9_line "$l" && n=$((n + 1))
	done < "$1"
	echo $n
}

is_patched() {
	grep -q "$MARKER" "$LSD_SH" 2> /dev/null
}

# lsd.sh without our lines, to compare with what was there before
strip_ours() {
	while IFS= read -r l || [ -n "$l" ]; do
		case "$l" in
			*"$MARKER"*) ;;
			*) out "$l" ;;
		esac
	done < "$1"
}

check_not_busy() {
	for lock in backup reboot flash; do
		[ -f "$SHMEM/$lock.mib" ] && die "M.I.B. $lock is running, try again when it has finished"
	done
	return 0
}

check_firmware() {
	[ -f "$LSD_JXE" ] || die "$LSD_JXE not found"
	log "checking the firmware, this takes about a minute..."
	sum=$(sha1_of "$LSD_JXE")
	[ -n "$sum" ] || die "no sha1sum tool found, cannot check the firmware"
	log "lsd.jxe sha1: $sum"
	[ "$sum" = "$SUPPORTED_JXE_SHA1" ] || die "unsupported firmware, VehLog is built for MHI2_ER_AU57x_K3663_1 only"
}

check_jar() {
	expected=$(cat "$PKG/$JAR_NAME.sha1" 2> /dev/null)
	[ -n "$expected" ] || return 1
	sum=$(sha1_of "$1")
	[ -n "$sum" ] && [ "$sum" = "$expected" ]
}

check_lsd_sh() {
	[ -f "$LSD_SH" ] || die "$LSD_SH not found"
	[ "$(count_j9_lines "$LSD_SH")" = 1 ] || die "lsd.sh does not have exactly one J9 start line, unknown layout"
	grep -q 'BOOTCLASSPATH' "$LSD_SH" || die "lsd.sh does not use BOOTCLASSPATH, unknown layout"
}

check_persist() {
	[ -d "$PERSIST" ] || die "$PERSIST not found, the boot guard needs it"
	( : > "$PERSIST/vehlog_test" ) 2> /dev/null || die "$PERSIST is not writable, the boot guard needs it"
	rm -f "$PERSIST/vehlog_test"
}

clear_flags() {
	rm -f "$FLAG_PENDING" "$FLAG_LOADED" "$FLAG_DISABLED" 2> /dev/null
}

# Runs on the MMX in lsd.sh at every HMI start. It loads the jar only after leaving
# a pending flag; VehLog deletes that flag once the HMI has run for 90 s. A flag still
# there on the next start means that start failed (or the MMI was off within 90 s),
# so the jar is not loaded any more until "install" is run again.
patch_block() {
	m="$MARKER"
	out "# VehLog boot guard, managed by vehlog.sh $m"
	out "VEHLOG_JAR=/eso/hmi/lsd/jars/$JAR_NAME $m"
	out "VEHLOG_FLAG=/mnt/persist/vehlog $m"
	out 'if [ -f $VEHLOG_JAR ] && [ ! -f ${VEHLOG_FLAG}_disabled ]; then '"$m"
	out '	rm -f ${VEHLOG_FLAG}_loaded '"$m"
	out '	if [ -f ${VEHLOG_FLAG}_pending ]; then '"$m"
	out '		( : > ${VEHLOG_FLAG}_disabled ) 2> /dev/null '"$m"
	out '		rm -f ${VEHLOG_FLAG}_pending '"$m"
	out '	elif ( : > ${VEHLOG_FLAG}_pending ) 2> /dev/null; then '"$m"
	out '		case "$BOOTCLASSPATH" in '"$m"
	out '			*-Xbootclasspath/p:*) BOOTCLASSPATH="${BOOTCLASSPATH%%-Xbootclasspath/p:*}-Xbootclasspath/p:$VEHLOG_JAR:${BOOTCLASSPATH#*-Xbootclasspath/p:}" ;; '"$m"
	out '			*) BOOTCLASSPATH="$BOOTCLASSPATH -Xbootclasspath/p:$VEHLOG_JAR" ;; '"$m"
	out '		esac '"$m"
	out '	fi '"$m"
	out 'fi '"$m"
}

BLOCK_LINES=15

# Writes the new lsd.sh to a temp file, checks it, and only then replaces lsd.sh.
# $1 = add | remove
rewrite_lsd_sh() {
	tmp="$LSD_SH.vehlog.tmp"
	ref="$LSD_SH.vehlog.ref"
	strip_ours "$LSD_SH" > "$ref" || { rm -f "$ref"; return 1; }
	if [ "$1" = add ]; then
		block=$(patch_block)
		done_block=0
		while IFS= read -r l || [ -n "$l" ]; do
			if [ $done_block = 0 ] && is_j9_line "$l"; then
				out "$block"
				done_block=1
			fi
			out "$l"
		done < "$ref" > "$tmp"
		want=$(( $(grep -c "$MARKER" "$tmp") ))
		[ $done_block = 1 ] && [ "$want" = "$BLOCK_LINES" ] || { rm -f "$tmp" "$ref"; return 1; }
	else
		cp "$ref" "$tmp" || { rm -f "$tmp" "$ref"; return 1; }
		grep -q "$MARKER" "$tmp" && { rm -f "$tmp" "$ref"; return 1; }
	fi
	# Checks: one J9 line, and every line that is not ours is exactly as before
	[ "$(count_j9_lines "$tmp")" = 1 ] || { rm -f "$tmp" "$ref"; return 1; }
	strip_ours "$tmp" > "$tmp.cmp"
	sum_a=$(sha1_of "$tmp.cmp")
	sum_b=$(sha1_of "$ref")
	if [ -z "$sum_a" ] || [ "$sum_a" != "$sum_b" ]; then
		rm -f "$tmp" "$tmp.cmp" "$ref"
		return 1
	fi
	rm -f "$tmp.cmp"
	cp "$tmp" "$LSD_SH" || { rm -f "$tmp" "$ref"; return 1; }
	rm -f "$tmp" "$ref"
	# And the file on the unit is now what we checked
	if [ "$1" = add ]; then
		[ "$(grep -c "$MARKER" "$LSD_SH")" = "$BLOCK_LINES" ] || return 1
	else
		grep -q "$MARKER" "$LSD_SH" && return 1
	fi
	[ "$(count_j9_lines "$LSD_SH")" = 1 ]
}

do_install() {
	log "== install (VehLog $VERSION) $(date 2> /dev/null)"
	check_not_busy
	[ -f "$PKG/$JAR_NAME" ] || die "$PKG/$JAR_NAME is missing on the SD card"
	check_jar "$PKG/$JAR_NAME" || die "$PKG/$JAR_NAME does not match $JAR_NAME.sha1, copy the vehlog folder to the SD card again"
	check_firmware
	check_lsd_sh
	check_persist

	mount_rw
	mkdir -p "$PKG/backup" 2> /dev/null
	[ -f "$PKG/backup/lsd.sh" ] || cp "$LSD_SH" "$PKG/backup/lsd.sh" 2> /dev/null
	if [ ! -f "$LSD_BAK" ]; then
		cp "$LSD_SH" "$LSD_BAK" || { mount_ro; die "could not back up lsd.sh"; }
		log "lsd.sh backed up to $LSD_BAK"
	fi
	mkdir -p "$JARS" 2> /dev/null
	cp "$PKG/$JAR_NAME" "$JARS/$JAR_NAME" || { mount_ro; die "could not copy $JAR_NAME to $JARS"; }
	if ! check_jar "$JARS/$JAR_NAME"; then
		rm -f "$JARS/$JAR_NAME"
		mount_ro
		die "the copied $JAR_NAME does not match the checksum, removed it again"
	fi
	log "copied $JAR_NAME to $JARS"
	if [ -f "$UNIT_LOG" ]; then
		mv -f "$UNIT_LOG" "$UNIT_LOG.old" 2> /dev/null && log "old unit log kept as $UNIT_LOG.old"
	fi
	if ! rewrite_lsd_sh add; then
		rm -f "$JARS/$JAR_NAME"
		mount_ro
		die "could not patch lsd.sh safely, lsd.sh was left as it was and the jar was removed"
	fi
	log "lsd.sh patched with the boot guard block ($BLOCK_LINES lines before the J9 line)"
	clear_flags
	mount_ro
	log "done. Restart the MMI, then leave it on for at least 2 minutes."
}

do_uninstall() {
	log "== uninstall (VehLog $VERSION) $(date 2> /dev/null)"
	check_not_busy
	[ -f "$LSD_SH" ] || die "$LSD_SH not found"
	mount_rw
	if is_patched; then
		if ! rewrite_lsd_sh remove; then
			mount_ro
			die "could not clean lsd.sh safely, it was left as it was. Backup: $LSD_BAK"
		fi
		log "lsd.sh block removed"
	else
		log "lsd.sh is not patched"
	fi
	if [ -f "$JARS/$JAR_NAME" ]; then
		rm -f "$JARS/$JAR_NAME" && log "$JAR_NAME removed"
	fi
	clear_flags
	mount_ro
	log "done. Restart the MMI. The log stays in $UNIT_LOG (use collect)."
}

do_status() {
	log "== status (VehLog $VERSION) $(date 2> /dev/null)"
	if is_patched; then log "lsd.sh:     patched ($(grep -c "$MARKER" "$LSD_SH") lines)"; else log "lsd.sh:     not patched"; fi
	if [ ! -f "$JARS/$JAR_NAME" ]; then
		log "jar:        not installed"
	elif check_jar "$JARS/$JAR_NAME"; then
		log "jar:        installed"
	else
		log "jar:        installed, but different from the SD card copy"
	fi
	if [ -f "$FLAG_DISABLED" ]; then
		log "boot guard: DISABLED the logger after a failed or short start, run install to re-arm"
	elif [ -f "$FLAG_LOADED" ]; then
		log "boot guard: logger ran on the last start"
	elif [ -f "$FLAG_PENDING" ]; then
		log "boot guard: start in progress (less than 90 s), or the last start failed"
	else
		log "boot guard: no start since install"
	fi
	if [ -f "$UNIT_LOG" ]; then
		log "unit log:   $UNIT_LOG, last lines:"
		tail -5 "$UNIT_LOG" 2> /dev/null | while IFS= read -r l; do log "  $l"; done
	else
		log "unit log:   none yet"
	fi
}

do_collect() {
	log "== collect $(date 2> /dev/null)"
	mkdir -p "$PKG/logs" 2> /dev/null
	n=1
	while [ -f "$PKG/logs/vehlog_$n.log" ]; do n=$((n + 1)); done
	if [ -f "$UNIT_LOG" ]; then
		cp "$UNIT_LOG" "$PKG/logs/vehlog_$n.log" && log "copied the log to vehlog/logs/vehlog_$n.log"
	else
		log "no log on the unit yet ($UNIT_LOG)"
	fi
	do_status
}

case "$1" in
	install) do_install ;;
	uninstall) do_uninstall ;;
	status) do_status ;;
	collect) do_collect ;;
	update)
		do_collect
		if [ -f "$JARS/$JAR_NAME" ] && check_jar "$JARS/$JAR_NAME"; then
			log "jar on the unit is already this version, nothing to update"
		else
			do_install
		fi
		;;
	*) echo "usage: vehlog.sh install|uninstall|status|collect|update"; exit 2 ;;
esac
