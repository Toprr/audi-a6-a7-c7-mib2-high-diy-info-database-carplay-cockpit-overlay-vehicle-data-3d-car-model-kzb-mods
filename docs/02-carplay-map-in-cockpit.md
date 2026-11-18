# 02: CarPlay / Android Auto map in the virtual cockpit

**This is not our project.** All credit goes to
**[MacsInSpace/mib-cluster-map](https://github.com/MacsInSpace/mib-cluster-map)** (GPL-3.0) and the projects it builds on.
Go there for downloads, docs and support. We only report what we saw on our car.

## Our result (Audi A6 Avant C7.5, MHI2_ER_AU57x_K3663_1)

- **v1.2.6 works with default settings.** (Their notes say v1.2.0 and v1.2.2 had problems: use a current release.)
- Install: empty FAT32 card, release contents in the root, card in **SD1** (SD2 empty), iPhone by **cable**,
  red menu → Update → SD → "MIB Cluster Map". The installer writes `ClusterMap/update_result.txt`
  (`CLUSTERMAP_INSTALL_OK`) on the card.
- In the log we saw: cockpit window **800×298**, display-manager **context 73**, plus the `NavActiveIgnore`
  and wheel-zoom jars added.
- Apple Maps and Google Maps show in the cockpit's **navigation tab**.
- The green menu gets a "ClusterMap" screen (save logs, settings).

## Things we learned around it

- **Only one CarPlay session at a time.** If you run CarPlay on an aftermarket tablet / Android head unit,
  the MMI can't run CarPlay at the same time, so the cockpit map is gone. Plugging the iPhone into the car's
  USB to charge will start MMI CarPlay (and take it away from the tablet).
- **The map style comes from the phone app.** The cockpit stream is drawn by the iPhone. You can't get the
  satellite view there even if the centre screen shows it: it's a separate, simplified "instrument cluster" view.
- **Wireless adapters** only relay one session; they can't "split" CarPlay to two screens
  (the session is encrypted/authenticated between phone and head unit: Apple MFi/FairPlay).
- **Combining with our 3D-car package:** we appended our CarLook component to mib-cluster-map's
  `metainfo2.txt` and ran both installers from one FinalScript, see `tools/swdl/build_package.py --cluster-map`
  and [docs/06](06-swdl-installer.md). Their files stay unchanged.
- Uninstall: their package has an `UNINSTALL.txt` you move one folder up, same idea as ours.

Related projects (not tested by us): [omonob/VW-MHI2-Carplay-Maps](https://github.com/omonob/VW-MHI2-Carplay-Maps),
[Lanye-z/MHI2Q-CarPlay-MMI-Mirror](https://github.com/Lanye-z/MHI2Q-CarPlay-MMI-Mirror),
[maxafanasiev/mib2q-carplay-wireless-cockpit](https://github.com/maxafanasiev/mib2q-carplay-wireless-cockpit).
