#!/bin/sh
# VehLog launcher for M.I.B. (green menu > M.I.B. > Advanced Settings > Run individual script)
#
# 1. TOUCHCURSOR and JOKERBTN: if their boot guard switched them off, re-arm them
#    with MIB2-Patcher's own "install" (from the mib2-patcher folder on this SD card)
# 2. VehLog:
#      file vehlog/UNINSTALL on the SD -> uninstall VehLog and the speed overlay player
#      VehLog installed                -> update: copy the log to the SD card, and
#                                         install the SD card version if it is newer
#      not installed                   -> install
# 3. Speed overlay: put the overlay stream-player in place (only over the original
#    mib-cluster-map v1.2.6 player, the original is kept), see vehlog/speedoverlay.sh
#
# Output: vehlog/vehlog_install.log on the SD card, logs in vehlog/logs/,
# MIB2-Patcher's own log in mib2-patcher/logs/
if [ -z "$VOLUME" ]; then
	VOLUME=/net/mmx/fs/sda0
fi
mount -uw "$VOLUME" 2> /dev/null
note() {
	echo "$*"
	{ echo "$*" >> "$VOLUME/vehlog/vehlog_install.log"; } 2> /dev/null
}
# First word of the first line of a text file, shell builtins only. The unit has no
# tr; a Windows line end stays on the word and the patterns below allow for it.
word_of() {
	_l=""
	read -r _l < "$1" 2> /dev/null
	set -- $_l
	echo "$1"
}

patcher_sh="$VOLUME/mib2-patcher/patcher.sh"
# $1 = patch name, $2 = its flag name in /mnt/persist (TOUCHCURSOR and JOKERBTN have
# the same kind of boot guard)
rearm_patch() {
	if [ -f /net/mmx/mnt/persist/$2_disabled ]; then
		note "== $1 was switched off by its boot guard, re-arming with MIB2-Patcher"
		if [ -f "$patcher_sh" ]; then
			/bin/sh "$patcher_sh" $1 install < /dev/null
			if [ -f /net/mmx/mnt/persist/$2_disabled ]; then
				note "$1: still disabled, see mib2-patcher/logs"
			else
				note "$1: re-armed, active after the MMI restart"
			fi
		else
			note "$1: $patcher_sh not found, use GEM > MIB2_Patcher > $1 > Install"
		fi
	else
		note "== $1: not switched off by its boot guard, nothing to do"
	fi
}
rearm_patch TOUCHCURSOR touchcursor
rearm_patch JOKERBTN jokerbtn

vehlog_sh="$VOLUME/vehlog/vehlog.sh"
if [ ! -f "$vehlog_sh" ]; then
	echo "$vehlog_sh not found, copy the vehlog folder to the SD card"
elif [ -f "$VOLUME/vehlog/UNINSTALL" ]; then
	/bin/sh "$VOLUME/vehlog/speedoverlay.sh" uninstall < /dev/null
	/bin/sh "$vehlog_sh" uninstall < /dev/null
elif grep -q '#VEHLOG' /net/mmx/mnt/app/eso/hmi/lsd/lsd.sh 2> /dev/null; then
	/bin/sh "$vehlog_sh" update < /dev/null
else
	/bin/sh "$vehlog_sh" install < /dev/null
fi
if [ -f "$vehlog_sh" ] && [ ! -f "$VOLUME/vehlog/UNINSTALL" ]; then
	/bin/sh "$VOLUME/vehlog/speedoverlay.sh" install < /dev/null
	/bin/sh "$VOLUME/vehlog/speedoverlay.sh" status < /dev/null
fi
# 3b. Overlay position: vehlog/speedoverlay.conf on the SD card is copied to
#     /mnt/persist/speedoverlay.conf (the player reads it when it starts)
if [ -f "$VOLUME/vehlog/speedoverlay.conf" ]; then
	if cp "$VOLUME/vehlog/speedoverlay.conf" /net/mmx/mnt/persist/speedoverlay.conf 2> /dev/null; then
		_pos=""
		while read -r _l; do _pos="$_pos $_l"; done < "$VOLUME/vehlog/speedoverlay.conf"
		note "== overlay position set:$_pos"
	else
		note "== overlay position: could not write /mnt/persist/speedoverlay.conf"
	fi
