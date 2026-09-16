# AAOS Interview Questions

> **Superseded.** Everything here has been merged into
> **[AAOS_MASTER.md](AAOS_MASTER.md)** — one file, 133 questions in learning
> order, simpler language, plus the core app-layer basics this file skipped
> and an appendix of corrected facts. Read that instead. This file is kept
> only for the JD analysis at the end.

Real questions asked in Android Automotive interviews, with short answers and
what the interviewer is checking for.

A ★ means **you can answer this from your own project**, not from memory.
Those are your strongest answers — always give the concrete version.

Sections 1-9 are what gets asked most. Sections 10-12 are the areas the job
descriptions demand and your project does not cover yet — study those. The
appendix at the end counts what 10 real Bengaluru JDs actually asked for.

**The one rule.** KPIT publishes their scoring criteria: *"No generic software
answers — automotive application is mandatory."* Every answer should mention
the car: the signal, the driver, the safety case, or the constraint. Say
"speed arrives in m/s from the VHAL at 10 Hz", not "I read a value".

---

## 1. Basics

**Q. What is Android Automotive OS?**

A full Android OS running on the car's own computer. No phone needed. The car
maker ships it, like a phone maker ships Android.

**Q. Android Auto vs AAOS?**

Android Auto: the app runs on the **phone** and draws on the car screen.
AAOS: the app is installed **in the car**. Same Car App Library, so the screen
code looks the same. Everything else differs.

**Q. Explain the AAOS architecture.**

Four layers, top to bottom:

```
Apps            your app, Car Launcher, Car SystemUI
Car API         CarPropertyManager, CarAudioManager...
CarService      Java system service, ~39 services inside it
Vehicle HAL     C++, talks to the car hardware
CAN bus         the actual vehicle network
```

★ You can add: a property read is **two binder hops** — app → CarPropertyService → VehicleHal → VHAL process. That's why latency in AAOS is usually binder latency.

**Q. How does AAOS differ from normal Android?**

Three additions: the Vehicle HAL layer, CarService and the Car APIs, and a
replaced UI layer (Car SystemUI, Car Launcher). Plus it is multi-user by
default and has driver distraction rules.

**Q. What is CarService?**

The bridge between apps and the vehicle. It is Java, runs as a privileged
system app, and hosts many services — property, audio, power, occupant zones,
watchdog, garage mode.

★ Check it yourself: `adb shell dumpsys car_service --list` shows 39.

**Q. What is the Vehicle HAL?**

The layer that hides the car's hardware. It exposes vehicle state as numbered
**properties** — speed, gear, fuel. Android only sees properties. The VHAL
translates them to and from CAN.

**Q. What is AGL and how does it compare?**

Automotive Grade Linux — a Linux-based automotive platform. Same purpose,
different trade-off: more OEM control, but no Play Store, no Android app
ecosystem, and you build the app framework yourself.

**Q. AAOS vs embedded Linux for infotainment?**

Android gives you a ready app ecosystem, a standard UI framework, Google
services and normal Android tooling. Embedded Linux gives more control and a
smaller footprint. The trade is ecosystem versus control.

**Q. What is Project Treble and why does it matter here?**

It separates vendor code from the Android framework, so the framework can be
updated without redoing the vendor's hardware code. For automotive it is what
makes the VHAL a swappable HAL with a versioned interface.

★ You can show it: `/vendor/etc/vintf/manifest/vhal-emulator-service.xml`
declares `format="aidl"`, version 3.

**Q. What is a digital cockpit?**

All the screens as one system — centre display, instrument cluster, sometimes
passenger and rear screens — often driven by one SoC. It is why AAOS is
multi-display and why "which display?" is a real question.

---

## 2. Vehicle data

**Q. How does an app read car data?**

```kotlin
val car = Car.createCar(context)
val pm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
val speed = pm.getProperty<Any>(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)
```

**Q. What is the VehicleProperty API?**

The standard list of vehicle properties with fixed IDs, types and units, so
the same code works across car makers. `PERF_VEHICLE_SPEED` means the same
thing in any AAOS car.

**Q. `getProperty` or `registerCallback`?**

`registerCallback` for anything that changes. The car pushes events to you.

★ You have the strong version: polling added up to a second of latency to an
overspeed warning, and injected test events only reach subscribers — so
polling code cannot see test data at all.

**Q. What are the property change modes?**

- **STATIC** — never changes (VIN)
- **ON_CHANGE** — only on change (gear, ignition)
- **CONTINUOUS** — sampled at a rate (speed, rpm)

You subscribe to CONTINUOUS with a rate; ON_CHANGE uses `SENSOR_RATE_ONCHANGE`.

