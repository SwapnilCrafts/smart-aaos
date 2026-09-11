# Smart AAOS

A hands-on **Android Automotive OS** project built to explore where the
app layer ends and the platform layer begins — vehicle data via VHAL,
car audio and media, Car App Library templates, and surface rendering.

Runs natively on the car's head unit (no phone required), with a second
module targeting Android Auto projection.

Findings from the investigation behind it are written up in
**[FINDINGS.md](FINDINGS.md)** — the permission model, real VHAL property
types and units, and several platform behaviours that contradict what the
documentation implies.

---

## Vehicle data: what is live, and what cannot be

The most useful thing this project taught me. Verified on an AAOS API 35
emulator with `cmd car_service get-carpropertyconfig`.

| Signal | Source | Why |
|---|---|---|
| Gear, ignition, parking brake | **live VHAL** | `CAR_POWERTRAIN` is `protectionLevel:normal` |
| Make, model, year, fuel capacity | **live VHAL** | `CAR_INFO` is `normal` |
| Speed, fuel, battery | **live VHAL** | `CAR_SPEED` / `CAR_ENERGY` are `dangerous` — a runtime request, like location |
| RPM, odometer, VIN | simulated | `CAR_ENGINE_DETAILED` / `CAR_MILEAGE` / `CAR_IDENTIFICATION` are `signature\|privileged` |

The middle row is the part worth knowing. I assumed for a long time that
everything interesting in the VHAL was privileged, because a missing
permission and an unobtainable one produce the *same* symptom:
`CarPropertyManager` reports the property as unsupported rather than
throwing. Speed and fuel were never blocked — the manifest declared them
and nothing ever called `requestPermissions()`. Only engine internals and
the vehicle's identity truly require a privileged install.

Every getter degrades to a simulated value rather than failing, so the UI
stays usable either way:

```
getGear (VHAL): P                 <- real
isEngineOn (VHAL): true           <- real
getRpm (simulated): 800.0 RPM     <- privileged, fell back
```

`VehicleHalManager.logAvailability()` prints the live/blocked split per
property at startup.

---

## Architecture

Vehicle data flows **one way, by push** — nothing polls.

```
                VHAL  (vehicle hardware / emulator)
                  |
                  |  property event
                  v
         CarPropertyManager.registerCallback
                  |                                VehicleHalManager
                  v
         VehicleDataService  --- simulated fallback for blocked properties
                  |
                  |  IVehicleDataCallback  (oneway AIDL)
                  v
         VehicleRepository   (VehicleSnapshot + listeners)
                  |
                  v
          VehicleViewModel   (LiveData)
                  |
                  v
   Screens  (Car App Library templates)
            TabTemplate: Drive / Music / Go / Info

   Media path, independent of the above:

   SmartMusicService (MediaBrowserServiceCompat + MediaSessionCompat)
                  |
                  v
   ExoPlayer / Media3  ->  car speakers
```

Requests still travel the other way as plain AIDL calls — vehicle info
(`getMake`, `getVin`), and the simulation hooks used by the debug harness.


Two modules, same `applicationId`:

| Module | Target | Entry point |
|---|---|---|
| `automotive` | Native AAOS head unit | `SmartCarAppService` -> `SmartSession` -> `HomeScreen` |
| `app` | Phone + Android Auto | `MainActivity` + `PhoneCarAppService` |

---

## What it demonstrates

**Vehicle / platform**
- `CarPropertyManager` reads with correct types and units, config-gated
  availability, and graceful degradation
- Permission protection levels and the privileged-app boundary
- An **AIDL service** boundary (`IVehicleDataService`) with Binder IPC,
  isolating the HAL from the UI
- `CarUxRestrictions` / driving-state awareness and driver-distraction
  gating

**Media**
- `MediaBrowserServiceCompat` + `MediaSessionCompat` + Media3 ExoPlayer
- Full **audio focus** handling: GAIN, LOSS, LOSS_TRANSIENT and
  LOSS_TRANSIENT_CAN_DUCK (ducks to 20% instead of pausing)
- Foreground service with a MediaStyle notification and media buttons
- `playFromSearch` for Assistant voice control
- Local library via MediaStore, with `READ_MEDIA_AUDIO` requested through
  the car host (the automotive module has no `Activity`)

**Car App Library / UI**
- `TabTemplate`, `ListTemplate`, `PaneTemplate`, `GridTemplate`,
  `PlaceListMapTemplate`, `MapTemplate`
