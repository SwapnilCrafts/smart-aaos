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

I first concluded that all of them were privileged and unreachable. That was
wrong, and the way it was wrong is the more useful finding: **two of the three
protection levels look identical from inside the app.** When a permission is
missing, `CarPropertyManager` does not throw — it reports the property as
unsupported. So "never asked for a runtime permission" and "can never hold
this permission" produce the exact same symptom, and it is tempting to assume
the second.

**Verification.** `pm list permissions` prints names, not levels. The level is
in `dumpsys`, in the `prot=` field:

```bash
adb shell dumpsys package permissions | grep -A3 'android.car.permission.CAR_SPEED'
#   sourcePackage=com.android.car.updatable
#   uid=10162 gids=[] type=0 prot=dangerous
```

| Permission | `prot=` | Gates | How the app gets it |
|---|---|---|---|
| `CAR_INFO` | `normal` | make, model, year, fuel capacity | granted at install |
| `CAR_POWERTRAIN` | `normal` | gear, ignition, parking brake | granted at install |
| `CAR_SPEED` | `dangerous` | `PERF_VEHICLE_SPEED` | **runtime request** |
| `CAR_ENERGY` | `dangerous` | `FUEL_LEVEL`, `EV_BATTERY_LEVEL` | **runtime request** |
| `CAR_ENGINE_DETAILED` | `signature\|privileged` | `ENGINE_RPM` | priv-app + allowlist |
| `CAR_MILEAGE` | `signature\|privileged` | `PERF_ODOMETER` | priv-app + allowlist |
| `CAR_IDENTIFICATION` | `signature\|privileged` | `INFO_VIN` | priv-app + allowlist |

So speed and fuel level — the two most obviously "vehicle" values in the app —
are ordinary runtime permissions, no different from location. They were never
blocked. The manifest declared them and **nothing ever called
`requestPermissions()`**, so they were never granted.

### Why the adb grant appeared to do nothing

Earlier I recorded that `pm grant` on `CAR_SPEED` "exits 0 and silently does
nothing". It exits 0 because it *succeeds* — against the wrong user. AAOS boots
the driver as user 10, and `pm grant` defaults to user 0:

```bash
adb shell dumpsys package com.swapnil.smart.aaos | grep -E 'User [0-9]+:|granted='
#   User 0:                                   <- where the grant landed
#     android.car.permission.CAR_SPEED: granted=true
#   User 10:                                  <- where the app actually runs
#     (CAR_SPEED absent entirely)
```

The fix is one flag:

```bash
adb shell pm grant --user 10 com.swapnil.smart.aaos android.car.permission.CAR_SPEED
adb shell pm grant --user 10 com.swapnil.smart.aaos android.car.permission.CAR_ENERGY
```

Four properties went live immediately, with **no change to the system image**:

```
Availability: PERF_VEHICLE_SPEED   LIVE VHAL      <- was blocked
Availability: FUEL_LEVEL           LIVE VHAL      <- was blocked
Availability: EV_BATTERY_LEVEL     LIVE VHAL      <- was blocked
Availability: INFO_FUEL_CAPACITY   LIVE VHAL
Availability: GEAR_SELECTION       LIVE VHAL
Availability: CURRENT_GEAR         LIVE VHAL
Availability: IGNITION_STATE       LIVE VHAL
Availability: PARKING_BRAKE_ON     LIVE VHAL
Availability: INFO_MAKE            LIVE VHAL
Availability: INFO_MODEL           LIVE VHAL
Availability: INFO_MODEL_YEAR      LIVE VHAL
Availability: ENGINE_RPM           blocked -> simulated
Availability: PERF_ODOMETER        blocked -> simulated
Availability: INFO_VIN             blocked -> simulated
```

This is the same user-10 trap that hides MediaStore from the app (finding 8).
It has now cost me a wrong conclusion twice, in two unrelated subsystems, which
is a good argument for making `--user 10` the default habit on AAOS rather than
something to remember.

