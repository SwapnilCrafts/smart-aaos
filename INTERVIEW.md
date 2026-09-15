# AAOS / Android Auto Interview Prep

Questions grouped basic → advanced, with short answers.

Markers show what you can personally back up:

- **[built]** — you did this in this project, with logs or code to show
- **[knowledge]** — you should know it, but have not done it yet. Do not
  imply otherwise; say "I have read the source but not built it."

Never bluff. "I have not done that, but here is what I understand and how I
would find out" is a strong answer in platform interviews, because the job is
mostly working out undocumented behaviour.

---

## 1. Basics

**Q. Android Auto vs Android Automotive OS?**

Android Auto is projection: the app runs on the **phone** and draws on the car
screen. AAOS is a full operating system running **on the car's head unit**, and
the app is installed in the car. Same Car App Library for UI, completely
different deployment. **[built — the repo has both modules; `automotive` is the
AAOS one and is the one I tested]**

**Q. Why can't you just use normal Activities and Views in a car app?**

Because the car maker's host app renders your UI, not you. You describe what
you want with **templates** and the host draws it in its own style, so every
app looks consistent and passes the OEM's distraction rules. You get a fixed
set of templates instead of arbitrary layouts.

Exception: AAOS media and some app types can use normal Activities. Templates
are mandatory for Android Auto projection.

**Q. What is `CarAppService`, `Session`, `Screen`?**

- `CarAppService` — the entry point the host binds to
- `Session` — one connection from the host; creates the first screen
- `Screen` — one screenful of UI; returns a `Template` from `onGetTemplate()`

`Screen` is a `LifecycleOwner`, so you can use `observe(this)` on LiveData.
**[built — I originally used `observeForever` in 17 places with no removal,
which leaks on a process-lifetime ViewModel; switching to `observe(this)`
fixed it]**

**Q. Name some templates and when you'd use them.**

`ListTemplate` (lists), `GridTemplate` (icon grids, good for gauges),
`PaneTemplate` (a few rows plus actions), `TabTemplate` (top-level tabs),
`MessageTemplate`, `PlaceListMapTemplate` (POI list with a map),
`NavigationTemplate`/`MapTemplate` (turn-by-turn, needs a drawn surface).
**[built — the app uses TabTemplate, ListTemplate, GridTemplate, MapTemplate]**

**Q. What are the car app categories?**

Declared in `automotive_app_desc.xml` — media, navigation, point-of-interest,
IOT, and more recently video/games for parked use. A published app declares
**one**. **[built — mine deliberately declares several, since it is a learning
demo and not for Play; I would split it to ship]**

**Q. What is distraction optimization?**

Rules limiting what a driver can see and do while moving — text length, number
of list items, no video, no free scrolling. The platform surfaces it through
`CarUxRestrictions`, and templates enforce much of it automatically.

---

## 2. Vehicle data

**Q. What is the VHAL?**

Vehicle Hardware Abstraction Layer. The boundary between Android and the
car's actual hardware. It exposes vehicle state as numbered **properties**
(speed, gear, fuel). Android talks to the VHAL; the VHAL talks to the CAN bus.
Apps never touch hardware directly.

**Q. How does an app read vehicle data?**

```kotlin
val car = Car.createCar(context)
val pm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
val speed = pm.getProperty<Any>(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)
```

**[built]**

**Q. `getProperty` vs `registerCallback` — which and why?**

`registerCallback` for anything that changes. The car pushes events to you;
polling wastes CPU and adds latency.

Concretely: injected test events reach **subscribers only** — they feed the
event stream, not the stored value. So polling code literally cannot see
injected data. **[built — this is why my app appeared to ignore injected
values until I moved to subscriptions; then an injected gear change showed up
as P → N → P immediately]**

**Q. What rates can you subscribe at?**

`SENSOR_RATE_ONCHANGE` for ON_CHANGE properties (gear, ignition), and
NORMAL/UI/FAST/FASTEST for CONTINUOUS ones (speed, rpm). Each property's
config caps the real maximum. **[built — speed and rpm cap at 10 Hz on the
emulator, fuel and battery at 100 Hz]**

**Q. My property read returns null. How do you debug it?**

In order:

1. Is it declared in the manifest?
2. Is it **granted**? A missing permission makes `CarPropertyManager` report
   the property as *unsupported* — it does not throw. So "never asked for it"
   and "can never have it" look identical.
3. What is the protection level? `dumpsys package permissions`, field `prot=`.
4. Is the type right? Check `cmd car_service get-carpropertyconfig`.

**[built — I hit every one of these]**

**Q. What are the permission protection levels, with car examples?**

| Level | Example | How to get it |
|---|---|---|
| `normal` | `CAR_INFO`, `CAR_POWERTRAIN` | granted at install |
| `dangerous` | `CAR_SPEED`, `CAR_ENERGY` | runtime request, like location |
| `signature\|privileged` | `CAR_ENGINE_DETAILED`, `CAR_MILEAGE`, `CAR_IDENTIFICATION` | `/system/priv-app` + allowlist |

