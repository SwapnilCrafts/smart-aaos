# Project Guide

What each file does, and what every command is for.

Written as a memory aid: come back after six months and this should tell you
what was built, where it lives, and how to test it again.

- [1. The two modules](#1-the-two-modules)
- [2. How data flows](#2-how-data-flows)
- [3. What each file does](#3-what-each-file-does)
- [4. Commands](#4-commands)
- [5. Reading the real VHAL source](#5-reading-the-real-vhal-source)
- [6. Profiling with Perfetto](#6-profiling-with-perfetto)
- [7. Where things went wrong](#7-where-things-went-wrong)

---

## 1. The two modules

| Module | Runs on | Status |
|---|---|---|
| `automotive` | the car itself (AAOS head unit) | built and tested |
| `systemui-overlay` | an RRO that restyles Car System UI | built and verified |
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
| `VendorProperties.kt` | The custom vendor property IDs, with the bit-field arithmetic (group \| area \| type \| id) explained. |
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
| `VendorPropertiesScreen.kt` | Reads the three custom vendor properties; drive mode is tappable, which writes back to the VHAL. |
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
| `install-systemui-overlay.sh` | Builds, platform-signs and installs the Car System UI overlay. |
| `capture-perfetto-startup.sh` | Records a startup trace for profiling. |
| `perfetto-startup.cfg` | Which data sources the trace records. |
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

### Car System UI overlay (RRO)

```bash
# build, platform-sign, install and enable
./tools/install-systemui-overlay.sh

# toggle without reinstalling
adb shell cmd overlay disable --user 0 com.swapnil.smartaaos.systemui.overlay
adb shell cmd overlay enable  --user 0 com.swapnil.smartaaos.systemui.overlay

# what overlays exist and which are on
adb shell cmd overlay list com.android.systemui

# find resource names you can override, by reading one that already ships
adb pull /product/overlay/googlecarui.theme.orange-com-android-systemui.apk
aapt2 dump resources googlecarui.theme.orange-com-android-systemui.apk
```

`--user 0` matters: CarSystemUI runs as user 0, not the driver user 10. Car
Launcher is the opposite. See [FINDINGS.md](FINDINGS.md) finding 14.

### Profiling

```bash
# capture a startup trace, then open it at https://ui.perfetto.dev
./tools/capture-perfetto-startup.sh /tmp/trace.pftrace

# is the package AOT-compiled? an APK hand-pushed into priv-app is NOT,
# which costs ~40% of main-thread startup time - see section 6
adb shell dumpsys package dexopt | grep -A3 com.swapnil.smart.aaos
adb shell cmd package compile -m speed -f com.swapnil.smart.aaos
```

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

## 5. Reading the real VHAL source

Kept outside this repo, at `~/Documents/AndroidProjects/aosp-reference/`.
10 MB, no build, no AOSP checkout:

```bash
mkdir -p ~/Documents/AndroidProjects/aosp-reference
cd ~/Documents/AndroidProjects/aosp-reference
git clone --depth=1 --filter=blob:none --sparse \
  -b android15-automotiveos-release \
  https://android.googlesource.com/platform/hardware/interfaces hardware-interfaces
cd hardware-interfaces
git sparse-checkout set automotive/vehicle
```

`android15-automotiveos-release` is the branch matching the API 35 automotive
emulator. `--filter=blob:none --sparse` is what keeps it at 10 MB instead of
several GB: only the files actually checked out are downloaded.

Worth reading, in this order:

| File | Lines | Why |
|---|---|---|
| `aidl/impl/hardware/include/IVehicleHardware.h` | 250 | the interface a real VHAL implements — start here |
| `aidl/impl/default_config/config/*.json` | — | the shipped property configs |
| `aidl/impl/default_config/JsonConfigLoader/src/JsonConfigLoader.cpp` | 724 | how those configs are parsed |
| `aidl/impl/fake_impl/hardware/src/FakeVehicleHardware.cpp` | 2614 | the emulator's implementation |
| `aidl/impl/utils/test_vendor_properties/.../TestVendorProperty.aidl` | — | how AOSP declares vendor property IDs |

These can be read and edited on macOS. Only *building* them needs Linux, since
they compile through Soong against AOSP's headers.

---

## 6. Profiling with Perfetto

`logcat` tells you *what happened*. `dumpsys` tells you *the state now*.
Perfetto tells you *who ran when, and for how long* — which is the only one of
the three that can explain a stall, a dropped frame, or slow startup.
`systrace` is the deprecated predecessor; perfetto replaced it.

```bash
./tools/capture-perfetto-startup.sh /tmp/trace.pftrace
```

The script's ordering matters: perfetto starts recording **first**, then the
app is force-stopped and launched inside the trace window. Launch first and
you trace a warm app, which is a different thing.

Open the result at **https://ui.perfetto.dev** — drag the file in; it is
parsed locally in the browser, nothing is uploaded.

### The config is the skill

`tools/perfetto-startup.cfg` picks the data sources. The automotive-relevant
part is the atrace categories:

| Category | Why |
|---|---|
| `binder_driver`, `binder_lock` | in AAOS nearly everything is another process — CarService, the VHAL, the templates host — so app latency is usually binder latency |
| `am`, `wm`, `view`, `gfx` | activity start, windows, frame rendering |
| `dalvik` | GC pauses, a classic cause of dropped frames |
| `android.log` data source | puts logcat on the same timeline as the scheduling data |

### Reading it with SQL instead of by eye

Perfetto traces are queryable. This is faster than scrolling and is how you
answer "what blocked the main thread":

```bash
python3 -m venv .venv && ./.venv/bin/pip install perfetto
```

```python
from perfetto.trace_processor import TraceProcessor
tp = TraceProcessor(trace='/tmp/trace.pftrace')

# slowest slices on OUR main thread
for r in tp.query("""
  select s.name, s.dur/1e6 as ms
  from slice s join thread_track tt on s.track_id = tt.id
  join thread t using(utid) join process p using(upid)
  where p.name like '%smart.aaos%' and t.is_main_thread = 1
  order by s.dur desc limit 12
"""):
    print(f"{r.ms:8.2f} ms  {r.name}")
```

**Always filter on `is_main_thread`.** Sorting slices by raw duration will
point you at the wrong thing: the two biggest slices in this app are ~900 ms
of emoji font loading that runs entirely on a background thread and blocks
nothing. Total time is not blocking time.

### What this found

That the privileged install had made startup ~40% slower, because an APK
pushed into `/system/priv-app` never gets dexopt and runs interpreted. Fixed
with `cmd package compile -m speed -f <pkg>`. Full numbers in
[FINDINGS.md](FINDINGS.md) finding 13.

---

## 7. Where things went wrong

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
