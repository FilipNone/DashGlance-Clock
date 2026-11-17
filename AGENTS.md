file for llm
in /docs documentation for the llm
keep everything here short

## Files

```
app/src/main/kotlin/com/filipnone/dashglanceclock/
  car/
    ClockCarAppService.kt    entry point Android Auto binds to
    ClockSession.kt          one car connection; wires screen and renderer together
    ClockScreen.kt           NavigationTemplate with the "switch face" button
    ClockSurfaceRenderer.kt  draws onto the car surface once a second
  clock/
    ClockRenderer.kt         Canvas drawing for both faces, shared by car and phone
    ClockMath.kt             hand angles and tick timing
    ClockPalette.kt          day and night colors
    ClockPreferences.kt      saved face, observable from car and phone
    SecondTicker.kt          main-thread callback aligned to the wall clock second
  media/
    ClockMediaService.kt     media app whose card shows the clock next to the map
    ClockMediaPlayer.kt      silent Media3 player: clock artwork, controls go to PhoneMusic
    PhoneMusic.kt            finds the phone's active music app and controls it
    MediaControlListener.kt  notification listener; needed to read other apps' sessions
  ui/ClockView.kt            phone view that wraps ClockRenderer
  MainActivity.kt            phone screen, with the creator credit
app/src/main/res/drawable/ic_launcher_*.xml   app icon; shared paths in res/values/icon_paths.xml
scripts/dhu.sh               starts the Desktop Head Unit
docs/branding.md             name, description, creator, colors, icon rules
docs/android-auto.md         sideloading, CAR.VALIDATOR, DHU troubleshooting
docs/images/                 README logo and showcase renders
```

## Toolchain

| Component | Version |
|---|---|
| Gradle (wrapper) | 9.9.1 |
| Android Gradle Plugin | 9.5.1, with built-in Kotlin |
| Kotlin | 2.4.21 |
| Car App Library | 1.8.0-rc01 (includes a security fix missing from 1.7.0) |
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