# 03 – The Kanzi `.kzb` format (as used in `content.kzb` on MIB2 High)

No public tool reads these files, so we decoded what we needed by hand. Everything below was verified on
`content.kzb` from `MHI2_ER_AU57x_K3663_1` (Audi A6/A7 C7.5): all 135 meshes can be parsed and rebuilt
**byte-identically**, the directory parses with no gaps and no overlaps.

Tools: [`tools/kzb/kzb_dir.py`](../tools/kzb/kzb_dir.py) (directory → JSON),
[`kzb_extract.py`](../tools/kzb/kzb_extract.py) (all meshes → OBJ),
[`kzbmesh.py`](../tools/kzb/kzbmesh.py) (parse/rebuild one mesh),
[`kzbpng.py`](../tools/kzb/kzbpng.py) (dump PNGs).

## 1. Directory (big-endian!)

The directory starts at **0x3c**: `u32 folderCount`, then that many folders, recursively:

```
folder:  u32 size, u16 nameLen, name (no \0), pad to 4,
         u32 subFolderCount, [subfolders...],
         u32 fileCount, [files...]
file:    u32 offset, u32 size, u32 type, u32 flags, u16 nameLen, name, pad to 4
```

Our file: 1203 folders, 5633 files, data from 0x3ca80 to the end, no gaps.
Files are addressed by their **1-based index in directory order** – that number is what other objects
store as a reference (`kzb_dir.json` keeps the list in that order: index = position + 1).

File types we identified:

| type | meaning |
|---|---|
| 10 | image (embedded PNG) |
| 110 | scene node (group / transform) |
| 120 | mesh node (points to a mesh) |
| 170 | placeholder / instance (points to another node – this is how the menu scenes pull in a car) |
| 500 | mesh data |
| 1121 | material |
| 2020 | texture reference |
| 1000, 510, 130, 140, 5 | not needed for our work |

Top-level folders: `/Scenes`, `/Mesh Data`, `/Materials`, `/Resource Files/Images/...`, etc.

## 2. Object blobs (little-endian)

Every entry starts with `u32 n0` followed by `n0` bytes of header, then the payload. For node types the
**last u32 of the blob** (`offset + size + 4 + n0 - 4`) is the **reference** (1-based file index):
for a type-120 mesh node it's the mesh, for a type-170 placeholder it's the node it instantiates.

Properties inside nodes and materials are stored as

```
u32 length, u32 propertyId, u32 valueType, u32 0, values...
```

| propertyId | what | values |
|---|---|---|
| 0x0b (length 0x50, type 2) | node transform | 16 floats, row-major, translation in floats 12/13/14 (x/y/z) |
| 0x60 (length 0x20, type 2) | ambient colour | 3 floats RGB 0..1 |
| 0x63 (length 0x20, type 2) | **diffuse colour** (e.g. paint) | 3 floats RGB 0..1 |
| 0x67 | specular colour | 3 floats |
| 0x68 | specular exponent | float |

Search the blob for `struct.pack('<III', length, id, type)`; the values start 16 bytes after that.

## 3. Mesh (type 500)

```
u32 attributeCount
per attribute: u16 nameLen, name, 0-3 pad bytes, u32 semantic (0=position, 1=normal, 3=uv), u32 type (6=half float, 7=float), u32 dims
u32 vertexCount
vertex data (interleaved, stride = sum of attribute sizes)
u32 subMeshCount
per sub-mesh: u32 materialRef, u32 indexCount, u32 indexSize (2), indices, pad to 4
tail: 8 zero bytes, 6 floats bounding box (min xyz, max xyz), 4 zero bytes
```

The padding after attribute names is sometimes relative to the **absolute file offset**, so
`kzbmesh.parse_auto()` tries both and only accepts a byte-exact rebuild. Positions are half floats (float16):
precision is ~1 mm near the car – fine for moving parts around.

Each sub-mesh uses one material – **recolouring a part = moving its triangles to another sub-mesh**,
or changing the material reference. Removing a part without changing sizes = make its triangles
**degenerate** (all three indices the same) – they are not drawn.

## 4. Images (type 10)

Plain PNG files. To replace one without changing the file layout, the new PNG must be **exactly** as long as
the old one: encode smaller, then pad with a `tEXt` chunk (see [docs/05](05-car-menu-icon.md)).
1215 PNGs in our file.

## 5. Coordinates of the C7 car models

Metres, **Y up**, X along the car (front at x ≈ −3.5, rear at x ≈ +3.9), Z across (left side = +Z,
car centre line at z ≈ −0.335). Some meshes are stored in centimetres with Z up (e.g. the A6 Avant
"Polymodel") and scaled/rotated by their node matrix (scale 0.015 + axis swap).
RS6 wheel centres: front left (−2.165, 0.52, 0.905), front right (−2.165, 0.52, −1.584), rear axle x = 2.208.

## 6. What is in our `content.kzb` (A6/A7 C7.5)

135 meshes, 523 792 triangles. Complete car sets (body, glass, lights, wheels, interior, reflection) for:
A6 saloon (AU571), S6 saloon, A6 Avant (AU572), S6 Avant, **RS6 Avant**, allroad, A7, S7, **RS7**, plus an A6 hybrid.
Material names like `/Materials/PhongTexturedCube/AU572_RS6_PA_Paint`.

Scenes per model: `<model>_Geometry` (the actual car), `<model>_CarMenu_Leftsided/Rightsided`
(car menu, left/right-hand drive) and `<model>_CharismaView_Left/Rightsided` (Audi drive select).
The menu scenes do not contain geometry: their `CarRoot/Exterior_Left`, `Interior_Left`, `Reflection_Left` …
are **type-170 placeholders that point into `<model>_Geometry`** – this is the hook for swapping the model
([docs/04](04-3d-car-model.md)).

Interesting detail: the RS6 interior mesh is modelled as right-hand drive and contains its own steering wheel
(an older round RS wheel with a round airbag); the RS7 and S7 interiors have a flat-bottom three-spoke wheel, the A6 interiors a standard multi-function wheel - all on the left.

## 7. Rendering it on the PC

`tools/preview/render_from_kzb.py` rebuilds the car-menu scene **from the finished file** exactly the way the
MMI resolves it (placeholder → geometry node → children → meshes, with all node matrices) and renders 9 views
with `tools/preview/glrender.py` (moderngl, MSAA, smooth normals, simple paint reflection). Use it to check
every modified file before it goes into the car. Needs `pip install numpy opencv-python moderngl`.
