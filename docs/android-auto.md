# Running on Android Auto

Tested on a Xiaomi 11 Lite 5G NE (Android 16, rooted), Android Auto 18.0, DHU 2.0.

## Sideloaded builds

Android Auto runs sideloaded Car App Library apps when Developer settings > Unknown sources is on. A plain `./gradlew installDebug` is enough. Faking Play as the installer (`pm install -i com.android.vending`) isn't needed.

Android Auto checks each app with `CAR.VALIDATOR` on the phone. To see its verdicts:

```sh
adb logcat -s CAR.VALIDATOR
```

- With a car or DHU connected, the app passes and `GH.AppIconFactory` logs its icon.
- With no car connected, it logs `Package DENIED; failed all other checks [com.filipnone.dashglanceclock]` next to `isPackageInstalledByPlayCheck service not connected`. That's why the app is missing from "Customize launcher" on the phone. Ignore it; the check passes once a car connects.

## First connection

Android Auto won't project until it has notification access. Until then the DHU only shows a screen asking for it. Tap "Go to phone" and allow it, or over adb:

```sh
adb shell cmd notification allow_listener 'com.google.android.projection.gearhead/com.google.android.gearhead.notifications.SharedNotificationListenerManager$ListenerService'
```

## DHU problems

| Symptom | Cause | Fix |
|---|---|---|
| `libc++.so.1 => not found` | missing C++ runtime | `sudo pacman -S libc++`, or unpack the package and set `LD_LIBRARY_PATH` |
| exits right after `Attached!` with code 0 | stdin closed; the DHU console quits on EOF | run it in a terminal, or pipe input in (`tail -f cmds.txt \| scripts/dhu.sh --usb`) |
| `Out of sync with phone. Exiting...` | phone still in accessory mode from an earlier run | `scripts/dhu.sh --usb` resets the phone's USB first; otherwise replug the cable |
| `day` command does nothing | probably Android Auto's day/night setting, which was set to Night on the test phone | try setting it to Automatic in Android Auto's settings (not tested) |

With stdin piped in, these DHU console commands help with testing: `tap X Y`, `screenshot FILE`, `day`, `night`. Coordinates match the 800 x 480 screenshot.

## Clock next to the map

Tested on Android Auto 18.0 with Waze and Metrolist. The only slot beside another app's map is the media card, so `media/ClockMediaService` is a Media3 media app that plays nothing. Its card shows the time as artwork, the date as title and the phone's current song as subtitle. Play/pause and skip go to whichever music app is active, through notification access.

- One tile per app: with both a media service and a `CarAppService`, the Android Auto launcher and dock open the media screen. The full-screen clock can't be reached while the media service exists.
- Android Auto draws the card. Apps only supply artwork, title, subtitle and buttons. Card artwork is dimmed to about 38% (white digits measure 97/255); the full media screen shows it undimmed.
- With two sessions playing, `GH.MediaNowPlayingApp` logs "Multiple playing browser services" and picks the one whose playback state changed last. DashGlance updates right after the music app, so it usually wins, but the music app's card can appear in between.
- `SuggestionManager` (navigation apps, Car API 5) works: a suggestion with the time as title shows as a card in the dashboard's right column. Android Auto only shows it while DashGlance is the active navigation app, though. Once Waze takes over, Android Auto keeps DashGlance bound and logs `SuggestionHost: Received Suggestions from 3p App` every minute, but hides the card.
- Suggestion icons from an adaptive-icon mipmap render as a plain white circle. Suggestions were dropped in favor of the media card.
- Android Auto 18.0 already contains dashboard widgets (`car_widgets`, `EarthPickerActivity`), but Google hasn't switched them on. Google's guide says widgets for cars are built with Jetpack Glance, so a Glance widget is the route once they ship.

## The action strip

`NavigationTemplate` hides the action strip after a few idle seconds. The first touch only brings it back, so pressing the face button takes two taps when the strip is hidden.
