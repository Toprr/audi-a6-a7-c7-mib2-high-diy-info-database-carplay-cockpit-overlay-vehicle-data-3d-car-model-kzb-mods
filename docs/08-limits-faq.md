# 08: Limits and FAQ

> **Note:** the answers below are our current understanding, not proven facts. "No" means: we found no way and it
> is *probably* not possible, not that it is impossible. If you know better, please open an issue.

**Can the cockpit show the satellite map?**
No. The cockpit stream comes from the phone (CarPlay "instrument cluster" view); the iPhone app decides how it looks.
The satellite setting of the main screen is not used there.

**Can the MMI CarPlay be sharper?**
No. MIB2 High drives the centre screen over LVDS at a fixed **800×480**. The phone renders for the resolution the
head unit announces; you can't make it send more, and the screen link couldn't carry it anyway. A "higher
resolution" setting on an aftermarket screen only scales.

**Touch on the MMI screen?**
MIB2 High in the C7 has no touch screen / touch controller. Aftermarket Android screens have touch, but for
their own Android system. For native CarPlay there is a good alternative: MIB2-Patcher's TOUCHCURSOR turns the
MMI **touchpad** into a cursor in CarPlay, and a tap clicks where it is (tested by us, see
[docs/10](10-star-button-and-touchpad-in-carplay.md)).

**CarPlay on an aftermarket tablet AND the MMI (for the cockpit map) at the same time?**
No. The iPhone runs **one** CarPlay session. Wireless adapters relay that one session; a "splitter" would have to
fake an Apple-authenticated head unit (MFi / FairPlay), as far as we know not possible. So: tablet CarPlay *or* cockpit map.
A common setup is CarPlay on the tablet + Bluetooth audio to the car radio.

**Aftermarket Android screens with an LVDS box (C7 MHI2)**
They sit between the MMI and its screen. Set the "original car resolution" to **800×480 / MHI2**, use OEM mode
while running MMI updates, and expect the factory camera to be shown by switching back to the OEM picture.

What we saw with one such screen (Android 15, ZLINK5 CarPlay, LVDS box, CAN connection):
- **Sound:** the box has an internal AUX path through its harness. Select AUX in the MMI by hand ("car aux switch
  mode" AUTO switched the MMI to Media instead) and connect the iPhone **only to the box**, so the phone has a
  single audio route. Turn off "stop the media when entering the OEM system" so music keeps playing on the OEM picture.
- **Steering-wheel buttons:** any MMI steering-wheel button (star, arrows) makes the box switch to the OEM picture.
  Only the skip button in the centre console skips in CarPlay without switching. No setting changed that
  (all "part control" types, CAN mode, key options tried); the seller said it is designed this way. It is the
  box reacting to the button on the car network, not the MMI opening a menu.
- **Our way out:** iPhone by cable on the **car's USB** (native MMI CarPlay). Then the cockpit map works,
  and with MIB2-Patcher the star button skips and the touchpad works as a cursor
  ([docs/10](10-star-button-and-touchpad-in-carplay.md)). The box simply stays on the OEM picture.

**Rear camera in the cockpit?**
Not with scripts: see [docs/07](07-display-manager-and-rear-camera.md).

**The steering-wheel star (joker) button doesn't skip tracks in CarPlay?**
- *Native CarPlay on the MMI:* the fix is **not our work**: it comes from
  [tfourj/MIB2-Patcher](https://github.com/tfourj/MIB2-Patcher). According to their findings, CarPlay only
  registers for the next/previous buttons, not for the joker button; their patch JOKERBTN (for `K3663`) makes
  the star button skip forward in CarPlay. **Tested by us on K3663 together with mib-cluster-map: it works.**
  Install, compatibility check and rollback: [docs/10](10-star-button-and-touchpad-in-carplay.md).
- *CarPlay on an aftermarket tablet with sound via AUX:* the MMI has no track control over AUX, so the star button
  has nothing to skip there. That is a limit of that setup; the patch doesn't help.

**Anything outside the cockpit's navigation tab?**
No. The instrument cluster only shows MMI video in its navigation tab. This applies to every solution, free or paid.

**Do I need to pay someone for CarPlay in the cockpit / a custom car model?**
No. CarPlay map: [mib-cluster-map](https://github.com/MacsInSpace/mib-cluster-map) (free, open source).
Car model + icon: this repo. It takes time and care, but no money.
