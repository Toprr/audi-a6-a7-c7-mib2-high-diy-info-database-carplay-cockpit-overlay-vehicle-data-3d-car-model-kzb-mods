# archive: earlier design iterations

These are the design steps before the final design in [`../design.py`](../design.py), kept as examples.
They are **not maintained** and not needed for anything in the car.

| file | what |
|---|---|
| `composite2.py`, `composite4.py` | first designs on the S6 Avant body (mesh 59): grille, calipers, rotor rims, simple software renderer |
| `rotor.py` | a custom 5-arm "rotor" rim modelled from a photo |
| `parts.py` | grille honeycomb (round and RS style), text badges, ACC sensor disc |
| `parts2.py` | analytic add-on parts: front lip, side skirt, roof spoiler, diffuser, rear lettering |
| `parts3.py` | add-on parts that follow sliced body contours, roof-spoiler extension |
| `parts4.py` | continuous front lip / skirts / splitters (now in `../bodykit.py`) |
| `parts5.py` | diffuser with 4 fins modelled from a photo |
| `render2.py`, `slicer.py` | OBJ loader and mesh slicer used by the files above (now in `../obj_preview.py` / `../bodykit.py`) |

The files import each other by their old names, so run them from this folder
(e.g. `python composite4.py out.jpg`). The numbers in them are for the K3663 meshes.
