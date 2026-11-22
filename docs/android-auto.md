# Running on Android Auto

Tested on a Xiaomi 11 Lite 5G NE (Android 16, rooted), Android Auto 18.0, DHU 2.0.

Class names like `jlg` or `lyu` come from a jadx decompile of Android Auto 18.0 (`adb shell pm path com.google.android.projection.gearhead`, pull `base.apk`). They're obfuscated and change with every release; search for the log tags instead.

## Sideloaded builds

Android Auto checks each app with `CAR.VALIDATOR` on the phone (`jlg.v()` in 18.0). To see its verdicts:

```sh
adb logcat -s CAR.VALIDATOR
```

What a sideloaded APK gets:

| Service | DHU | Real car |
|---|---|---|
| `ClockMediaService` (media) | allowed with Unknown sources | allowed with Unknown sources |
| `ClockCarAppService` (Car App Library) | allowed, Unknown sources not needed | `Package DENIED; failed all other checks` |

- The DHU passes because the validator skips its checks for head units that report make "Google" and model "Desktop Head Unit" (`jll.a`, flag `AppValidation__dhu_bypass_validation`, default on). Two AAR units ("tangorpro [AAR]", "Seahawk [AAR]") are skipped too.
- In a real car, Android Auto turns Unknown sources off while it checks Car App Library apps (`qwn.f`), unless the server flag `UnknownSources__allow_full_screen_apps` is on (default off). Google's docs say the same: the setting "doesn't apply to apps built using the Android for Cars App Library".
- Car App Library apps then need a trusted installer: Google Play (Internal App Sharing and internal test tracks skip review) or one of the Korean stores in `AppValidation__allowed_3p_installers`. Root can override the flags.
- `adb install -i com.android.vending` doesn't help: Android 11+ reports `com.android.shell` as the initiating installer, and Play's `Finsky.IsValid` check fails. Tested with a car identity on the DHU; still denied.
- With no car connected, the check fails with `isPackageInstalledByPlayCheck service not connected`, so the app is missing from "Customize launcher" on the phone.

To see what a real car shows, give the DHU a car identity. Any make other than Google works:

```ini
[general]
touch = true
resolution = 1280x720
dpi = 160

[identity]
make = Toyota
model = Corolla
year = 2022

[sensors]
location = true
night_mode = true
driving_status = true
```

Save it outside the SDK and pass an absolute path: `scripts/dhu.sh --usb -c /path/to/car.ini`. The phone treats it as a new car and asks you to accept Android Auto's terms again.

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

With stdin piped in, these DHU console commands help with testing: `tap X Y`, `screenshot FILE`, `day`, `night`. Coordinates match the screenshot size (800 x 480 by default, 1280 x 720 with `-c config/default_720p.ini`).

For screenshots with a map, set a mock location first so your own doesn't show up:

```sh
adb shell appops set com.android.shell android:mock_location allow
for p in gps network fused; do
  adb shell cmd location providers add-test-provider $p
  adb shell cmd location providers set-test-provider-enabled $p true
  adb shell cmd location providers set-test-provider-location $p --location 45.0337,7.6664
done
# afterwards: remove-test-provider for each, then appops ... android:mock_location default
```

## Clock next to the map

Tested on Android Auto 18.0 with Waze and Metrolist. The only slot beside another app's map is the media card, so DashGlance Card's `media/ClockMediaService` is a Media3 media app that plays nothing. Its card shows the time as artwork, the date as title and the phone's current song as subtitle. Play/pause and skip go to whichever music app is active, through notification access.

- At 1280 x 720 the card is tall and narrow and crops the square artwork to its middle 52%, so the digits stay inside that band.
- Android Auto draws the card. Apps only supply artwork, title, subtitle and buttons. Card artwork is dimmed to about 38% (white digits measure 97/255); the full media screen shows it undimmed.

### Why two apps

