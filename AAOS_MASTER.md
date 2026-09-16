# AAOS Master Guide — Concepts and Interview Questions

One file. Everything in order, from the simplest idea to the deepest one.
Simple English, short answers, and a memory hook where the idea is hard.

## Contents

| Part | Topic | Questions |
|---|---|---|
| [The map](#the-map--learn-this-before-anything-else) | the one picture everything hangs off | — |
| [1](#part-1--what-aaos-is) | What AAOS is | Q1-Q12 |
| [2](#part-2--car-app-library-and-templates) | Car App Library and templates | Q13-Q24 |
| [3](#part-3--vehicle-data) | Vehicle data | Q25-Q39 |
| [4](#part-4--permissions-and-system-apps) | Permissions and system apps | Q40-Q47 |
| [5](#part-5--media-and-audio) | Media and audio | Q48-Q55 |
| [6](#part-6--driver-distraction-and-car-ui) | Driver distraction and car UI | Q56-Q64 |
| [7](#part-7--app-architecture-services-aidl-threading) | Architecture, AIDL, threading | Q65-Q74 |
| [8](#part-8--aosp-internals) | AOSP internals | Q75-Q84 |
| [9](#part-9--hmi-and-car-system-ui) | **HMI and Car System UI** | Q85-Q93 |
| [10](#part-10--power-updates-and-performance) | Power, updates, performance | Q94-Q102 |
| [11](#part-11--build-system-and-native-debugging) | Build system, native debugging | Q103-Q109 |
| [12](#part-12--testing-safety-and-process) | Testing, safety, process | Q110-Q119 |
| [13](#part-13--test-automation) | Test automation | Q120-Q122 |
| [14](#part-14--android-auto) | Android Auto | Q123-Q127 |
| [15](#part-15--your-project) | Your project | Q128-Q133 |
| [16](#part-16--questions-to-ask-them) | Questions to ask them | — |
| [A](#appendix-a--corrections-to-my-old-pdf-notes) | **Corrections to my old PDF notes** | — |
| [B](#appendix-b--command-cheat-sheet) | Command cheat sheet | — |
| [C](#appendix-c--numbers-worth-memorising) | Numbers worth memorising | — |
| [D](#appendix-d--what-10-bengaluru-job-descriptions-actually-asked-for) | What 10 Bengaluru JDs asked for | — |
| [E](#appendix-e--how-to-revise-this) | How to revise this | — |

Part 9 and Appendix A are marked because they matter most: Part 9 is the most
requested skill in the job market, and Appendix A is the list of things you
would otherwise say confidently and wrongly.

## How to read this

- Questions are numbered **Q1 to Q133**, in learning order. Part 1 needs no
  background. Each part uses what came before it. Do not jump.
- **Answer** — say this in the interview. Nothing extra.
- **Remember** — a one-line hook so the idea stays in your head.
- **Your proof** — you did this yourself and can show it. These are your
  strongest answers. Always give the concrete version.
- **Careful** — a common wrong belief. Several of these were in my own older
  PDF notes. See [Appendix A](#appendix-a--corrections-to-my-old-pdf-notes).

## The one rule for every answer

KPIT publishes their scoring line: *"No generic software answers — automotive
application is mandatory."*

So every answer must mention the car — the signal, the driver, the safety
case, or the constraint.

Weak: *"I read a value from a service."*
Strong: *"Speed arrives from the VHAL as a float in metres per second, pushed
at 10 Hz, and I warn the driver above 80 km/h."*

> **Remember:** no car in the answer = no mark.

## The map — learn this before anything else

Everything in this document sits somewhere on this picture.

```
  YOUR APP            (templates, media, your UI)
      |  Car API      CarPropertyManager, CarAudioManager...
  CAR SERVICE         Java system service, 39 services inside
      |  AIDL / binder
  VEHICLE HAL         C++ process, turns hardware into "properties"
      |
  CAN BUS             the real vehicle network, ECUs
```

Four things to fix in your mind:

1. Android never sees the car's wires. It only sees **properties** — numbered
   values like speed, gear, fuel.
2. The **VHAL** is the translator between properties and the car.
3. **CarService** is the gatekeeper. Apps talk to it, not to the VHAL.
4. So one property read is **two binder hops**: app -> CarService -> VHAL.
   That is why slowness in AAOS is usually binder slowness.

---

# PART 1 — What AAOS is

**Q1. What is Android Automotive OS?**

**Answer:** A full Android operating system running on the car's own computer.
No phone is involved. The car maker ships it in the vehicle, the same way a
phone maker ships Android on a phone.

> **Remember:** AAOS = Android **is** the car.

**Q2. What is Android Auto?**

**Answer:** Projection. The app runs on the **phone** and only draws its
screen on the car's display. The car is a monitor with touch.

> **Remember:** Android Auto = Android **on the phone**, borrowing the car
> screen.

**Q3. AAOS vs Android Auto — what actually differs for a developer?**

**Answer:** The screen code is almost the same, because both use the Car App
Library templates. Everything else differs.

| | AAOS | Android Auto |
|---|---|---|
| App runs on | the car | the phone |
| Vehicle data | yes, via VHAL | **no** |
| `android.car.*` permissions | yes | do not exist |
| Install | in the car | on the phone |
| Test with | emulator | DHU + real phone |
| Lives as long as | the car is on | the phone stays connected |

> **Remember:** same templates, different computer.

**Q4. What is the AAOS architecture?**

**Answer:** Give the four layers from [the map](#the-map--learn-this-before-anything-else):
apps, Car API, CarService, Vehicle HAL, and below that the CAN bus.

**Your proof:** add that a property read is two binder hops, so latency in
AAOS is usually binder latency.

**Q5. How is AAOS different from normal Android?**

**Answer:** Three additions.

1. The **Vehicle HAL** — a new hardware layer that does not exist on a phone.
2. **CarService and the Car APIs** — the car-specific framework.
3. A **replaced UI layer** — Car SystemUI and Car Launcher instead of the
   phone ones.

Plus two behaviour changes: it is **multi-user by default**, and it has
**driver distraction rules** that the platform enforces.

**Q6. What is CarService?**

**Answer:** The bridge between apps and the vehicle. It is written in Java,
runs as a privileged system app, and holds many smaller services inside it —
property, audio, power, occupant zones, watchdog, garage mode.

**Your proof:** `adb shell dumpsys car_service --list` shows **39** services
on your emulator.

**Q7. What is the Vehicle HAL?**

**Answer:** The layer that hides the car's hardware from Android. It exposes
vehicle state as numbered **properties**. Android only ever sees properties;
the VHAL translates them to and from the real vehicle network.

> **Remember:** VHAL = translator. Car speaks CAN, Android speaks properties.

**Q8. What is a digital cockpit?**

**Answer:** All the screens treated as one system — centre display,
instrument cluster, sometimes passenger and rear screens — often driven by a
single chip. This is why AAOS is multi-display, and why **"which display?"**
is a real interview question.

**Q9. What is Project Treble and why does it matter here?**

**Answer:** It separates vendor code from the Android framework, so the
framework can be updated without redoing the vendor's hardware code. In
automotive it is what makes the VHAL a swappable HAL with a **versioned**
interface.

**Your proof:** `/vendor/etc/vintf/manifest/vhal-emulator-service.xml` on your
emulator declares `format="aidl"`, version **3**.

**Q10. What is AGL and how does it compare?**

**Answer:** Automotive Grade Linux — a Linux-based automotive platform. Same
goal, different trade-off: more OEM control, but no Play Store, no Android app
ecosystem, and you build the app framework yourself.

**Q11. AAOS or embedded Linux for infotainment?**

**Answer:** Android gives a ready app ecosystem, a standard UI framework,
Google services and normal Android tooling. Embedded Linux gives more control
and a smaller footprint. The trade is **ecosystem versus control**.

**Q12. Which Android version is which API level?**

**Answer:** AAOS started at Android 10 (API 29), so an AAOS app usually sets
`minSdk = 29`.

| Android | API |
|---|---|
| 10 | 29 |
| 13 | 33 |
| 14 | 34 |
| **15** | **35** |
| 16 | 36 |

**Careful:** my old notes said `compileSdk = 36 // Android 15`. That is wrong.
36 is Android 16. Android 15 is **35**.

---

# PART 2 — Car App Library and templates

This is the app layer. It is the same code for AAOS and Android Auto.

**Q13. What is the Car App Library?**

**Answer:** A Jetpack library (`androidx.car.app`). You do not build layouts.
You build **templates** — a description of what you want on screen. The car's
**host** app reads your description and draws it in the car maker's own style.

> **Remember:** you write the words, the host draws the picture.

**Q14. Why templates instead of normal layouts?**

**Answer:** Two reasons.

1. **Safety.** The host enforces the distraction limits, so an unsafe UI
   cannot ship by accident.
2. **Consistency.** Every app matches the car's look, and the car maker can
   restyle all apps at once.

**Q15. What are the three classes every car app has?**

**Answer:**

```
CarAppService   the entry point. The host binds to it.
   |
Session         one connection. Created when the host connects.
   |
Screen          one screen. Returns a Template from onGetTemplate().
```

```kotlin
class MyCarAppService : CarAppService() {
    override fun createHostValidator() = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession() = MySession()
}

class MySession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = HomeScreen(carContext)
}

class HomeScreen(ctx: CarContext) : Screen(ctx) {
    override fun onGetTemplate(): Template = ListTemplate.Builder()
        .setTitle("Home")
        .setSingleList(itemList)
        .build()
}
```

> **Remember:** Service -> Session -> Screen -> Template.

**Q16. How does a Screen update itself?**

**Answer:** You never draw. You change your data, then call **`invalidate()`**.
The host then calls `onGetTemplate()` again and redraws.

> **Remember:** `invalidate()` is the only way to refresh. There is no
> `setText`.

**Q17. What is the Screen lifecycle?**

**Answer:** A `Screen` is a `LifecycleOwner`. It gets the normal states —
`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy` — so you
can start listening in `onStart` and stop in `onStop`.

Screens live on a **stack**. `screenManager.push(next)` adds one; the host's
back button pops it.

**Careful:** because `Screen` is a `LifecycleOwner`, use
`liveData.observe(this) { invalidate() }`. My old notes used
`observeForever { invalidate() }` — that never removes the observer, so the
Screen leaks. It was a real leak in our code and we fixed it.

**Q18. Which templates should you know?**

**Answer:**

| Template | Use it for |
|---|---|
| `ListTemplate` | rows of items — the workhorse |
| `GridTemplate` | icon grid; good for gauges and tiles |
| `PaneTemplate` | a few rows of detail plus actions |
| `MessageTemplate` | a message, an error, a confirmation |
| `TabTemplate` | top-level tabs |
| `SearchTemplate` | text search |
| `PlaceListMapTemplate` | list next to a map |
| `NavigationTemplate` / `MapTemplate` | turn-by-turn; you draw a surface |

**Q19. How many items can a list hold?**

**Answer:** Ask the host. Do not hardcode a number.

```kotlin
val max = carContext.getCarService(ConstraintManager::class.java)
    .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
```

**Careful:** my old notes said "ListTemplate max 6 items". That is not a rule,
it is host-dependent. On your emulator `getMaxCumulativeContentItems()`
returned **21**. Exceeding the real limit is a crash or a Play Store
rejection, not a warning — which is exactly why you read it at runtime.

**Q20. What is the action strip?**

**Answer:** A small row of actions the host places in its own chrome —
usually top right. You add `Action` objects; the host decides where they go.
There is also `setHeaderAction(Action.BACK)` and `Action.APP_ICON`.

The host limits how many actions it will show, so put the important one first.

**Q21. How does the car know your app is a car app?**

**Answer:** Two manifest pieces.

```xml
<meta-data
    android:name="com.android.automotive"
    android:resource="@xml/automotive_app_desc" />

<service android:name=".MyCarAppService" android:exported="true">
    <intent-filter>
        <action android:name="androidx.car.app.CarAppService" />
        <category android:name="androidx.car.app.category.MEDIA" />
    </intent-filter>
</service>
```

And `res/xml/automotive_app_desc.xml` lists the categories:

```xml
<automotiveApp>
    <uses name="media" />
</automotiveApp>
```

**Q22. What are the app categories, and can you pick any?**

**Answer:** No. Google only allows certain categories for third-party apps —
**media**, **navigation**, **point of interest**, **IOT**, messaging, and
parking/charging. A generic "dashboard" or "vehicle info" app is **not** a
third-party category. Those are OEM system apps.

> **Remember:** the category is a permission from Google, not a label you
> choose.

**Q23. What is `minCarApiLevel`?**

**Answer:** The oldest host your app supports, declared in the manifest:

```xml
<meta-data android:name="androidx.car.app.minCarApiLevel"
           android:value="1" />
```

If you call a newer template API you must guard it with
`carContext.carAppApiLevel`, or older cars crash.

**Q24. What is `HostValidator` and why does it matter?**

**Answer:** Your `CarAppService` is `exported="true"`, so any app on the
device can bind to it and read whatever you put on screen. `HostValidator`
checks the **certificate** of whoever binds, and rejects anyone who is not a
real car host.

Real hosts: `com.google.android.projection.gearhead` (Android Auto) and
`com.google.android.autosimulator` (DHU).

> **Careful:** `ALLOW_ALL_HOSTS_VALIDATOR` is for development only. Shipping
> it is the classic mistake — and a review rejection.

---

# PART 3 — Vehicle data

The heart of AAOS. Nothing here exists on Android Auto.

**Q25. How does an app read car data?**

**Answer:**

```kotlin
val car = Car.createCar(context)
val pm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
val speed = pm.getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)
```

The `0` is the **area id**. Global properties use 0; per-seat or per-door
properties use an area mask.

**Q26. What is the VehicleProperty API?**

**Answer:** The standard list of vehicle properties, each with a fixed ID, a
fixed type and fixed units. So the same app code works across car makers —
`PERF_VEHICLE_SPEED` means the same thing in any AAOS car.

**Q27. `getProperty` or `registerCallback`?**

**Answer:** `registerCallback` for anything that changes. The car **pushes**
events to you.

```kotlin
pm.registerCallback(callback, PERF_VEHICLE_SPEED, 10f)  // 10 Hz
```

**Your proof — this is a strong answer, give the long version.** Polling added
up to a full second of latency to an overspeed warning. Worse: injected test
events only reach **subscribers**, so polling code cannot see test data at
all. You deleted three 1 Hz polling loops for that reason.

> **Remember:** the car pushes. Do not pull.

**Q28. What are the property change modes?**

**Answer:**

| Mode | Meaning | Example |
|---|---|---|
| `STATIC` | never changes | VIN |
| `ON_CHANGE` | an event only when the value changes | gear, ignition |
| `CONTINUOUS` | sampled at a rate you ask for | speed, rpm |

For `ON_CHANGE` you subscribe with `SENSOR_RATE_ONCHANGE`. For `CONTINUOUS`
you pass a rate in Hz, between the config's min and max.

**Your proof:** on your emulator speed and rpm cap at **10 Hz**; fuel and
battery allow **100 Hz**.

**Q29. What are the property IDs you should know?**

**Answer:** Do not memorise many. Know how to look them up, and know these
six.

| Property | ID |
|---|---|
| `PERF_VEHICLE_SPEED` | `0x11600207` |
| `ENGINE_RPM` | `0x11600305` |
| `FUEL_LEVEL` | `0x11600307` |
| `GEAR_SELECTION` | `0x11400400` |
| `PERF_ODOMETER` | `0x11600204` |
| `IGNITION_STATE` | `0x11400409` |

**Careful:** my old notes listed `PROP_ODOMETER = 0x11600101` and
`PROP_ENGINE_ON = 0x11200401`. Both are **wrong** — checked on the emulator,
both return *"not supported by HAL"*. The correct ones are the last two rows
above.

Look any of them up instead of trusting a note:

```bash
adb shell cmd car_service get-carpropertyconfig PERF_VEHICLE_SPEED
```

**Q30. How do you read a property ID?**

**Answer:** It is a **bit field**, not a random number. Four parts:

```
0x 1 1 600207
   |  |    |
   |  |    +-- unique id for this property
   |  +------- type   (0x00600000 = FLOAT)
   +---------- group  (0x10000000 = SYSTEM) + area (0x01000000 = GLOBAL)
```

| Field | Value | Meaning |
|---|---|---|
| group | `0x10000000` | system (AOSP standard) |
| | `0x20000000` | **vendor** (the OEM's own) |
| area | `0x01000000` | global |
| type | `0x00400000` | INT32 |
| | `0x00600000` | FLOAT |
| | `0x00100000` | STRING |

> **Remember:** group, area, type, number. Four fields OR'd together.

**Q31. My property read returns null. How do you debug it?**

**Answer:** In this order.

1. Is it declared in the manifest?
2. Is it actually **granted**? A missing permission makes the property look
   **unsupported** — it does **not** throw a SecurityException.
3. Is it the right protection level? `adb shell dumpsys package permissions`,
   look at the `prot=` field.
4. Is it the right type and area? `cmd car_service get-carpropertyconfig`.

**Your proof:** you hit all four.

> **Remember:** in AAOS a missing permission looks like missing hardware.
> That one sentence is worth the whole question.

**Q32. What are the unit traps?**

**Answer:** Units are not what you expect, and guessing is fatal.

| Property | Type | Unit |
|---|---|---|
| speed | FLOAT | **metres per second** — multiply by 3.6 for km/h |
| fuel level | FLOAT | **millilitres** |
| EV battery level | FLOAT | **watt-hours** |
| odometer | FLOAT | already **kilometres** |
| seatbelt | BOOLEAN | **per seat**, not one global flag |

**Your proof:** all five were wrong in your code first.

> **Remember:** speed is m/s. Fuel is ml. Battery is Wh. Odometer is km.

**Q33. Can an app write vehicle data?**

**Answer:** Sometimes. Each property's config says READ, WRITE or READ_WRITE,
and a write needs its own permission. Safety-critical actuation — steering,
braking, throttle — is **not exposed to apps at all**.

**Your proof:** you wrote one, a vendor drive-mode property, and hit the
Kotlin boxing trap:

```kotlin
// wrong: Int::class.java is primitive int.class
pm.setProperty(Int::class.javaObjectType, DRIVE_MODE, AREA_GLOBAL, mode)
```

`Int::class.java` is `int.class`. `setProperty` needs the **boxed** class, so
you need `Int::class.javaObjectType` — which is `java.lang.Integer`. And you
cannot write `Integer::class.java` in Kotlin, because Kotlin maps it back to
`Int`.

**Q34. How would you add a property the standard VHAL does not have?**

**Answer:** Use the **vendor group**, `0x20000000` — the car maker's own
namespace. Build the ID as group | area | type | number.

```kotlin
private fun vendorId(type: Int, uniqueId: Int) =
    GROUP_VENDOR or AREA_GLOBAL or type or uniqueId

val DRIVE_MODE     = vendorId(TYPE_INT32,  0x0001)   // 0x21400001
val SERVICE_DUE_KM = vendorId(TYPE_FLOAT,  0x0002)   // 0x21600002
val BATTERY_HEALTH = vendorId(TYPE_STRING, 0x0003)   // 0x21100003
```

**Your proof:** you added those three and needed **no C++**. The reference
VHAL loads its property list from JSON in
`/vendor/etc/automotive/vhalconfig/`, so a JSON file plus a VHAL restart is
enough. You then wrote to `DRIVE_MODE` and read the new value back.

**Q35. How would you simulate a property that does not exist yet?**

**Answer:** Two ways.

1. Add it to the VHAL's JSON config — no C++, works end to end.
2. Put a mock behind your own interface, so the app can be built before the
   real signal exists.

The second is normal in automotive, because the vehicle signal usually arrives
**later** than the app that needs it.

**Q36. How do you test vehicle data with no car?**

**Answer:**

```bash
# what does the platform support?
adb shell cmd car_service get-carpropertyconfig PERF_VEHICLE_SPEED

# one-shot value (ON_CHANGE properties)
adb shell cmd car_service inject-vhal-event 289408009 2

# a stream (CONTINUOUS properties) - id, value, seconds apart, duration
adb shell cmd car_service inject-continuous-events 291504647 31 -s 5 -d 180

# what does the platform think the car is doing?
adb shell dumpsys car_service --services CarDrivingStateService
```

**Careful:** my old notes wrote `adb shell dumpsys car_service
inject-vhal-event`. The command is **`cmd car_service`**, not `dumpsys`.
`dumpsys` reads state; `cmd` sends commands.

**Your proof:** a single `inject-vhal-event` does **not** stick for a
CONTINUOUS property — the VHAL's own generator overwrites your value on the
next sample. Use `inject-continuous-events` for speed and rpm.

**Q37. What is the CAN bus, and how does it reach Android?**

**Answer:** A broadcast vehicle network. Each frame has an ID and up to 8
bytes. Signals are packed inside those bytes, and a **DBC file** describes
where each signal sits.

It **never touches Android directly**. Something below the VHAL reads CAN and
turns a signal into a property.

> **Remember:** CAN -> (vendor code) -> VHAL property -> CarService -> app.

**Q38. Where does AUTOSAR fit?**

**Answer:** A different world. AUTOSAR is the standard for deeply embedded
ECUs doing real-time control — engine, brakes, body. Android is the cockpit.

They meet at a **gateway**: a signal from an AUTOSAR ECU travels over CAN and
appears in Android as a VHAL property.

**Q39. What is the difference between CAN, LIN, FlexRay and Automotive Ethernet?**

**Answer:** Speed and purpose.

| Bus | Speed | Used for |
|---|---|---|
| LIN | very slow | cheap things — window switches, mirrors |
| CAN / CAN-FD | medium | most powertrain and body signals |
| FlexRay | fast, time-triggered | chassis control where timing is fixed |
| Automotive Ethernet | fastest | cameras, displays, ADAS, big data |

Infotainment usually sits on Ethernet for video and reads vehicle signals that
arrived over CAN.

---

# PART 4 — Permissions and system apps

**Q40. What are the car permission levels?**

**Answer:** Three, and they behave completely differently.

| Level | Example | How you get it |
|---|---|---|
| `normal` | `CAR_INFO`, `CAR_POWERTRAIN` | automatic at install |
| `dangerous` | `CAR_SPEED`, `CAR_ENERGY` | **ask the user at runtime** |
| `signature\|privileged` | `CAR_ENGINE_DETAILED`, `CAR_MILEAGE`, `CAR_IDENTIFICATION` | system app only |

**Careful — this is the single most important correction in this document.**
My old notes said car permissions *"are granted by the OEM, not user-granted
like normal permissions."* That is wrong for the common ones. Checked with
`dumpsys package permissions`:

- `CAR_SPEED` -> `prot=dangerous` — a normal app asks at runtime and gets it
- `CAR_ENERGY` -> `prot=dangerous` — same
- `CAR_INFO` -> `prot=normal` — granted at install, no prompt

Only engine detail, mileage and VIN are locked to system apps.

> **Remember:** speed and fuel are ordinary runtime permissions, like
> location. RPM and VIN are not.

**Q41. How do you request them?**

**Answer:** Runtime car permissions use the Car App Library's own API, not
`ActivityCompat`:

```kotlin
val missing = VehiclePermissions.missingRuntime(carContext)
if (missing.isNotEmpty()) {
    carContext.requestPermissions(missing) { granted, _ ->
        if (granted.isNotEmpty()) VehicleRepository.connect(carContext)
    }
}
```

**Q42. Why can a normal app not read RPM or the VIN?**

**Answer:** They are `signature|privileged`. Since Android 9 a privileged app
must be **both**:

1. installed in `/system/priv-app`, **and**
2. listed in a `privapp-permissions` allowlist XML.

Only whoever builds the system image can do both.

**Q43. So how would you build an instrument cluster app?**

**Answer:** As a privileged system app shipped inside the system image, not
from the Play Store. Four steps, and each one fails **silently** if you miss it:

```bash
adb root && adb remount
adb push app.apk /system/priv-app/SmartAaos/SmartAaos.apk
adb shell chmod 644 /system/priv-app/SmartAaos/SmartAaos.apk
adb push privapp-permissions-com.swapnil.smart.aaos.xml \
         /system/etc/permissions/
adb reboot     # priv-app is scanned only at boot
```

**Your proof:** you did this and got 14 of 14 properties live, a real VIN, and
`Engine Critical — High RPM (5600)` from injected data. You also found that
after this, a **normal install keeps the privileged permissions** — the
package becomes an *updated system app* (`FLAG_UPDATED_SYSTEM_APP`).

> **Remember:** `/system/priv-app` + allowlist XML + reboot. All three, or
> nothing.

**Q44. Why is AAOS multi-user, and what breaks?**

**Answer:** The system boots as **user 0**. The driver is a separate user,
normally **user 10**, so driver profiles are real Android users. Anything
user-scoped silently targets the wrong user if you forget:

- `pm grant` defaults to **user 0** while your app runs as **10**
  -> use `pm grant --user 10`
- `/sdcard` is user 0's storage. The driver's is `/data/media/10`
- **CarSystemUI runs as user 0. Car Launcher runs as user 10.**

**Your proof:** this cost you a wrong conclusion twice — once with `pm grant`,
once with MediaStore.

> **Remember:** system is 0, driver is 10. Always name the user.

**Q45. How would you do driver authentication?**

**Answer:** Use the multi-user framework — driver profiles are users. Add PIN
or biometrics if the hardware has it.

The car-specific part: a profile can be tied to a key fob or a phone, and
switching driver must **not** interrupt playback or navigation.

**Q46. What is sepolicy, and when do you touch it?**

**Answer:** SELinux rules that say which process may touch which file or
service. A new HAL or system service needs policy or it simply will not start.

Denials appear in logcat as:

```
avc: denied { read } for scontext=u:r:myapp:s0
     tcontext=u:object_r:vehicle_data:s0 tclass=file
```

Reading those four fields — action, who, what, kind — is the skill.

**Q47. What is app signing and why does it matter more here?**

**Answer:** `signature` permissions are granted only to apps signed with the
**same key as the platform**. On a car, that key belongs to the OEM. So a
third-party app can never hold them, whatever it declares.

**Your proof:** the emulator is a `test-keys` build
(`ro.build.tags=test-keys`), so its platform key is the **public AOSP**
`platform.pk8`. That is why you could platform-sign an overlay locally and it
would be impossible on a production car.

---

# PART 5 — Media and audio

**Q48. How does media playback work on AAOS?**

**Answer:** You do not draw a player. The car's media UI does. You provide two
things:

```
MediaBrowserServiceCompat   your library, so the car can browse it
MediaSessionCompat          playback state + controls, so the car can drive it
```

> **Remember:** you are the library and the engine. The car is the dashboard.

**Q49. What does `MediaBrowserServiceCompat` need?**

**Answer:** Two methods.

```kotlin
class MusicService : MediaBrowserServiceCompat() {

    override fun onGetRoot(pkg: String, uid: Int, hints: Bundle?)
        : BrowserRoot? = BrowserRoot("root", null)   // return null to refuse

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        result.detach()                  // if you need to load in background
        result.sendResult(buildItems(parentId))
    }
}
```

`onGetRoot` is also your security check — you can return `null` for a caller
you do not trust.

**Q50. What does `MediaSessionCompat` do?**

**Answer:** It publishes what is playing and accepts commands. The car's UI,
the steering wheel buttons and the voice assistant all talk to it.

```kotlin
session = MediaSessionCompat(this, "MusicService").apply {
    setCallback(object : MediaSessionCompat.Callback() {
        override fun onPlay() { player.play() }
        override fun onPause() { player.pause() }
        override fun onSkipToNext() { player.seekToNext() }
        override fun onPlayFromSearch(query: String?, extras: Bundle?) { ... }
    })
    isActive = true
}
sessionToken = session.sessionToken     // must be set for browsing to work
```

Two objects to keep fresh: `PlaybackStateCompat` (playing/paused, position,
allowed actions) and `MediaMetadataCompat` (title, artist, art, duration).

> **Remember:** metadata = what it is. PlaybackState = what it is doing.

**Q51. What is audio focus, and why does it matter more in a car?**

**Answer:** Audio focus decides who is allowed to play. You request it before
playing and you must react when you lose it.

In a car, navigation prompts and safety warnings **must** interrupt music. So:

| You get | You do |
|---|---|
| `AUDIOFOCUS_LOSS` | stop, release focus |
| `AUDIOFOCUS_LOSS_TRANSIENT` | pause, resume after |
| `..._TRANSIENT_CAN_DUCK` | lower the volume, keep playing |
| `AUDIOFOCUS_GAIN` | restore volume / resume |

Get it wrong and the driver misses a turn instruction. That is the answer they
want — not the API list.

**Q52. How do you request it correctly?**

**Answer:** With `AudioAttributes` that describe **why** you are playing,
because in a car the attributes decide the routing.

```kotlin
val attrs = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()

val request = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
    .setAudioAttributes(attrs)
    .setOnAudioFocusChangeListener(listener)
    .build()
```

**Q53. What are car audio zones?**

**Answer:** Separate outputs per occupant area, so the driver and the rear
seats can play different things at the same time. Each zone has volume groups,
and the audio policy routes a stream to a zone based on its
`AudioAttributes.USAGE_*`.

**Your proof:** your emulator really has two — `primary zone:0` and
`rear seat zone 1:1` — visible in
`dumpsys car_service --services CarAudioService`.

**Q54. Can your app control multi-zone audio?**

**Answer:** No. Zones and volume groups are `@SystemApi` — not in the SDK jar
— and the permissions are `signature|privileged`. Even
`registerCarVolumeCallback` throws.

From a framework role it is your job. From an app, your only lever is
`AudioAttributes.USAGE_*`.

**Q55. Media is playing through the wrong speakers. Debug it.**

**Answer:** Routing comes from `AudioAttributes.USAGE_*` plus the zone config,
so there are only two suspects.

```bash
adb shell dumpsys car_service --services CarAudioService
adb shell dumpsys audio
```

**Your proof — suspect the app first.** You found
`ExoPlayer.Builder().build()` with **no AudioAttributes set at all**, so the
stream silently took a default route. The config was fine.

> **Remember:** no AudioAttributes = the car guesses. It will guess wrong.

---

# PART 6 — Driver distraction and car UI

**Q56. What is driver distraction optimisation?**

**Answer:** Platform-enforced rules limiting what the driver may do while the
car is moving: short text, a limited number of list items, no video, no free
scrolling, no keyboard entry.

Roughly above **5 km/h**, complex interaction is not allowed.

> **Remember:** the driver should look away for under **2 seconds**. Every
> rule comes from that one number.

**Q57. How do you read the restrictions at runtime?**

**Answer:** `CarUxRestrictionsManager`.

```kotlin
val mgr = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE)
        as CarUxRestrictionsManager
mgr.registerListener { r ->
    if (r.isRequiresDistractionOptimization) showSimpleUi()
}
```

**Your proof — and this is a better answer than the API.** On your emulator it
did **not** work as documented. Two real problems:

1. `getCarManager` returned `null` right at connect, and needed a retry.
2. It reported the **cluster display's** restrictions (value 511 — fully
   restricted) while the main display was unrestricted (0), so it sent no
   events for the screen the driver was using.

So you gated the UI on **motion** instead, and wrote down why. The lesson to
land: *verify the API on your hardware before you depend on it.*

**Q58. How do you design a UI for a car?**

**Answer:** Big touch targets, few items per screen, high contrast for
sunlight glare, short text, voice where possible, and everything reachable in
one or two steps.

**Q59. How do you handle different screen sizes?**

**Answer:** Use `dp` and vector drawables, let the templates lay themselves
out, and never hardcode pixels. Car screens range from small portrait to very
wide landscape, and the cluster is different again.

**Your proof:** you measured a real constraint — the host reserves **121 dp**
below list and grid templates. It is density-invariant and the app cannot
remove it.

**Q60. How do you handle localisation in a car?**

**Answer:** String resources per language, RTL support, locale-specific
formats — plus the car-specific part: **units**. Speed and distance must
follow the **car's** display unit setting, not just the locale. A UK car may be
set to miles.

**Q61. What is `car-ui-lib`?**

**Answer:** The shared component library (`CarUi`) used by car apps and system
apps — toolbars, lists, preferences. Because everyone uses it, the OEM can
restyle every app through **one** overlay target instead of app by app.

**Q62. What is a progress bar / loading state in a template app?**

**Answer:** You do not animate it. You set the list to a loading state and
call `invalidate()` when data arrives:

```kotlin
ListTemplate.Builder()
    .setLoading(true)         // host draws its own spinner
    .build()
```

Same for a `Row` — `setBrowsable(true)` or an `addText` progress string. The
host owns the animation, so it stays within the distraction rules.

**Q63. How do you show an alert or warning to the driver?**

**Answer:** Two levels.

1. `CarToast` — brief, non-blocking.
2. `MessageTemplate` pushed on the screen stack — for something the driver
   must acknowledge.

For a real safety warning the host also has a **notification** path with
`CarAppExtender`, and `PlaceListNavigationTemplate`-style apps use
`NavigationManager` for turn alerts.

**Q64. How do you support voice?**

**Answer:** Two separate things, and my two old notes disagreed with each
other here — so be precise:

1. **Voice search for media** — the assistant calls your `MediaSession`'s
   `onPlayFromSearch(query, extras)`. This is the one Google requires for a
   media app.
2. **Voice for navigation** — a nav app registers with `NavigationManager`
   and reacts to the host, it does not receive a broadcast.

> **Remember:** media voice arrives through `MediaSession`. Nav arrives
> through `NavigationManager`. Neither is a `BroadcastReceiver`.

---

# PART 7 — App architecture: services, AIDL, threading

**Q65. Why would a car app need its own background service?**

**Answer:** Because vehicle data outlives any one screen. Real car data comes
from a system service, so putting it behind a service of your own means the
app talks to it the same way it will talk to the real thing later.

It also lets one collector feed several screens, instead of every screen
opening its own VHAL subscription.

**Q66. What is a foreground service and when do you need one?**

**Answer:** A service the user can see, with a permanent notification. You
need one for anything that must keep running when the UI is gone — music
playback being the obvious case.

```kotlin
startForeground(1, notification)   // must be within seconds of start
```

Since Android 14 you must declare a **type**:

```xml
<service android:name=".MusicService"
         android:foregroundServiceType="mediaPlayback" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

**Q67. What is AIDL and why use it?**

**Answer:** Android Interface Definition Language. You describe an interface
in a `.aidl` file; the build generates the proxy and stub so a cross-process
call looks like a normal method call.

You need it whenever your service runs in a different process — which is
always true for CarService and the VHAL.

**Q68. What is `oneway` in AIDL?**

**Answer:** The call returns immediately without waiting for the other side to
finish. Use it when the caller must not block.

**Your proof:** you used `oneway` on the callback because your service
broadcasts from the **VHAL event thread**. A slow client blocking there would
stall the property stream for every other listener.

> **Remember:** `oneway` = fire and forget. No return value allowed.

**Q69. What types can cross AIDL?**

**Answer:** Primitives, `String`, `List`, `Map`, `Parcelable`, and other
Binder interfaces. Anything custom must implement `Parcelable`. Direction tags
`in`, `out`, `inout` exist because the data is **copied**, not shared.

```java
interface IVehicleDataService {
    float getSpeed();
    int   getDriveMode();
    boolean setDriveMode(int mode);
    void registerCallback(IVehicleDataCallback cb);
}
```

**Q70. What is MVVM, and what is the car-specific part?**

**Answer:** Model - ViewModel - View. The ViewModel holds state and survives
configuration changes; the View only renders it.

The car-specific part: the **source is a push stream from another process**.
So the chain is:

```
VHAL --registerCallback--> HalManager (units, caching, null-safety)
     --oneway AIDL-------> DataService
     --------------------> Repository (one snapshot, thread-safe)
     --------------------> ViewModel --> Screen.invalidate()
```

**Careful:** my old notes had the ViewModel **polling** the service with
`delay(1000)` in a loop. That is the design we deleted. It is wrong for three
reasons: extra binder traffic, up to a second of added latency on a safety
warning, and injected test events never reach a poller at all.

**Q71. How do you pass data safely between threads here?**

**Answer:** Immutable snapshots. The binder thread writes a whole new
snapshot; the main thread reads it. Mark the field `@Volatile` so the write is
visible, and dispatch listeners on the main thread.

**Your proof:** that is exactly what `VehicleRepository` does — written from a
binder thread, read from the main thread, listeners posted to main.

**Q72. Coroutines or callbacks for vehicle data?**

**Answer:** The VHAL gives you a callback. Wrap it once in a
`callbackFlow`/`StateFlow` if you want coroutines, but do **not** convert a
push stream into a polling loop, which is what `while(true) { delay() }` does.

**Q73. What is a memory leak risk in a car app specifically?**

**Answer:** Long-lived objects. A car app's process can stay alive for the
whole drive, so a leak has hours to grow rather than minutes.

The three to name:

1. A registered `CarPropertyManager` callback never unregistered.
2. `observeForever` with no `removeObserver` — **your real bug**.
3. A static or singleton holding a `Context` or a `Screen`.

**Q74. Where do you set up and tear down?**

**Answer:** Symmetrically, at the same lifecycle level.

| Register in | Unregister in |
|---|---|
| `Screen.onStart` | `Screen.onStop` |
| `Service.onCreate` | `Service.onDestroy` |
| `Session` create | `Session` destroy |

If you cannot name the matching teardown, you have a leak.

---

# PART 8 — AOSP internals

**Q75. What happens during Android boot?**

**Answer:**

```
bootloader
  -> kernel
    -> init stage 1   mount partitions, load SELinux
    -> init stage 2   parse .rc files, start property service,
                      servicemanager, surfaceflinger, logd
      -> Zygote
        -> SystemServer
          -> CarService        (AAOS only)
            -> Car Launcher
```

The automotive difference: **boot time is a requirement, not a nicety.** The
driver expects a screen almost immediately, and the reversing camera must work
very early — often before Android is fully up.

**Q76. What is Zygote?**

**Answer:** A process that preloads the framework classes and then **forks**
to create every app process. Forking is why app startup is fast — the
framework is already in memory and shared between apps.

**Q77. What is `init.rc`?**

**Answer:** The script `init` parses to define services and actions — what to
start, as which user, in which order. A HAL like the VHAL is declared there:

```
service vendor.vehicle-hal-emulator /vendor/bin/hw/android...vehicle@V3-emulator-service
    class early_hal
    user vehicle_network
```

**Your proof:** note `class early_hal` — it starts **before** normal HALs,
because the framework needs vehicle data early in boot.

**Q78. What is Binder?**

**Answer:** Android's IPC mechanism. Processes talk through the kernel binder
driver using proxy and stub objects, so a remote call looks like a local one.

Two facts worth knowing: the transaction buffer is about **1 MB per process**
(so do not send bitmaps), and there is a limited pool of binder threads (so a
blocking call can starve a service).

Everything in AAOS crosses binder — CarService, the VHAL, the template host.

**Q79. HIDL or AIDL for HALs?**

**Answer:** HIDL came with Treble to separate vendor code. It had its own IDL,
its own binder domain and its own service manager.

**Stable AIDL replaced it from Android 11** — the same IDL as apps, versioned
interfaces, normal binder. All new HALs, including the automotive VHAL, are
AIDL.

Both appear in job descriptions because older trees still contain HIDL.

**Q80. What is a VINTF manifest?**

**Answer:** The file that declares which HAL interfaces a device actually
provides, and at which version, so the framework can check compatibility at
build time and boot time.

**Your proof:** your emulator's declares `android.hardware.automotive.vehicle`
version **3**, instance `IVehicle/default`, `format="aidl"`.

**Q81. What does the VHAL interface look like in C++?**

**Answer:** `IVehicleHardware.h` — about six pure virtual methods. The two
that matter are the async ones:

```cpp
StatusCode getValues(std::shared_ptr<const GetValuesCallback> cb,
                     const std::vector<GetValueRequest>& requests);
StatusCode setValues(std::shared_ptr<const SetValuesCallback> cb,
                     const std::vector<SetValueRequest>& requests);
```

They take a **callback** rather than returning a value, because a real read
may have to go and ask an ECU over CAN. The reference implementation is
`FakeVehicleHardware.cpp`, and its property list comes from
`JsonConfigLoader.cpp`.

> **Remember:** get and set are asynchronous, because the car is slow.

**Q82. How do you add a new system service in AOSP?**

**Answer:** Define the AIDL interface, implement the service, register it with
`servicemanager`, add sepolicy so clients may bind, and declare it in `init`
if it runs in its own process.

For a car feature it usually goes **inside CarService** instead of becoming a
new process.

**Q83. How is CarService updated separately from the platform?**

**Answer:** It is packaged as updatable car framework code, similar in idea to
mainline modules, so car fixes can ship without a full platform OTA.

**Your proof:** this is why car permissions show
`sourcePackage=com.android.car.updatable` in `dumpsys`.

**Q84. What is an occupant zone?**

**Answer:** The mapping of users, displays and seats. It answers "which person
is using which screen".

**Your proof:** `dumpsys car_service --services CarOccupantZoneService` on
your emulator shows display **0 = MAIN** and display **3 = CLUSTER**.

---

# PART 9 — HMI and Car System UI

The biggest theme in the Bengaluru job descriptions. Study this part hardest.

**Q85. What is Car System UI?**

**Answer:** `CarSystemUI` replaces phone SystemUI. It owns the system bars —
status and nav become the car's top and bottom bars — plus the HVAC panel,
notifications, the volume UI and the keyguard.

OEMs customise it heavily. It is most of what makes a car "look like that
brand".

**Q86. What is Car Launcher?**

**Answer:** The home screen — usually a maps card, a media card and app
shortcuts. It is an AOSP app (`CarLauncher`) that OEMs replace or restyle. It
also hosts the app grid, and it is the first screen after ignition, so its
startup time is scrutinised.

**Q87. How do OEMs customise system UI without forking it?**

**Answer:** **RRO — Runtime Resource Overlay.** An APK with **no code** that
replaces another package's resources at runtime. The OEM's branding then
survives Android upgrades, because there is no fork to re-merge.

```xml
<manifest package="com.swapnil.systemui.overlay">
    <overlay android:targetPackage="com.android.systemui"
             android:priority="2"
             android:isStatic="false" />
    <application android:hasCode="false" />
</manifest>
```

```bash
adb install overlay.apk
adb shell cmd overlay enable --user 0 com.swapnil.systemui.overlay
```

**Q88. What did you learn writing one?**

**Answer — three things a reader would not know.** You wrote an RRO targeting
`com.android.systemui`, replaced six colours, and changed the system bar
accent from blue to teal.

**1. Resources match by name, so a wrong name fails silently.** Nothing
errors; the colour just does not change. The reliable way to find valid names
is to read an overlay that already ships:

```bash
adb pull /product/overlay/googlecarui.theme.orange-com-android-systemui.apk
aapt2 dump resources googlecarui.theme.orange-com-android-systemui.apk
# 163 resources the stock Google theme replaces
```

**2. Enable it for the user the *target* runs as.** CarSystemUI is `u0_a223` —
**user 0** — even though the driver is user 10. Enabling for user 10 fails
with `SecurityException: Unable to retrieve overlay information`. Car Launcher
is the opposite: user 10.

**3. A third-party overlay cannot touch CarSystemUI at all.** The install
error names the only three routes:

> *signed with different certificates, and the overlay lacks
> `<overlay android:targetName>`*

So you need **one** of:

- the **same certificate** as the target, or
- the target declares `<overlayable name="...">` and you name it with
  `targetName`, or
- you are **preinstalled** in a trusted partition.

`aapt2 dump overlayable CarSystemUI.apk` prints nothing, so route two is
closed — it is not built to be overlaid by third parties. You got through by
route one, because the emulator is a `test-keys` build and its platform
certificate is the public AOSP one.

On a production car that key is the OEM's secret, and the overlay ships inside
the system image.

**Q89. Do you need a full AOSP build to do System UI work?**

**Answer:** It depends which kind, and this is a good answer to give because
most candidates assume the whole area needs Linux and a platform build.

- For **appearance** — no. An emulator, `aapt2` and a signing key is enough,
  and that is most of day-to-day OEM HMI work.
- For **behaviour** — yes. Changing what CarSystemUI *does*, not how it looks,
  is Java inside a platform-signed system app.

**Q90. Which framework services would you touch doing System UI work?**

**Answer:** The standard Android ones, not the car-specific ones.

| Service | What you ask it |
|---|---|
| `ActivityManager` | what is running, task and process state |
| `WindowManager` | windows, z-order, which display a window is on |
| `PackageManager` | installed apps, resolving intents, components |
| `LauncherApps` | the launcher's view of apps and shortcuts |

The car twist is **multi-display**: windows and activities are display-scoped,
so "which display" is part of nearly every question.

**Q91. How would you show an app on the cluster display?**

**Answer:** Launch it with `ActivityOptions.setLaunchDisplayId()`, and the
display must be one the occupant zone allows.

```kotlin
val opts = ActivityOptions.makeBasic().setLaunchDisplayId(3)
startActivity(intent, opts.toBundle())
```

AAOS also has `ClusterHomeService` and `FixedActivityService` for activities
pinned to a display.

**Q92. Is Jetpack Compose used in AAOS?**

**Answer:** Increasingly yes, for system apps and OEM HMI, where you own the
whole Activity.

But **not** for Car App Library apps. Those are templates rendered by the
host, so there is no view tree of yours to compose.

**Q93. What are the HMI performance constraints?**

**Answer:** Two numbers the OEM measures: **time to first frame from
ignition**, and **dropped frames while driving**.

So: nothing blocking on the main thread during startup, no heavy work in
`onCreate`, and watch memory — a head unit has far less RAM than a modern
phone, and it is shared with the cluster and the camera.

**Your proof:** you have a measured example — the dexopt regression in Q100.

---

# PART 10 — Power, updates and performance

**Q94. How does power management work in a car?**

**Answer:** The car tells Android its power state through the VHAL, and
`CarPowerManagementService` handles the transitions — on, shutdown prepare,
suspend, resume.

The key difference from a phone: **the user switches the vehicle off
abruptly.** There is no "please wait". So the system must save state fast and
resume fast.

```bash
adb shell dumpsys car_service --services CarPowerManagementService
```

**Q95. What is Garage Mode?**

**Answer:** Deferred work — updates, idle jobs, log upload — that runs
**after the car is switched off**, while power is still available. So it never
competes with the driver for CPU or network.

```bash
adb shell cmd car_service garage-mode query
adb shell cmd car_service garage-mode on
```

> **Remember:** Garage Mode = the car does its homework after you leave.

**Q96. What is CarWatchdog?**

**Answer:** It kills or reports processes that stop responding or that abuse
disk I/O. A car cannot show an "app not responding" dialog at 100 km/h, so the
platform is far more aggressive than a phone.

If your service registers with it, it must answer the health checks on time.

**Q97. How do OTA updates work in a car?**

**Answer:** **A/B (seamless) updates** — two slots. You write the inactive
slot, switch on reboot, and roll back automatically if boot fails.

The car adds four requirements on top:

1. Staged rollout, per region and per VIN.
2. The vehicle must be parked and have enough charge.
3. The install work runs in Garage Mode.
4. Rollback matters far more — a bricked head unit is a workshop visit.

**Q98. Logcat, dumpsys, Perfetto — when do you use which?**

**Answer:** Three tools, three different questions.

| Tool | The question it answers |
|---|---|
| `logcat` | **what happened?** — events, exceptions, your own logs |
| `dumpsys` | **what is the state right now?** — a service's current view |
| **Perfetto** | **who ran when, and for how long?** — timing |

Only Perfetto can explain a stall, because only Perfetto has a timeline.

> **Remember:** logcat = story. dumpsys = snapshot. Perfetto = stopwatch.

**Q99. What exactly is Perfetto, and what replaced what?**

**Answer:** Perfetto is the system-wide tracing tool. It records, on one
timeline: kernel scheduling (`linux.ftrace`), framework trace points via
atrace categories (`binder_driver`, `am`, `wm`, `gfx`, `dalvik`), and logcat
(`android.log`).

It **replaced systrace**. Systrace was the old Python wrapper around the same
ftrace data; Perfetto is the current system with a proper UI and an **SQL**
query layer.

Why it matters in a car: startup time and jank are contractual requirements,
and the cause is usually another process — binder to a busy service, or the
CPU being taken by the cluster. Only a system-wide trace shows another
process.

```bash
adb shell perfetto -c /data/misc/perfetto-configs/startup.cfg --txt \
    -o /data/misc/perfetto-traces/trace
adb pull /data/misc/perfetto-traces/trace
```

**Q100. Give a real performance problem you found and fixed.**

**Answer — your best performance answer, tell it as a story.**

After installing the app to `/system/priv-app` by hand, startup regressed
about **40%**. Perfetto showed the time inside `bindApplication`.

The cause: an APK **pushed** into the system partition never goes through
`dexopt`, so it ran **interpreted**. `dumpsys package` confirmed it:

```
status=run-from-apk
```

The fix was one command:

```bash
adb shell cmd package compile -m speed -f com.swapnil.smart.aaos
```

`bindApplication` went from **366 ms to 148 ms**.

The point to land: *a deployment method changed runtime performance.* Nothing
in the code changed at all.

**Q101. What is the classic mistake when reading a profiler trace?**

**Answer:** Fixing the biggest slice.

**Your proof:** in your trace the two largest slices were about **900 ms of
emoji font loading** — and they block **nothing**, because
`is_main_thread = 0`. They run on a background thread.

> **Remember:** total time is not blocking time. Filter to the main thread
> first, then look for the biggest slice.

**Q102. How do you keep the system responsive during critical operations?**

**Answer:** Four rules.

1. Keep work off the main thread.
2. Never block on a binder call to a service that may be busy.
3. Respect the watchdog's health checks.
4. Give safety-relevant paths — cluster, reversing camera — priority over
   infotainment.

For concurrency itself: `HandlerThread` or an executor for background work,
immutable snapshots passed between threads, and `@Volatile` where one thread
writes and another reads.

---

# PART 11 — Build system and native debugging

**Q103. What is Soong, and how does it differ from Make?**

**Answer:** Soong is AOSP's build system. You write **declarative**
`Android.bp` files in **Blueprint** format instead of imperative `Android.mk`
Makefiles. Soong generates Ninja files, and Ninja builds.

Legacy `.mk` files still exist in older trees, which is why job descriptions
mention both.

> **Remember:** `.bp` = declarative, no commands. `.mk` = a Makefile.

**Q104. What does an `Android.bp` module look like?**

**Answer:**

```
cc_library {
    name: "libmylib",
    srcs: ["src/Foo.cpp"],
    shared_libs: ["libbase", "liblog"],
    vendor: true,
}
```

Module type, name, sources, dependencies. No commands and no conditionals —
which is the point, because a declarative file can be analysed.

Module types worth knowing: `cc_library`, `cc_binary`, `android_app`,
`java_library`, `aidl_interface`, `prebuilt_etc`.

**Q105. How is an AIDL HAL interface declared to the build?**

**Answer:** An `aidl_interface` module, with frozen versions checked in under
`aidl_api/`.

**Freezing is what makes a HAL interface stable** — once a version is frozen,
changing it is a deliberate, versioned act rather than an edit.

**Q106. What is a tombstone and how do you read one?**

**Answer:** A file written when a **native** process crashes, in
`/data/tombstones/`.

It contains the signal (`SIGSEGV`, `SIGABRT`), the faulting address, the
registers and a backtrace. You **symbolise** the backtrace against the build's
unstripped symbols to turn addresses into function names.

```bash
adb shell ls /data/tombstones/
adb bugreport          # collects them for you
```

**Q107. How do you debug an ANR?**

**Answer:** An ANR means the **main thread did not respond in time**. The
system writes traces to `/data/anr/traces.txt`.

Read the main thread's stack. It is almost always one of three things:

1. blocked on a lock,
2. doing disk I/O,
3. **waiting on a binder call to a busy service** — the common one in AAOS,
   because so much is cross-process.

**Q108. Which tool for which symptom?**

**Answer:**

| Symptom | Tool |
|---|---|
| wrong behaviour | logcat |
| Java exception | logcat |
| **native** crash | tombstone |
| app frozen, "not responding" | ANR trace |
| slow or janky, but working | Perfetto |
| current state of a service | dumpsys |

**Q109. Is C++ mandatory for AAOS work?**

**Answer:** It depends on the layer, and it is worth being honest about this.

| Layer | Language |
|---|---|
| Apps, templates, media | Kotlin / Java |
| CarService, framework, System UI | **Java** |
| VHAL, HALs, native services | **C++** |
| Build files | Blueprint |

So: app and framework roles are Java/Kotlin. Only VHAL-and-below roles need
C++. Most Bengaluru job descriptions ask for HMI and framework work, so
Java/Kotlin covers the majority — but C++ opens the platform roles.

**Your proof:** you added three vendor properties to the VHAL with **no C++
at all**, because the reference VHAL is JSON-config driven.

---

# PART 12 — Testing, safety and process

**Q110. What is hard about testing automotive apps?**

**Answer:** You do not have the car. On top of that: signals are hard to
reproduce, timing matters, there are several displays and several users, and
compliance has to be **demonstrated**, not assumed.

**Q111. What is MIL / SIL / HIL?**

**Answer:** Test stages with progressively more real hardware.

| Stage | What is real |
|---|---|
| **MIL** — model in the loop | nothing; a model on a workstation |
| **SIL** — software in the loop | real software, simulated vehicle |
| **HIL** — hardware in the loop | real ECU hardware, simulated vehicle around it |

Then a bench, then a real vehicle.

**Your proof:** an AAOS emulator **is** SIL. Everything you did is SIL — say
so plainly. Claiming vehicle testing you did not do is the fastest way to lose
credibility.

**Q112. How would you debug an issue that only happens in a real vehicle?**

**Answer:** Get the logs off the car first — a bug report or persistent
logging — because you cannot attach a debugger while driving.

Then reproduce it on a HIL rig by **replaying the CAN trace**. Work with the
integration team; the cause is often a signal that behaves differently on the
real bus than in simulation.

**Q113. What is ISO 26262, and what ASIL is infotainment?**

**Answer:** ISO 26262 is the automotive functional safety standard. It rates a
function by what happens when it **fails**, from **ASIL A** (least severe) to
**ASIL D** (most).

Infotainment is usually **QM — Quality Management**, meaning no ASIL at all,
because a media app failing injures nobody.

But parts of the cockpit **are** safety-relevant: cluster telltales, warning
lamps, the reversing camera. Those can be ASIL A or B — which is why the
cluster is often a separate ECU, and often not Android.

The mature answer: *"the IVI is generally QM; the safety case lives in the
cluster and camera paths, and my job is not to compromise them."*

> **Remember:** QM is not "unsafe". It means the failure is not a safety
> hazard.

**Q114. What is ASPICE?**

**Answer:** A **process** maturity rating — how you develop software, not what
the software does. OEMs require suppliers to be assessed, so it is
contractual.

Day to day it means: requirements traced to design, to code, to tests, and
recorded review evidence.

> **Remember:** ISO 26262 = is the product safe. ASPICE = is the process
> disciplined.

**Q115. What are the safety considerations when building infotainment?**

**Answer:** Five.

1. Minimise distraction.
2. Never obscure safety information.
3. Fail safe — a crash in media must not affect driving.
4. Protect personal data.
5. Meet the region's regulations.

**Q116. How do you handle vehicle data privacy?**

**Answer:** Vehicle data **is** personal data — location, driving behaviour,
even fuel habits. So: ask consent, take the minimum, keep it in the car where
possible, gate it behind permissions, and be clear about what leaves the
vehicle.

GDPR applies to a car sold in Europe exactly as it does to a phone app.

**Q117. What about secure boot and encryption?**

**Answer:** A chain of trust from the bootloader upward, so only signed images
run, plus file-based encryption for user data.

In a car it also protects against **physical access**, because the hardware is
reachable — someone can open the dashboard.

**Q118. How would you write unit tests for vehicle logic?**

**Answer:** Put the vehicle behind an interface, then test the logic with fake
values. The unit under test should never need a car.

The valuable tests are the boundaries: the m/s to km/h conversion, the
overspeed threshold, and the **null** path when a permission is missing.

**Q119. What is the difference between a unit test, an instrumentation test
and a UI test here?**

**Answer:**

| Kind | Runs on | Tests |
|---|---|---|
| unit (JVM) | your machine | conversions, thresholds, state |
| instrumentation | emulator | service binding, AIDL, permissions |
| UI (UIAutomator) | emulator | what the driver actually sees |

Only the second and third can catch a permission or a multi-user mistake,
because those do not exist on the JVM.

---

# PART 13 — Test automation

A separate career track in some job descriptions, and adjacent to what you
already do by hand.

**Q120. How would you automate testing for an AAOS app?**

**Answer:** UIAutomator or Appium for the UI, driven from Python or Kotlin,
running against an AAOS emulator in CI.

The automotive-specific half: you must also drive the **vehicle state**. So
the harness injects VHAL properties to put the car into a state, and only then
asserts the UI.

**Your proof:** you already do the second half by hand.
`cmd car_service inject-continuous-events` is exactly what such a harness
wraps.

**Q121. What is hard about automating automotive UI?**

**Answer:** Three things.

1. For template apps the UI is drawn by the **host**, so your own view
   hierarchy is not what is under test.
2. Multiple displays — the test must say which one.
3. Driving state changes what is even permitted, so the test has to control
   speed and gear to cover both parked and moving behaviour.

**Q122. How would you put this in CI?**

**Answer:** Jenkins or GitHub Actions, a headless AAOS emulator,
`gradlew assemble`, install, run the suite, and publish results and traces as
artifacts.

Boot the emulator from a **known snapshot** so the vehicle state is
deterministic — otherwise the tests flake on leftover injected values.

---

# PART 14 — Android Auto

**Q123. Your app works on AAOS. What changes for Android Auto?**

**Answer:** The templates barely change. Four things do:

1. The app runs on the **phone**, so there are **no `android.car.*`
   permissions and no vehicle data**.
2. A different artifact — a separate application module.
3. The session lives and dies with the **phone connection**.
4. You test with **DHU** (Desktop Head Unit), not an emulator.

**Q124. Why is there no vehicle data on Android Auto?**

**Answer:** Your process is on the phone. There is no VHAL on your side, and
projection does not expose vehicle properties.

So if a feature needs speed or fuel, it **must** be AAOS. That is a real
product decision, not a limitation to work around.

**Q125. How do you structure one codebase for both?**

**Answer:** Two application modules that do **not** depend on each other —
they are genuinely separate apps — plus a `:shared` library module for models
and business logic.

**Your proof:** you can name the cost of not doing it. `MusicData.kt` and
`LocalSongLoader.kt` are copy-pasted into both modules and have already
drifted **20 and 48 lines**. That is the argument for a shared module,
measured rather than asserted.

**Q126. What is DHU?**

**Answer:** Desktop Head Unit — a desktop app that pretends to be a car head
unit. You connect a real phone by USB, and the phone projects to the DHU
window.

You need a **physical phone**; an emulator cannot do it.

**Q127. How does publishing differ for Android Auto?**

**Answer:** Google **manually reviews** Android Auto apps for driver safety
and must explicitly approve them — typically one to three weeks.

Reviewers check template compliance, driving restrictions and voice support.
**Exceeding a template's item limit is a rejection, not a warning** — which
is why Q19 matters.

---

# PART 15 — Your project

Guaranteed questions. Rehearse these until they are boring.

**Q128. Walk me through your architecture.**

**Answer:** One-way push, no polling.

```
VHAL  --registerCallback-->  VehicleHalManager
                             (unit conversion, caching, null if unreadable)
      --IVehicleDataCallback, oneway AIDL-->  VehicleDataService
      -->  VehicleRepository  -->  VehicleViewModel  -->  Screens
```

Four decisions to defend:

1. **A separate service behind AIDL** — because real vehicle data comes from a
   system service, so the app talks to it the same way either way.
2. **`oneway`** — because the broadcast runs on the VHAL event thread.
3. **Every getter nullable** — so a missing permission degrades instead of
   crashing.
4. **Unit conversion only at the HAL boundary** — one place, so nothing
   downstream can get m/s and km/h confused.

**Q129. Why did you remove the polling?**

**Answer:** There were three 1 Hz loops, one doing six binder round trips a
second. But the real reason is **correctness**, not cost: speed is CONTINUOUS
and already pushed at 10 Hz, so polling added latency to an overspeed warning
— and injected test events never reached the polling code at all.

**Q130. How did you test it?**

**Answer:** Emulator plus `cmd car_service` for configs, injection and driving
state; `dumpsys` for platform state; logcat for behaviour; Perfetto for
timing.

Plus a Simulation screen — because six simulate helpers existed with **no
call sites**, so the whole alert path was unreachable from the UI. Writing the
screen is what found that.

**Q131. What would you do differently?**

**Answer:** Extract a `:shared` module first — the drift in Q125 is the
measured cost of not doing it.

Also: split into one module per category, and **check protection levels before
assuming the platform is closed** — the assumption in Q40 cost real time.

**Q132. What is the weakest part of the project?**

**Answer:** Say it plainly; interviewers trust a candidate who can.

- The UI is a demo — it mixes app categories, and a dashboard is not a
  third-party category at all (Q22).
- The Android Auto module is **untested** — that needs a physical phone with
  DHU.
- It runs on an emulator, so the timing numbers are indicative, not vehicle
  numbers.

**Q133. Tell me about a time you were wrong.**

**Answer — the strongest thing you have. Give two.**

**The empty band under list screens.** You blamed leftover rows, then a host
crop. Four probes and a density test disproved both. It is a **121 dp host
reserve** that the app cannot remove.

**Gauge labels reading "ry 80%".** You blamed host cropping and added a
scaling workaround. Instrumentation showed the workaround did nothing. The
real cause was `Align.CENTER` text paint with a left-edge x coordinate. One
line.

The point to land: **measure before concluding, and prefer the test that can
disprove you.**

---

# PART 16 — Questions to ask them

These tell you which job it actually is, which matters more than anything else
in the interview.

1. Is this role app layer, CarService and framework, or VHAL and below?
2. Is your VHAL in-house C++ or from a supplier? How much CAN work is yours?
3. Do you carry your own `privapp-permissions` and sepolicy?
4. How do you test — hardware, bench, emulator, or a virtualised VHAL?
5. Is the cluster Android or a separate ECU?
6. Is the HMI a fork of CarSystemUI, or overlays on top of it?

Question 6 is the one that tells you whether the "HMI development" in the job
description means RROs or means Java in a platform build.

---

# APPENDIX A — Corrections to my old PDF notes

My two earlier PDFs — `AAOS_DeepLearning_Notes.pdf` and
`Complete AAOS & Android Auto Developer Guide.pdf` — are worth keeping. They
cover the core app layer well.

But nine facts in them are wrong. They were checked against a running
emulator, not against an opinion. **Rehearsing a wrong answer is worse than
not knowing**, so fix these first.

### 1. Two property IDs do not exist

| My PDF said | Device said | The real value |
|---|---|---|
| `PROP_ODOMETER = 0x11600101` | *not supported by HAL* | `0x11600204` (`PERF_ODOMETER`) |
| `PROP_ENGINE_ON = 0x11200401` | *not supported by HAL* | `0x11400409` (`IGNITION_STATE`) |

Speed, RPM, fuel and gear in the PDF are all correct.

### 2. The permission claim — the most important one

My PDF said car permissions *"are granted by the OEM, not user-granted like
normal permissions."*

Wrong for the common ones. `dumpsys package permissions` shows three different
levels: `CAR_SPEED` and `CAR_ENERGY` are **`dangerous`** — a normal app asks
at runtime and gets them. `CAR_INFO` is **`normal`**. Only engine detail,
mileage and VIN are privileged. See Q40.

### 3. Wrong adb command

My PDF wrote `adb shell dumpsys car_service inject-vhal-event`.

It is **`cmd car_service`**. `dumpsys` reads state; `cmd` sends commands.

### 4. A single inject does not stick for speed

Not an error in the PDF so much as a missing warning. For a CONTINUOUS
property the VHAL's generator overwrites your injected value on the next
sample. Use `inject-continuous-events`. See Q36.

### 5. The polling ViewModel

My PDF's `VehicleViewModel` polled the service in a loop with `delay(1000)`.
That is the design we deleted — see Q70 for why it is wrong, and Q129 for the
replacement.

### 6. `observeForever` leaks

My PDF used `liveData.observeForever { invalidate() }` in a `Screen`. It never
removes the observer. `Screen` is a `LifecycleOwner`, so use
`observe(this) { invalidate() }`. This was a real leak in our code.

### 7. Wrong API level comment

`compileSdk = 36 // Android 15` — API 35 is Android 15. 36 is Android 16.

### 8. "ListTemplate max 6 items" is not a rule

It is host-dependent. Your emulator's `ConstraintManager` reported **21**
cumulative content items. Read it at runtime — see Q19.

### 9. The two PDFs contradict each other on voice

One says voice navigation arrives through a `BroadcastReceiver`, the other
says the `NavigationCallback` singleton. Neither is the media path. The
correct split is in Q64: media voice arrives through
`MediaSession.onPlayFromSearch`, navigation through `NavigationManager`.

---

# APPENDIX B — Command cheat sheet

Every command here was run on a real emulator. Copy from this, not from
memory.

### Look at the platform

```bash
adb shell dumpsys car_service --list                    # 39 services
adb shell dumpsys car_service --services CarPropertyService
adb shell dumpsys car_service --services CarAudioService
adb shell dumpsys car_service --services CarOccupantZoneService
adb shell dumpsys car_service --services CarDrivingStateService
adb shell dumpsys car_service --services CarPowerManagementService
```

### Vehicle properties

```bash
# is it supported, what type, what area, what rate range?
adb shell cmd car_service get-carpropertyconfig PERF_VEHICLE_SPEED
adb shell cmd car_service get-carpropertyconfig            # all of them

# one-shot value, for ON_CHANGE properties
adb shell cmd car_service inject-vhal-event 289408009 2

# a stream, for CONTINUOUS properties: id value -s seconds -d duration
adb shell cmd car_service inject-continuous-events 291504647 31 -s 5 -d 180
```

### Permissions

```bash
adb shell dumpsys package permissions | grep -A2 CAR_SPEED   # read prot=
adb shell dumpsys package com.swapnil.smart.aaos | grep -i permission
adb shell pm grant --user 10 com.swapnil.smart.aaos android.car.permission.CAR_SPEED
```

### Install as a privileged system app

```bash
adb root && adb remount
adb shell mkdir -p /system/priv-app/SmartAaos
adb push app.apk /system/priv-app/SmartAaos/SmartAaos.apk
adb shell chmod 644 /system/priv-app/SmartAaos/SmartAaos.apk
adb push privapp-permissions-com.swapnil.smart.aaos.xml /system/etc/permissions/
adb reboot
```

### Vendor properties via JSON

```bash
adb push SmartAaosVendorProperties.json /vendor/etc/automotive/vhalconfig/
adb shell stop && adb shell start        # restart so the VHAL reloads
adb shell cmd car_service get-carpropertyconfig 0x21400001
```

### Runtime resource overlay

```bash
adb install overlay.apk
adb shell cmd overlay list
adb shell cmd overlay enable --user 0 com.swapnil.systemui.overlay
adb shell cmd overlay dump com.swapnil.systemui.overlay

# find valid resource names from an overlay that already ships
adb pull /product/overlay/googlecarui.theme.orange-com-android-systemui.apk
aapt2 dump resources googlecarui.theme.orange-com-android-systemui.apk
aapt2 dump overlayable CarSystemUI.apk       # empty = not overlayable
```

### Performance

```bash
# is it compiled or interpreted?
adb shell dumpsys package com.swapnil.smart.aaos | grep status=

# force compile
adb shell cmd package compile -m speed -f com.swapnil.smart.aaos

# trace startup
adb push startup.cfg /data/misc/perfetto-configs/
adb shell perfetto -c /data/misc/perfetto-configs/startup.cfg --txt \
    -o /data/misc/perfetto-traces/trace
adb pull /data/misc/perfetto-traces/trace
```

### Crashes and freezes

```bash
adb logcat -b crash
adb shell ls /data/tombstones/
adb shell cat /data/anr/traces.txt
adb bugreport
```

### Multi-user

```bash
adb shell pm list users               # 0 = system, 10 = driver
adb shell am get-current-user
adb shell dumpsys package com.android.systemui | grep userId
```

### Power and garage mode

```bash
adb shell cmd car_service garage-mode query
adb shell cmd car_service garage-mode on
```

---

# APPENDIX C — Numbers worth memorising

Concrete numbers are what separate a real answer from a recited one. All of
these were measured on your own emulator.

| Number | What it is |
|---|---|
| **39** | services inside `car_service` |
| **2** | binder hops for one property read |
| **10 Hz** | max sample rate for speed and rpm |
| **100 Hz** | max sample rate for fuel and battery |
| **121 dp** | host-reserved band below list and grid templates |
| **21** | cumulative content items the host allowed, not 6 |
| **0 and 3** | display ids — MAIN and CLUSTER |
| **0 and 1** | audio zones — primary and rear seat |
| **0 / 10** | user ids — system / driver |
| **366 -> 148 ms** | `bindApplication` before and after dexopt |
| **~40%** | startup regression from pushing an un-dexopted APK |
| **900 ms** | the emoji slices that block nothing |
| **14 / 14** | properties live after the privileged install |
| **~5 km/h** | above this, distraction rules apply |
| **2 seconds** | the longest a driver should look away |
| **QM** | the ASIL level of infotainment |
| **v3** | the AIDL VHAL version on your emulator |
| **35** | the API level of Android 15 |

---

# APPENDIX D — What 10 Bengaluru job descriptions actually asked for

Ten real job descriptions, counted by how many mentioned each skill.

| Skill | Mentioned in |
|---|---|
| **HMI / System UI / Car Launcher** | **5 / 10** |
| Java or Kotlin app development | 5 / 10 |
| AOSP / platform build | 4 / 10 |
| CarService / framework | 3 / 10 |
| C++ / native / VHAL | 3 / 10 |
| Test automation | 2 / 10 |
| CAN / vehicle networks | 2 / 10 |

### What this changes

The single most requested skill is **HMI and System UI** — the area covered in
Part 9. C++ and deep VHAL work is the **least** requested of the technical
areas, at 3 of 10.

So the study order that matches the market is:

1. **Part 9** — HMI, System UI, RRO. Most asked, and you now have a real
   overlay to talk about.
2. **Parts 3 and 4** — vehicle data and permissions. Your deepest area, and
   the one with the most verified detail.
3. **Part 2** — Car App Library. Assumed knowledge; being vague here is
   costly.
4. **Part 8** — AOSP internals. Asked in the platform roles.
5. **Part 11** — Soong and native debugging. Only for the deepest roles.

### The five different jobs hiding behind one title

"Android Automotive Developer" in Bengaluru means one of five jobs. Find out
which before you prepare:

1. **App developer** — templates, media, Kotlin. Parts 2, 5, 6.
2. **HMI / System UI developer** — CarSystemUI, Launcher, RRO. Part 9.
3. **Framework developer** — CarService, AIDL, system services. Parts 7, 8.
4. **Platform / BSP developer** — VHAL, C++, Soong, sepolicy. Parts 3, 11.
5. **Test automation engineer** — Appium, CI, VHAL injection. Part 13.

Part 16 is how you find out which one it is.

---

# APPENDIX E — How to revise this

**First pass — understand.** Read Parts 1 to 3 in order. Do not skip. Almost
every later answer depends on the property model in Part 3.

**Second pass — fix the wrong facts.** Read Appendix A properly. These are the
answers you would otherwise give confidently and incorrectly.

**Third pass — the numbers.** Appendix C. A candidate who says "10 Hz" instead
of "quite fast" sounds like someone who has actually run it.

**Fourth pass — out loud.** Part 15, the project questions, spoken. Especially
Q133. A rehearsed story about being wrong is the most convincing thing in an
interview, and it is the one thing nobody can prepare for you.

**Before every interview** — read Part 16, and pick the two parts that match
the role from Appendix D.

### The three answers to get right no matter what

If you only remember three things from this document:

1. **A missing car permission looks like missing hardware** — the property
   reports unsupported, it does not throw. (Q31)
2. **Speed is m/s, fuel is ml, battery is Wh.** (Q32)
3. **The car pushes, you do not poll** — and injected test events only reach
   subscribers. (Q27)

Each one has a story behind it that you personally lived through, which is why
they will hold up under follow-up questions.
