file for llm
in /docs documentation for the llm
keep everything here short

## Files

Two apps from one module, as product flavors of `app`: `card` (DashGlance Card, applicationId `com.filipnone.dashglanceclock`, the old single app's ID) and `fullscreen` (DashGlance Clock, `com.filipnone.dashglanceclock.fullscreen`). Separate packages because Android Auto drops a package's media service when the same package has a Car App Library service (docs/android-auto.md).

```
app/src/main/                shared by both apps
  AndroidManifest.xml        application, MainActivity; flavors add their services
  kotlin/.../clock/
    ClockRenderer.kt         draws a design, shared by car and phone
    AnalogFace.kt            analog drawing, driven by AnalogDesign data
    DigitalFace.kt           digital layout, driven by DigitalDesign data
    GlyphPainters.kt         digits as font, 7-segment or dot matrix
    ClockDesign.kt           design model: dial, marks, hands, digits, date, colors
    ClockDesigns.kt          catalog; entries in PhoneDesigns, VintageCarDesigns, ModernCarDesigns, RetroSciFiDesigns
    ClockPalette.kt          colors and Ink roles; MaterialYouPalette.kt reads wallpaper colors
    ClockPreferences.kt      saved face, one design per face, stacked; observable. File "clock" for Clock, "card" for Card
    DateFormats.kt           date styles in the phone's language
    ClockMath.kt             hand angles and tick timing
    SecondTicker.kt          main-thread callback each second, or 25 fps while a design moves (ClockDesign.motion)
  kotlin/.../ui/
    Credit.kt                GitHub credit link at the bottom of both phone screens
    ClockView.kt             phone view that wraps ClockRenderer
    DesignPicker.kt          live design previews for a given list, one sideways row per group
    DesignSheet.kt           bottom sheet around the picker; pull up for all groups, clock shrinks; hops 1s after opening or a face switch
    SheetHandle.kt           sheet's drag handle; the line bends into a ^ during the hop
  res/values/icon_paths.xml  icon dial and ticks; each flavor adds its mark
app/src/fullscreen/          DashGlance Clock
  kotlin/.../car/
    ClockCarAppService.kt    entry point Android Auto binds to
    ClockSession.kt          one car connection; wires screen and renderer together
    ClockScreen.kt           NavigationTemplate with the analog/digital button; no design picker
    ClockSurfaceRenderer.kt  draws onto the car surface once a second
  kotlin/.../MainActivity.kt phone screen: clock, design sheet, credit
  res/layout-land/activity_main.xml   landscape: clock left, designs right, no sheet
  res/drawable/ic_launcher_*.xml      icon with the Android Auto mark
app/src/card/                DashGlance Card
  kotlin/.../media/
    ClockMediaService.kt     media app whose card shows the clock next to the map
    ClockMediaPlayer.kt      silent Media3 player: chosen design as artwork, controls go to PhoneMusic
    CardDesigns.kt           designs the card can show (no animation, readable once dimmed), their card form, Lagonda redrawn for the card
    CardArtwork.kt           draws the clock where the card leaves it visible; player and phone preview share it
    PhoneMusic.kt            finds the phone's active music app and controls it
    MediaControlListener.kt  notification listener; needed to read other apps' sessions
    MusicControls.kt         in-app on/off on top of notification access; off unbinds the listener
    SessionListNudge.kt      brief spare session after playback starts, so Android Auto re-reads the session order and gives the card to the clock
  kotlin/.../MainActivity.kt phone screen: card preview, hint, music controls switch, design sheet with Stacked switch, credit
  kotlin/.../ui/CardPreview.kt   the card as a 1280 x 720 screen shows it; tap switches face
  res/layout/design_title_row.xml     design list title with the Stacked switch, in both layouts
  res/layout-land/activity_main.xml   landscape: preview left, designs right, no sheet
  res/drawable/switch_*.xml  pill switch; ease-out animated-selector track, style in themes.xml
  res/drawable/ic_launcher_*.xml      icon with music notes
app/src/test, testCard, testFullscreen   JVM tests for shared code and each app
scripts/dhu.sh               starts the Desktop Head Unit
docs/branding.md             names, description, creator, colors, icon rules
docs/android-auto.md         sideloading, CAR.VALIDATOR, DHU vs real car, media card rules, DHU troubleshooting
docs/designs.md              design catalog, adding a design, brand names
docs/images/                 README logo and showcase renders
```

## Toolchain

| Component | Version |
|---|---|
| Gradle (wrapper) | 9.9.1 |
| Android Gradle Plugin | 9.5.1, with built-in Kotlin |
| Kotlin | 2.4.21 |
| Car App Library | 1.8.0-rc01 (includes a security fix missing from 1.7.0) |
| Material Components | 1.14.0, only for BottomSheetBehavior; the theme stays framework Material |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| JDK | 17 or newer (the system JDK 21 works) |

The Android SDK is in `~/Android/Sdk`. Platform 37, build tools 36 and the Desktop Head Unit (`extras/google/auto`) were installed with `android sdk install`.

## Build and test

```sh
./gradlew assembleDebug                           # app/build/outputs/apk/{card,fullscreen}/debug/
./gradlew installCardDebug installFullscreenDebug # install both on the connected phone
./gradlew test                                    # JVM tests for both apps, Robolectric included
./gradlew lintCardDebug lintFullscreenDebug
```