★ Speed and rpm cap at 10 Hz on the emulator, fuel and battery at 100 Hz.

**Q. My property read returns null. Why?**

Check in this order:

1. Declared in the manifest?
2. Actually granted? A missing permission makes the property look
   **unsupported** — it does not throw.
3. Right protection level? `dumpsys package permissions`, field `prot=`.
4. Right type and area? `cmd car_service get-carpropertyconfig`.

★ You hit all four.

**Q. Any unit traps?**

Yes, and guessing is fatal:

- speed — FLOAT, **metres per second**
- fuel level — FLOAT, **millilitres**
- EV battery — FLOAT, **Wh**
- odometer — FLOAT, already **km**
- seatbelt — BOOLEAN **per seat**, not a global flag

★ All five were wrong in your code first.

**Q. How would you add a property the standard VHAL doesn't have?**

Use the **vendor group**, `0x20000000` — the OEM's own namespace. The ID is a
bit field: group | area | type | number. So
`0x21400001` = vendor + global + INT32 + 1.

★ You added three, and no C++ was needed — the reference VHAL loads JSON from
`/vendor/etc/automotive/vhalconfig/`.

**Q. How would you simulate a property the emulator doesn't have?**

Two ways. Add it to the VHAL's JSON config (no C++). Or write a mock service
behind your own interface, so the app can be developed before the real signal
exists — which is normal in automotive, because the vehicle signal often
arrives later than the app.

**Q. How do you test vehicle data with no car?**

```bash
adb shell cmd car_service get-carpropertyconfig PERF_VEHICLE_SPEED
adb shell cmd car_service inject-continuous-events 291504647 31 -s 5 -d 180
adb shell dumpsys car_service --services CarDrivingStateService
```

★ Trap you found: a single `inject-vhal-event` does not stick for CONTINUOUS
properties — the VHAL's generator overwrites it.

**Q. What is the CAN bus and how does it relate to Android?**

A broadcast vehicle network. Frames have an ID and up to 8 bytes; signals are
packed into those bytes, described by a DBC file. It **never touches Android
directly** — something below the VHAL reads CAN and turns a signal into a
property.

**Q. Where does AUTOSAR fit?**

A different world: the standard for deeply embedded ECUs doing real-time
control — engine, brakes, body. Android is the cockpit. They meet at a
gateway: a signal from an AUTOSAR ECU travels over CAN and appears in Android
as a VHAL property.

**Q. Can an app write vehicle data?**

Sometimes. A property's config says READ, WRITE or READ_WRITE, and writes need
their own permission. Safety-critical actuation is not exposed to apps at all.

★ You wrote one — a vendor drive-mode property — and hit the Kotlin boxing
trap: use `Int::class.javaObjectType`, not `Int::class.java`.

---

## 3. Permissions and system apps

**Q. What are the car permission levels?**

| Level | Example | How you get it |
|---|---|---|
| `normal` | `CAR_INFO`, `CAR_POWERTRAIN` | automatic at install |
| `dangerous` | `CAR_SPEED`, `CAR_ENERGY` | ask at runtime, like location |
| `signature\|privileged` | `CAR_ENGINE_DETAILED`, `CAR_MILEAGE`, `CAR_IDENTIFICATION` | system app only |

★ The middle row surprises people. Speed and fuel are ordinary runtime
permissions — you assumed they were locked and were wrong.

**Q. Why can't a normal app read RPM or the VIN?**

They are `signature|privileged`. Since Android 9 a privileged app must be
**both** in `/system/priv-app` **and** listed in a `privapp-permissions` XML.
Only whoever builds the system image can do that.

**Q. So how would you build an instrument cluster?**

As a privileged system app shipped in the image, not from the Play Store. Four
steps, each silent if missed: boot writable, push to `/system/priv-app` mode
644, install the allowlist XML, reboot (priv-app is scanned only at boot).

★ You did it, and found that afterwards a normal install keeps the privileged
permissions — the package becomes an *updated system app*.

**Q. Why is AAOS multi-user, and what breaks?**

The system boots user 0; the driver is a separate user, normally **10**.
Anything user-scoped silently targets the wrong user if you forget:

- `pm grant` defaults to user 0 while the app runs as 10
- `/sdcard` is user 0's; the driver's storage is `/data/media/10`

★ This cost you a wrong conclusion twice.

**Q. How would you do user authentication in a car?**

Use the multi-user framework — driver profiles are real users. Add PIN or
biometrics if the hardware has it. The car-specific part: profiles can be tied
to a key fob or phone, and switching driver must not interrupt playback or
navigation.

**Q. What is sepolicy and when do you touch it?**

