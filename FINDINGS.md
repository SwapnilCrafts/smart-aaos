# AAOS Platform Findings

Notes from making this app's vehicle-data path actually work on the
Android Automotive OS emulator (`sdk_gcar_arm64`, API 35, userdebug,
1280x720 @ 160 dpi).

Each finding lists the symptom, the command used to verify it, and the
conclusion. Everything here was measured on a running emulator, not
taken from documentation — several items contradict what the docs imply.

---

## 1. What a third-party app can actually read

**Symptom.** Speed, RPM and fuel always showed simulated values, no matter
what the code did.

**Verification.**

```bash
adb shell pm list permissions -f | grep -A5 "android.car.permission"
adb shell dumpsys package <pkg> | sed -n '/install permissions/,/User 0:/p'
```

| Permission | Protection level | Third-party app |
|---|---|---|
| `CAR_INFO` | `normal` | granted at install |
| `CAR_POWERTRAIN` | `normal` | granted at install |
| `READ_CAR_DISPLAY_UNITS` | `normal` | granted at install |
| `CAR_SPEED` | not grantable | **blocked** |
| `CAR_ENERGY` | not grantable | **blocked** |
| `CAR_ENGINE_DETAILED` | `signature\|privileged` | **blocked** |
| `CAR_MILEAGE` | `signature\|privileged` | **blocked** |
| `CAR_IDENTIFICATION` | `signature\|privileged` | **blocked** |

`adb shell pm grant` does **not** help. For `CAR_POWERTRAIN` it fails
loudly (`SecurityException: ... is not a changeable permission type`,
because `normal` permissions are already granted). For `CAR_SPEED` and
`CAR_ENERGY` it exits 0 and silently does nothing — the permission never
appears in the package's permission state.

**Resulting split**, logged by `VehicleHalManager.logAvailability()`:

```
LIVE VHAL              blocked -> simulated
GEAR_SELECTION         PERF_VEHICLE_SPEED
CURRENT_GEAR           ENGINE_RPM
IGNITION_STATE         FUEL_LEVEL
PARKING_BRAKE_ON       EV_BATTERY_LEVEL
INFO_MAKE              PERF_ODOMETER
INFO_MODEL             INFO_VIN
INFO_MODEL_YEAR
INFO_FUEL_CAPACITY
```

**Conclusion.** A normally installed app can read gear, ignition, parking
brake and vehicle info. Speed, RPM, fuel, battery, odometer and VIN are
unreachable by design. An instrument cluster is therefore **not a
third-party app category** — it requires a privileged system app
installed to `/system/priv-app/` with a privapp-permissions allowlist.

Declaring `CAR_POWERTRAIN` is free and worth doing: it turns gear and
ignition from simulated into live.

---

## 2. Property types and units

**Symptom.** Reads returned null even for properties the app had
permission for.

**Verification.**

```bash
adb shell cmd car_service get-carpropertyconfig PERF_VEHICLE_SPEED
adb shell cmd car_service get-property-value FUEL_LEVEL
```

| Property | Type | Unit | Note |
|---|---|---|---|
| `PERF_VEHICLE_SPEED` | FLOAT | **METER_PER_SEC** | not km/h; negative in reverse |
| `ENGINE_RPM` | FLOAT | RPM | |
| `PERF_ODOMETER` | FLOAT | **KILOMETER** | already km, do not divide |
| `FUEL_LEVEL` | FLOAT | **MILLILITER** | not a 0..1 fraction |
| `INFO_FUEL_CAPACITY` | FLOAT | MILLILITER | divide to get a percentage |
| `EV_BATTERY_LEVEL` | FLOAT | **WATT_HOUR** | not a fraction |
| `INFO_EV_BATTERY_CAPACITY` | FLOAT | WATT_HOUR | |
| `CURRENT_GEAR` | INT32 | — | reports 1st..5th, never GEAR_DRIVE |
| `GEAR_SELECTION` | INT32 | — | reports what the driver chose ("D") |
| `IGNITION_STATE` | INT32 | — | ON = 4, START = 5 |
| `SEAT_BELT_BUCKLED` | BOOLEAN | — | **SEAT area type**, per seat ID |
| `PARKING_BRAKE_ON` | BOOLEAN | — | GLOBAL |

Two easy mistakes: reading a FLOAT property as INT32, and reading
`SEAT_BELT_BUCKLED` at `AREA_GLOBAL` when it is only exposed per seat
(`ROW_1_LEFT`, `ROW_1_RIGHT`, ...).

For UI, prefer `GEAR_SELECTION` over `CURRENT_GEAR`, and map the value
yourself — `VehicleGear.toString()` returns `"GEAR_PARK"`, which is a
host-facing name, not a dashboard label.

