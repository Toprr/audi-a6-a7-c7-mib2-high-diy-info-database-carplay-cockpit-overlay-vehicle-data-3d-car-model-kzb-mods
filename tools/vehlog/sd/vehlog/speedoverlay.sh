#!/bin/sh
#
# Speed overlay 0.1: swaps mib-cluster-map's stream-player (v1.2.6) for the same
# player with a small speed box drawn over the cockpit map. The speed comes from
# VehLog (/tmp/vehlog_speed); without it the player draws the map exactly as before.
#
# Usage: speedoverlay.sh install | uninstall | status
#
#   install    only if the player on the unit is the original mib-cluster-map v1.2.6
#              player (sha1 checked) or already ours. The original is kept as
#              stream-player.mcm126 next to it.
#   uninstall  puts the original back (sha1 checked).
#
# The new player is used from the next CarPlay / Android Auto start (MMI restart).

STOCK_SHA1="0e38137fe58434d6722a09c5e5c67c10e91c0f92"
# Earlier builds of our overlay player (they may be on the unit already)
OLD_OURS="479d82463046be4542a41b68119ddabbd9cd3c46 74978212b4ee078873024fd5e7a0bb0e9fcbda13 72902184037727f4eeea6485233bf1dab03224ac 8ff3d58834b929a71b6c7a30fde9acc6cac77257"
OURS="stream-player.speed"

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
NAV="$APP_MOUNT/navigation"
PLAYER="$NAV/stream-player"
ORIG="$NAV/stream-player.mcm126"
SHMEM="$T/net/rcc/dev/shmem"

log() {
	echo "$*"
	{ echo "$*" >> "$LOG"; } 2> /dev/null
}

die() {
	log "ERROR (speed overlay): $*"
	log "nothing more was changed"
	exit 1
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

mount_rw() {
	[ -n "$T" ] && return 0
	mount -uw "$APP_MOUNT" 2> /dev/null || log "WARNING: could not remount $APP_MOUNT read-write"
}

mount_ro() {
	[ -n "$T" ] && return 0
	mount -ur "$APP_MOUNT" 2> /dev/null
}

check_not_busy() {
	for lock in backup reboot flash; do
		[ -f "$SHMEM/$lock.mib" ] && die "M.I.B. $lock is running, try again when it has finished"
	done
	return 0
}

# Copies $1 over the player the way mib-cluster-map does it: a .new file next to
# it, checked, then renamed (a running player cannot be overwritten).
put_player() {
	src="$1"; want="$2"
	rm -f "$PLAYER.new"
	cp "$src" "$PLAYER.new" || { rm -f "$PLAYER.new"; return 1; }
	[ "$(sha1_of "$PLAYER.new")" = "$want" ] || { rm -f "$PLAYER.new"; return 1; }
	chmod 755 "$PLAYER.new" || { rm -f "$PLAYER.new"; return 1; }
	mv -f "$PLAYER.new" "$PLAYER" || { rm -f "$PLAYER.new"; return 1; }
	[ "$(sha1_of "$PLAYER")" = "$want" ]
}

do_install() {
	log "== speed overlay install"
	check_not_busy
	[ -f "$PKG/$OURS" ] || die "$PKG/$OURS is missing on the SD card"
	ours_sha=$(cat "$PKG/$OURS.sha1" 2> /dev/null)
	[ -n "$ours_sha" ] || die "$PKG/$OURS.sha1 is missing on the SD card"
	[ "$(sha1_of "$PKG/$OURS")" = "$ours_sha" ] || die "$OURS on the SD card is damaged, copy it again"
	[ -f "$PLAYER" ] || die "$PLAYER not found (is mib-cluster-map installed?)"
	cur=$(sha1_of "$PLAYER")
	[ -n "$cur" ] || die "no sha1sum tool found"
	if [ "$cur" = "$ours_sha" ]; then
		log "the speed overlay player is already installed"
		return 0
	fi
	old=0
	for o in $OLD_OURS; do [ "$cur" = "$o" ] && old=1; done
	if [ $old = 1 ]; then
		[ -f "$ORIG" ] && [ "$(sha1_of "$ORIG")" = "$STOCK_SHA1" ] || die "an older overlay player is installed but the original copy is missing, not touching it"
		log "replacing an older overlay player (sha1 $cur)"
	else
		[ "$cur" = "$STOCK_SHA1" ] || die "the player on the unit is not the mib-cluster-map v1.2.6 one (sha1 $cur), not touching it"
	fi
	mount_rw
	if [ ! -f "$ORIG" ] || [ "$(sha1_of "$ORIG")" != "$STOCK_SHA1" ]; then
		cp "$PLAYER" "$ORIG" && [ "$(sha1_of "$ORIG")" = "$STOCK_SHA1" ] || { rm -f "$ORIG"; mount_ro; die "could not keep a copy of the original player"; }
		log "original player kept as $ORIG"
	fi
	if ! put_player "$PKG/$OURS" "$ours_sha"; then
		put_player "$ORIG" "$STOCK_SHA1"
		mount_ro
		die "could not install the overlay player, the original is in place"
	fi
	mount_ro
	log "speed overlay player installed. Active after the MMI restart."
}

do_uninstall() {
	log "== speed overlay uninstall"
	check_not_busy
	[ -f "$ORIG" ] || die "no original player found at $ORIG"
	[ "$(sha1_of "$ORIG")" = "$STOCK_SHA1" ] || die "$ORIG is not the original player, not using it"
	mount_rw
	put_player "$ORIG" "$STOCK_SHA1" || { mount_ro; die "could not put the original player back"; }
	mount_ro
	log "original mib-cluster-map player is back. Active after the MMI restart."
}

do_status() {
	cur=$(sha1_of "$PLAYER")
	ours_sha=$(cat "$PKG/$OURS.sha1" 2> /dev/null)
	if [ "$cur" = "$ours_sha" ]; then
		log "speed overlay: installed (current version)"
	elif [ "$cur" = "$STOCK_SHA1" ]; then
		log "speed overlay: not installed (original v1.2.6 player)"
	else
		log "speed overlay: unknown player on the unit (sha1 $cur)"
	fi
	if [ -f "$ORIG" ]; then log "original copy: $ORIG"; fi
}

case "$1" in
	install) do_install ;;
	uninstall) do_uninstall ;;
	status) do_status ;;
	*) echo "usage: speedoverlay.sh install|uninstall|status"; exit 2 ;;
esac
