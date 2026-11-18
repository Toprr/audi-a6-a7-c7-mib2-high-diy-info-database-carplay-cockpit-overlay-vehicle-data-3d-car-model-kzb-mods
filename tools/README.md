# tools

Python 3 on the PC (`pip install numpy opencv-python moderngl`), ksh on the unit.
Set `MIB_WORK` to a folder that contains **your own** `content.kzb` (from an M.I.B. backup): intermediate files go there too.

| folder | what | docs |
|---|---|---|
| `kzb/` | read the Kanzi `.kzb`: directory, meshes → OBJ, mesh parse/rebuild, PNG dump | [03](../docs/03-kzb-format.md) |
| `carlook/` | the in-place edits we installed: model swap + paint, menu icon, lowering/rims/calipers/black parts | [04](../docs/04-3d-car-model.md), [05](../docs/05-car-menu-icon.md) |
| `preview/` | PC renderer + design builder (try designs before editing the file, render the finished file). Start with `design.py`; `archive/` holds old iterations | [04](../docs/04-3d-car-model.md) |
| `swdl/` | SD-update package builder + the safe installer that runs on the unit | [06](../docs/06-swdl-installer.md) |
| `camera/` | rear-camera research scripts (read-only scan, RAM-only cockpit test) | [07](../docs/07-display-manager-and-rear-camera.md) |

Order we ran:

```
python kzb/kzb_dir.py   $MIB_WORK/content.kzb $MIB_WORK/kzb_dir.json
python kzb/kzb_extract.py $MIB_WORK/content.kzb $MIB_WORK/obj
python carlook/stage1_swap_model.py $MIB_WORK/content.kzb $MIB_WORK/content_stage1.kzb $MIB_WORK/kzb_dir.json
python carlook/make_menu_icon.py
python carlook/stage1b_menu_icon.py
python carlook/stage2_lower_rims_calipers_black.py
python preview/render_from_kzb.py $MIB_WORK/content_stage2.kzb $MIB_WORK/check.jpg
python swdl/build_package.py --orig $MIB_WORK/content.kzb --new $MIB_WORK/content_stage2.kzb --sha1sum <M.I.B.>/apps/sbin/sha1sum --train <your train> --out SD_CARLOOK
```

On the original K3663 file this reproduces our installed file byte for byte (SHA1 `674a2ef4c80dcee898dec2c6ff439c6d7f5a6b10`).
The design-specific numbers (which triangles are "rings", wheel positions, colours) are for the RS6 Avant model;
adapt them for other models and keep the `assert`s.
