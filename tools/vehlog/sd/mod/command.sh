#!/bin/sh
# VehLog launcher for M.I.B. (green menu > M.I.B. > Advanced Settings > Run individual script)
#
# 1. TOUCHCURSOR: if its boot guard switched it off, re-arm it with MIB2-Patcher's
#    own "install" (from the mib2-patcher folder on this SD card)
# 2. VehLog:
#      file vehlog/UNINSTALL on the SD -> uninstall
#      VehLog installed                -> update: copy the log to the SD card, and
#                                         install the SD card version if it is newer
#      not installed                   -> install
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
	/bin/sh "$vehlog_sh" uninstall < /dev/null
elif grep -q '#VEHLOG' /net/mmx/mnt/app/eso/hmi/lsd/lsd.sh 2> /dev/null; then
	/bin/sh "$vehlog_sh" update < /dev/null
else
	/bin/sh "$vehlog_sh" install < /dev/null
fi
sleep 10