---

## 3. `Float::class.java` silently breaks CarPropertyManager

**Symptom.** `getProperty(Float::class.java, id, area)` failed even on a
property confirmed to be FLOAT with permission granted.

**Cause.** In Kotlin, `Float::class.java` resolves to the **primitive**
`float.class`. `CarPropertyManager`'s generic overload type-checks the
class you pass against the **boxed** `java.lang.Float` the HAL returns,
and a primitive class never matches.

**Fix.** Either use `Float::class.javaObjectType`, or avoid the generic
overload entirely:

```kotlin
// getProperty(int, int) is <E> CarPropertyValue<E>, so E must be pinned
val value: CarPropertyValue<Any>? = pm.getProperty<Any>(propertyId, areaId)
val f = (value?.value as? Number)?.toFloat()
```

Prefer `getCarPropertyConfig(id) != null` for availability checks — it
returns null when unsupported *or* unpermitted, so it costs no exception.
Cache the result: it only changes when the car service reconnects, and
re-querying on a 1 Hz poll produced 231 framework warnings
(`W CarPropertyManager: Missing required permissions to access property`)
where 4 would do.

---

## 4. Injected VHAL events reach subscribers, not `getProperty`

**Symptom.** `inject-vhal-event` reported success but the app's polled
values never changed.

**Verification.**

```bash
adb shell cmd car_service inject-vhal-event PERF_VEHICLE_SPEED 0 25.0
adb shell cmd car_service get-property-value PERF_VEHICLE_SPEED
# -> Value: 0.0 METER_PER_SEC   (timestamp updates, value does not)

adb shell cmd car_service inject-continuous-events 0x11600207 25.0 -s 5 -d 15
# same result when polled
```

But the injection *was* delivered — `CarDrivingStateService`, a
`registerCallback` subscriber, changed state from parked to moving at
exactly that moment.

**Conclusion.** Injection feeds the property **event stream**, not the
stored value that `getProperty` returns. Polling can therefore never see
injected data. To exercise injected values you must subscribe with
`CarPropertyManager.registerCallback` — which is how production code reads
`CONTINUOUS` properties anyway (`PERF_VEHICLE_SPEED` supports 1–10 Hz).

**Confirmed.** After switching this app from polling to
`registerCallback(callback, propertyId, rateHz)`, an injected gear change
reached the UI for the first time:

```bash
adb shell cmd car_service inject-vhal-event GEAR_SELECTION 0 1   # NEUTRAL
# app log:  getGear (VHAL): P  ->  getGear (VHAL): N
```

Use `SENSOR_RATE_ONCHANGE` (0 Hz) for `ON_CHANGE` properties and a real rate
for `CONTINUOUS` ones. `registerCallback` returns `false` rather than throwing
when a property is not permitted, so a subscription table can list everything
the app would like and let the unpermitted entries fail quietly - 4 of 9
subscribe on this emulator.

Note `inject-vhal-event` accepts a `SCREAMING_SNAKE_CASE` name, while
`inject-continuous-events` requires a numeric property ID.

---

## 5. Injected events latch the host into "driving"

**Symptom.** Every screen started showing *"You can't use this feature
while driving"*, long after testing had finished.

**Verification.**

```bash
adb shell dumpsys car_service --services CarDrivingStateService
# Current Driving State: 2
adb shell dumpsys car_service --services CarUxRestrictionsManagerService
# Display id: 0 UXR: DO: true UxR: 255
```

Driving state is `0` parked, `1` idling, `2` moving. Injecting a non-zero
speed and `GEAR_SELECTION = GEAR_DRIVE` had latched it at moving, so the
host applied full UX restrictions and blocked the app's templates.

**Reset.**

```bash
adb shell cmd car_service inject-vhal-event GEAR_SELECTION 0 4    # PARK
adb shell cmd car_service inject-vhal-event PERF_VEHICLE_SPEED 0 0.0
```

`cmd car_service enable-uxr false` also exists for disabling app blocking
during development.

**Conclusion.** Vehicle-data injection has side effects beyond your own
app: the platform consumes the same events. Worth knowing before blaming
your own code for a distraction lockout.

**Sharper than expected:** speed is not required. Injecting *any* non-PARK
gear at zero speed is enough - `CarDrivingStateService` moves to `IDLING`
(state `1`), whose baseline configuration on this image is `Requires DO? true`,
and the entire app is replaced by "You can't use this feature while driving".
Testing a gear change and testing the UI are therefore mutually exclusive
unless UX restrictions are disabled first.

---

