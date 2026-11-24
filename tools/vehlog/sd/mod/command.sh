#!/bin/sh
# VehLog launcher for M.I.B. (green menu > M.I.B. > Advanced Settings > Run individual script)
#
# 1. TOUCHCURSOR: if its boot guard switched it off, re-arm it with MIB2-Patcher's
#    own "install" (from the mib2-patcher folder on this SD card)
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

patcher_sh="$VOLUME/mib2-patcher/patcher.sh"
if [ -f /net/mmx/mnt/persist/touchcursor_disabled ]; then
	note "== TOUCHCURSOR was switched off by its boot guard, re-arming with MIB2-Patcher"
	if [ -f "$patcher_sh" ]; then
		/bin/sh "$patcher_sh" TOUCHCURSOR install < /dev/null
		if [ -f /net/mmx/mnt/persist/touchcursor_disabled ]; then
			note "TOUCHCURSOR: still disabled, see mib2-patcher/logs"
		else
			note "TOUCHCURSOR: re-armed, active after the MMI restart"
		fi
	else
		note "TOUCHCURSOR: $patcher_sh not found, use GEM > MIB2_Patcher > TOUCHCURSOR > Install"
	fi
else
	note "== TOUCHCURSOR: not switched off by its boot guard, nothing to do"
fi

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
		note "== overlay position set: $(tr '\n' ' ' < "$VOLUME/vehlog/speedoverlay.conf")"
	else
		note "== overlay position: could not write /mnt/persist/speedoverlay.conf"
	fi
fi
# 3c. CarPlay card position in the cockpit (mib-cluster-map's own setting files,
#     read when the iPhone connects): vehlog/carplay_card.txt = left|right|top|iphone
CP_NAV=/net/mmx/mnt/app/root/mibr-carplay111-nav
if [ -f "$VOLUME/vehlog/carplay_card.txt" ]; then
	card=$(head -1 "$VOLUME/vehlog/carplay_card.txt" | tr -d ' \r')
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
# 4. Read-only display manager snapshot (which windows the cockpit shows right now),
#    saved as vehlog/logs/dmdt_<n>/. Run it once with the CarPlay map and once with the
#    Audi map in the cockpit.
if [ -f "$VOLUME/vehlog/dmdt_snapshot.sh" ]; then
	sn=1
	while [ -d "$VOLUME/vehlog/logs/dmdt_$sn" ]; do sn=$((sn + 1)); done
	mkdir -p "$VOLUME/vehlog/logs" 2> /dev/null
	on -f mmx /bin/sh /fs/sda0/vehlog/dmdt_snapshot.sh $sn < /dev/null > /dev/null 2>&1
	if [ -f "$VOLUME/vehlog/logs/dmdt_$sn/done.txt" ]; then
		note "== display snapshot saved: vehlog/logs/dmdt_$sn ($(cat "$VOLUME/vehlog/logs/dmdt_$sn/carplay_state.txt" 2> /dev/null | head -1))"
	else
		note "== display snapshot: could not be taken"
	fi
fi
sleep 10