**Conclusion.** The real boundary is narrower than it looks. A normal install
can read gear, ignition, parking brake, vehicle info, **speed, fuel and
battery** — it just has to ask. Only RPM, odometer and VIN genuinely require a
privileged system app in `/system/priv-app` with a privapp-permissions
allowlist.

An instrument cluster still is not a third-party app category, but the reason
is narrower than "the VHAL is closed": the interesting driving values are
available, and what is withheld is the engine internals and the vehicle's
identity.

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

## 9. Car audio: one public method, and that is it

**Symptom.** "Multi-zone audio" is a common automotive requirement, but none of
the zone or volume APIs are reachable.

**Verification.**

```bash
adb shell pm list permissions -f | grep -A6 "CAR_CONTROL_AUDIO"
javap -classpath .../android.car.jar android.car.media.CarAudioManager
```

Both `CAR_CONTROL_AUDIO_VOLUME` and `CAR_CONTROL_AUDIO_SETTINGS` are
`signature|privileged`. The *public* `CarAudioManager` has exactly three
methods — `isAudioFeatureEnabled`, `registerCarVolumeCallback`,
`unregisterCarVolumeCallback`. Audio zones, volume groups,
`getVolumeGroupCount` and `setGroupVolume` are `@SystemApi` and absent from the
SDK jar entirely.

Of those three, only the first actually works for a third-party app:

```
audio feature DYNAMIC_ROUTING      true
audio feature VOLUME_GROUP_MUTING  true
audio feature VOLUME_GROUP_EVENTS  true
audio feature AUDIO_MIRRORING      false
audio feature OEM_AUDIO_SERVICE    false
registerCarVolumeCallback rejected: SecurityException:
    requires permission android.car.permission.CAR_CONTROL_AUDIO_VOLUME
```

Even *observing* volume is gated.

**Conclusion.** The app-side half of car audio is not zone control — it is
declaring the correct `AudioAttributes.USAGE_*`, because the car's audio policy
is what maps a usage onto a bus, zone and volume group. An `ExoPlayer` built
without audio attributes takes the default route; setting `USAGE_MEDIA` +
`CONTENT_TYPE_MUSIC` explicitly puts it on the media bus that the media volume
knob controls. Audio focus (including `LOSS_TRANSIENT_CAN_DUCK`) is the other
half, and needs no permission.

---

## 10. CarUxRestrictionsManager needs a retry, and reports the wrong display

**Symptom one.** `getCarManager` returns null for the UX restriction service,
even though the car service is connected and the same pattern works for
`CarPropertyManager`.

```
Car lifecycle: connected=true
String-keyed lookup returned: null
Manager not ready, retry 1/4
Listening for UX restriction changes     <- succeeds on the retry
```

Both the `String`-keyed and the typed `getCarManager(Class)` overload return
null at the moment the connection callback fires. A retry ~1.5 s later
succeeds. `CarPropertyManager` does not need this, so it is manager-specific.

**Symptom two — the more important one.** The values do not belong to the
display the app is on.

```
# platform, while parked:
Display id: 0  UXR: DO: false  UxR: 0      <- the app runs here
Display id: 3  UXR: DO: true   UxR: 511    <- the cluster

# what the app's own manager reports:
UXR: requiresDO=true flags=0x1ff maxItems=21     (0x1ff == 511)
```

And when display 0's restrictions genuinely changed (`DO: true UxR: 16` after
moving to IDLING), the app's listener delivered **no event at all**.

**Conclusion.** On this platform the manager resolves to a different display
and does not deliver change events, so gating UI on
`isRequiresDistractionOptimization()` would disable the app while parked. Read
it for diagnostics; do not drive behaviour from it without first confirming it
tracks your own display. Nothing is lost by ignoring it — the host enforces the
real restrictions itself and will replace the app outright when it must.

`getMaxCumulativeContentItems()` returned 21, which is a genuinely useful
number when deciding how much to put in a `ListTemplate`.

---

## 11. A privileged install is one-time; the normal dev loop survives it

**Goal.** Read the three properties no ordinary APK can reach: `ENGINE_RPM`
(`CAR_ENGINE_DETAILED`), `PERF_ODOMETER` (`CAR_MILEAGE`) and `INFO_VIN`
(`CAR_IDENTIFICATION`).

