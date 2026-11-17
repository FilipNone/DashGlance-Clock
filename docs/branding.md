# Branding

## Name and text

| Item | Value | Source of truth |
|---|---|---|
| Name | DashGlance Clock | `app_name` in `res/values/strings.xml` |
| Description | Clock for Android Auto: open-source clock with easy-to-read faces and presets. | GitHub repo description |
| Subtitle | Open-source clock for Android Auto with easy-to-read faces and presets | README.md |
| Creator | FilipNone | `creator_name` |
| Creator link | https://github.com/FilipNone | `creator_url` |
| Package / applicationId | `com.filipnone.dashglanceclock` | `app/build.gradle.kts` |

Write the name as two words, with a capital G in DashGlance. Don't translate it. Code identifiers use `DashGlanceClock` (theme, Gradle project) or `dashglanceclock` (package).

The car launcher takes its name and icon from `android:label` and `android:icon` on `ClockCarAppService` in the manifest. The phone launcher reads the same attributes on `<application>`. Both point to the same resources.

## Colors

| Name | Hex | Use |
|---|---|---|
| Orange | `#FF8A3D` | icon background and triangle, clock accent, credit link |
| Ink | `#17171C` | icon dial |
| Paper | `#F5F5F5` | icon ticks and arrow |

Use only these three. The icon colors live in `res/values/colors.xml` as `brand_orange`, `brand_ink` and `brand_paper`. The clock's own colors are in `ClockPalette.kt`, and its dark accent matches the icon orange.

## Icon

The icon is a dark clock dial with 12 ticks and our own drawing of the Android Auto mark on it: an orange rounded triangle with a paper arrow, its tip pointing at 12. The mark isn't copied from Google's files. Its geometry is in `icon_paths.xml`.

Google's brand rules only allow the real Android Auto icon inside UI screenshots, and Play's impersonation policy covers icons that suggest a link to another app. Expect questions about this icon if the app goes to Play review.

- Files: `drawable/ic_launcher_foreground.xml`, `drawable/ic_launcher_monochrome.xml`, `mipmap-anydpi/ic_launcher.xml`. The shared path data is in `values/icon_paths.xml`.
- Canvas: 108 x 108 units. Launchers can crop to a 66-unit circle centered on (54, 54), so keep everything inside it.
- Symmetry: every shape mirrors around x = 54. When you add a shape on the left at x, put its twin at 108 - x.
- Rounding: use round caps and joins on strokes. The triangle and arrow get their round corners from a stroke in their own fill color, 4.6 units wide on the triangle and 2.3 on the arrow, which also makes each shape that much larger than its path.
- Monochrome: for themed icons the launcher keeps only the alpha channel and tints it, so the dial is left out and the triangle becomes an outline around the solid arrow.
- Play Store: `app/src/main/ic_launcher-playstore.png`, 512 x 512, exported from the same vectors. Export it again after changing the icon.
- README: `docs/images/logo.png` is the Play Store image with rounded corners cut in, because GitHub strips CSS from READMEs. The showcase images next to it are 1280 x 720 renders from `ClockRenderer`, scaled to 960 x 540 with rounded corners.