SELinux rules saying which process may touch which file or service. A new HAL
or system service needs policy or it will not start. Denials appear in logcat
as `avc: denied { ... } scontext= tcontext= tclass=` — reading those four
fields is the skill.

---

## 4. Media and audio

**Q. How does media playback work on AAOS?**

`MediaBrowserService` exposes your library so the car's media UI can browse
it, `MediaSession` exposes playback state and controls, and you handle audio
focus. The car's UI drives your app; you do not draw the player.

**Q. How would you integrate a third-party streaming service?**

Implement `MediaBrowserServiceCompat` for browsing, `MediaSessionCompat` for
controls, handle audio focus properly, support voice search, and keep playing
in the background. Then declare the media category.

**Q. What is audio focus and why does it matter more in a car?**

It decides who is allowed to play. In a car, navigation prompts and warnings
must interrupt music — so you must duck or pause when you lose focus, and
resume correctly. Getting it wrong means a driver misses a turn instruction.

**Q. What are car audio zones?**

Separate outputs per occupant area, so the driver and rear seats can play
different things. Each zone has volume groups. The audio policy routes a
stream to a zone based on its `AudioAttributes.USAGE_*`.

★ Your emulator really has two: `primary zone:0` and `rear seat zone 1:1`.

**Q. Can your app control multi-zone audio?**

No. Zones and volume groups are `@SystemApi`, not in the SDK jar, and the
permissions are `signature|privileged`. Even `registerCarVolumeCallback`
throws. From a framework role it is the job; from an app the only lever is
`AudioAttributes.USAGE_*`.

**Q. Media plays through the wrong speakers. Debug it.**

Routing comes from `AudioAttributes.USAGE_*` plus the zone config, so check
whether the app labels its stream or the config is wrong:

```bash
adb shell dumpsys car_service --services CarAudioService
adb shell dumpsys audio
```

★ Suspect the app first — you found `ExoPlayer.Builder().build()` with **no**
AudioAttributes set at all, so the stream took a default route silently.

---

## 5. UI and driver distraction

**Q. What is driver distraction optimisation?**

Rules limiting what the driver can do while moving — short text, limited list
items, no video, no free scrolling, no text entry. Roughly above 5 km/h,
complex interaction is not allowed.

**Q. Why templates instead of normal layouts?**

The car maker's host renders your UI, not you. You describe what you want and
the host draws it in its own style and enforces the limits. So every app looks
consistent and nothing unsafe can ship by accident.

**Q. Name the templates you'd use.**

`ListTemplate`, `GridTemplate` (icon grids, good for gauges), `PaneTemplate`,
`TabTemplate` (top-level tabs), `MessageTemplate`, `PlaceListMapTemplate`,
`NavigationTemplate` / `MapTemplate` (needs a drawn surface).

**Q. How do you design UI for a car?**

Big touch targets, few items, high contrast for glare, short text, voice where
possible, and everything reachable in one or two steps. Assume the driver
looks for under two seconds.

**Q. How do you handle different screen sizes and resolutions?**

Use `dp` and vector graphics, let templates lay themselves out, and never
hardcode pixels. Screens range from small portrait to wide landscape, and the
cluster is a different size again.

★ You measured a real one: the host reserves **121 dp** below list and grid
templates, density-invariant, and it cannot be removed from the app.

**Q. What is CarUi / Car System UI / Car Launcher?**

`car-ui-lib` gives OEM-themable components. `CarSystemUI` replaces phone
SystemUI — system bars, HVAC panel, notifications. `CarLauncher` is the home
screen. OEMs customise them, usually with **RRO** overlay APKs rather than
forking.

**Q. How do you handle localisation?**

String resources per language, RTL support, locale-specific formats — and the
car-specific part: units. Speed and distance must follow the car's display
units, not just the locale.

**Q. How would you gate the UI on driver distraction?**

`CarUxRestrictionsManager` — but verify it works on your hardware first.

★ On yours it did not: `getCarManager` returned null at connect and needed a
retry, and it reported the **cluster display's** restrictions (511) while the
main display was unrestricted (0), sending no events. So you gated on motion
instead and documented why.

---

## 6. AOSP internals

**Q. What happens during Android boot?**

```
bootloader -> kernel -> init (stage 1: mount partitions, load SELinux)
           -> init (stage 2: parse .rc files, start property service,
                    start servicemanager, surfaceflinger, logd)
           -> Zygote -> SystemServer -> (AAOS) CarService -> launcher
```

Automotive difference: boot time is a requirement, not a nicety — the driver
expects a screen almost immediately, and the reversing camera must work early.

**Q. What is Zygote?**