**What it takes.** Four things, and missing any one of them fails silently:

1. Boot the AVD with `-writable-system`. Without it `adb remount` refuses with
   `Device must be bootloader unlocked` — misleading, since no bootloader is
   involved; the system image is simply mounted read-only.
2. Push the APK to `/system/priv-app/SmartAAOS/`, mode 644. PackageManager
   skips priv-app APKs it cannot read.
3. Install a `privapp-permissions` allowlist XML naming the package and each
   privileged permission. **Since Android 9, being in priv-app is not enough
   on its own** — an unlisted privileged permission is just denied.
4. Reboot. `/system/priv-app` is scanned at boot only, so pushing an APK there
   on a running system does nothing until the package database is rebuilt.

Scripted end to end in `tools/install-as-privileged-app.sh`, with
`tools/revert-privileged-app.sh` to undo it.

**Result.** All 14 properties the app reads went live, and the subscription
count went from 4 to 9:

```
Subscribed to 9 of 9 properties
Availability: ENGINE_RPM      LIVE VHAL     <- was blocked
Availability: PERF_ODOMETER   LIVE VHAL     <- was blocked
Availability: INFO_VIN        LIVE VHAL     <- was blocked
```

Real injected values then flowed through `registerCallback` into the UI:

```bash
adb shell cmd car_service inject-continuous-events 291504647 31 -s 5 -d 180
# app: getSpeed (VHAL): 111.6 km/h     (31 m/s x 3.6, exact)
# app: getRpm   (VHAL): 5600.0 RPM
# UI:  Engine  Critical - High RPM (5600)
# UI:  1GCARVIN123456789
```

### The part I expected to be painful and wasn't

I assumed a privileged install would mean abandoning the normal build-and-run
loop, since Android Studio installs to `/data/app`. It does not. Installing the
same APK the ordinary way afterwards:

```bash
adb install -r --user 10 automotive-debug.apk
# package:/data/app/~~iMBiJgz.../base.apk     <- resolves from /data now
# CAR_MILEAGE: granted=true                   <- privileged perms survive
# Subscribed to 9 of 9 properties             <- still fully live
```

Once a package has been scanned from `/system`, a later install over it is
treated as an **updated system app**: the `/data` APK takes effect but the
package keeps its system origin, and therefore its privileged permissions.

This cuts both ways, and it is the reason `adb uninstall` alone cannot undo
this: uninstalling an updated system app just reverts to the system APK
underneath. The `/system` copy must be deleted and the device rebooted.

**Conclusion.** The privileged install is a one-time setup step per emulator,
not a change to how the app is developed. That is roughly how OEM app teams
work: the platform-side allowlist is configured once in the system image, and
day-to-day development is ordinary app development on top of it.

---

## 12. A custom vendor VHAL property needs no C++

**Assumption I started with.** Adding a vehicle property means writing a VHAL
implementation in C++ and building AOSP from source. That is what the job
adverts imply, and it is why I had this queued as a multi-week phase needing a
Linux machine.

**What is actually true.** The reference VHAL is config-driven. Its binary
contains the path it scans:

```bash
adb pull /vendor/bin/hw/android.hardware.automotive.vehicle@V3-emulator-service
strings ./android.hardware.automotive.vehicle@V3-emulator-service | grep -i vhal
# /vendor/etc/automotive/vhalconfig/
# /vendor/etc/automotive/vhaloverride/
# persist.vendor.vhal_init_value_override
```

Every `.json` file in that directory is loaded at service start:

```
I FakeVehicleHardware: loading properties from /vendor/etc/automotive/vhalconfig/
I FakeVehicleHardware: loading properties from .../DefaultProperties.json
I FakeVehicleHardware: loading properties from .../SmartAaosVendorProperties.json
```

So a new property is a config change plus a service restart. No C++, no AOSP
checkout, no reflash.

### Property IDs are bit fields, not identifiers

This is the part that has to be right, because the VHAL derives the type from
the ID itself:

