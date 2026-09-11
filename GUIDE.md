# Project Guide

What each file does, and what every command is for.

Written as a memory aid: come back after six months and this should tell you
what was built, where it lives, and how to test it again.

- [1. The two modules](#1-the-two-modules)
- [2. How data flows](#2-how-data-flows)
- [3. What each file does](#3-what-each-file-does)
- [4. Commands](#4-commands)
- [5. Where things went wrong](#5-where-things-went-wrong)

---

## 1. The two modules

| Module | Runs on | Status |
|---|---|---|
| `automotive` | the car itself (AAOS head unit) | built and tested |
| `app` | a phone, projected to the car screen (Android Auto) | untested, needs a physical phone + DHU |

Both use the same **Car App Library**, so the screen code looks the same. The
difference is where the app runs. AAOS = the app is installed in the car.
Android Auto = the app is installed on the phone and drawn on the car screen.

All the work so far is in `automotive`.

---

## 2. How data flows

The car pushes values up; nothing polls.

```
VHAL (the car's hardware layer)
  |
  |  CarPropertyManager.registerCallback   <- we subscribe once
  v
VehicleHalManager          reads properties, fixes units, caches values
  |
  |  IVehicleDataCallback (AIDL, oneway)   <- crosses to another process
  v
VehicleDataService         adds simulated fallback, broadcasts to clients
  |
  v
VehicleRepository          holds the latest VehicleSnapshot
  |
  v
VehicleViewModel           exposes LiveData
  |
  v
Screens (Dashboard, Home, ...)
```

Why a separate service and AIDL at all? To show a real process boundary. In a
production car, vehicle data usually comes from a separate system service, not
from inside the UI process, so this mirrors that shape.

`oneway` on the callback matters: the broadcast happens on the VHAL event
thread, and a slow client must never be able to block it.

---

## 3. What each file does

### Vehicle layer (`vehicle/`)

| File | What it does |
|---|---|
| `VehicleHalManager.kt` | Talks to the car. Reads VHAL properties, converts units (m/s to km/h, millilitres to percent), subscribes to 9 properties, caches values. Returns `null` when a property is unreadable instead of throwing. |
| `VehiclePermissions.kt` | Lists which car permissions are `normal`, `dangerous` and `privileged`, and which are still missing. Added because the app declared permissions it never requested. |
| `VehicleDataService.kt` | The service other code binds to. Serves real VHAL values when available, simulated ones when not. Broadcasts to all clients on change. |
| `VehicleRepository.kt` | Client side of the service. Binds, registers the callback, keeps the latest `VehicleSnapshot`. |
| `UxRestrictionsRepository.kt` | Reads the platform's "driver is distracted" state. Diagnostics only — see note in section 5. |
| `CarAudioInfo.kt` | Reports which car audio features exist. Most of `CarAudioManager` is closed to normal apps. |

### Screens (`ui/screens/`)

| File | What it does |
|---|---|
| `HomeScreen.kt` | The four tabs (Drive, Music, Go, Info). Also starts everything: service binding, alerts, permissions, location. |
| `DashboardScreen.kt` | The gauge cluster. `GridTemplate` with six large gauges. Blocked while moving. |
| `DiagnosticsScreen.kt` | Vehicle info (make, model, VIN) and system health. |
| `SimulationScreen.kt` | Debug screen to trigger faults and alerts by hand. Added because six simulate functions existed with no way to reach them. |
| `NavigationScreen.kt` | Map screen with Mumbai destinations, real distances, working back button, hand-off to the car's maps app. |
| `MapSurfaceRenderer.kt` | Draws the map onto the car's surface, respecting the safe area the host reserves. |
| `MusicScreen.kt`, `PlayerScreen.kt` | Song list and now-playing. |

### Media (`media/`)

| File | What it does |
|---|---|
| `SmartMusicService.kt` | `MediaBrowserService` + ExoPlayer. Audio focus, media buttons, voice search, notification. |
| `SongRepository.kt` | Single source of songs. Uses device songs if permission is granted, otherwise bundled demo songs. |
| `LocalSongLoader.kt` | Reads songs from MediaStore. |
| `MusicData.kt` | The `Song` data class and the bundled demo list. |

### Support

| File | What it does |
|---|---|
| `viewmodel/VehicleViewModel.kt` | Turns snapshots into LiveData for screens. Event-driven, no timer. |
| `viewmodel/CarViewModelStore.kt` | A `ViewModelStoreOwner` that lives for the process, because car screens are not Activities. |
| `utils/AlertRepository.kt` | Decides when to raise an alert (over 100 km/h, over 5000 rpm, low fuel). |
| `utils/AlertManager.kt` | Just the `VehicleAlert` data class and `Severity` enum. |
| `utils/CarLocationProvider.kt` | GPS location for real distances on the map. |
| `utils/AlbumArtLoader.kt` | Loads album art bitmaps. |
| `ui/GaugeDrawer.kt` | Draws the gauges as bitmaps with Canvas. |
| `ui/AppIcons.kt` | Draws the tab icons in code, so there are no drawable assets. |
| `ui/NavigationCallback.kt` | Lets screens trigger playback without holding a service reference. |
| `car/SmartCarAppService.kt`, `car/SmartSession.kt` | Entry point the car host launches. |

### Tools (`tools/`)

| File | What it does |
|---|---|
| `install-as-privileged-app.sh` | Puts the app inside the car's OS so it can read RPM, odometer and VIN. |
| `revert-privileged-app.sh` | Undoes the above. |
| `privapp-permissions-*.xml` | The allowlist saying which privileged permissions the app may hold. |

---

## 4. Commands

### Build and install

```bash
# build the car app
./gradlew :automotive:assembleDebug

# build and install in one step
./gradlew :automotive:installDebug

# build both modules (check nothing broke)
./gradlew :automotive:assembleDebug :app:assembleDebug
```

### Start the emulator

```bash
# normal start
emulator -avd AAOS_API35_UserDebug

# start so the system folder can be modified (needed for the privileged install)
emulator -avd AAOS_API35_UserDebug -writable-system -no-snapshot-load
```

`-no-snapshot-load` forces a cold boot. Without it the emulator resumes a
saved state and your changes may not apply.

### Launch the app

The automotive module has no Activity of its own. The car host provides one:

```bash
adb shell am start --user 10 \
  -n com.swapnil.smart.aaos/androidx.car.app.activity.CarAppActivity

adb shell am force-stop --user 10 com.swapnil.smart.aaos
```

`--user 10` is required. On a car the driver is user 10, not user 0.

### Check which vehicle data is real

```bash
adb logcat -c                                  # clear the log first
adb shell am force-stop --user 10 com.swapnil.smart.aaos
adb shell am start --user 10 -n com.swapnil.smart.aaos/androidx.car.app.activity.CarAppActivity
sleep 8
adb logcat -d | grep 'Availability:'
```

Every line should say `LIVE VHAL`. Any line saying `blocked -> simulated`
means a permission is missing.

### Feed the car fake driving data

```bash
# speed 31 m/s (111.6 km/h) and 5600 rpm, 5 times a second, for 180 seconds
adb shell cmd car_service inject-continuous-events 291504647 31 -s 5 -d 180
adb shell cmd car_service inject-continuous-events 291504901 5600 -s 5 -d 180

# back to standing still, gear in Park
adb shell cmd car_service inject-vhal-event 291504647 0.0
adb shell cmd car_service inject-vhal-event 289408000 4
```

Use `inject-continuous-events` for speed and rpm. A single `inject-vhal-event`
does not stick for those, because the car's own generator overwrites it.

Useful property IDs:

| Property | Decimal | Unit |
|---|---|---|
| `PERF_VEHICLE_SPEED` | 291504647 | metres per second |
| `ENGINE_RPM` | 291504901 | rpm |
| `PERF_ODOMETER` | 291504644 | kilometres |
| `GEAR_SELECTION` | 289408000 | 4 = Park, 8 = Drive |
| `FUEL_LEVEL` | 291504903 | millilitres |

### Inspect the car's properties

```bash
# what type and unit a property really is - check this before writing code
adb shell cmd car_service get-carpropertyconfig PERF_VEHICLE_SPEED

# the current stored value
adb shell cmd car_service get-property-value 291504647
```

### Permissions

```bash
# the protection level is the `prot=` field; pm list does not show it
adb shell dumpsys package permissions | grep -A3 'android.car.permission.CAR_SPEED'

# what this app holds, per user
adb shell dumpsys package com.swapnil.smart.aaos | grep -E 'User [0-9]+:|granted='

# grant a runtime permission - the user flag is essential
adb shell pm grant --user 10 com.swapnil.smart.aaos android.car.permission.CAR_SPEED
```

### The privileged install

```bash
# 1. start the emulator writable
emulator -avd AAOS_API35_UserDebug -writable-system -no-snapshot-load

# 2. build
./gradlew :automotive:assembleDebug

# 3. install into the car's OS (reboots the emulator)
./tools/install-as-privileged-app.sh

# undo
./tools/revert-privileged-app.sh
```

Only needed for RPM, odometer and VIN. Everything else works without it.

A normal `installDebug` afterwards keeps the privileged permissions, because
the package then counts as an updated system app.

But it does **not** survive closing the emulator. On this AVD `adb remount`
cannot allocate scratch on `/data` and falls back to free space on super, and
that overlay is discarded when the emulator process exits. So:

- survives `adb reboot` — yes
- survives closing and reopening the emulator — no, re-run the script

### Custom vendor properties

```bash
./tools/install-vendor-properties.sh
```

Adds three properties to the VHAL from a JSON config. No C++ needed — the
reference VHAL loads every `.json` in `/vendor/etc/automotive/vhalconfig/` at
startup. Same persistence caveat as above.

| Property | ID | Type | Access |
|---|---|---|---|
| `VENDOR_DRIVE_MODE` | `0x21400001` | INT32 | read/write |
| `VENDOR_SERVICE_DUE_KM` | `0x21600002` | FLOAT | read |
| `VENDOR_BATTERY_HEALTH` | `0x21100003` | STRING | read |

See them in the app at **Info → Vendor Properties**. Drive mode is tappable,
which writes back to the VHAL.

### Driver distraction ("can't use this while driving")

```bash
# is the car considered parked, idling or moving?
adb shell dumpsys car_service --services CarDrivingStateService

# what restrictions apply, per display
adb shell dumpsys car_service --services CarUxRestrictionsManagerService

# turn the restrictions off entirely while testing
adb shell cmd car_service enable-uxr false
```

`Display id: 0` is the main screen. `DO: false UxR: 0` means unrestricted.

If the app is stuck saying you can't use a feature while driving, inject
gear = Park and speed = 0 (see above). Injected events latch the driving
state, and any gear other than Park counts as driving even at zero speed.

### Media

```bash
# voice search
adb shell am start-foreground-service \
  -a android.media.action.MEDIA_PLAY_FROM_SEARCH \
  -n com.swapnil.smart.aaos/.media.SmartMusicService \
  --es query 'Kesariya'

# media buttons
adb shell input keyevent 85    # play / pause
adb shell input keyevent 87    # next
adb shell input keyevent 88    # previous
```

### Location for the map

```bash
adb shell cmd location set-location-enabled true --user 10
adb emu geo fix 72.8777 19.0760     # longitude first, then latitude
```

### Screenshots

A car has several displays, so plain `screencap` can produce an empty file.
Get the real display id first:

```bash
adb shell dumpsys SurfaceFlinger --display-id
adb exec-out screencap -p -d 4619827259835644672 > shot.png
```

### Songs from the device

The app runs as user 10, so `/sdcard` (user 0) is invisible to it:

```bash
adb push song.mp3 /data/media/10/Music/
adb shell am force-stop com.android.providers.media.module
adb shell content call --uri content://media --method scan_volume \
  --arg external_primary
```

---

## 5. Where things went wrong

Short version. Full detail with proof is in [FINDINGS.md](FINDINGS.md).

| Problem | Cause |
|---|---|
| Speed and fuel always fake | The app declared the permissions but never requested them. They are `dangerous`, not privileged. |
| `pm grant` seemed to do nothing | It worked, on user 0. The app runs as user 10. |
| Reads returned null | Wrong types and units. Speed is a float in m/s, fuel is millilitres, odometer is already km. |
| Injected values never appeared | Injection feeds the event stream, not the stored value. Polling could never see it; subscriptions can. |
| "Can't use while driving" while parked | An injected non-Park gear latched the platform into driving. |
| Empty band below lists | The host reserves 121 dp under list and grid templates. Cannot be changed from the app. |
| Gauge labels cut off | The text paint was centred while the draw call passed a left-edge x position. |
| Emulator kept crashing | The Mac disk was full. Not an emulator or adb fault. |
| Volume control rejected | `CarAudioManager` volume and zones are system-only. Not buildable as a normal app. |
| UX restrictions reported wrongly | The app's manager returns another display's values and sends no events, so the UI is not gated on it. |