A process that preloads the framework classes, then forks to create every app
process. Forking is why app startup is fast — the framework is already in
memory and shared.

**Q. What is init.rc?**

The script init parses to define services and actions — what to start, as
which user, in which order. A HAL like the VHAL is declared there:

```
service vendor.vehicle-hal-emulator /vendor/bin/hw/android...vehicle@V3-emulator-service
    class early_hal
    user vehicle_network
```

★ Note `class early_hal` — it starts before normal HALs, because the framework
needs vehicle data early.

**Q. What is Binder?**

Android's IPC mechanism. Processes talk through the kernel binder driver with
proxy and stub objects, so a call looks like a normal method call. Everything
in AAOS crosses it — CarService, the VHAL, the templates host.

**Q. HIDL vs AIDL for HALs?**

HIDL came with Treble to separate vendor code; it had its own IDL, its own
binder domain and its own service manager. **Stable AIDL replaced it** from
Android 11 — same IDL as apps, versioned interfaces, normal binder. New HALs,
including the automotive VHAL, are AIDL.

**Q. What is `oneway` in AIDL and when do you use it?**

The call returns immediately without waiting for the other side. Use it when
the caller must not block.

★ You used it because your service broadcasts from the VHAL event thread — a
slow client blocking there would stall the property stream for everyone.

**Q. How do you add a new system service in AOSP?**

Define the AIDL interface, implement the service, register it with
`servicemanager`, add sepolicy so clients may bind, and declare it in init if
it is its own process. For car features it usually goes inside CarService
instead of being a new process.

**Q. How is CarService updated separately from the platform?**

It is packaged as updatable car framework code, similar in idea to mainline
modules, so car fixes ship without a full platform OTA.

★ It is why car permissions show `sourcePackage=com.android.car.updatable`.

---

## 7. Power, updates, performance

**Q. How does power management work in a car?**

The car tells Android about power state through the VHAL, and
`CarPowerManagementService` handles the transitions — on, shutdown prepare,
suspend, resume. The key difference from a phone: the user turns the vehicle
off abruptly, and the system must save state and resume fast.

**Q. What is Garage Mode?**

Deferred work — updates, idle jobs — that runs **after the car is switched
off** while power is still available, so it never competes with the driver.
`adb shell cmd car_service garage-mode on|query`.

**Q. What is CarWatchdog?**

It kills or reports processes that stop responding or abuse I/O. A car cannot
show an ANR dialog at 100 km/h, so the platform is more aggressive than a
phone. Your service must answer its health checks.

**Q. How do OTA updates work in a car?**

A/B (seamless) updates: two slots, write the inactive one, switch on reboot,
roll back if boot fails. In a car, add staged rollout per region and VIN, a
requirement that the vehicle is parked and charged, and update work in Garage
Mode. A bricked head unit is a workshop visit, so rollback matters more.

**Q. How do you debug a performance problem?**

Three tools, three questions: `logcat` says what happened, `dumpsys` says the
state now, **perfetto** says who ran when and for how long. Only perfetto
explains a stall.

★ You found a real one: after installing to `/system/priv-app` by hand,
startup regressed ~40%, because a pushed APK never gets dexopt
(`status=run-from-apk`) and ran interpreted. `cmd package compile -m speed -f`
fixed it — `bindApplication` 366 → 148 ms.

**Q. What's the classic mistake reading a profiler trace?**

Fixing the biggest slice. ★ In your trace the two largest were ~900 ms of
emoji font loading that block **nothing** — `is_main_thread = 0`. Total time
is not blocking time; filter on the main thread first.

**Q. How do you keep the system responsive during critical operations?**

Keep work off the main thread, never block on binder calls to services that
may be busy, respect the watchdog's health checks, and give safety-relevant
paths — cluster, camera — priority over infotainment.

**Q. How do you manage concurrency?**

`HandlerThread` or executors for background work, immutable snapshots passed
between threads, and `@Volatile` where a value is written on one thread and
read on another.

★ Your repository does exactly that: a snapshot written from a binder thread,
read from the main thread, with listeners dispatched to the main thread.

---

## 8. Testing, safety and process

**Q. What are the challenges in testing automotive apps?**

You do not have the car. Signals are hard to reproduce, timing matters, there
are several displays and users, and compliance has to be demonstrated, not
assumed.

**Q. What is MIL / SIL / HIL?**

Stages with progressively more real hardware:

- **MIL** model on a workstation
- **SIL** real software, simulated vehicle — *an AAOS emulator is SIL*
- **HIL** real ECU hardware, simulated vehicle around it

Then bench, then vehicle. ★ Everything you did is SIL — say so plainly.