```
0x21400001
  2_______   group  VENDOR  0x20000000   (SYSTEM is 0x10000000)
  _1______   area   GLOBAL  0x01000000   (SEAT, DOOR, WINDOW ... exist too)
  __4_____   type   INT32   0x00400000   (FLOAT 0x00600000, STRING 0x00100000)
  ___00001   id     an OEM's own number within the vendor group
```

Anything under `0x20000000` belongs to the manufacturer, and Google will never
assign a conflicting meaning to it. That is how a real car exposes hardware the
standard VHAL has no property for.

### Symbolic names in the stock configs do not work for new properties

The shipped configs name properties through C++ enums:

```json
{ "property": "TestVendorProperty::MIXED_TYPE_PROPERTY_FOR_TEST" }
```

Those resolve only for names compiled into the VHAL, so a genuinely new
property cannot use that form. The loader accepts a raw integer instead, which
is what makes this possible without touching C++:

```json
{
    "property": 557842433,
    "defaultValue": { "int32Values": [1] },
    "access": "VehiclePropertyAccess::READ_WRITE",
    "changeMode": "VehiclePropertyChangeMode::ON_CHANGE"
}
```

Unknown keys are ignored, so `_comment` fields can be used to document the file
even though JSON has no comment syntax. Verified: the file still parses and the
properties still read.

### Reading them needs the privileged install

`CAR_VENDOR_EXTENSION` gates every vendor property and is
`signature|privileged`, so this only works on top of finding 11.

CarService says so explicitly when the app touches one, and the wording shows
the permission is only a default:

```
CAR.PropertyHalServiceConfigs: no custom vendor write permission for:
    0x21400001, default to PERMISSION_VENDOR_EXTENSION
```

So an OEM can map individual vendor properties to their own permissions rather
than lumping them all behind `CAR_VENDOR_EXTENSION` — which matters in a real
car, where one preinstalled app should be able to read a battery metric without
also gaining write access to every other vendor property.

### Writing hits the boxed-type trap again, from the other side

Finding 3 was about `Float::class.java` resolving to primitive `float.class` on
a read. The write path has the mirror-image problem.
`setProperty` is `<E> setProperty(Class<E>, int, int, E)`:

```kotlin
pm.setProperty(Int::class.java, id, area, mode)         // int.class, matches nothing
pm.setProperty(Integer::class.java, id, area, mode)     // will not compile:
                                                        // Kotlin maps Integer back to Int,
                                                        // so E cannot be inferred
pm.setProperty(Int::class.javaObjectType, id, area, mode)  // correct
```

`Int::class.javaObjectType` is `java.lang.Integer`, which is what the property
carries, and `E` infers as `Int` so a plain Kotlin `Int` can be passed.

### These changes are not persistent

`adb remount` on this AVD reports:

```
Failed to allocate scratch on /data, fallback to use free space on super
Using overlayfs for /system
```

That overlay holds the priv-app install, the permission allowlist and the
vendor JSON — and it is discarded when the emulator process exits. It survives
`adb reboot` inside a running emulator, which is why I initially believed it was
persistent: the install script reboots, and that worked. It does not survive
closing the emulator. Both install scripts have to be re-run after each start.

### Confirmed against AOSP source

The above was all worked out from the device. Afterwards I shallow-cloned the
VHAL to check it, which takes 10 MB and no build:

```bash
git clone --depth=1 --filter=blob:none --sparse \
  -b android15-automotiveos-release \
  https://android.googlesource.com/platform/hardware/interfaces
cd interfaces && git sparse-checkout set automotive/vehicle
```

Each empirical finding holds:

**The config directory** — `fake_impl/hardware/src/FakeVehicleHardware.cpp`:

```cpp
constexpr char DEFAULT_CONFIG_DIR[]  = "/vendor/etc/automotive/vhalconfig/";
constexpr char OVERRIDE_CONFIG_DIR[] = "/vendor/etc/automotive/vhaloverride/";
```

**Numeric IDs are a deliberate branch**, not an accident —
`default_config/JsonConfigLoader/src/JsonConfigLoader.cpp`:

