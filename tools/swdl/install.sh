#!/bin/ksh
# CarLook installer: replaces /mnt/app/eso/content.kzb (3D car model, menu icons) with heavy checks,
# or puts the original back (uninstall). Runs on the RCC (called by the SWDL FinalScript) and reaches
# the multimedia CPU through /net/mmx.
#
# Fill in the four values below for YOUR unit and YOUR modified file (see docs/05-swdl-installer.md):
#   ORIG  = SHA1 of the untouched content.kzb from your own unit (take it from your M.I.B. backup)
#   NEW   = SHA1 of your modified content.kzb
#   SIZE  = size in bytes (both files must have the same size - all edits are done in place)
#   TRAIN = your software train, exactly as in /net/rcc/dev/shmem/version.txt ("Current train = '...'")
# Example values are from an Audi A6 C7.5 with MHI2_ER_AU57x_K3663_1.
VOLUME="$1"; PKG=$VOLUME/CarLook
T=/net/mmx/mnt/app/eso/content.kzb
TMPN=/net/mmx/mnt/app/eso/content.kzb.carlook-new
ORIG=cb7ecbbba8ebb6d79c5eeb7af6edcabde9db155a; NEW=0000000000000000000000000000000000000000; SIZE=53066629
TRAIN=MHI2_ER_AU57x_K3663_1
# sha1sum for QNX/ARM is not part of the firmware - copy apps/sbin/sha1sum from M.I.B. (GPL-2.0) to CarLook/bin/
SHA1=$PKG/bin/sha1sum
APPRW=0
log(){ echo "[CarLook] $*"; }
fin(){ if [ $APPRW -eq 1 ]; then sync 2>/dev/null; if mount -ur /net/mmx/mnt/app 2>/dev/null || on -f mmx mount -ur /mnt/app; then APPRW=0; log "/mnt/app is read-only again"; else log "WARNING: /mnt/app is still writable - restart the MMI before switching off"; fi; fi; }
fail(){ log "ABORT: $*"; log "Nothing was changed on the original unless stated above."; fin; exit 1; }
trap fin EXIT
# Shell built-ins only: the RCC has no awk, tail or sync (sync only prints an error, harmless).
sha(){ _r=$($SHA1 "$1" 2>/dev/null); _r=${_r%%[ 	]*}; [ ${#_r} -eq 40 ] && echo "$_r"; }
sz(){ ( _l=$(ls -ln "$1" 2>/dev/null) || exit 1; set -f; set -- $_l; echo "$5" ); }
# free space in KB: third-to-last word of the df output (= "Avail" column of the last line)
dfkb(){ ( _o=$(df $1 /net/mmx/mnt/app 2>/dev/null) || exit 1; set -f; set -- $_o; [ $# -ge 3 ] || exit 1; shift $(($# - 3)); echo "$1" ); }
tokb(){ _v=$1; case "$_v" in *[Gg]) _m=1048576;; *[Mm]) _m=1024;; *[Kk]) _m=1;; *[0-9]) _m=1;; *) return 1;; esac; _v=${_v%[GgMmKk]}; _v=${_v%%.*}; case "$_v" in ''|*[!0-9]*) return 1;; esac; echo $((_v * _m)); }
log "===== CarLook ====="
[ -x $SHA1 ] || chmod +x $SHA1 2>/dev/null
[ -x $SHA1 ] || fail "sha1sum not executable"
grep -q "Current train = '$TRAIN'" /net/rcc/dev/shmem/version.txt || fail "wrong software: $(grep 'Current train' /net/rcc/dev/shmem/version.txt)"
log "Software OK: $TRAIN"
MODE=install
for f in $PKG/*; do case "${f##*/}" in [Uu][Nn][Ii][Nn][Ss][Tt][Aa][Ll][Ll]*) MODE=uninstall;; esac; done
log "Mode: $MODE"
[ -f $T ] || fail "$T not found"
CUR=$(sha $T); log "Unit: size $(sz $T), SHA1 $CUR"
if [ "$MODE" = install ]; then
  [ "$CUR" = "$NEW" ] && { log "Already installed. Nothing to do."; log "CARLOOK_OK"; exit 0; }
  [ "$CUR" = "$ORIG" ] || fail "file on the unit is NOT the known original - nothing will be changed"
  SRC=$PKG/data/content.kzb; WANT=$NEW
  if [ "$(sha $PKG/backup/content.kzb.original)" != "$ORIG" ]; then
    log "Backing up the original to SD ..."; on -f mmx cp $T $PKG/backup/content.kzb.original || fail "backup copy failed"; sync 2>/dev/null
    [ "$(sha $PKG/backup/content.kzb.original)" = "$ORIG" ] || fail "backup on SD is broken"
  fi
  log "Backup on SD verified"
else
  [ "$CUR" = "$ORIG" ] && { log "Original is already on the unit. Nothing to do."; log "CARLOOK_OK"; exit 0; }
  [ "$CUR" = "$NEW" ] || fail "file on the unit is neither the original nor CarLook - nothing will be changed"
  SRC=$PKG/backup/content.kzb.original; WANT=$ORIG
fi
[ "$(sz $SRC)" = "$SIZE" ] || fail "source file on SD has the wrong size"
[ "$(sha $SRC)" = "$WANT" ] || fail "source file on SD is damaged"
log "Source file on SD verified"
AVAIL=$(tokb "$(dfkb -k)")
[ -n "$AVAIL" ] && [ "$AVAIL" -gt 0 ] || AVAIL=$(tokb "$(dfkb -h)")
log "df: $(df -h /net/mmx/mnt/app 2>/dev/null)"
log "Free space /mnt/app: ${AVAIL} KB"
[ -n "$AVAIL" ] && [ "$AVAIL" -gt 60000 ] || fail "not enough free space on /mnt/app (need 60000 KB)"
mount -uw /net/mmx/mnt/app 2>/dev/null || on -f mmx mount -uw /mnt/app || fail "/mnt/app not writable"
APPRW=1; log "/mnt/app writable"
rm -f $TMPN
on -f mmx cp $SRC $TMPN || { rm -f $TMPN; fail "copy to the unit failed"; }
sync 2>/dev/null
[ "$(sha $TMPN)" = "$WANT" ] || { rm -f $TMPN; fail "copy on the unit is damaged - deleted"; }
log "Copy on the unit verified"
on -f mmx mv -f $TMPN $T || { rm -f $TMPN; fail "rename failed"; }
sync 2>/dev/null
END=$(sha $T)
if [ "$END" != "$WANT" ]; then
  log "ERROR: final check differs ($END) - restoring the original from SD"
  on -f mmx cp $PKG/backup/content.kzb.original $TMPN && [ "$(sha $TMPN)" = "$ORIG" ] && on -f mmx mv -f $TMPN $T
  log "After restore: $(sha $T) (original would be $ORIG)"
  fail "installation rolled back"
fi
log "Final check OK: $END"
fin
log "CARLOOK_OK - please restart the MMI (hold the power/volume knob for 10 s)"