fi
# 3c. CarPlay card position in the cockpit (mib-cluster-map's own setting files,
#     read when the iPhone connects): vehlog/carplay_card.txt = left|right|top|iphone
CP_NAV=/net/mmx/mnt/app/root/mibr-carplay111-nav
if [ -f "$VOLUME/vehlog/carplay_card.txt" ]; then
	card=$(word_of "$VOLUME/vehlog/carplay_card.txt")
	case "$card" in
		left|left?) card=left ;;
		right|right?) card=right ;;
		top|top?) card=top ;;
		iphone|iphone?) card=iphone ;;
	esac
	mount -uw /net/mmx/mnt/app 2> /dev/null
	[ -f "$CP_NAV.maneuverLayout" ] && [ ! -f "$CP_NAV.maneuverLayout.vehlog.bak" ] && cp "$CP_NAV.maneuverLayout" "$CP_NAV.maneuverLayout.vehlog.bak" 2> /dev/null
	case "$card" in
		left|right|top)
			if echo "$card" > "$CP_NAV.maneuverLayout" && : > "$CP_NAV-query.enabled"; then
				note "== CarPlay card position set to $card (active when the iPhone connects next time)"
			else
				note "== CarPlay card position: could not write $CP_NAV.maneuverLayout"
			fi
			;;
		iphone)
			rm -f "$CP_NAV.maneuverLayout"
			if [ ! -f "$CP_NAV.surface" ] && [ ! -f "$CP_NAV.showSpeedLimit" ] && [ ! -f "$CP_NAV.showCompass" ] && [ ! -f "$CP_NAV.showETA" ]; then
				rm -f "$CP_NAV-query.enabled"
			fi
			note "== CarPlay card position left to the iPhone again"
			;;
		*) note "== carplay_card.txt: '$card' not understood (left, right, top or iphone)" ;;
	esac
	mount -ur /net/mmx/mnt/app 2> /dev/null
fi
# 3d. Street name on the cockpit while CarPlay is up: vehlog/street_blank.txt = on|off
#     (VehLog 0.6 then sends a blank street name; without CarPlay nothing changes)
if [ -f "$VOLUME/vehlog/street_blank.txt" ]; then
	sb=$(word_of "$VOLUME/vehlog/street_blank.txt")
	case "$sb" in
		on|on?) sb=on ;;
		off|off?) sb=off ;;
	esac
	case "$sb" in
		on) : > /net/mmx/mnt/persist/vehlog_street_blank 2> /dev/null && note "== street name blank with CarPlay: on (active within a few seconds)" ;;
		off) rm -f /net/mmx/mnt/persist/vehlog_street_blank && note "== street name blank with CarPlay: off" ;;
		*) note "== street_blank.txt: '$sb' not understood (on or off)" ;;
	esac
fi
# 3f. Listen only (BAP, ExLAP), looking for the oil temperature: vehlog/listen.txt = on|off
#     (VehLog 1.0; off = /mnt/persist/vehlog_listen_off, BAP listening stops within 2 s)
if [ -f "$VOLUME/vehlog/listen.txt" ]; then
	li=$(word_of "$VOLUME/vehlog/listen.txt")
	case "$li" in
		on|on?) li=on ;;
		off|off?) li=off ;;
	esac
	case "$li" in
		on) rm -f /net/mmx/mnt/persist/vehlog_listen_off && note "== listen (BAP, ExLAP): on" ;;
		off) : > /net/mmx/mnt/persist/vehlog_listen_off 2> /dev/null && note "== listen (BAP, ExLAP): off" ;;
		*) note "== listen.txt: '$li' not understood (on or off)" ;;
	esac