**[built]** The middle row surprises people. Speed and fuel are ordinary
runtime permissions — I wrongly assumed they were privileged for a while,
because the failure mode is identical.

**Q. Property types and units — any traps?**

Yes, and guessing is fatal:

- `PERF_VEHICLE_SPEED` — FLOAT, **metres per second** (not km/h)
- `FUEL_LEVEL` — FLOAT, **millilitres** (not a 0–1 fraction)
- `EV_BATTERY_LEVEL` — FLOAT, **Wh** (not a percentage)
- `PERF_ODOMETER` — FLOAT, already **kilometres**
- `SEAT_BELT_BUCKLED` — BOOLEAN **per seat area**, not a global bitfield

Always check `get-carpropertyconfig` first. **[built — every one of these was
wrong in my code initially]**

**Q. A Kotlin-specific trap with `CarPropertyManager`?**

`Float::class.java` is **primitive** `float.class`, so the generic overload
never matches the boxed `java.lang.Float` a `CarPropertyValue` carries. Use
`getProperty<Any>(id, area)`.

On writes it is the mirror image: `Int::class.java` is `int.class` and fails;
`Integer::class.java` will not compile because Kotlin maps it back to `Int`.
`Int::class.javaObjectType` is correct. **[built — both]**

---

## 3. Platform level

**Q. Why can't an instrument cluster be a Play Store app?**

RPM, odometer and VIN sit behind `signature|privileged` permissions. Since
Android 9 a privileged app must be **both** in `/system/priv-app` **and**
named in a `privapp-permissions` allowlist XML. Only the OEM can put files
there, because it means building the system image.

**[built — I installed my own app that way on an emulator; all 14 properties
went from partly simulated to fully live]**

**Q. Walk me through making an app privileged.**

1. Boot the emulator with `-writable-system` (otherwise `adb remount` fails
   with a misleading "Device must be bootloader unlocked")
2. Push the APK to `/system/priv-app/<Name>/`, mode **644** — PackageManager
   silently skips unreadable APKs
3. Install the allowlist XML in `/system/etc/permissions/`
4. **Reboot** — `priv-app` is scanned only at boot

Each step fails silently if skipped. **[built — scripted in `tools/`]**

**Q. Does that break the normal dev loop?**

No, and this surprised me. A later `adb install -r` puts the APK in
`/data/app` but the package keeps its privileged permissions, because it is now
an **updated system app**. Same reason `adb uninstall` cannot undo it — it
just reverts to the system APK underneath. **[built]**

**Q. What is a vendor property and how is the ID formed?**

The `0x20000000` group is the OEM's own namespace, for hardware the standard
VHAL has no property for. The ID is a bit field and the VHAL derives the type
from it:

```
0x21400001 = 0x20000000 VENDOR
           | 0x01000000 GLOBAL area
           | 0x00400000 INT32
           | 0x00000001 my id
```

**[built — I added three: an INT32 read/write drive mode, a FLOAT service
interval and a STRING battery health]**

**Q. Do you need C++ to add a vendor property?**

No. The reference VHAL loads every `.json` in
`/vendor/etc/automotive/vhalconfig/` at startup, so a property that just
stores and returns a value is pure config. C++ is needed when the property
needs **behaviour** — a computed value, or real CAN traffic behind it.

**[built — JSON version working end to end; I have read `IVehicleHardware.h`
and `FakeVehicleHardware.cpp` but not compiled them, since AOSP does not build
on macOS]**

**Q. Which permission guards vendor properties?**

`CAR_VENDOR_EXTENSION` by default, which is `signature|privileged`. CarService
logs `no custom vendor write permission for: 0x..., default to
PERMISSION_VENDOR_EXTENSION` — so an OEM **can** map individual properties to
their own permissions, which matters when one app should read a metric without
gaining write access to everything. **[built]**

**Q. Describe the VHAL's C++ interface.**

`IVehicleHardware.h`, about six pure virtual methods:
`getAllPropertyConfigs`, `getValues`, `setValues`, `checkHealth`,
`registerOnPropertyChangeEvent`, `dump`.

Reads and writes are **asynchronous**: you return `StatusCode` immediately and
call the callback later, because a real read goes to the CAN bus and blocking
the binder thread would stall the system. `FakeVehicleHardware` queues each
request and a handler thread serves it from an in-memory store — that store is
the only "fake" part; the threading is real.

**[knowledge — read, not built]**

**Q. Any non-obvious constraint in that interface?**

`getPropertyConfig(propId)` exists with a default implementation purely for
early boot. Android queries a few properties (`VHAL_HEARTBEAT`,
`CURRENT_POWER_POLICY`, watchdog ones) before full config discovery finishes,
and if `getAllPropertyConfigs()` is slow — waiting on ethernet, say — **boot
blocks**. So you override it to answer fast for those few.
**[knowledge — from the header comments]**

**Q. Why is AAOS multi-user, and what breaks?**

The head unit boots a system user (0) and the driver is a separate user,
normally **10**. Anything user-scoped needs the right user or it silently
targets the wrong one:

