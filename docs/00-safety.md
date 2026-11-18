# 00: Safety first: how not to brick your MIB2 High

> **Read this first:** this is information, not a product. You can do a lot wrong, see the [README](../README.md). Your own risk.

What we did on our car, in this order:

1. **Full backup with M.I.B.** ([Mr-MIBonk/M.I.B._More-Incredible-Bash](https://github.com/Mr-MIBonk/M.I.B._More-Incredible-Bash)):
   the standard backup (EEPROM, flash, persistence, `lsd.jxe` …) **and** the advanced backup
   *"app.img to folder structure"*. Only the advanced backup contains the HMI graphics, including
   `eso/content.kzb` (3D car, icons) and `eso/hmi/splashscreen/*.canim`. Keep two copies of it on different disks.
2. **Never write a file to the unit that you have not verified on the PC.** All our `content.kzb` edits are
   *in place*: same file size, same directory, only known byte ranges changed. The scripts check this.
3. **Install only through the official update path** (red engineering menu → Update → SD). Our installer
   refuses to touch anything unless the file on the unit is byte-identical (SHA1) to the original it expects,
   makes a verified backup to the SD card first, copies to a temp name, verifies, renames, verifies again and
   rolls back automatically if the final check fails ([docs/06](06-swdl-installer.md)).
4. **Have the rollback ready before you install:** the same package with `UNINSTALL.txt` moved one folder up
   puts the original back (also SHA1-checked).
5. Charger on, ignition on, engine off, don't drive, don't open doors repeatedly (the unit may sleep).
6. Display-manager experiments (`dmdt`) are RAM-only: a reboot (hold the power/volume knob ~10 s) resets them.

What can go wrong and how bad it is:

| Situation | Effect | Fix |
|---|---|---|
| Corrupt `content.kzb` | 3D car / icons missing or HMI crash loop | install the original again via SD (SSH from M.I.B. helps) |
| Wrong `metainfo2.txt` checksum | update menu refuses the package | nothing happened, fix the package |
| Script missing a tool on the unit | our installer aborts before changing anything | see the tool list in [docs/01](01-unit-basics.md) |
| `dmdt` context changed | wrong picture in centre screen or cockpit | reboot |

Recommended: install the M.I.B. SSH server once, so you still have a way in if the HMI ever fails.