fi
# 3e. CarPlay safe area: vehlog/carplay_safearea.txt = pixels to raise its bottom edge
#     (0 = mib-cluster-map's original). mib-cluster-map's file
#     /mnt/app/root/mibr-carplay111-safearea.conf (x= y= w= h=, read when the iPhone
#     connects) tells the iPhone where the car and its labels go; a shorter h moves
#     the street name up, x, y and w (and so the turn card at the top) stay. Always
#     worked out from the original, kept as .vehlog.orig; if mib-cluster-map wrote a
#     new file since (reinstall), that one becomes the original.
SA=/net/mmx/mnt/app/root/mibr-carplay111-safearea.conf
sa_read() {
	SX=""; SY=""; SW=""; SH=""
	while read -r _l; do
		case "$_l" in
			x=*) SX=${_l#x=} ;;
			y=*) SY=${_l#y=} ;;
			w=*) SW=${_l#w=} ;;
			h=*) SH=${_l#h=} ;;
		esac
	done < "$1"
	case "$SX$SY$SW$SH" in ''|*[!0-9]*) return 1 ;; esac
	[ -n "$SX" ] && [ -n "$SY" ] && [ -n "$SW" ] && [ -n "$SH" ]
}
if [ -f "$VOLUME/vehlog/carplay_safearea.txt" ]; then
	raise=$(word_of "$VOLUME/vehlog/carplay_safearea.txt")
	case "$raise" in *[!0-9]) raise=${raise%?} ;; esac   # Windows line end
	case "$raise" in
		''|*[!0-9]*) note "== carplay_safearea.txt: '$raise' not understood (a number of pixels, 0 = original)" ;;
		*)
		if [ ! -f "$SA" ]; then
			note "== CarPlay safe area: $SA not found (mib-cluster-map uses the whole window), nothing changed"
		elif ! sa_read "$SA"; then
			note "== CarPlay safe area: $SA not in the expected form, nothing changed"
		else
			mount -uw /net/mmx/mnt/app 2> /dev/null
			cur="$SX $SY $SW $SH"
			ours=""
			[ -f "$SA.vehlog.ours" ] && sa_read "$SA.vehlog.ours" && ours="$SX $SY $SW $SH"
			orig=""
			[ -f "$SA.vehlog.orig" ] && sa_read "$SA.vehlog.orig" && orig="$SX $SY $SW $SH"
			if [ -z "$orig" ] || { [ "$cur" != "$ours" ] && [ "$cur" != "$orig" ]; }; then
				cp "$SA" "$SA.vehlog.orig" && orig="$cur" && note "== CarPlay safe area: original kept ($cur as x y w h)"
			fi
			if [ -z "$orig" ]; then
				note "== CarPlay safe area: could not keep a copy of the original, nothing changed"
			elif [ "$raise" -eq 0 ]; then
				cp "$SA.vehlog.orig" "$SA" && rm -f "$SA.vehlog.ours" && note "== CarPlay safe area: original again ($orig), active when the iPhone connects next time"
			else
				set -- $orig
				nh=$(($4 - raise))
				if [ "$raise" -gt 150 ] || [ "$nh" -lt 120 ]; then
					note "== CarPlay safe area: raising by $raise would leave too little ($nh of $4 px high), nothing changed"
				elif { echo "x=$1"; echo "y=$2"; echo "w=$3"; echo "h=$nh"; } > "$SA.vehlog.ours" && cp "$SA.vehlog.ours" "$SA"; then
					note "== CarPlay safe area: bottom edge $raise px higher (x y w h: $orig -> $1 $2 $3 $nh), active when the iPhone connects next time"
				else
					cp "$SA.vehlog.orig" "$SA" 2> /dev/null
					note "== CarPlay safe area: could not write $SA, original put back"
				fi
			fi
			mount -ur /net/mmx/mnt/app 2> /dev/null
		fi
		;;
	esac
fi
# 4. Read-only display manager snapshot (which windows the cockpit shows right now),
#    saved as vehlog/logs/dmdt_<n>/. Run it once with the CarPlay map and once with the
#    Audi map in the cockpit.
if [ -f "$VOLUME/vehlog/dmdt_snapshot.sh" ]; then
	sn=1
	while [ -d "$VOLUME/vehlog/logs/dmdt_$sn" ]; do sn=$((sn + 1)); done
	mkdir -p "$VOLUME/vehlog/logs" 2> /dev/null
	on -f mmx /bin/sh /fs/sda0/vehlog/dmdt_snapshot.sh $sn < /dev/null > /dev/null 2>&1
	if [ -f "$VOLUME/vehlog/logs/dmdt_$sn/done.txt" ]; then
		note "== display snapshot saved: vehlog/logs/dmdt_$sn ($(word_of "$VOLUME/vehlog/logs/dmdt_$sn/carplay_state.txt"))"
	else
		note "== display snapshot: could not be taken"
	fi
fi
sleep 10