**Q. How would you debug an issue that only happens in a real vehicle?**

Get logs off the car first — a bug report or persistent logging, because you
cannot attach a debugger while driving. Then reproduce on a HIL rig by
replaying the CAN trace. Work with the integration team; the cause is often a
signal that behaves differently on the real bus.

**Q. What ASIL is infotainment?**

Usually **none — QM (Quality Management)**. ISO 26262 rates by what happens
when it fails, and a media app failing injures nobody.

But parts of the cockpit are safety-relevant: cluster telltales, warning
lamps, the reversing camera. Those can be ASIL A/B, which is why the cluster
is often a separate ECU — and often not Android.

The mature answer: *"the IVI is generally QM; the safety case lives in the
cluster and camera paths, and my job is to not compromise them."*

**Q. What is ASPICE?**

A process maturity rating for how you develop software, not what it does. OEMs
require suppliers to be assessed, so it is contractual. Day to day it means
requirements traced to design, code and tests, and recorded review evidence.

**Q. What are the safety considerations when building infotainment?**

Minimise distraction, never obscure safety information, fail safe (a crash in
media must not affect driving), protect personal data, and meet the region's
regulations.

**Q. How do you handle vehicle data privacy?**

Vehicle data is personal — location, driving behaviour. Ask consent, take the
minimum, keep it local where possible, gate it behind permissions, and be
clear about what leaves the car.

**Q. What about secure boot and encryption?**

A chain of trust from the bootloader up, so only signed images run, plus file
based encryption for user data. In a car it also protects against physical
access, since the hardware is reachable.

---

## 9. Your project

Guaranteed questions. Rehearse these until boring.

**Q. Walk me through your architecture.**

One-way push, no polling:

```
VHAL --registerCallback--> VehicleHalManager (units, caching, null if unreadable)
     --IVehicleDataCallback, oneway AIDL--> VehicleDataService
     --> VehicleRepository --> VehicleViewModel --> Screens
```

Four decisions to defend: a separate service behind AIDL because real vehicle
data comes from a system service; `oneway` because the broadcast runs on the
VHAL event thread; every getter nullable so a missing permission degrades
instead of crashing; unit conversion only at the HAL boundary.

**Q. Why did you remove the polling?**

Three 1 Hz loops, one doing six binder round trips a second. But the real
reason is correctness: speed is CONTINUOUS and already pushed at 10 Hz, so
polling added latency to an overspeed warning — and injected test events never
reached polling code at all.

**Q. How did you test it?**

Emulator plus `cmd car_service` for configs, injection and driving state;
`dumpsys` for platform state; logcat for behaviour; perfetto for timing. Plus
a Simulation screen, because six simulate helpers existed with no call sites —
the whole alert path was unreachable from the UI.

**Q. What would you do differently?**

Extract a `:shared` module. ★ Right now `MusicData.kt` and
`LocalSongLoader.kt` are copy-pasted into both app modules and have already
drifted 20 and 48 lines — which is the argument for shared code, measured.

Also split into one category per module, and check protection levels before
assuming the platform is closed.

**Q. What is the weakest part?**

The UI is a demo: it mixes app categories, and a dashboard is not a
third-party category at all. The Android Auto module is untested — that needs
a physical phone with DHU. And it is an emulator, so timing numbers are
indicative.

**Q. Tell me about a time you were wrong.**

The strongest thing you have. The empty band under list screens: you blamed
leftover rows, then a host crop. Four probes and a density test disproved both
— it is a 121 dp host reserve.

And gauge labels reading "ry 80%": you blamed host cropping and added a
scaling workaround; instrumentation showed the workaround did nothing. The real
cause was `Align.CENTER` text paint with a left-edge x. One line.

The point to land: measure before concluding, and prefer the test that can
disprove you.

---

## 10. HMI and System UI

The biggest theme in the JDs, and your weakest area. Study these.

**Q. What is Car System UI and what would you change in it?**

