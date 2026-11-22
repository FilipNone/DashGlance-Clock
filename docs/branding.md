# Branding

## Name and text

| Item | Value | Source of truth |
|---|---|---|
| Name of the project and the full-screen app | DashGlance Clock | `app_name` in `fullscreen/res/values/strings.xml` |
| Name of the card app | DashGlance Card | `app_name` in `card/res/values/strings.xml` |
| Description | Clock for Android Auto: open-source clock with easy-to-read faces and presets. | GitHub repo description |
| Subtitle | Open-source clock for Android Auto with easy-to-read faces and presets | README.md |
| Creator | FilipNone | `creator_name` |
| Creator link | https://github.com/FilipNone | `creator_url` |
| applicationId, card app | `com.filipnone.dashglanceclock` | `productFlavors` in `app/build.gradle.kts` |
| applicationId, full-screen app | `com.filipnone.dashglanceclock.fullscreen` | same |

Write the name as two words, with a capital G in DashGlance. Don't translate it. Code identifiers use `DashGlanceClock` (theme, Gradle project) or `dashglanceclock` (package).

The card app keeps the old single app's ID, so 0.2 installs update to it. Release APKs are named `DashGlance-Card-<version>.apk` and `DashGlance-Clock-<version>.apk`.

The car launcher takes each app's name and icon from `android:label` and `android:icon` on its Android Auto service (`ClockCarAppService` or `ClockMediaService`). The phone launcher reads the same attributes on `<application>`. Both point to the same resources.

## Colors

| Name | Hex | Use |
|---|---|---|
| Orange | `#FF8A3D` | icon background and triangle, clock accent, credit link |
| Ink | `#17171C` | icon dial |
| Paper | `#F5F5F5` | icon ticks and arrow |

Use only these three. The icon colors live in `res/values/colors.xml` as `brand_orange`, `brand_ink` and `brand_paper`. The DashGlance design's colors are in `ClockPalette.kt`, and its dark accent matches the icon orange. The other clock designs bring their own colors (see designs.md); this rule covers the icon, the app chrome and the DashGlance design.

## Icon

Both icons are a dark clock dial with 12 ticks and a mark on it:

- DashGlance Clock: our own drawing of the Android Auto mark, an orange rounded triangle with a paper arrow, its tip pointing at 12. It isn't copied from Google's files.
- DashGlance Card: music notes in the same two colors as the Android Auto mark. Two orange eighth notes joined by a slanted beam, and a paper eighth note on top of the right one, shifted right and down, with a thin gap in the dial color between them. The heads meet their stems with rounded inner corners, like the Apple Music note.

The dial and ticks are in `main/res/values/icon_paths.xml`, each mark in its flavor's `res/values/icon_paths.xml`.

Google's brand rules only allow the real Android Auto icon inside UI screenshots, and Play's impersonation policy covers icons that suggest a link to another app. Expect questions about the DashGlance Clock icon if it goes to Play review.

- Files: `mipmap-anydpi/ic_launcher.xml` in `main`, and `drawable/ic_launcher_foreground.xml` and `drawable/ic_launcher_monochrome.xml` in each flavor.
- Canvas: 108 x 108 units. Launchers can crop to a 66-unit circle centered on (54, 54), so keep everything inside it.
- Symmetry: the dial, ticks and Android Auto mark mirror around x = 54. When you add a shape on the left at x, put its twin at 108 - x. The notes aren't symmetric; as a group they sit inside the 66-unit circle, at most 25 units from the center.
- Rounding: use round caps and joins on strokes. The triangle and arrow get their round corners from a stroke in their own fill color, 4.6 units wide on the triangle and 2.3 on the arrow, which also makes each shape that much larger than its path. The notes have no strokes left: each is one merged outline (see below).
- Monochrome: for themed icons the launcher keeps only the alpha channel and tints it, so the dial is left out. The triangle becomes an outline around the solid arrow; the notes stay solid, and the gap keeps them apart.
- Changing the notes: draw them in Inkscape with strokes and fillets as separate shapes, then for each note run Path > Stroke to Path and Path > Union. For the gap, grow a copy of the paper note by 1.3 units on every side (a 2.6-unit stroke, then Union) and subtract it from the orange note (Path > Difference). Paste each result's `d` into `card/res/values/icon_paths.xml`.
- Play Store: `ic_launcher-playstore.png` in each flavor's folder (`app/src/card`, `app/src/fullscreen`), 512 x 512, rendered from the same path data with `rsvg-convert`. Render it again after changing an icon.
- README: `docs/images/logo.png` is the DashGlance Clock Play Store image with rounded corners cut in, because GitHub strips CSS from READMEs. The showcase images next to it are 1280 x 720 DHU screenshots with rounded corners; the two face shots are scaled to 960 x 540. Take map shots with a mock location (see android-auto.md). `designs.png` isn't a screenshot: it's a 1280-wide sheet drawn with `ClockRenderer` from a throwaway Robolectric test, every design except the DashGlance faces at 10:08:36 in night colors, on the Ink background with Orange group titles.