## 6. The templates host reserves 121 dp below list and grid templates

**Symptom.** A large empty band below the content of every list screen,
even with items still queued below the fold.

**Verification.** Four probes, measured from screenshots by pixel:

| Probe | Result |
|---|---|
| 14 single-line rows, one list | host drew **5**, stopped at y=497, 9 rows still queued |
| 14 two-line rows | drew 3 and **clipped row 4 mid-row**, 10 queued |
| same + `ActionStrip` | no change (nested in `TabTemplate` the strip is not even rendered) |
| standalone pushed lists | identical cap (503 / 495) — so not `TabTemplate` nesting |

Row pitch is 65 px single-line, 97 px two-line. Content extent by
template, on a surface whose nav bar starts at y=624:

| Template | Last content y | Dead band |
|---|---|---|
| `ListTemplate` | 503 | 121 px |
| `GridTemplate` | 466 | 158 px |
| `PlaceListMapTemplate` | 623 | 0 |

The band is **density-invariant** — 121 px at 160 dpi and 91 px at
120 dpi are both exactly **121 dp**.

**Conclusion.** A fixed host reserve, not an app bug and not fixable from
the app. No combination of row count, row height, sectioning or action
strip changes it. Only surface-based templates (`MapTemplate`,
`MapWithContentTemplate`) use the full height, because the app draws every
pixel. Relevant corollary: `Row.setImage()` is an *icon* slot, so 360x360
gauge bitmaps render as unreadable specks — `GridTemplate` with
`IMAGE_TYPE_LARGE` renders them legibly.

---

## 7. Navigation hand-off: `startActivity`, not `startCarApp`

**Symptom.** `CarContext.startCarApp(ACTION_NAVIGATE, ...)` failed with
`Remote startCarApp call failed`.

**Cause.** `startCarApp` targets the **Android Auto projected** templates
host. On native AAOS the app is an ordinary app on the head unit, so the
correct call is a standard `ACTION_VIEW` intent with a `geo:` URI and
`FLAG_ACTIVITY_NEW_TASK`.

Resolve before starting, so a missing maps app is a message rather than an
exception:

```kotlin
val geo = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(name)})")
val intent = Intent(Intent.ACTION_VIEW, geo).addFlags(FLAG_ACTIVITY_NEW_TASK)
if (packageManager.resolveActivity(intent, MATCH_DEFAULT_ONLY) != null) { ... }
```

**Note.** Bare AAOS emulator images ship only
`com.android.car.mapsplaceholder` and resolve neither `geo:` nor
`ACTION_NAVIGATE`. Use an image with Google Play to test the success path.

---

## 8. The app runs as user 10, not user 0

**Symptom.** Files pushed to `/sdcard/Music/` were invisible to the app,
and MediaStore returned nothing.

**Cause.** AAOS runs the driver as **user 10**. `adb push /sdcard/...`
writes user 0's storage. `/storage/emulated/10` is blocked by the FUSE
mount even for root; the backing path is `/data/media/10`.

A plain volume rescan also silently no-ops — MediaProvider has to be
restarted first:

```bash
adb root
adb push song.mp3 /data/media/10/Music/
adb shell am force-stop --user 10 com.android.providers.media.module
adb shell content call --user 10 --uri content://media \
  --method scan_volume --arg external_primary
adb shell content query --user 10 \
  --uri content://media/external/audio/media --projection title
adb unroot
```

Also relevant: the automotive module has no `Activity`, so runtime
permissions such as `READ_MEDIA_AUDIO` must be requested through
`CarContext.requestPermissions()`.

---

## Command reference

```bash
# what the VHAL exposes, and its real type/unit
adb shell cmd car_service get-carpropertyconfig <NAME|0xID>
adb shell cmd car_service get-property-value   <NAME|0xID> [areaId]

# feed the event stream (subscribers only, not getProperty)
adb shell cmd car_service inject-vhal-event       <NAME|0xID> <areaId> <value>
adb shell cmd car_service inject-continuous-events <0xID> <value> -s <Hz> -d <sec>

# driver distraction
adb shell dumpsys car_service --services CarDrivingStateService
adb shell dumpsys car_service --services CarUxRestrictionsManagerService
adb shell cmd car_service enable-uxr false

# permissions
adb shell pm list permissions -f | grep -A5 android.car.permission
adb shell dumpsys package <pkg> | grep -A20 "install permissions"

# this app's own availability report
adb logcat -s SmartAAOS_VHAL:D | grep Availability

# screenshot a specific display (AAOS has several)
adb shell dumpsys SurfaceFlinger --display-id
adb exec-out screencap -p -d <displayId> > shot.png
```