- Custom **`SurfaceCallback`** rendering with pan/zoom, honouring the
  host's `onStableAreaChanged` safe area
- Correct back handling via `CarContext.getOnBackPressedDispatcher()`
- Navigation **handed off** to the platform maps app via a `geo:` intent
  rather than reimplemented

---

## Features

- Music: song list, playback, play/pause/next/previous, progress,
  background playback, voice search, local device library
- Drive: live gear and ignition from VHAL, drawn gauges, odometer
- Go: destination picker on the host's real map, drawn route preview on
  an app-rendered surface, hand-off to the installed navigation app
- Info: diagnostics with live vehicle info, dashboard, and a simulation
  harness for driving faults and alerts

---

## Running it

Requires Android Studio and an AAOS emulator (API 33+).

```bash
git clone https://github.com/SwapnilCrafts/smart-aaos.git
# open in Android Studio, sync, select the `automotive` run configuration
```

Useful checks once it is running:

```bash
# which properties are genuinely live
adb logcat -s SmartAAOS_VHAL:D | grep Availability

# voice search (note: service lives in the .media package)
adb shell am start-foreground-service \
  -a android.media.action.MEDIA_PLAY_FROM_SEARCH \
  -n com.swapnil.smart.aaos/.media.SmartMusicService \
  --es query 'Kesariya'

# media buttons
adb shell input keyevent 85   # play/pause
adb shell input keyevent 87   # next

# set a location for the map (longitude first)
adb shell cmd location set-location-enabled true --user 10
adb emu geo fix 72.8777 19.0760
```

Driving faults and alerts are triggered from **Info -> Simulation**
(overspeed, engine fault, low fuel, manual DTC), since the alert
thresholds are never crossed by normal simulated driving.

More commands, including MediaStore on a multi-user head unit, are in
[FINDINGS.md](FINDINGS.md).

---

## Honest notes

- **This is a learning demo, not a shippable product.** It deliberately
  spans several car app categories (media *and* navigation *and* a
  dashboard). A real app declares exactly one category, and a dashboard
  is not a third-party category at all. Kept combined on purpose, to
  cover more of the platform surface in one project.
- **RPM, odometer and VIN are simulated** and will stay that way until the
  app is installed as a privileged system app (`tools/install-as-privileged-app.sh`).
  This is a permission boundary, not a missing feature. Speed, fuel and
  battery are live once the runtime permissions are granted.
- **The empty band below list screens is a host reserve** of 121 dp,
  measured density-invariant, and cannot be removed from the app. Only
  surface-based templates use the full screen height.
- **Gauges are hard to read in list rows.** `Row.setImage()` is an icon
  slot, so a 360x360 bitmap is scaled to a thumbnail. `GridTemplate` with
  `IMAGE_TYPE_LARGE` is the fix.
- **The Android Auto module is untested** — that needs a physical phone
  with the Desktop Head Unit.

---

## Roadmap

- [x] Media playback, audio focus, voice search, background playback
- [x] Local device library via MediaStore
- [x] Real VHAL reads with correct types, units and graceful fallback
- [x] AIDL vehicle service boundary
- [x] Surface-rendered map with safe-area insets and back handling
- [x] Navigation hand-off to the platform maps app
- [x] `CarUxRestrictions` listener (diagnostics only — reports another display)
- [x] Simulation harness for faults and alerts
- [x] `CarPropertyManager.registerCallback` subscriptions instead of polling
- [x] Car audio: explicit `AudioAttributes` routing + capability report
      (zones and volume groups are `signature|privileged` — see FINDINGS.md)
- [ ] Gauges on `GridTemplate` for legibility
- [x] Runtime car permissions requested properly, making speed / fuel /
      battery live on a normal install
- [ ] Privileged system app install, for RPM / odometer / VIN
      (scripted in `tools/`, blocked on emulator disk space)
- [ ] Custom vendor VHAL property implemented in AOSP (C++)
- [ ] Android Auto verification via DHU

---

## Tech

Kotlin · Android Automotive OS · Car App Library 1.7.0 · `android.car` /
`CarPropertyManager` · AIDL · MediaBrowserServiceCompat ·
MediaSessionCompat · Media3 ExoPlayer · AGP 8.13.2 · minSdk 29 ·
compileSdk 36

## Developer

**Swapnil Patil** — [@SwapnilCrafts](https://github.com/SwapnilCrafts)

## License

MIT
