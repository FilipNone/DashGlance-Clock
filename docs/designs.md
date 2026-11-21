# Clock designs

Designs are picked on the phone only. The car keeps its single analog/digital button, and draws whichever design the phone saved for that face. Each face remembers its own design (`analog_design`, `digital_design` in the `clock` prefs file).

In portrait the designs sit in a bottom sheet under the clock. Collapsed, it shows the first group; pulling it up or tapping its header shows the rest, and each group is a row you swipe sideways. In short windows such as split screen, the collapsed sheet shows only its header. A second after the app opens, and a second after a face switch, the collapsed sheet rises 48dp to peek at the next group while its handle bends from a line into a ^. It holds there for 50ms, then drops like a ball and bounces twice, to 16% and 4% of the lift; the ^ flattens as it lands. The whole hop takes about 1.25s. Dragging or tapping the sheet stops the hop. In landscape (`layout-land`) the screen splits in half: the clock and credit on the left, the full design list on the right, opened at the selected design.

## Catalog

| Group | Analog | Digital |
|---|---|---|
| Original | DashGlance | DashGlance |
| Phone brands | Material You, Apple iOS, Samsung One UI, Xiaomi HyperOS, Nothing OS, OnePlus OxygenOS, Huawei HarmonyOS | same seven |
| Vintage cars | Mercedes 1970s, Porsche 1970s, Jaguar 1960s, Ferrari 1960s, Chevrolet 1950s, Rolls-Royce 1930s, Initial D SB709 | Lagonda 1976 (red LED), Cadillac 1980s (amber radio display), Toyota 1980s (green VFD), Volkswagen 1990s (LCD) |
| Modern cars | none; modern cars are digital | Tesla, Mercedes MBUX, BMW iDrive, Audi Virtual Cockpit, Porsche Taycan, Polestar, Hyundai Ioniq, Rivian |
| Retro and sci-fi | none | Nixie tube, Cyberpunk 2077, Tron Legacy, Camcorder, Flip clock |

Material You takes its colors from the wallpaper on Android 12 and newer, and falls back to Material's baseline purple on older phones. Designs with separate day and night palettes follow the car's day/night mode. Vintage and most modern car designs keep one palette, as the originals did.

## Vintage references

Each vintage design was drawn to match photos of the real clock:

| Design | Matched against |
|---|---|
| Mercedes 1970s | W123 instrument cluster clock (1979 300D, 1983 240D) |
| Porsche 1970s | 911 clock, five-gauge cluster (1973 911E, quartz version) |
| Jaguar 1960s | E-Type Series 1 clock inside the rev counter (1962, 1963) |
| Ferrari 1960s | 330 GTC and 275 GTB centre-dash clock (1966) |
| Chevrolet 1950s | 1957 Bel Air rectangular dash clock |
| Rolls-Royce 1930s | 1934 Phantom II instrument panel clock |
| Initial D SB709 | the cream quartz alarm clock (model SB709 / 4RA709LX) seen in Initial D |
| Lagonda 1976 | Lagonda Series 2 LED readouts (1981); the clock window itself was never seen lit |
| Cadillac 1980s | 1986 Fleetwood Brougham radio clock |
| Toyota 1980s | 1978–80 Cressida clock and 1985 Corolla AE86 dash clock |
| Volkswagen 1990s | Passat B5 and Golf IV clock in the rev counter; the red display next to it is the trip computer |

Maker names and monograms printed on the originals are left off. Only generic words such as "Quarz-Zeit", "ELECTRIC" and "QUARTZ ALARM" stay.

The retro and sci-fi designs are not copies of a particular screen:

- Nixie tube draws each digit as a glowing numeral in its own tube, with the other nine as faint wires behind it.
- Cyberpunk 2077 uses angular digits of our own (`Glyphs.Blade`) in yellow with a cyan edge. The date sits under the right end of the time with circuit traces between the letters, like the "2077" under the game's logo. Most seconds have a short glitch burst.
- Tron Legacy is thin glowing cyan over a grid floor that moves toward you, with a light pulse running along the horizon.
- Camcorder is the date and time stamp that 80s and 90s camcorders printed into the video: "PM 10:08" over "NOV. 20 2026" in a pixel font, with VHS color bleed and scanlines.
- Flip clock puts the hours and minutes on split cards. When the minute changes, each card that changes turns over: the top flap falls forward and the bottom flap lands over the old number, in half a second.

Cyberpunk 2077 and Tron Legacy are animated all the time. While one of them is showing, the phone and the car redraw the clock 25 times a second instead of once. The flip clock only redraws that fast for the half second its cards turn.

## Adding a design

1. Add an `AnalogDesign` or `DigitalDesign` to the list in `PhoneDesigns`, `VintageCarDesigns` or `ModernCarDesigns`. Every field has a default, so a design only lists what differs from the DashGlance face.
2. Give it a new, stable `id`. Saved choices are stored by id; an unknown id falls back to DashGlance.
3. Run `./gradlew testDebugUnitTest`. `ClockRendererTest` draws every design in several sizes, in 12- and 24-hour time, and fails if anything lands outside the bounds or two designs come out identical.

If a design needs something the model can't express, add a field to `ClockDesign.kt` with a default that keeps the existing designs unchanged, and draw it in `AnalogFace` or `DigitalFace`.

## Brand names

The designs are our own drawings in the style of each brand. They use no logos, fonts or artwork from the brands, and the labels only say what inspired them. Before a Play Store release, consider renaming them to generic names (for example "Bavarian 1970s"), since Play's impersonation policy also covers brand names. The README has a disclaimer line for the trademarks.