```cpp
template <class T>
Result<T> JsonValueParser::parseValue(const std::string& fieldName,
                                      const Json::Value& value) const {
    if (!value.isString()) {
        return convertValueToType<T>(fieldName, value);   // raw number
    }
    // otherwise resolve "Type::CONSTANT" through the enum tables
```

So a string is treated as a constant name and anything else as a literal. Both
forms are supported by design.

**Unknown keys are ignored** because the loader only ever asks for fields by
name (`parentJsonNode.isMember(fieldName)`); it never enumerates the object. So
`_comment` keys are safe.

**The bit-field arithmetic is how AOSP writes it too** —
`utils/test_vendor_properties/.../TestVendorProperty.aidl`:

```java
VENDOR_CLUSTER_SWITCH_UI = 0x0F34 + 0x20000000 + 0x01000000 + 0x00400000;
//                         id     + VENDOR     + GLOBAL     + INT32
```

Identical to the `vendorId()` helper in `VendorProperties.kt`, which is
reassuring: the ID scheme was not guesswork.

### What the C++ work would actually be

The interface a real VHAL implements is small —
`hardware/include/IVehicleHardware.h` is 250 lines with about six pure virtual
methods:

```cpp
virtual std::vector<VehiclePropConfig> getAllPropertyConfigs() const = 0;
virtual StatusCode getValues(callback, requests) = 0;
virtual StatusCode setValues(callback, requests) = 0;
virtual StatusCode checkHealth() = 0;
virtual void registerOnPropertyChangeEvent(...) = 0;
virtual DumpResult dump(options) = 0;
```

`FakeVehicleHardware.cpp` is the 2,600-line reference implementation of it.
Reading and modifying either needs nothing but an editor; only *compiling* them
needs Linux, because they build through Soong against AOSP's headers.

**Conclusion.** The C++/AOSP work is real, but it is not the entry point to
vendor properties. Defining the interface, choosing the ID, wiring the
permission and consuming it from the app is all doable on a stock emulator in
an afternoon. C++ is needed when a property needs *behaviour* — a computed
value, or real hardware and CAN traffic behind it — not to exist.

---

## 13. The privileged install made startup 40% slower

Found with Perfetto, and it is a direct consequence of finding 11.

**Symptom.** Nothing visible — the app worked. It showed up only in a trace.

```bash
./tools/capture-perfetto-startup.sh /tmp/before.pftrace
```

The main thread's slowest slices were all dex loading, and the filenames say
where from:

```
178.09 ms  /system/priv-app/SmartAAOS/SmartAAOS.apk
169.27 ms  OpenDexFilesFromOat(/system/priv-app/SmartAAOS/SmartAAOS.apk)
158.68 ms  Open dex file /system/priv-app/SmartAAOS/SmartAAOS.apk
 90.71 ms  Verify dex file /system/priv-app/SmartAAOS/SmartAAOS.apk
```

Plus 2,430 ms of `Compiling baseline` across 8,608 slices — that is the JIT
compiling at runtime, work that should have happened at install time.

**Cause.** `adb push` into `/system/priv-app/` is a file copy. It does not run
dexopt, because dexopt is something `pm install` does. So the package had no
AOT artifacts at all:

```bash
adb shell dumpsys package dexopt | grep -A3 com.swapnil.smart.aaos
#   path: /system/priv-app/SmartAAOS/SmartAAOS.apk
#     arm64: [status=run-from-apk] [reason=unknown] [primary-abi]
#       [location is error]
```

`status=run-from-apk` and `location is error`, and no `.odex`/`.vdex` beside
the APK. The app was running interpreted, verifying its dex on every cold
start.

**Fix.** Compile it, which is what an OEM build does with `dex2oat` at build
time:

```bash
adb shell cmd package compile -m speed -f com.swapnil.smart.aaos
#   arm64: [status=verify] [reason=cmdline]
#     [location is /data/dalvik-cache/arm64/system@priv-app@...@classes.dex]
```

**Measured, same config, one run each:**