`CarSystemUI` replaces phone SystemUI. It owns the system bars (status and
nav become the car's top and bottom bars), the HVAC panel, notifications,
volume UI and the keyguard. OEMs customise it heavily — it is most of what
makes a car look like that brand.

**Q. How do OEMs customise it without forking?**

**RROs** — runtime resource overlays. An APK with **no code** that replaces
another package's resources at runtime, so the OEM's branding survives Android
upgrades without maintaining a fork.

★ You wrote one. It targets `com.android.systemui`, replaces six colours, and
changed the system bar accent from blue to teal. Three things you learned
doing it that a reader would not know:

**1. Resources match by name, so guessing fails silently.** The reliable way
to find valid names is to read an overlay that already ships:

```bash
adb pull /product/overlay/googlecarui.theme.orange-com-android-systemui.apk
aapt2 dump resources ...   # 163 resources the stock Google theme replaces
```

**2. Enable it for the user the *target* runs as.** CarSystemUI is `u0_a223` —
user 0 — even though the driver is user 10. Enabling for 10 fails with
`SecurityException: Unable to retrieve overlay information`. Car Launcher is
the opposite, user 10.

**3. A third-party overlay cannot touch CarSystemUI at all.** The install
error names the only three routes:

> *signed with different certificates, and the overlay lacks
> `<overlay android:targetName>`*

So: same certificate as the target, or the target declares
`<overlayable name="...">` and you name it, or you are preinstalled in a
trusted partition. `aapt2 dump overlayable CarSystemUI.apk` prints nothing, so
route two is closed — it is not built to be overlaid by third parties.

You got through because the emulator is a `test-keys` build, so its platform
certificate is the public AOSP one; signing with `platform.pk8` satisfies
route one. On a production car that key is the OEM's secret and the overlay
ships inside the system image.

**Q. So do you need an AOSP build to do System UI work?**

For **appearance**, no — an emulator, `aapt2` and a signing key is enough, and
that is most of day-to-day OEM HMI work. For **behaviour** — changing what
CarSystemUI does, not how it looks — yes, because that is Java inside a
platform-signed system app.

★ This is a good answer to give, because most candidates assume the whole area
needs Linux and a platform build.

**Q. What is Car Launcher?**

The home screen — typically a maps card, a media card and app shortcuts. It is
an AOSP app (`CarLauncher`) that OEMs replace or restyle. It also hosts the
"app grid" and often the first screen a driver sees after ignition, so its
startup time is scrutinised.

**Q. What is car-ui-lib?**

The shared component library (`CarUi`) that car apps and system apps use —
toolbars, lists, preferences — so every app can be themed consistently by the
OEM through one overlay target instead of app by app.

**Q. Which framework services would you touch doing System UI work?**

The standard Android ones, not car-specific:

- **ActivityManager** — what is running, task and process state
- **WindowManager** — windows, z-order, which display a window is on
- **PackageManager** — installed apps, resolving intents, querying components
- **LauncherApps** — the launcher's view of installed apps and shortcuts

The car twist is **multi-display**: windows and activities are display-scoped,
so "which display" is part of almost every question.

**Q. How would you show an app on the cluster display?**

Launch it with `ActivityOptions.setLaunchDisplayId()`, and the display must be
one the occupant zone allows. AAOS also has `ClusterHomeService` and
`FixedActivityService` for activities pinned to a display.

★ You can show real display topology:
`dumpsys car_service --services CarOccupantZoneService` — display 0 MAIN,
display 3 CLUSTER on your emulator.

**Q. Is Jetpack Compose used in AAOS?**

Increasingly yes for system apps and OEM HMI, where you own the whole
Activity. But **not** for Car App Library apps — those are templates rendered
by the host, so there is no view tree of yours to compose.

**Q. What are the HMI performance constraints?**

Time to first frame from ignition, and no dropped frames while driving. So:
nothing blocking on the main thread during startup, no heavy work in
`onCreate`, and watch memory because the head unit has far less than a modern
phone and is shared with the cluster and camera.

★ You have a measured example — the dexopt regression that cost 40% of
main-thread startup work.

---

## 11. Build system and native debugging

Named in the deepest JD. You have 41 `Android.bp` files locally — read a few
and these become answerable.

**Q. What is Soong, and how does it differ from Make?**

Soong is AOSP's build system. You write declarative `Android.bp` files in
**Blueprint** format instead of imperative `Android.mk` Makefiles. Soong
generates Ninja files and builds. Legacy `.mk` files still exist in older
trees, which is why both appear in job specs.

**Q. What does an Android.bp module look like?**

```
cc_library {
    name: "libmylib",
    srcs: ["src/Foo.cpp"],
    shared_libs: ["libbase", "liblog"],
    vendor: true,
}
```

Declarative: module type, name, sources, dependencies. No commands, no
conditionals — which is the point, because it can be analysed.

Module types worth knowing: `cc_library`, `cc_binary`,
`android_app`, `java_library`, `aidl_interface`, `prebuilt_etc`.

**Q. How is an AIDL HAL interface declared to the build?**

An `aidl_interface` module with versions frozen under `aidl_api/`. Freezing is
what makes a HAL interface stable — once frozen, changing it is a versioned,
deliberate act rather than an edit.

**Q. What is a tombstone and how do you read one?**

A file written when a native process crashes, in `/data/tombstones/`. It has
the signal (SIGSEGV, SIGABRT), the faulting address, registers, and a
backtrace. You symbolise the backtrace against the build's unstripped symbols
to get function names.

`adb shell ls /data/tombstones/`, and `adb bugreport` collects them.

**Q. How do you debug an ANR?**

An ANR means the main thread did not respond in time. The system writes traces
to `/data/anr/traces.txt`. Read the main thread's stack: it is usually blocked
on a lock, on disk I/O, or on a **binder call to a busy service** — which is
the common one in AAOS, because so much is cross-process.

**Q. Logcat, tombstone, ANR trace, Perfetto — when do you use which?**

| Symptom | Tool |
|---|---|
| wrong behaviour | logcat |
| Java exception | logcat |
| native crash | tombstone |
| app frozen, "not responding" | ANR trace |
| slow, janky, but working | Perfetto |
| current state of a service | dumpsys |

★ You can speak to logcat, dumpsys and Perfetto from real use.

---

## 12. Test automation

A separate track (HARMAN's JD), and adjacent to what you already do.

**Q. How would you automate testing for an AAOS app?**

UIAutomator or Appium for UI, driven from Python or Kotlin, running on an AAOS
emulator in CI. The automotive-specific part: you must also drive the
**vehicle state**, so the test harness injects VHAL properties to put the car
in a state before asserting the UI.

★ You already do the second half by hand:
`cmd car_service inject-continuous-events` is exactly what such a harness
wraps.

**Q. What is hard about automating automotive UI?**

The UI is drawn by the host, not your app, so your view hierarchy is not
directly under test for template apps. Multiple displays. And driving state
changes what is even permitted, so tests must control speed and gear to test
both parked and moving behaviour.

**Q. How would you put this in CI?**

Jenkins or GitHub Actions, a headless AAOS emulator, `gradlew assemble` then
install, then run the suite; publish results and traces as artifacts. Boot the
emulator with a known snapshot so vehicle state is deterministic.

---

## 13. Android Auto

**Q. Your app works on AAOS. What changes for Android Auto?**

The templates barely change. What changes: the app runs on the phone, so there
are **no `android.car.*` permissions and no vehicle data**; a different
artifact (`app` vs `app-automotive`); the session lives and dies with the
phone connection; and you test with DHU, not an emulator.

**Q. Why is there no vehicle data on Android Auto?**

Your process is on the phone. There is no VHAL on your side, and projection
does not expose vehicle properties. If a feature needs speed, it must be AAOS.

**Q. How do you structure one codebase for both?**

Two application modules that do **not** depend on each other — they are
separate apps — plus a `:shared` library for models and logic.

**Q. What is HostValidator?**

Your `CarAppService` is bound by a host; a malicious app could pretend to be
one. `HostValidator` checks the host's certificate. Production hosts:
`com.google.android.projection.gearhead`, and
`com.google.android.autosimulator` for DHU. The trap is shipping the
permissive development validator.

**Q. How does publishing differ?**

Google **manually reviews** Android Auto apps for driver safety and must
explicitly approve them — one to three weeks. Reviewers check template
compliance, driving restrictions and voice support. Exceeding a template's
item limit is a rejection, not a warning.

---

## 14. Ask them

These tell you which job it actually is, which matters more than anything:

- Is this app layer, CarService and framework, or VHAL and below?
- Is your VHAL in-house C++ or from a supplier? How much CAN work is yours?
- Do you carry your own privapp-permissions and sepolicy?
- How do you test — hardware, bench, emulator, or virtualised VHAL?
- Which AOSP version, and how far do you diverge from upstream?
- Is the cluster driven by AAOS, or a separate ECU?
- Is ASPICE applied here, and what does it mean day to day?

---

## What 10 Bengaluru JDs actually ask for

Counted across the ten job descriptions you collected. This is the real
demand signal, and it changes the priorities.

| Skill | In how many | Note |
|---|---|---|
| **Java + Kotlin** | 9 / 10 | universal. Kotlin-only reads as an app developer |
| **AOSP / AAOS** | 8 / 10 | named even in app-level roles |
| **HMI / System UI** | **5 / 10** | the single biggest theme |
| ADB / Logcat debugging | 6 / 10 | always listed, never optional |
| Binder IPC / AIDL | 4 / 10 | |
| Car Service / Car APIs | 3 / 10 | fewer than you would expect |
| C++ | 3 / 10 | only in deep platform and audio roles |
| Linux fundamentals | 3 / 10 | processes, memory, synchronisation, IPC |
| Audio / multimedia stack | 2 / 10 | specialist track |
| Perfetto / Systrace | 1 / 10 | but in the deepest role |
| Soong / Blueprint / Make | 1 / 10 | same role |
| Jetpack Compose | 1 / 10 | rising |
| Python + test automation | 1 / 10 | separate track |
| **Android Automotive Emulator** | 1 / 10 | **explicitly named — you have real depth here** |

### Three things this changes

**1. HMI and System UI is the job, not VHAL.** Five of ten. Car Service
appears in only three. You have gone deep on exactly the part the market asks
for least, and barely touched the part it asks for most.

**2. Deep framework experience is often not required.** The Ford HMI role says
it outright: *"Exposure to Android Framework concepts is desirable, but deep
framework development experience is not mandatory."* That is your best entry
point — it wants strong Android UI plus HMI, which is closest to what you
already are.

**3. C++ really is not the blocker.** Three of ten, and both are specialist
roles (platform/HAL, and audio with ALSA and OMX). Confirms that C++ is a
later investment, not a gate.

### The five roles these JDs actually describe

| Role | What they want | Your fit |
|---|---|---|
| **HMI / IVI HMI** | Android UI, HMI implementation, Kotlin/Java, MVVM, Compose | **closest fit** — needs System UI exposure |
| **AAOS System UI / framework** | Car Service, ActivityManager, WindowManager, PackageManager, Launcher, Notifications | reachable, needs study |
| **Platform / HAL** | C++ and Java expert, HAL, ART, init/Zygote/boot, AIDL+HIDL, Soong, tombstones | not yet |
| **Audio / multimedia** | AudioFlinger, Audio HAL, ALSA, OMX, Stagefright, C/C++ | not yet |
| **Test automation** | Kotlin/Java + Python, Appium/UIAutomator, Jenkins, CI/CD | adjacent — you have the debugging half |

### Revised priorities

1. **Car System UI and Car Launcher** — was #2 in your gaps, is now clearly #1.
   Five of ten JDs. Read the AOSP packages, learn how RROs customise them.
2. **Standard Android system services** — `ActivityManager`, `WindowManager`,
   `PackageManager`, `LauncherApps`. Named explicitly, and none of it needs an
   AOSP build to study.
3. **Soong and Blueprint** — free right now. You already have **41
   `Android.bp` files** in `~/Documents/AndroidProjects/aosp-reference/`.
   Read a `cc_library` and an `aidl_interface` module and you can speak to it.
4. **Native debugging** — tombstones, ANR analysis, native crash. Listed
   beside Perfetto, which you now have.
5. **Jetpack Compose** — appearing in AAOS JDs now.
6. C++, audio stack — later, role-dependent.

---

## Which job you're interviewing for

Three different jobs, three different interviews:

| | App on AAOS | **Framework / platform** | HAL / BSP |
|---|---|---|---|
| You write | Car App Library apps | CarService, Car SystemUI, Car Launcher | VHAL, CAN, board bring-up |
| Language | Kotlin | **Java + Kotlin** | **C++** |
| Jobs | few | **most** | specialist |

Most postings are the middle one. Your project is mostly the first, but the
platform half — priv-app install, allowlists, vendor properties, AIDL — is
genuinely the middle. Lead with that.

## Interview rounds

**KPIT** — 5 stages: application, recruiter screen, **assessment (C/C++,
embedded basics, aptitude)**, technical domain round, manager + HR. Note the
C/C++ test comes *before* the Android round.

**Tata Elxsi** — 4 rounds: telephonic technical, written (technical +
reasoning + verbal), managerial, HR. DSA and design patterns are live.

**Consultancies (Luxoft-style)** — fewer, deeper rounds, no aptitude test.
Their spec names `adb`, `systrace`, `dumpsys`, `logcat` explicitly, so expect
a "here is a symptom, find it" discussion.

## Sources

- Anand Gaur, AAOS interview questions — https://www.linkedin.com/pulse/android-automotive-os-interview-questions-andanswers-anand-gaur-ensef
- KPIT interview process — https://hyring.com/jobseeker-toolkit/interview-questions/company/kpit-technologies
- Luxoft AAOS framework role — https://career.luxoft.com/jobs/senior-android-framework-developer-android-automotive-audio-media-experts-18803
- Tata Elxsi process — https://www.glassdoor.co.in/Interview/TATA-ELXSI-Software-Engineer-Interview-Questions-EI_IE115262.0,10_KO11,28.htm
- ISO 26262 / ASIL — https://www.jamasoftware.com/requirements-management-guide/automotive-engineering/asil/
- AOSP boot and init — https://aospbooks.github.io/aosp-internal-book/04-boot-and-init/
- Binder IPC, HIDL/AIDL — https://source.android.com/docs/core/architecture/hidl/binder-ipc