The launcher lists each component as its own app. `GH.MediaAppProvider` (`lyu.b()`) drops a package's media browser service when the same package has a `CarAppService` that passes the validator, unless that service also has the SETTINGS category. The rule matches on package name, so DashGlance ships as two packages: the `card` flavor has only the media service, the `fullscreen` flavor only the `CarAppService`.

When both were in one package (0.2), which one you got depended on the head unit:

- DHU, and head units the validator skips: one tile, the full-screen clock. The media session showed up as `ComponentInfo{com.filipnone.dashglanceclock/}` with `playbackOnly=true`, which never gets the card, so the card showed the music app.
- Real car with a sideloaded APK: the `CarAppService` was denied, so the tile opened the media screen and the clock could take the card.

With two packages, tested on the DHU with both release builds installed:

- DHU: two DashGlance tiles, music note and Android Auto mark. The full-screen clock and the clock card show at the same time.
- DHU with a car identity: only DashGlance Card. The sideloaded full-screen app is still denied.

One APK can't install two packages. Splits of an app bundle share one package name too.

Tried and dropped:

- Adding the MEDIA category to `ClockCarAppService`: one tile, and opening it fails with "Binder invocation to an incorrect interface" (`GET_APP_VERSION`).
- Adding the SETTINGS category: two tiles, but the full-screen clock fails with the same error.
- Disabling one service at runtime (`pm disable`): Android Auto only picks up the change after a reconnect, because it caches the app list per connection and doesn't listen for package changes.

### Which app the card shows

- With nothing playing, the card shows the default media app, saved in Android Auto's `default_app` preferences as `component_3PROJECTED`.
- When something plays, `GH.MediaNowPlayingApp` (`mej.f`) takes the first playing session and saves it as the new default. With two playing it logs "Multiple playing browser services".
- The order is the session list Android Auto last got from `MediaSessionManager`, with the media app open on screen (if any) put first. Android sends a new list only when a session is created, released or switched on or off. A session that starts playing moves to the top of Android's own list (`MediaSessionStack` logs "Pushing session to top"), but nobody is told, so Android Auto keeps the old order.
- DashGlance follows the music app, so it starts playing after it and sits on top of Android's list, but Android Auto's copy can still have the music app first.

`media/SessionListNudge` fixes the order: 0.5 s after DashGlance starts playing, it opens an empty session for 0.3 s, so Android sends the current list. Tested on the DHU with a car identity and Metrolist, same steps each time (pause, force-stop DashGlance, reconnect, Android Auto resumes Metrolist):

- Without the nudge, Android Auto chose Metrolist every time for the whole session.
- With it, Metrolist won for the first two picks, then DashGlance from the nudge on, and kept the card through pause, play and skip.

Limits:

- While another media app's own screen is open, that app gets the card. Back on the map, DashGlance gets it again.
- Music controls have to be on. Without them DashGlance never plays, so any playing music app takes the card.
- If the music app gets back on top without DashGlance following it, the next list change hands it the card. That didn't happen with Metrolist (Media3); Spotify wasn't tested.

### Other slots tried

- `SuggestionManager` (navigation apps, Car API 5) works: a suggestion with the time as title shows as a card in the dashboard's right column. Android Auto only shows it while DashGlance is the active navigation app, though. Once Waze takes over, Android Auto keeps DashGlance bound and logs `SuggestionHost: Received Suggestions from 3p App` every minute, but hides the card.
- Suggestion icons from an adaptive-icon mipmap render as a plain white circle. Suggestions were dropped in favor of the media card.
- Android Auto 18.0 already contains dashboard widgets (`car_widgets`, `EarthPickerActivity`), but Google hasn't switched them on. Google's guide says widgets for cars are built with Jetpack Glance, so a Glance widget is the route once they ship.

## The action strip

`NavigationTemplate` hides the action strip after a few idle seconds. The first touch only brings it back, so pressing the face button takes two taps when the strip is hidden.
