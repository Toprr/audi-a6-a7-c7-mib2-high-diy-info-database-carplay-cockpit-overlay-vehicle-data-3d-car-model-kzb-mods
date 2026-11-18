# 01: MIB2 High basics (Audi C7 / C7.5)

Tested unit: Audi A6 Avant C7.5 2018, hardware **MU1425**, software train **`MHI2_ER_AU57x_K3663_1`**
(AU57x = the same software for A6 C7 (AU571 saloon / AU572 Avant/allroad) and A7 (AU573)).
Colour cluster (FIS) between the dials, fed by the MMI over MOST.

## Two computers in one box

| Name | CPU / role | Path seen from the other side |
|---|---|---|
| **MMX** | Nvidia Tegra (QNX 6.5), runs the HMI, 3D, graphics, `/mnt/app` | `/net/mmx/...` from the RCC |
| **RCC** | radio / car communication, runs the SWDL **FinalScript** | `/net/rcc/...` |

The SD card is mounted on the MMX as `/fs/sda0` (slot SD1) or `/fs/sdb0` (SD2), and on the RCC as
`/net/mmx/fs/sda0`. Run a command on the MMX from the RCC with `on -f mmx <cmd>`.

## Menus

- **Red engineering menu** (software update): hold BACK + the top-left soft key. Update → SD → pick the package.
  When the unit asks about "Version back documentation" press *Abbrechen/Cancel*.
- **Green engineering menu** (GEM): has to be enabled by coding first (developer mode in the MMI control unit, address 5F); then it opens with a long press of CAR + MENU (key combination can differ by model year). mib-cluster-map
  and mib2-toolbox add their own screens there (`.esd` files in `/mnt/app/eso/hmi/engdefs/`).
  Scripts started from the GEM run as long as the screen is open; start them in the background (`( ... ) &`)
  if they must keep running after you close the menu.

## Interesting files

| Path on the unit | What |
|---|---|
| `/mnt/app/eso/content.kzb` | Kanzi asset package: 3D car models (all C7 variants), materials, scenes, many UI images incl. the main-menu "Car" icon. ~51 MB. Read-only mount (`mount -uw /mnt/app` to change) |
| `/mnt/app/eso/hmi/splashscreen/*.canim` + `StartupImageConfig.txt` | boot animations ([docs/09](09-startup-image-canim.md)) |
| `/mnt/app/eso/hmi/lsd/lsd.jxe` | the Java HMI |
| `/net/rcc/dev/shmem/version.txt` | contains `Current train = '...'`: use it to make scripts refuse to run on other software |
| `/eso/bin/apps/dmdt` | display manager tool ([docs/07](07-display-manager-and-rear-camera.md)) |

## Tools that are NOT on the unit (learned the hard way)

- **RCC:** no `awk`, no `tail`, no `sync`, no `sha1sum`. Our first installer used `awk` and aborted safely.
  Use shell built-ins: `${var%%[ \t]*}`, `set -- $line` to split words, `$((...))` for maths.
  Bring `sha1sum` yourself (M.I.B. ships one for QNX/ARM: `apps/sbin/sha1sum`).
- **MMX:** no `printf`, `tr`, `cmp`, `diff`.
- Both have `ksh`, `sh`, `ls`, `cp`, `mv`, `mount`, `df`, `grep`, `sleep`, `expr`, `on`.
- File size without awk: `set -- $(ls -ln file); echo $5` (same trick M.I.B. uses).

## Getting `content.kzb` from your own car

M.I.B. → Backup → advanced → *app.img to folder structure*. Afterwards it is in the backup on the SD card under
`.../<hardware>-appimg/eso/content.kzb`. Note its SHA1: every tool here wants the untouched original.
Our original (K3663, A6/A7 C7.5) was `cb7ecbbba8ebb6d79c5eeb7af6edcabde9db155a`, 53 066 629 bytes.
If yours differs, the offsets in the scripts may differ too: the scripts look things up by path and
assert what they expect, so they stop instead of writing garbage.
