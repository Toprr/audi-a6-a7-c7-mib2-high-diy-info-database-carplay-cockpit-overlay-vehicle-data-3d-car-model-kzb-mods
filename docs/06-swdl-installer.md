# 06 – Installing a modified `content.kzb` through the SD update (SWDL)

Tools: [`tools/swdl/build_package.py`](../tools/swdl/build_package.py) builds the card,
[`tools/swdl/install.sh`](../tools/swdl/install.sh) is the installer that runs on the unit.

## How the SD update can run your own script

The red-menu update reads `metainfo2.txt` in the card root. With the right flags it accepts unsigned content and,
after copying, runs a **FinalScript** on the RCC. The pattern (used by M.I.B. and mib-cluster-map too):

```
[common]
vendor = "CarLook"
skipCheckSignatureAndVariant = "true"
skipMetaCRC = "true"
skipFileCopyCrc = "true"
skipCheckVariant = "true"
skipCheckRegion = "true"
variant = "FM?-*-*-*-*"
release = "Name shown in the update menu"
FinalScript = "./CarLook/final/finalScript.sh"
FinalScriptChecksum = "<sha1 of finalScript.sh>"
FinalScriptMaxTime = "900"
FinalScriptName = "Final Script"

[CarLook\final\Dir]
FileSize = "<size of hashes.txt>"
CheckSum = "<sha1 of hashes.txt>"           # hashes.txt lists finalScript.sh + its sha1

[CarLook]                                    # one component so the update has something to "install"
...
[CarLook\version\0\default\File]
CheckSumSize = "524288"
CheckSum = "<sha1 of the first 512 KB of version/CarLook>"
FileSize = "<size>"
Version = "1"                                # raise it if the unit says "already installed"
Source = "../../CarLook"
Destination = "/HBpersistence/SWDL/Log/service/CarLook"   # only a tiny marker file is copied
```

Notes from our runs:
- `metainfo2.txt` with **CRLF** line endings (like M.I.B./mib-cluster-map) – that's the layout we tested.
- If any checksum is wrong, the menu refuses the package – nothing happens on the unit.
- The update copies only the small `version/CarLook` marker. The real work (the 51 MB file) is done by the
  FinalScript → `install.sh`, with our own checks.
- Choose "Cancel" when asked about "Version back documentation", then restart the MMI (hold power ~10 s).

## Installer design (`install.sh`)

Runs on the RCC, reaches the MMX through `/net/mmx`, uses only shell built-ins (the RCC has no awk/tail/sync).

1. Refuse to run on other software (`Current train = '...'` in `/net/rcc/dev/shmem/version.txt`).
2. Mode: install, or **uninstall** if a file named `UNINSTALL*` sits in `CarLook/`.
3. SHA1 of the file on the unit must be the **known original** (install) or **our file** (uninstall) –
   anything else → abort, nothing changed. Already done → "nothing to do".
4. Back up the original to `CarLook/backup/content.kzb.original` on the card (if not already there) and verify it.
5. Verify size + SHA1 of the source file on the card.
6. Check free space on `/mnt/app` (≥ 60 MB, parsed from `df` without awk, `-k` and `-h` formats).
7. `mount -uw /mnt/app`, copy to `content.kzb.carlook-new`, verify SHA1, **rename** over the original (atomic on
   the same file system), verify again.
8. If the final check fails: restore the original from the card automatically and report.
9. Always remount `/mnt/app` read-only (trap on exit).
10. Everything is logged to `CarLook/result.txt` on the card. Success line: `CARLOOK_OK`.

`sha1sum` is not on the unit – put the QNX/ARM binary from M.I.B. (`apps/sbin/sha1sum`, GPL-2.0) into
`CarLook/bin/`. The builder does that for you.

## Building the card

```
python tools/swdl/build_package.py --orig work/content.kzb --new work/content_stufe2.kzb \
    --sha1sum <M.I.B.>/apps/sbin/sha1sum --train MHI2_ER_AU57x_K3663_1 --out SD_CARLOOK --name "My CarLook"
```

- With `--cluster-map <unpacked mib-cluster-map release>` you get **one** card that installs mib-cluster-map
  (their installer, unchanged) and then CarLook – this is the exact layout we ran in the car.
  The FinalScript skips the cluster-map part if the card already says `CLUSTERMAP_INSTALL_OK`.
- Without it you get a CarLook-only card (same pattern, but that exact standalone layout was not run in our car).

Copy the folder contents to an empty FAT32 card, card in SD1, red menu → Update → SD → your name → Start.
Afterwards read `CarLook/result.txt`.

**Uninstall:** move `CarLook/to_uninstall_move_this_file_up/UNINSTALL.txt` into `CarLook/` and run the update
again → the original comes back (SHA1-checked). Keep this card – it holds the backup of your original.

## Testing without a car

We ran the installer on Windows/Git-Bash with stub commands (`on`, `mount`, `df` …) and a PATH **without**
awk/tail/printf, in these scenarios: fresh install, second run, uninstall, foreign file on the unit
(must abort), too little space, `df` missing, damaged source file. Do the same after every change.