| Slice | Before | After |
|---|---|---|
| `bindApplication` | 366.1 ms | **148.1 ms** |
| `activityStart` | 282.4 ms | **136.5 ms** |
| `Open dex file` | 158.7 ms | **10.5 ms** |
| `Verify dex file` | 156.8 ms | **0.0 ms** |
| `OpenDexFilesFromOat` | 169.3 ms | **34.8 ms** |
| `Compiling baseline` | 2430.7 ms | 1627.7 ms |
| main-thread top-level slice time | 2084.9 ms | **1254.9 ms** |

Roughly 40% off main-thread startup work, and dex verification eliminated.
Single runs on an emulator with a debug APK, so treat the numbers as
indicative of the mechanism rather than a benchmark.

### A thing I got wrong while reading the trace

The two largest slices in the whole process were emoji font work:

```
455.39 ms  EmojiCompat.FontRequestEmojiCompatConfig.buildTypeface
435.97 ms  EmojiCompat.MetadataRepo.create
```

Nearly 900 ms, and my first instinct was that this was the startup problem.
It is not. Querying which thread they were on:

```sql
select s.name, s.dur/1e6 ms, t.is_main_thread
from slice s join thread_track tt on s.track_id=tt.id
join thread t using(utid) join process p using(upid)
where p.name like '%smart.aaos%' and s.name like '%Emoji%'
```

`is_main_thread = 0` — all of it runs on a dedicated `EmojiCompatInit`
thread, with only 34 ms of `EmojiCompatInitializer` on main. Big, but off the
critical path.

This is the whole reason to use a profiler rather than a timer: **total time
is not the same as blocking time**, and a list of slices sorted by duration
will point you at the wrong thing if you ignore which thread they are on.

### Why this matters in a car more than on a phone

A phone app starting 200 ms slower is invisible. A head unit has a
time-to-first-frame budget measured from ignition, because the driver expects
the screen up almost immediately, and the reversing camera and media surface
compete for the same early-boot CPU. Startup work on the critical path is a
requirement, not a nicety — which is why `systrace`/`perfetto` appear by name
in AAOS job specs.

**Conclusion.** The mechanism is worth remembering beyond this project: any
APK placed into a system partition by hand has no dexopt artifacts, so it
runs interpreted until something compiles it. That applies to every
`/system/priv-app` install done outside a build, which is exactly how
developers test privileged apps.

---

## 14. Customising Car System UI without an AOSP build

**Why this mattered.** Across ten Bengaluru AAOS job descriptions, HMI and
System UI work appeared in five — more than Car Service (three) or C++
(three). It was the largest gap in this project, and the obvious assumption is
that it needs a platform build on Linux. It does not.

**What an RRO is.** A Runtime Resource Overlay is an APK with no code that
replaces another package's resources at runtime. It is how an OEM rebrands the
car while leaving AOSP untouched, so their look survives Android upgrades:

```xml
<manifest package="com.swapnil.smartaaos.systemui.overlay">
    <application android:hasCode="false" />
    <overlay
        android:targetPackage="com.android.systemui"
        android:priority="2"
        android:isStatic="false" />
</manifest>
```

That, plus a `colors.xml`, is the entire module. Note CarSystemUI is still
called `com.android.systemui` on AAOS.

### Finding the resource names, rather than guessing

An overlay matches resources **by name**, so a typo silently overlays nothing.
The reliable way to discover valid names is to read an overlay that already
ships on the device:

```bash
adb pull /product/overlay/googlecarui.theme.orange-com-android-systemui.apk
aapt2 dump resources googlecarui.theme.orange-com-android-systemui.apk
# resource 0x7f010000 color/car_background
# resource 0x7f01000a color/car_primary
# ... 163 resources in total
```

The device also ships dozens of *disabled* overlays, so the mechanism can be
proven before writing anything:

```bash
adb shell cmd overlay list com.android.systemui
# [ ] com.android.systemui.googlecarui.theme.orange.rro     <- disabled
adb shell cmd overlay enable --user 0 com.android.systemui.googlecarui.theme.orange.rro
```

That turned the system bar accent from blue to orange, which confirmed the
pipeline worked before any code existed.

### Trap 1: the overlay must be enabled for the user its target runs as

The driver is user 10, but CarSystemUI is not:

```bash
adb shell 'ps -A -o USER,PID,NAME | grep systemui'
# u0_a223  4350  com.android.systemui        <- user 0
```

Enabling for user 10 fails with `SecurityException: Unable to retrieve overlay
information`. Car Launcher, by contrast, *is* user 10. So the right user
differs per target.

This is the AAOS multi-user trap for the third time in this project, after
permissions (finding 1) and MediaStore (finding 8). The lesson has stopped
being "remember user 10" and become **"ask which user this component runs
as"**.

### Trap 2: a third-party overlay cannot touch CarSystemUI

Installing the overlay debug-signed fails with a precise and useful error:

```
Overlay com.swapnil.smartaaos.systemui.overlay and target
com.android.systemui signed with different certificates, and the overlay
lacks <overlay android:targetName>
```

That names all three legitimate routes:

1. sign with the **same certificate as the target**
2. the target declares `<overlayable name="...">` and the overlay names it
   with `android:targetName`
3. be **preinstalled** in a trusted partition by the system image

Route 2 is closed, and this is checkable rather than assumed:

```bash
adb pull /system_ext/priv-app/CarSystemUI/CarSystemUI.apk
aapt2 dump overlayable CarSystemUI.apk     # prints nothing
```

CarSystemUI exposes no overlayable resources, so it is not designed to be
overlaid by third parties at all — only by whoever builds the image.

### The way through: the emulator is a test-keys build

```bash
adb shell getprop ro.build.tags
# test-keys
```

`test-keys` means the image is signed with the **public** AOSP platform
certificate, which lives in the AOSP `build` repo. So route 1 is available on
a development device, and needs a 1 MB sparse clone rather than a platform
build:

```bash
git clone --depth=1 --filter=blob:none --sparse \
  -b android15-automotiveos-release \
  https://android.googlesource.com/platform/build
git -C build sparse-checkout set target/product/security

apksigner sign \
  --key  build/target/product/security/platform.pk8 \
  --cert build/target/product/security/platform.x509.pem \
  systemui-overlay-debug.apk
# Signer #1 certificate DN: CN=Android, O=Android, L=Mountain View...
```

It then installs, and `cmd overlay enable --user 0` applies it.

Worth being clear about what this is and is not: on a production car the
platform key is the OEM's secret, and an overlay ships inside the signed
system image. Platform-signing against the public test keys works because a
development image trusts a publicly known certificate — which is precisely why
`test-keys` builds are never shipped.

**Result**, verified with screenshots at each stage:

| Stage | System bar accent |
|---|---|
| stock | blue |
| stock Google `theme.orange` RRO enabled | orange |
| own overlay, platform-signed, priority 2 | **teal `#00BFA5`** |

Scripted in `tools/install-systemui-overlay.sh`, module `systemui-overlay/`.

**Conclusion.** The part of AAOS the job market asks for most is the part that
needs least infrastructure. Resource-level System UI customisation — the actual
day-to-day of OEM HMI work — needs an emulator, `aapt2`, and a public key. No
Linux, no `repo sync`, no `dex2oat`. The thing that genuinely requires a
platform build is changing CarSystemUI's *behaviour*, not its appearance.

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

# permissions: the protection level is the `prot=` field, not in pm list
adb shell dumpsys package permissions | grep -A3 'android.car.permission.CAR_SPEED'
adb shell dumpsys package <pkg> | grep -E 'User [0-9]+:|granted='

# grant a runtime car permission -- note the user flag, AAOS drives user 10
adb shell pm grant --user 10 <pkg> android.car.permission.CAR_SPEED

# install as a privileged system app (needs -writable-system); see tools/
./tools/install-as-privileged-app.sh
./tools/revert-privileged-app.sh

# feed CONTINUOUS properties: a single inject-vhal-event does not stick
adb shell cmd car_service inject-continuous-events 291504647 31 -s 5 -d 180

# this app's own availability report
adb logcat -s SmartAAOS_VHAL:D | grep Availability

# screenshot a specific display (AAOS has several)
adb shell dumpsys SurfaceFlinger --display-id
adb exec-out screencap -p -d <displayId> > shot.png
```
