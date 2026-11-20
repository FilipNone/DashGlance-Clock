file for llm
in /docs documentation for the llm
keep everything here short

## Files

```
app/src/main/kotlin/com/filipnone/dashglanceclock/
  car/
    ClockCarAppService.kt    entry point Android Auto binds to
    ClockSession.kt          one car connection; wires screen and renderer together
    ClockScreen.kt           NavigationTemplate with the analog/digital button; no design picker
    ClockSurfaceRenderer.kt  draws onto the car surface once a second
  clock/
    ClockRenderer.kt         draws a design, shared by car and phone
    AnalogFace.kt            analog drawing, driven by AnalogDesign data
    DigitalFace.kt           digital layout, driven by DigitalDesign data
    GlyphPainters.kt         digits as font, 7-segment or dot matrix
    ClockDesign.kt           design model: dial, marks, hands, digits, date, colors
    ClockDesigns.kt          catalog; entries in PhoneDesigns, VintageCarDesigns, ModernCarDesigns, RetroSciFiDesigns
    ClockPalette.kt          colors and Ink roles; MaterialYouPalette.kt reads wallpaper colors
    ClockPreferences.kt      saved face and one design per face, observable from car and phone
    DateFormats.kt           date styles in the phone's language
    ClockMath.kt             hand angles and tick timing
    SecondTicker.kt          main-thread callback each second, or 25 fps while a design moves (ClockDesign.motion)
  media/
    ClockMediaService.kt     media app whose card shows the clock next to the map
    ClockMediaPlayer.kt      silent Media3 player: clock artwork, controls go to PhoneMusic
    PhoneMusic.kt            finds the phone's active music app and controls it
    MediaControlListener.kt  notification listener; needed to read other apps' sessions
    MusicControls.kt         in-app on/off on top of notification access; off unbinds the listener
  ui/ClockView.kt            phone view that wraps ClockRenderer
  ui/DesignPicker.kt         live design previews, one sideways row per group
  ui/DesignSheet.kt          bottom sheet around the picker; pull up for all groups, clock shrinks
app/src/main/res/drawable/switch_*.xml   pill music-controls switch; ease-out animated-selector track, style in themes.xml
  MainActivity.kt            phone screen: clock, design sheet, GitHub credit footer
app/src/main/res/layout-land/activity_main.xml   landscape: clock left, designs right, no sheet
app/src/main/res/drawable/ic_launcher_*.xml   app icon; shared paths in res/values/icon_paths.xml
scripts/dhu.sh               starts the Desktop Head Unit
docs/branding.md             name, description, creator, colors, icon rules
docs/android-auto.md         sideloading, CAR.VALIDATOR, DHU troubleshooting
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
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on the connected phone
./gradlew testDebugUnitTest    # JVM tests, Robolectric included
./gradlew lintDebug
```