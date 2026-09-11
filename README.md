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
| Speed, RPM, fuel, battery, odometer | simulated | `CAR_SPEED` / `CAR_ENERGY` / `CAR_ENGINE_DETAILED` / `CAR_MILEAGE` are `signature\|privileged` |
| VIN | simulated | `CAR_IDENTIFICATION` is `signature\|privileged` |

A normally installed app **cannot** read speed or fuel — by design, not by
bug. An instrument cluster is a privileged system app in
`/system/priv-app/` with a privapp-permissions allowlist, not something
you install from Play. Every getter degrades to a simulated value rather
than failing, so the UI stays usable either way:

```
getGear (VHAL): P                 <- real
isEngineOn (VHAL): true           <- real
getSpeed (simulated): 0.0 km/h    <- blocked, fell back
```

`VehicleHalManager.logAvailability()` prints the live/blocked split per
property at startup.

---

## Architecture

```
Car screen (Car App Library templates)
    |
    |  TabTemplate: Drive / Music / Go / Info
    v
Screens ---- observe LiveData ----> VehicleViewModel  (1 Hz poll)
    |                                     |
    |                                     v
    |                            VehicleRepository
    |                                     |
    |                          AIDL: IVehicleDataService
    |                                     |
    |                            VehicleDataService
    |                              |            |
    |                     VehicleHalManager   simulated fallback
    |                              |
    |                     CarPropertyManager -> CarService -> VHAL
    v
SmartMusicService (MediaBrowserServiceCompat + MediaSessionCompat)
    |
    v
ExoPlayer / Media3 -> car speakers
```

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
- **Speed, RPM and fuel are simulated** and will stay that way until the
  app is installed as a privileged system app. This is a permission
  boundary, not a missing feature.
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
- [x] Simulation harness for faults and alerts
- [ ] `CarPropertyManager.registerCallback` subscriptions instead of polling
- [ ] `CarAudioManager`: audio zones and volume groups
- [ ] Gauges on `GridTemplate` for legibility
- [ ] Privileged system app install, for real speed / RPM / fuel
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