- `pm grant <pkg> <perm>` succeeds against user 0 while the app runs as 10 —
  looks like a no-op
- `/sdcard` is user 0; the driver's storage is `/data/media/10`

**[built — this cost me a wrong conclusion twice, in permissions and in
MediaStore]**

**Q. `CarUxRestrictions` — how do you use it?**

Listen for restriction changes and simplify the UI when the driver is
distracted. Two real gotchas I hit: `getCarManager` returns null for it at the
moment the connection callback fires, so it needs a bounded retry, unlike
`CarPropertyManager`. And on my emulator it reported **another display's**
values (511 while display 0 was 0) and delivered no events — so I report it
for diagnostics and gate the UI on motion instead, with the reason recorded at
the call site. **[built]**

**Q. Why did you put vehicle data behind an AIDL service?**

To model the real shape: in a production car, vehicle data comes from a
separate system service, not from inside the UI process. It also forced the
right discipline — the callback is `oneway`, because the broadcast originates
on the VHAL event thread and a slow client must never block the property
stream. **[built]**

**Q. Multi-zone car audio — can you implement it?**

Not as a third-party app. Zones, volume groups and `setGroupVolume` are all
`@SystemApi`, absent from the SDK jar, and both audio permissions are
`signature|privileged`. Even `registerCarVolumeCallback` throws
SecurityException. The app-side lever is `AudioAttributes.USAGE_*`, which is
how the car's audio policy decides routing.

**[built — I found ExoPlayer had no AudioAttributes set at all, so the stream
was taking a default route; setting `USAGE_MEDIA` explicitly fixed it]**

---

## 4. Testing and tooling

**Q. How do you test vehicle features without a car?**

The AAOS emulator plus `cmd car_service`:

```bash
adb shell cmd car_service get-carpropertyconfig PERF_VEHICLE_SPEED
adb shell cmd car_service inject-continuous-events 291504647 31 -s 5 -d 180
adb shell dumpsys car_service --services CarDrivingStateService
```

**[built]**

**Q. Difference between `inject-vhal-event` and `inject-continuous-events`?**

`inject-vhal-event` is a single event, fine for ON_CHANGE properties like
gear. For CONTINUOUS properties like speed and rpm it does **not stick** —
the VHAL's own generator overwrites it. Use `inject-continuous-events`.
**[built — lost time to this]**

**Q. Anything dangerous about injecting events?**

Yes. Injected events latch `CarDrivingStateService`. Any gear other than Park
puts the platform into driving **even at zero speed**, and the app then refuses
features as distraction-restricted until you inject Park and speed 0 back.
**[built — I did exactly this to myself and spent a while blaming the app]**

**Q. How would you test on Android Auto rather than AAOS?**

The Desktop Head Unit, which mirrors a physical phone's projection on the
desktop. **[knowledge — DHU is installed but I have no physical Android phone,
so the `app` module is untested and I say so in the README]**

---

## 5. About your project

Expect these. Keep answers concrete.

**Q. Walk me through the architecture.**

One-way push, no polling:

```
VHAL → CarPropertyManager.registerCallback → VehicleHalManager
     → IVehicleDataCallback (oneway AIDL) → VehicleDataService
     → VehicleRepository → VehicleViewModel → Screens
```

`VehicleHalManager` owns unit conversion and returns `null` when a property is
unreadable, so the service can substitute a simulated value. A missing
permission degrades instead of crashing.

**Q. What was the hardest bug?**

Two candidates, both cases of my own conclusion being wrong:

The empty band below list screens. I blamed leftover rows, then a host crop.
Four probes and a density test disproved both — it is a **121 dp
density-invariant host reserve** for list and grid templates, unfixable from
the app. Only surface-based templates use full screen height.

Gauge labels reading "ry 80%". I blamed host centre-cropping and added a
scaling workaround. Instrumentation proved the workaround irrelevant: the real
cause was `textPaint()` setting `Align.CENTER` while the draw call passed a
left-edge x. One-line fix, and I reverted the wrong workaround.

The lesson I would actually give: measure before concluding, and prefer a
probe that can disprove you.

**Q. What would you do differently?**

Split the app into one category per module, since a published app declares
one. Check permission protection levels and property configs **first** rather
than assuming the platform is closed. And gate UI on a signal I have verified
delivers events, which `CarUxRestrictions` did not on my hardware.

**Q. What is not finished?**

The Android Auto module is untested — that needs a physical phone with DHU.
Multi-zone audio is not buildable as a third-party app. A C++ VHAL property
needs a Linux host, since AOSP does not build on macOS.

---

## 6. Questions to ask them

Good signal, and they show you know the landscape:

- Do you work on the app layer, CarService, or the VHAL?
- Is the VHAL C++ in-house or from a supplier? How much CAN work is yours?
- How do you test — real hardware, bench, emulator, or a virtualised VHAL?
- Which AOSP version are you on, and how far do you diverge from upstream?
- Do you carry your own privapp-permissions and sepolicy?
- Is the cluster a separate display driven by AAOS, or a separate ECU?
