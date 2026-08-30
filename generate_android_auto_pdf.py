from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    HRFlowable, PageBreak
)
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_JUSTIFY

OUTPUT = "/Users/swapnilpatil/Documents/AndroidProjects/SmartAAOS/AndroidAuto_DeepLearning_Notes.pdf"

styles = getSampleStyleSheet()

# ── Styles ───────────────────────────────────────────────────────────────────
title_style = ParagraphStyle("DocTitle", parent=styles["Title"],
    fontSize=24, textColor=colors.HexColor("#0D1B2A"), spaceAfter=6, alignment=TA_CENTER)
subtitle_style = ParagraphStyle("DocSub", parent=styles["Normal"],
    fontSize=11, textColor=colors.HexColor("#1565C0"), spaceAfter=3, alignment=TA_CENTER)
topic_banner = ParagraphStyle("TopicBanner", parent=styles["Normal"],
    fontSize=14, textColor=colors.white, backColor=colors.HexColor("#0D47A1"),
    spaceBefore=16, spaceAfter=2, leftIndent=-6, rightIndent=-6,
    borderPad=8, fontName="Helvetica-Bold")
section_title = ParagraphStyle("SectionTitle", parent=styles["Normal"],
    fontSize=11, textColor=colors.HexColor("#0D47A1"), spaceBefore=10, spaceAfter=3,
    fontName="Helvetica-Bold")
what_style = ParagraphStyle("WhatIs", parent=styles["Normal"],
    fontSize=9.5, leading=15, spaceAfter=5, textColor=colors.HexColor("#1A237E"),
    backColor=colors.HexColor("#E8EAF6"), borderColor=colors.HexColor("#7986CB"),
    borderWidth=1, borderPad=7)
why_style = ParagraphStyle("Why", parent=styles["Normal"],
    fontSize=9.5, leading=15, spaceAfter=5, textColor=colors.HexColor("#1B5E20"),
    backColor=colors.HexColor("#E8F5E9"), borderColor=colors.HexColor("#66BB6A"),
    borderWidth=1, borderPad=7)
how_style = ParagraphStyle("How", parent=styles["Normal"],
    fontSize=9.5, leading=15, spaceAfter=5, textColor=colors.HexColor("#212121"),
    alignment=TA_JUSTIFY)
body_style = ParagraphStyle("Body", parent=styles["Normal"],
    fontSize=9.5, leading=15, spaceAfter=5, textColor=colors.HexColor("#212121"),
    alignment=TA_JUSTIFY)
bullet_style = ParagraphStyle("Bullet", parent=styles["Normal"],
    fontSize=9.5, leading=14, spaceAfter=3, leftIndent=14, textColor=colors.HexColor("#212121"))
code_style = ParagraphStyle("Code", parent=styles["Code"],
    fontSize=7.8, leading=11, fontName="Courier",
    backColor=colors.HexColor("#F8F8F8"), borderColor=colors.HexColor("#CFD8DC"),
    borderWidth=1, borderPad=7, spaceAfter=7, spaceBefore=4,
    textColor=colors.HexColor("#1A1A1A"))
note_style = ParagraphStyle("Note", parent=styles["Normal"],
    fontSize=9, leading=13, backColor=colors.HexColor("#FFF9C4"),
    borderColor=colors.HexColor("#F9A825"), borderWidth=1, borderPad=6,
    spaceAfter=6, textColor=colors.HexColor("#333333"))
tip_style = ParagraphStyle("Tip", parent=styles["Normal"],
    fontSize=9, leading=13, backColor=colors.HexColor("#E0F2F1"),
    borderColor=colors.HexColor("#00897B"), borderWidth=1, borderPad=6,
    spaceAfter=6, textColor=colors.HexColor("#004D40"))
key_style = ParagraphStyle("Key", parent=styles["Normal"],
    fontSize=9, leading=13, backColor=colors.HexColor("#FCE4EC"),
    borderColor=colors.HexColor("#E91E63"), borderWidth=1, borderPad=6,
    spaceAfter=6, textColor=colors.HexColor("#880E4F"))
warn_style = ParagraphStyle("Warn", parent=styles["Normal"],
    fontSize=9, leading=13, backColor=colors.HexColor("#FFF3E0"),
    borderColor=colors.HexColor("#F57C00"), borderWidth=1, borderPad=6,
    spaceAfter=6, textColor=colors.HexColor("#E65100"))
footer_style = ParagraphStyle("Footer", parent=styles["Normal"],
    fontSize=8, textColor=colors.HexColor("#9E9E9E"), alignment=TA_CENTER)

# ── Helpers ──────────────────────────────────────────────────────────────────
def Banner(num, title):
    return Paragraph(f"  TOPIC {num}   {title}", topic_banner)
def SecTitle(t): return Paragraph(t, section_title)
def WhatIs(t): return Paragraph(f"<b>What is it?</b>  {t}", what_style)
def WhyUse(t): return Paragraph(f"<b>Why do we use it?</b>  {t}", why_style)
def Body(t): return Paragraph(t, body_style)
def Bullet(t): return Paragraph(f"&nbsp;&nbsp;&nbsp;•&nbsp;&nbsp;{t}", bullet_style)
def Code(text):
    t = text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br/>").replace(" ","&nbsp;")
    return Paragraph(f'<font name="Courier" size="7.8">{t}</font>', code_style)
def Note(t): return Paragraph(f"<b>Note:</b>  {t}", note_style)
def Tip(t):  return Paragraph(f"<b>Tip:</b>  {t}", tip_style)
def Key(t):  return Paragraph(f"<b>Key Point:</b>  {t}", key_style)
def Warn(t): return Paragraph(f"<b>Warning:</b>  {t}", warn_style)
def SP(h=5): return Spacer(1, h)
def HR(): return HRFlowable(width="100%", thickness=0.5, color=colors.HexColor("#BDBDBD"), spaceAfter=4)

def Table2(data, widths, hc="#0D47A1", rows=None):
    if rows is None:
        rows = [colors.HexColor("#EEF2FF"), colors.white]
    t = Table(data, colWidths=widths, hAlign="LEFT")
    t.setStyle(TableStyle([
        ("BACKGROUND",     (0,0),(-1,0),  colors.HexColor(hc)),
        ("TEXTCOLOR",      (0,0),(-1,0),  colors.white),
        ("FONTNAME",       (0,0),(-1,0),  "Helvetica-Bold"),
        ("FONTSIZE",       (0,0),(-1,-1), 8.5),
        ("ROWBACKGROUNDS", (0,1),(-1,-1), rows),
        ("GRID",           (0,0),(-1,-1), 0.4, colors.HexColor("#BDBDBD")),
        ("VALIGN",         (0,0),(-1,-1), "MIDDLE"),
        ("LEFTPADDING",    (0,0),(-1,-1), 6),
        ("RIGHTPADDING",   (0,0),(-1,-1), 6),
        ("TOPPADDING",     (0,0),(-1,-1), 4),
        ("BOTTOMPADDING",  (0,0),(-1,-1), 4),
    ]))
    return t

# ════════════════════════════════════════════════════════════════════════════
doc = SimpleDocTemplate(OUTPUT, pagesize=A4,
    leftMargin=18*mm, rightMargin=18*mm, topMargin=18*mm, bottomMargin=18*mm)
story = []

# ── COVER ────────────────────────────────────────────────────────────────────
story += [
    SP(25),
    Paragraph("Android Auto", title_style),
    Paragraph("Complete Deep Learning Notes", subtitle_style),
    SP(5), HR(), SP(5),
    Paragraph("From Phone to Car Display — Everything You Need to Know", subtitle_style),
    Paragraph("by <b>Swapnil Patil</b>", subtitle_style),
    SP(12),
    Body(
        "Android Auto is Google's platform for projecting Android app experiences from a connected "
        "phone onto a car's infotainment display. Unlike Android Automotive OS (AAOS) — where the app "
        "runs natively on the car's hardware — Android Auto runs on your phone and streams the UI "
        "to the car screen over USB or wireless. This document covers every major concept required "
        "to build, test, and publish Android Auto apps — from setup and architecture to templates, "
        "media integration, voice control, security, and testing with the Desktop Head Unit (DHU)."
    ),
    SP(6),
    Body(
        "This guide is designed for developers who already understand general Android development "
        "and want to specifically master Android Auto. Every topic includes a plain-language "
        "explanation of what it is, why it matters for car apps, and detailed code examples "
        "with inline comments."
    ),
    SP(10),
]

toc = [
    ["#", "Topic", "Core Concept"],
    ["1",  "Android Auto Overview",                "What it is, how projection works, architecture"],
    ["2",  "Android Auto vs AAOS",                 "Deep comparison — when to use each"],
    ["3",  "Phone Module Setup",                   "app/ module, car_app artifact, build.gradle"],
    ["4",  "Manifest Setup for Android Auto",      "automotive_app_desc.xml, meta-data, permissions"],
    ["5",  "Android Auto App Categories",          "media, navigation, notification, IOT"],
    ["6",  "CarAppService & Session (Auto)",       "Same API, phone-side lifecycle differences"],
    ["7",  "Android Auto Connection Lifecycle",    "Phone ↔ car session states, events"],
    ["8",  "Template-Based UI on Android Auto",    "Same templates, phone rendering differences"],
    ["9",  "Media Apps on Android Auto",           "MediaBrowserService, MediaSession, controls"],
    ["10", "Navigation Apps on Android Auto",      "NavigationSession, SurfaceContainer, routing"],
    ["11", "Android Auto Permissions",             "No CAR_* permissions, phone-based security"],
    ["12", "HostValidator & Security",             "Trusted hosts, certificate pinning, production"],
    ["13", "Testing with Desktop Head Unit (DHU)", "DHU setup, launch, debug, simulate scenarios"],
    ["14", "Android Auto Emulator Testing",        "AVD setup, Android Auto in emulator"],
    ["15", "AAOS vs Android Auto Code Differences","What changes: manifest, artifacts, permissions"],
    ["16", "Publishing to Play Store (Auto)",      "automotive feature flag, review process, policy"],
]
story.append(Table2(toc, [10*mm, 62*mm, None], "#0D47A1"))
story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 1 — Android Auto Overview
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(1, "Android Auto Overview"))
story.append(SP(6))
story.append(WhatIs(
    "Android Auto is a platform from Google that allows your Android app to run on a "
    "phone and project a simplified, driver-safe UI onto a car's infotainment display. "
    "The phone does all the computation; the car screen just shows the result. "
    "The two devices communicate over USB (wired) or Wi-Fi (wireless Android Auto, Android 11+). "
    "The car's infotainment unit runs a Host app (provided by Google or the OEM) that renders "
    "the UI templates your app sends over the connection."
))
story.append(SP(4))
story.append(WhyUse(
    "Android Auto has a much larger reach than AAOS because it works with any Android phone "
    "on thousands of car models — you don't need a car with built-in AAOS hardware. "
    "As of 2024, Android Auto is available in over 500 car models worldwide. "
    "If you want to reach the broadest possible audience of car users today, "
    "Android Auto is the right target. AAOS will eventually be more common, "
    "but Android Auto is what most drivers currently use."
))
story.append(SP(5))
story.append(SecTitle("How Android Auto Projection Works"))
story.append(Code(
    "┌─────────────────────────────────────────────────────────┐\n"
    "│              ANDROID AUTO ARCHITECTURE                  │\n"
    "├─────────────────────────────────────────────────────────┤\n"
    "│                                                         │\n"
    "│  ┌─────────────────┐   USB / Wireless   ┌───────────┐  │\n"
    "│  │   ANDROID PHONE  │ ←────────────────→ │ CAR HEAD  │  │\n"
    "│  │                 │                    │   UNIT    │  │\n"
    "│  │  Your App       │   Templates (IPC)  │           │  │\n"
    "│  │  CarAppService  │ ──────────────────→│ Auto Host │  │\n"
    "│  │  MusicService   │                    │ (renders) │  │\n"
    "│  │  ExoPlayer      │ ←── Input Events ──│           │  │\n"
    "│  │  (all logic)    │   (touch, buttons) │ Display   │  │\n"
    "│  └─────────────────┘                    └───────────┘  │\n"
    "│                                                         │\n"
    "│  Phone = Compute + Logic     Car = Display + Input      │\n"
    "└─────────────────────────────────────────────────────────┘"
))
story.append(SP(4))
story.append(SecTitle("Key Components"))
comp = [
    ["Component",        "Where it runs", "Role"],
    ["Your App",         "Phone",         "All business logic, media playback, templates"],
    ["CarAppService",    "Phone",         "Entry point — car host binds to this"],
    ["Android Auto Host","Car head unit", "Renders templates, sends input events back"],
    ["Car screen",       "Car hardware",  "Displays projected UI, receives user touch"],
    ["MediaSession",     "Phone",         "Exposes playback controls to the car host"],
]
story.append(Table2(comp, [38*mm, 28*mm, None], "#1565C0",
    [colors.HexColor("#E3F2FD"), colors.white]))
story.append(SP(5))
story.append(SecTitle("What Android Auto Can Run"))
story += [
    Bullet("<b>Media apps</b> — music, podcasts, audiobooks (most common)"),
    Bullet("<b>Navigation apps</b> — turn-by-turn directions with map rendering"),
    Bullet("<b>Messaging apps</b> — read/reply messages via voice"),
    Bullet("<b>POI apps</b> — points of interest, parking, EV charging"),
    SP(4),
]
story.append(Key(
    "Android Auto only supports apps in specific categories. You cannot build a generic "
    "productivity or social app for Android Auto — only the approved categories above."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 2 — Android Auto vs AAOS
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(2, "Android Auto vs Android Automotive OS (AAOS)"))
story.append(SP(6))
story.append(WhatIs(
    "Android Auto and AAOS both use the Car App Library and look similar in code, "
    "but they are fundamentally different platforms. Android Auto is a projection system — "
    "your app runs on the phone, the car just shows the picture. "
    "AAOS is a full embedded OS — your app runs on the car's own computer without any phone. "
    "Understanding this distinction is critical for deciding which platform to target "
    "and how to structure your project."
))
story.append(SP(5))

diff = [
    ["Aspect",               "Android Auto",                        "Android Automotive OS"],
    ["App runs on",          "Android phone",                       "Car's own CPU/ECU"],
    ["Phone required?",      "Yes — always connected",              "No — completely standalone"],
    ["Car hardware needed?", "Any car with AA support (500+ models)","AAOS car (Volvo, Polestar, etc.)"],
    ["Internet access",      "Phone's SIM / WiFi",                  "Car's built-in SIM / WiFi"],
    ["Gradle module",        "app/ (uses artifact: app)",           "automotive/ (uses: app-automotive)"],
    ["Car API access",       "None (no CarPropertyManager)",        "Full OEM APIs available"],
    ["Vehicle sensors",      "Not accessible",                      "Speed, RPM, fuel, HVAC, etc."],
    ["OEM integration",      "Limited to screen projection",        "Deep native integration"],
    ["Market reach",         "Very large (billions of Android users)","Growing (newer AAOS cars)"],
    ["Update mechanism",     "User updates phone app",              "OTA update or Play Store on car"],
    ["Background execution", "On phone — normal Android rules",     "On car — AAOS power management"],
    ["Testing",              "Desktop Head Unit (DHU) or real car", "AAOS emulator or real AAOS car"],
    ["minSdk",               "API 23 minimum (recommended 26)",     "API 29 minimum (required)"],
]
story.append(Table2(diff, [40*mm, 65*mm, None], "#0D47A1",
    [colors.HexColor("#E8EAF6"), colors.white]))
story.append(SP(5))
story.append(SecTitle("Which Platform Should You Target?"))
story += [
    Bullet("<b>Android Auto</b> — if you want maximum reach today, simpler integration, no vehicle data needed"),
    Bullet("<b>AAOS</b> — if you need vehicle sensor data, deep OEM integration, or a standalone car-first experience"),
    Bullet("<b>Both</b> — the Car App Library makes it easy to support both from one codebase with separate modules"),
    SP(4),
]
story.append(Tip(
    "For a new app targeting car users in 2024-2025, support both: use the shared Car App Library "
    "for common UI logic, with the app/ module for Android Auto and automotive/ for AAOS. "
    "Most Car App Library code is identical between the two."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 3 — Phone Module Setup
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(3, "Phone Module Setup — app/ Module"))
story.append(SP(6))
story.append(WhatIs(
    "The app/ module is the Android Auto phone module. It is a standard Android application module "
    "that targets a regular Android phone. Your CarAppService, Session, and Screen classes live here "
    "(or in a shared module). The critical difference from the AAOS automotive/ module is the "
    "Gradle dependency: you use the 'app' artifact (not 'app-automotive') from the Car App Library. "
    "This artifact includes classes for phone-side rendering and communication with the car host."
))
story.append(SP(4))
story.append(WhyUse(
    "The app/ module is what gets installed on the user's phone via the Play Store. "
    "When the user connects their phone to a car that supports Android Auto, the car's "
    "Android Auto host discovers and connects to your CarAppService in this module. "
    "Without the correct artifact (app vs app-automotive), the car host won't recognize "
    "your service and the app won't appear in the car."
))
story.append(SP(5))
story.append(SecTitle("app/build.gradle.kts"))
story.append(Code(
    "plugins {\n"
    "    id(\"com.android.application\")\n"
    "    id(\"org.jetbrains.kotlin.android\")\n"
    "}\n\n"
    "android {\n"
    "    compileSdk = 36\n\n"
    "    defaultConfig {\n"
    "        applicationId = \"com.swapnil.smart.aaos\"  // same as automotive module\n"
    "        minSdk    = 23     // Android Auto works from API 23\n"
    "                           // (recommended: 26 for full feature support)\n"
    "        targetSdk = 36\n"
    "        versionCode = 1\n"
    "        versionName = \"1.0\"\n"
    "    }\n\n"
    "    compileOptions {\n"
    "        sourceCompatibility = JavaVersion.VERSION_11\n"
    "        targetCompatibility = JavaVersion.VERSION_11\n"
    "    }\n"
    "    kotlinOptions { jvmTarget = \"11\" }\n"
    "}\n\n"
    "dependencies {\n"
    "    // KEY DIFFERENCE: use 'app' NOT 'app-automotive'\n"
    "    implementation(libs.androidx.car.app)              // phone artifact\n\n"
    "    // Same media dependencies as automotive module\n"
    "    implementation(libs.media3.exoplayer)\n"
    "    implementation(libs.androidx.media)\n"
    "    implementation(libs.androidx.lifecycle.viewmodel)\n"
    "    implementation(libs.androidx.lifecycle.livedata)\n"
    "}"
))
story.append(SecTitle("Artifact Difference — Critical"))
art = [
    ["Module",      "Artifact",       "Class included",                    "When to use"],
    ["app/",        "app",            "CarAppActivity (phone projection)",  "Android Auto (phone module)"],
    ["automotive/", "app-automotive", "CarAppActivity (native AAOS)",      "AAOS (car module)"],
]
story.append(Table2(art, [18*mm, 28*mm, 68*mm, None], "#B71C1C",
    [colors.HexColor("#FFEBEE"), colors.white]))
story.append(SP(4))
story.append(SecTitle("Root build.gradle.kts — Multi-Module Setup"))
story.append(Code(
    "// Root build.gradle.kts\n"
    "plugins {\n"
    "    alias(libs.plugins.android.application) apply false\n"
    "    alias(libs.plugins.kotlin.android)      apply false\n"
    "}\n\n"
    "// settings.gradle.kts — include both modules\n"
    "include(\":app\")        // Android Auto phone module\n"
    "include(\":automotive\") // AAOS car module"
))
story.append(Warn(
    "Do NOT add the automotive module as a dependency of the app module (or vice versa). "
    "They are separate applications published independently to the Play Store. "
    "Shared code should go into a separate :shared library module."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 4 — Manifest Setup
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(4, "Manifest Setup for Android Auto"))
story.append(SP(6))
story.append(WhatIs(
    "The AndroidManifest.xml for an Android Auto app has specific declarations that differ from "
    "both a regular phone app and an AAOS app. You must declare the car application metadata, "
    "link the automotive_app_desc.xml category file, and declare your CarAppService with the "
    "correct intent filter. Incorrect manifest setup is the #1 reason Android Auto apps "
    "don't appear in the car launcher."
))
story.append(SP(4))
story.append(WhyUse(
    "The Android Auto host on the car scans all installed apps on the connected phone, "
    "looking for apps with the CarAppService intent filter and the correct metadata. "
    "Without the meta-data declaration, your app is invisible to the car host. "
    "Without the correct category in automotive_app_desc.xml, your app won't appear in "
    "the right section of the car launcher (media, navigation, etc.)."
))
story.append(SP(5))
story.append(SecTitle("app/AndroidManifest.xml — Complete Setup"))
story.append(Code(
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
    "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n\n"
    "    <!-- Required for background music playback -->\n"
    "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\"/>\n"
    "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK\"/>\n"
    "    <uses-permission android:name=\"android.permission.INTERNET\"/>\n\n"
    "    <!-- NOTE: No CAR_SPEED, CAR_ENGINE etc. — Android Auto has NO vehicle sensor access -->\n\n"
    "    <application\n"
    "        android:label=\"Smart Auto\"\n"
    "        android:icon=\"@mipmap/ic_launcher\">\n\n"
    "        <!-- CRITICAL: links to automotive_app_desc.xml -->\n"
    "        <!-- This is how the Android Auto host discovers your app category -->\n"
    "        <meta-data\n"
    "            android:name=\"com.google.android.gms.car.application\"\n"
    "            android:resource=\"@xml/automotive_app_desc\"/>\n\n"
    "        <!-- Minimum Car App API level (1 = widest compatibility) -->\n"
    "        <meta-data\n"
    "            android:name=\"androidx.car.app.minCarApiLevel\"\n"
    "            android:value=\"1\"/>\n\n"
    "        <!-- CarAppActivity: required for Android Auto phone projection -->\n"
    "        <activity\n"
    "            android:name=\"androidx.car.app.activity.CarAppActivity\"\n"
    "            android:exported=\"true\"\n"
    "            android:launchMode=\"singleTask\"\n"
    "            android:theme=\"@android:style/Theme.DeviceDefault.NoActionBar\">\n"
    "            <intent-filter>\n"
    "                <action android:name=\"android.intent.action.MAIN\"/>\n"
    "                <category android:name=\"android.intent.category.LAUNCHER\"/>\n"
    "            </intent-filter>\n"
    "        </activity>\n\n"
    "        <!-- CarAppService: the Android Auto host binds to this -->\n"
    "        <service\n"
    "            android:name=\".car.SmartCarAppService\"\n"
    "            android:exported=\"true\">\n"
    "            <intent-filter>\n"
    "                <!-- Required: identifies this as a Car App service -->\n"
    "                <action android:name=\"androidx.car.app.CarAppService\"/>\n"
    "                <!-- Required: declares media category -->\n"
    "                <category android:name=\"androidx.car.app.category.MEDIA\"/>\n"
    "            </intent-filter>\n"
    "        </service>\n\n"
    "        <!-- Music service: Android Auto uses this for media controls -->\n"
    "        <service\n"
    "            android:name=\".media.SmartMusicService\"\n"
    "            android:exported=\"true\">\n"
    "            <intent-filter>\n"
    "                <action android:name=\"android.media.browse.MediaBrowserService\"/>\n"
    "                <action android:name=\"android.media.action.MEDIA_PLAY_FROM_SEARCH\"/>\n"
    "            </intent-filter>\n"
    "        </service>\n\n"
    "        <!-- MediaButtonReceiver: handles steering wheel media buttons -->\n"
    "        <receiver android:name=\"androidx.media.session.MediaButtonReceiver\"\n"
    "                  android:exported=\"true\">\n"
    "            <intent-filter>\n"
    "                <action android:name=\"android.intent.action.MEDIA_BUTTON\"/>\n"
    "            </intent-filter>\n"
    "        </receiver>\n\n"
    "    </application>\n"
    "</manifest>"
))
story.append(SecTitle("automotive_app_desc.xml — Android Auto vs AAOS"))
story.append(Code(
    "<!-- app/src/main/res/xml/automotive_app_desc.xml -->\n"
    "<!-- For Android Auto (phone module) -->\n"
    "<automotiveApp>\n"
    "    <uses name=\"media\" />\n"
    "    <!-- Other options: \"navigation\", \"notification\", \"video\" -->\n"
    "</automotiveApp>\n\n"
    "<!-- AAOS uses the SAME file format in automotive/ module -->\n"
    "<!-- The Car App Library handles both from the same XML -->"
))
story.append(Note(
    "The automotive_app_desc.xml file must be placed in res/xml/ of the app/ module for Android Auto, "
    "and in res/xml/ of the automotive/ module for AAOS. They can have the same content if your "
    "app supports the same categories on both platforms."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 5 — App Categories
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(5, "Android Auto App Categories"))
story.append(SP(6))
story.append(WhatIs(
    "Android Auto only allows specific categories of apps to run on the car display. "
    "These categories are declared in automotive_app_desc.xml and the CarAppService intent filter. "
    "Each category gets different UI templates, permissions, and placement in the car launcher. "
    "Submitting an app to the wrong category causes Play Store rejection."
))
story.append(SP(4))
story.append(WhyUse(
    "Google restricts what kind of apps can run on Android Auto to protect driver safety. "
    "Only categories that have a legitimate, driver-safe use case in a car are allowed. "
    "Each category also gets purpose-built templates — a navigation app gets map rendering support, "
    "a media app gets playback controls, a messaging app gets TTS read-aloud support. "
    "Choosing the right category unlocks the correct set of templates and APIs."
))
story.append(SP(5))

cats = [
    ["Category",         "Intent Filter Category",                   "Use Case",                         "Key Templates"],
    ["Media",            "androidx.car.app.category.MEDIA",          "Music, podcast, audiobook, radio", "ListTemplate, PaneTemplate"],
    ["Navigation",       "androidx.car.app.category.NAVIGATION",     "Turn-by-turn directions, maps",    "NavigationTemplate + SurfaceContainer"],
    ["Point of Interest","androidx.car.app.category.POI",            "Parking, EV charging, restaurants","PlaceListMapTemplate"],
    ["IOT",              "androidx.car.app.category.IOT",            "Smart devices, remote control",    "ListTemplate, MessageTemplate"],
]
story.append(Table2(cats, [22*mm, 48*mm, 50*mm, None], "#1565C0",
    [colors.HexColor("#E3F2FD"), colors.white]))
story.append(SP(5))

story.append(SecTitle("Media Category — Most Common"))
story.append(Code(
    "<!-- automotive_app_desc.xml -->\n"
    "<automotiveApp>\n"
    "    <uses name=\"media\" />\n"
    "</automotiveApp>\n\n"
    "<!-- AndroidManifest.xml -->\n"
    "<service android:name=\".car.SmartCarAppService\" android:exported=\"true\">\n"
    "    <intent-filter>\n"
    "        <action android:name=\"androidx.car.app.CarAppService\"/>\n"
    "        <category android:name=\"androidx.car.app.category.MEDIA\"/>  <!-- media -->\n"
    "    </intent-filter>\n"
    "</service>"
))
story.append(SecTitle("Navigation Category — Special Surface Access"))
story.append(Code(
    "<!-- automotive_app_desc.xml -->\n"
    "<automotiveApp>\n"
    "    <uses name=\"navigation\" />\n"
    "</automotiveApp>\n\n"
    "<!-- AndroidManifest.xml -->\n"
    "<service android:name=\".car.SmartCarAppService\" android:exported=\"true\">\n"
    "    <intent-filter>\n"
    "        <action android:name=\"androidx.car.app.CarAppService\"/>\n"
    "        <category android:name=\"androidx.car.app.category.NAVIGATION\"/> <!-- nav -->\n"
    "    </intent-filter>\n"
    "</service>\n\n"
    "<!-- Navigation apps get access to SurfaceContainer for drawing a map -->\n"
    "<!-- Only navigation category apps can use NavigationTemplate -->"
))
story.append(Key(
    "SmartAAOS is a media app — it uses androidx.car.app.category.MEDIA. "
    "Do not mix categories (e.g., listing both media and navigation) — this causes Play Store rejection."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 6 — CarAppService & Session (Android Auto)
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(6, "CarAppService & Session on Android Auto"))
story.append(SP(6))
story.append(WhatIs(
    "CarAppService and Session work the same way in Android Auto as in AAOS — "
    "CarAppService is the entry point the car host binds to, and Session manages one active "
    "car display connection. The code is identical between the app/ and automotive/ modules "
    "because both use the same Car App Library API. The difference is under the hood: "
    "in Android Auto the Session runs on the phone and the templates are projected to the car; "
    "in AAOS the Session runs natively on the car's hardware."
))
story.append(SP(4))
story.append(WhyUse(
    "Because the Car App Library abstracts the platform, you can share CarAppService and Screen "
    "implementations between Android Auto and AAOS. In SmartAAOS, the code in SmartCarAppService.kt, "
    "SmartSession.kt, and all Screen files would work with no changes if moved to the app/ module. "
    "This is the main benefit of the Car App Library architecture."
))
story.append(SP(5))
story.append(SecTitle("CarAppService — Identical Code for Both Platforms"))
story.append(Code(
    "// This code works in BOTH app/ (Android Auto) AND automotive/ (AAOS)\n"
    "class SmartCarAppService : CarAppService() {\n\n"
    "    override fun createHostValidator(): HostValidator {\n"
    "        // Android Auto: validates the Android Auto host app on the car\n"
    "        // AAOS: validates the OEM car host\n"
    "        // In production use HostValidator.Builder() with known host signatures\n"
    "        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR  // development only\n"
    "    }\n\n"
    "    override fun onCreateSession(): Session = SmartSession()\n\n"
    "    override fun onDestroy() {\n"
    "        super.onDestroy()\n"
    "        CarViewModelStore.clear()\n"
    "    }\n"
    "}"
))
story.append(SecTitle("Session Lifecycle Differences"))
story.append(Code(
    "// Android Auto Session lifecycle events\n"
    "class SmartSession : Session() {\n\n"
    "    override fun onCreateScreen(intent: Intent): Screen {\n"
    "        // Called when Android Auto connects to your phone app\n"
    "        // The car screen is now showing your app's UI\n"
    "        return HomeScreen(carContext)\n"
    "    }\n\n"
    "    override fun onNewIntent(intent: Intent) {\n"
    "        // Called for voice commands or deep links while session is active\n"
    "        handleVoiceIntent(intent)\n"
    "    }\n\n"
    "    // NOTE: No onStart/onStop in Session — lifecycle is managed by the car host\n"
    "    // The session ends when the user disconnects the phone from the car\n"
    "}"
))

lifecycle = [
    ["Event",                  "Trigger",                                   "What happens"],
    ["onCreateSession()",      "Car host binds to CarAppService",           "Session created, screen prepared"],
    ["onCreateScreen()",       "First screen requested by car host",        "Return your HomeScreen"],
    ["onNewIntent()",          "New intent arrives (voice, deep link)",     "Handle voice commands"],
    ["Session active",         "Phone connected to car",                    "All screens visible, interactive"],
    ["Session ends",           "Phone disconnected / user exits app",       "Service may be unbound"],
]
story.append(Table2(lifecycle, [38*mm, 60*mm, None], "#1B5E20",
    [colors.HexColor("#E8F5E9"), colors.white]))
story.append(SP(4))
story.append(Tip(
    "In Android Auto, the Session can also end when the car is turned off or the user "
    "switches to another app. Always handle cleanup in CarAppService.onDestroy() rather "
    "than assuming the session will end cleanly."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 7 — Android Auto Connection Lifecycle
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(7, "Android Auto Connection Lifecycle"))
story.append(SP(6))
story.append(WhatIs(
    "The Android Auto connection lifecycle describes all the states a phone-to-car connection goes through: "
    "from initial USB/wireless pairing, through the car host discovering and binding your app, "
    "to the session becoming active and the car screen showing your UI, all the way to disconnection. "
    "Understanding this lifecycle is critical for managing resources correctly — "
    "starting and stopping services, requesting audio focus, and cleaning up connections."
))
story.append(SP(4))
story.append(WhyUse(
    "Resource leaks and crashes in Android Auto apps almost always happen because developers "
    "don't properly handle lifecycle events — leaving ExoPlayer running after disconnect, "
    "not releasing MediaBrowserCompat connections, or holding audio focus after the session ends. "
    "Knowing exactly when each callback fires helps you write clean, reliable code."
))
story.append(SP(5))
story.append(Code(
    "Full Android Auto Connection Lifecycle:\n"
    "\n"
    "1. User connects phone to car (USB or Wireless)\n"
    "   └─ Android Auto app on car launches and discovers AA-compatible apps on phone\n"
    "\n"
    "2. Car host binds to CarAppService on phone\n"
    "   └─ CarAppService.onCreateHostValidator() called\n"
    "   └─ CarAppService.onCreateSession() called → Session object created\n"
    "\n"
    "3. Car host requests first screen\n"
    "   └─ Session.onCreateScreen(intent) called → HomeScreen returned\n"
    "   └─ HomeScreen.onGetTemplate() called → ListTemplate built and sent to car\n"
    "   └─ Car screen renders the template — user sees your app\n"
    "\n"
    "4. Session is ACTIVE\n"
    "   └─ User taps/speaks → car host sends input event back to phone\n"
    "   └─ setOnClickListener / MediaControllerCompat callbacks fire on phone\n"
    "   └─ Screen.invalidate() → new template sent to car → screen updates\n"
    "\n"
    "5. New intent arrives (voice command)\n"
    "   └─ Session.onNewIntent(intent) called\n"
    "   └─ handleVoiceIntent() → playFromSearch() → PlayerScreen pushed\n"
    "\n"
    "6. User exits app or disconnects phone\n"
    "   └─ ScreenManager cleared\n"
    "   └─ Session destroyed\n"
    "   └─ CarAppService.onDestroy() called → clean up ViewModels, release resources\n"
    "   └─ MusicService continues running in foreground (phone-side) if music is playing"
))
story.append(SP(4))
story.append(Note(
    "The music service (SmartMusicService) is NOT stopped when the Android Auto session ends — "
    "it continues running on the phone as a foreground service. The user can still see the "
    "media notification on their phone and control playback from there."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 8 — Template-Based UI on Android Auto
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(8, "Template-Based UI on Android Auto"))
story.append(SP(6))
story.append(WhatIs(
    "Android Auto uses the exact same Car App Library templates as AAOS — ListTemplate, PaneTemplate, "
    "MessageTemplate, NavigationTemplate, etc. The template code you write is platform-agnostic; "
    "the Car App Library serializes it and sends it to the car host, which renders it. "
    "On Android Auto, the car head unit's Android Auto app does the rendering. "
    "On AAOS, the OEM's template host does it. Your code doesn't change."
))
story.append(SP(4))
story.append(WhyUse(
    "This is the greatest strength of the Car App Library — write once, run on both platforms. "
    "The template system guarantees driver safety by enforcing strict limits on text, "
    "actions, and screen complexity. You cannot bypass these limits even if you wanted to — "
    "the library validates your template structure before serializing it."
))
story.append(SP(5))
story.append(SecTitle("Full Template Catalogue"))
tmpl = [
    ["Template",             "Category",  "Description",                                    "Key Limit"],
    ["ListTemplate",         "All",       "Scrollable list with rows and images",            "Max 6 items (API 1)"],
    ["PaneTemplate",         "All",       "Detail pane with rows + action buttons",          "Max 2 actions"],
    ["MessageTemplate",      "All",       "Simple title + message text",                     "Max 2 actions"],
    ["GridTemplate",         "All",       "Grid of image tiles (album art grid)",            "Max 8 items"],
    ["SearchTemplate",       "All",       "Search input field + results list",               "Max 6 results"],
    ["NavigationTemplate",   "Nav only",  "Full-screen map + turn-by-turn overlay",          "Nav category only"],
    ["PlaceListMapTemplate", "POI/Nav",   "Map + list of nearby places",                    "Max 6 places"],
    ["RoutePreviewTemplate", "Nav only",  "Route overview before starting navigation",       "Max 3 routes"],
    ["SignInTemplate",       "All (API4)","Login form for account-based apps",               "API 4+"],
    ["LongMessageTemplate",  "All (API2)","Scrollable long text (terms, etc.)",              "API 2+"],
]
story.append(Table2(tmpl, [36*mm, 18*mm, 60*mm, None], "#1565C0",
    [colors.HexColor("#E3F2FD"), colors.white]))
story.append(SP(5))
story.append(SecTitle("CarIcon — Images in Templates"))
story.append(Body(
    "All images in templates must be wrapped in a CarIcon. You can create CarIcons from "
    "a Bitmap (e.g., album art loaded with Glide), a vector drawable resource, or a "
    "standard system icon. The car host scales them appropriately for the display."
))
story.append(Code(
    "// From a Bitmap (album art loaded from URL)\n"
    "val carIcon = CarIcon.Builder(\n"
    "    IconCompat.createWithBitmap(bitmap)\n"
    ").build()\n\n"
    "// From a drawable resource\n"
    "val carIcon = CarIcon.Builder(\n"
    "    IconCompat.createWithResource(carContext, R.drawable.ic_music_note)\n"
    ").build()\n\n"
    "// From a standard Car App icon\n"
    "val carIcon = CarIcon.APP_ICON  // built-in: app, alert, back, compose, error\n\n"
    "// Use in a Row\n"
    "Row.Builder()\n"
    "    .setTitle(song.title)\n"
    "    .setImage(carIcon, Row.IMAGE_TYPE_LARGE)  // LARGE = big album art thumbnail\n"
    "    .build()"
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 9 — Media Apps on Android Auto
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(9, "Media Apps on Android Auto"))
story.append(SP(6))
story.append(WhatIs(
    "A media app on Android Auto is built around three layers: "
    "(1) MediaBrowserServiceCompat — the service that exposes your media catalogue and hosts the MediaSession; "
    "(2) MediaSession — the control hub that receives play/pause/skip commands from the car; "
    "(3) ExoPlayer (or MediaPlayer) — the actual audio engine on the phone. "
    "The Android Auto host connects to your MediaBrowserServiceCompat using MediaBrowserCompat, "
    "discovers your song list via onLoadChildren(), and controls playback via MediaSession callbacks."
))
story.append(SP(4))
story.append(WhyUse(
    "Android Auto's media controls (on-screen buttons, steering wheel buttons, voice commands, "
    "and the Android Auto 'media' UI section) all work through the MediaSession and "
    "MediaBrowserServiceCompat APIs. Without implementing these, the Android Auto host "
    "cannot control your app's playback and your app will not appear in the car's media picker. "
    "The media architecture is mandatory for any music or audio app on Android Auto."
))
story.append(SP(5))
story.append(SecTitle("Android Auto Media Architecture"))
story.append(Code(
    "┌──────────────────────────────────────────────────────────────────┐\n"
    "│  ANDROID AUTO MEDIA ARCHITECTURE (phone-side)                    │\n"
    "├──────────────────────────────────────────────────────────────────┤\n"
    "│                                                                  │\n"
    "│  ┌────────────────────────────────────────────────────────────┐  │\n"
    "│  │  SmartMusicService extends MediaBrowserServiceCompat       │  │\n"
    "│  │                                                            │  │\n"
    "│  │  onGetRoot()      ← AA host asks: can you connect?        │  │\n"
    "│  │  onLoadChildren() ← AA host asks: what songs do you have? │  │\n"
    "│  │                                                            │  │\n"
    "│  │  ┌──────────────────────────┐  ┌──────────────────────┐  │  │\n"
    "│  │  │  MediaSessionCompat      │  │  ExoPlayer           │  │  │\n"
    "│  │  │  sessionToken ──────────────→ (audio engine)       │  │  │\n"
    "│  │  │  Callback: onPlay()      │  │  stream from URL     │  │  │\n"
    "│  │  │  Callback: onPause()     │  │  handle errors       │  │  │\n"
    "│  │  │  Callback: onSkipNext()  │  │  auto-advance        │  │  │\n"
    "│  │  └──────────────────────────┘  └──────────────────────┘  │  │\n"
    "│  └────────────────────────────────────────────────────────────┘  │\n"
    "│                          ↑ ↓ IPC                                 │\n"
    "│  ┌────────────────────────────────────────────────────────────┐  │\n"
    "│  │  PlayerScreen (Car App UI — also on phone, projected)      │  │\n"
    "│  │  MediaBrowserCompat ────────────────────────────────────→  │  │\n"
    "│  │  MediaControllerCompat ── transportControls ─────────────→ │  │\n"
    "│  └────────────────────────────────────────────────────────────┘  │\n"
    "│                          ↑ Projected to car screen               │\n"
    "└──────────────────────────────────────────────────────────────────┘"
))
story.append(SecTitle("Media Queue — Providing Song List to Android Auto"))
story.append(Body(
    "In addition to onLoadChildren(), Android Auto's media UI shows a playback queue. "
    "You should set the queue on MediaSession so the car's native media player screen "
    "can display 'Up Next' properly."
))
story.append(Code(
    "// Set the playback queue on MediaSession\n"
    "val queue = MusicData.songs.mapIndexed { index, song ->\n"
    "    val desc = MediaDescriptionCompat.Builder()\n"
    "        .setMediaId(song.id)\n"
    "        .setTitle(song.title)\n"
    "        .setSubtitle(song.artist)\n"
    "        .build()\n"
    "    MediaSessionCompat.QueueItem(desc, index.toLong())\n"
    "}\n"
    "session.setQueue(queue)\n"
    "session.setQueueTitle(\"Smart AAOS Playlist\")\n\n"
    "// Also set active queue item (currently playing song)\n"
    "session.setPlaybackState(\n"
    "    PlaybackStateCompat.Builder()\n"
    "        .setState(STATE_PLAYING, position, 1.0f)\n"
    "        .setActiveQueueItemId(currentIndex.toLong())  // highlight current song\n"
    "        .build()\n"
    ")"
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 10 — Navigation Apps on Android Auto
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(10, "Navigation Apps on Android Auto"))
story.append(SP(6))
story.append(WhatIs(
    "Navigation apps on Android Auto are a special category that gets access to a Surface — "
    "a raw drawing canvas provided by the car host where you can render a map using Canvas2D or OpenGL. "
    "On top of this map surface, you overlay a NavigationTemplate with turn-by-turn instructions, "
    "ETA, distance, and action buttons. Navigation apps must declare the NAVIGATION category and "
    "implement NavigationSession instead of a plain Session."
))
story.append(SP(4))
story.append(WhyUse(
    "The navigation category is special because rendering a map requires custom drawing — "
    "you cannot represent a dynamic map with Car App Library templates alone. "
    "The SurfaceContainer API bridges this gap: it gives your app a guaranteed portion of the "
    "screen to draw on (the map area), while the car host renders the overlaid template UI "
    "(turn-by-turn card, action buttons) on top. This is how Google Maps, Waze, and other "
    "navigation apps work on Android Auto."
))
story.append(SP(5))
story.append(SecTitle("NavigationSession — Session Subclass for Nav Apps"))
story.append(Code(
    "// Navigation apps extend NavigationSession instead of Session\n"
    "class SmartNavigationSession : NavigationSession() {\n\n"
    "    override fun onCreateScreen(intent: Intent): Screen {\n"
    "        return MapScreen(carContext)\n"
    "    }\n\n"
    "    // Called when the car host provides a Surface to draw the map on\n"
    "    override fun onCreateMap(surfaceContainer: SurfaceContainer): MapController {\n"
    "        return SmartMapController(carContext, surfaceContainer)\n"
    "    }\n"
    "}"
))
story.append(SecTitle("SurfaceContainer — Drawing the Map"))
story.append(Code(
    "class SmartMapController(\n"
    "    private val carContext: CarContext,\n"
    "    private val surfaceContainer: SurfaceContainer\n"
    ") : MapController() {\n\n"
    "    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {\n"
    "        // Surface is ready — start drawing your map\n"
    "        val surface = surfaceContainer.surface\n"
    "        val canvas = surface.lockCanvas(null)\n"
    "        // Draw map tiles, route, current location...\n"
    "        canvas.drawBitmap(mapTileBitmap, 0f, 0f, null)\n"
    "        surface.unlockCanvasAndPost(canvas)\n"
    "    }\n\n"
    "    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {\n"
    "        // Surface gone — stop drawing, release resources\n"
    "    }\n\n"
    "    override fun onVisibleAreaChanged(visibleArea: Rect) {\n"
    "        // Car host tells you which area of the surface is visible\n"
    "        // (the rest may be covered by the template overlay)\n"
    "        redrawMap(visibleArea)\n"
    "    }\n"
    "}"
))
story.append(SecTitle("NavigationTemplate"))
story.append(Code(
    "// NavigationTemplate overlays on top of the map surface\n"
    "NavigationTemplate.Builder()\n"
    "    .setNavigationInfo(\n"
    "        RoutingInfo.Builder()\n"
    "            .setCurrentStep(\n"
    "                Step.Builder(\"Turn right onto MG Road\")\n"
    "                    .setManeuver(Maneuver.Builder(Maneuver.TYPE_TURN_RIGHT).build())\n"
    "                    .setRoad(\"MG Road\")\n"
    "                    .build()\n"
    "            )\n"
    "            .setDistanceToStep(Distance.create(0.5, Distance.UNIT_KILOMETERS))\n"
    "            .build()\n"
    "    )\n"
    "    .setDestinationTravelEstimate(\n"
    "        TravelEstimate.Builder(\n"
    "            Distance.create(12.5, Distance.UNIT_KILOMETERS),\n"
    "            DateTimeWithZone.create(/* ETA */ ...)\n"
    "        ).build()\n"
    "    )\n"
    "    .setActionStrip(ActionStrip.Builder().addAction(muteAction).build())\n"
    "    .build()"
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 11 — Permissions
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(11, "Android Auto Permissions"))
story.append(SP(6))
story.append(WhatIs(
    "Android Auto apps use standard Android permissions declared in the phone module's manifest. "
    "There are NO special car permissions like CAR_SPEED or CAR_ENGINE_DETAILED — those are "
    "exclusive to AAOS where the app runs on the car hardware and can access vehicle sensors. "
    "Android Auto apps only access data available on the phone (storage, internet, microphone, etc.). "
    "Vehicle data (speed, RPM) is simply not accessible from Android Auto."
))
story.append(SP(4))
story.append(WhyUse(
    "Understanding the permission boundary between Android Auto and AAOS prevents a common "
    "developer mistake: assuming Android Auto can access vehicle sensors. "
    "It cannot. If your app needs real vehicle data (speed for driving restrictions, "
    "fuel level for alerts), you must target AAOS. "
    "Android Auto apps use only phone-side data sources."
))
story.append(SP(5))

perms = [
    ["Permission",                                   "Platform",   "Used For"],
    ["FOREGROUND_SERVICE",                           "Both",       "Keep music service alive in background"],
    ["FOREGROUND_SERVICE_MEDIA_PLAYBACK",            "Both",       "Foreground service type for audio"],
    ["INTERNET",                                     "Both",       "Stream audio from URLs"],
    ["READ_MEDIA_AUDIO",                             "Both",       "Access local music files"],
    ["RECORD_AUDIO",                                 "Auto only",  "Voice input (messaging apps)"],
    ["android.car.permission.CAR_SPEED",             "AAOS only",  "Read vehicle speed from car sensor"],
    ["android.car.permission.CAR_ENGINE_DETAILED",   "AAOS only",  "Read RPM, engine temp from car"],
    ["android.car.permission.CAR_ENERGY",            "AAOS only",  "Read fuel level, battery level"],
    ["android.car.permission.CAR_INFO",              "AAOS only",  "Read VIN, model, year from car"],
]
story.append(Table2(perms, [80*mm, 20*mm, None], "#0D47A1",
    [colors.HexColor("#E8EAF6"), colors.white]))
story.append(SP(5))
story.append(SecTitle("Minimal Permissions for Android Auto Media App"))
story.append(Code(
    "<!-- app/AndroidManifest.xml — Android Auto media app -->\n"
    "<uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\"/>\n"
    "<uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK\"/>\n"
    "<uses-permission android:name=\"android.permission.INTERNET\"/>\n\n"
    "<!-- Optional: local file playback -->\n"
    "<uses-permission android:name=\"android.permission.READ_MEDIA_AUDIO\"/>\n\n"
    "<!-- NOT included (AAOS only): -->\n"
    "<!-- android.car.permission.CAR_SPEED -->\n"
    "<!-- android.car.permission.CAR_ENGINE_DETAILED -->\n"
    "<!-- android.car.permission.CAR_ENERGY -->"
))
story.append(Warn(
    "Adding CAR_SPEED or other CAR_* permissions to the app/ module's manifest will NOT grant "
    "vehicle sensor access — they will simply be ignored on a phone. Worse, it may cause "
    "Play Store reviewers to flag your app as requesting inappropriate permissions."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 12 — HostValidator & Security
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(12, "HostValidator & Security"))
story.append(SP(6))
story.append(WhatIs(
    "HostValidator is a Car App Library class that controls which car hosts are allowed to "
    "connect to your CarAppService. A 'host' is the Android Auto app on the car head unit, "
    "or the OEM template host on AAOS. Since your CarAppService is exported (visible to other apps), "
    "a malicious app could theoretically bind to it and control your media service. "
    "HostValidator prevents this by requiring the connecting host to match a known certificate."
))
story.append(SP(4))
story.append(WhyUse(
    "Security: a CarAppService that allows any host to connect could be bound by a malicious app "
    "on the same device, gaining access to your media controls and potentially user data. "
    "In production, you should restrict connections to known, trusted car hosts "
    "(Google's Android Auto host, Samsung DeX, known OEM hosts) using their signing certificates. "
    "ALLOW_ALL_HOSTS_VALIDATOR is only for development and testing."
))
story.append(SP(5))
story.append(SecTitle("Development vs Production HostValidator"))
story.append(Code(
    "// DEVELOPMENT ONLY — allows any host to connect (includes your DHU)\n"
    "override fun createHostValidator(): HostValidator {\n"
    "    return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR\n"
    "}\n\n"
    "// PRODUCTION — restrict to known trusted hosts\n"
    "override fun createHostValidator(): HostValidator {\n"
    "    return HostValidator.Builder(applicationContext)\n"
    "        // Allow Google's Android Auto host\n"
    "        .addAllowedHost(\n"
    "            \"com.google.android.projection.gearhead\",  // AA package name\n"
    "            HostValidator.ANDROID_AUTO_DIGEST            // Google's cert digest\n"
    "        )\n"
    "        // Allow Android Auto Simulator (for CI testing)\n"
    "        .addAllowedHost(\n"
    "            \"com.google.android.autosimulator\",\n"
    "            HostValidator.ANDROID_AUTO_SIMULATOR_DIGEST\n"
    "        )\n"
    "        .build()\n"
    "}"
))
story.append(SecTitle("Known Host Package Names"))
hosts = [
    ["Host",                          "Package Name",                           "Context"],
    ["Android Auto (production)",     "com.google.android.projection.gearhead", "Real car, production AA"],
    ["Android Auto Simulator",        "com.google.android.autosimulator",       "Testing, DHU"],
    ["Android Auto (older versions)", "com.google.android.apps.auto",           "Legacy devices"],
    ["AAOS template host",            "Varies by OEM",                          "AAOS cars"],
]
story.append(Table2(hosts, [46*mm, 68*mm, None], "#4A148C",
    [colors.HexColor("#F3E5F5"), colors.white]))
story.append(SP(4))
story.append(Note(
    "HostValidator.ANDROID_AUTO_DIGEST and HostValidator.ANDROID_AUTO_SIMULATOR_DIGEST "
    "are pre-defined constants in the Car App Library. You don't need to hardcode certificate hashes manually."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 13 — Testing with DHU
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(13, "Testing with Desktop Head Unit (DHU)"))
story.append(SP(6))
story.append(WhatIs(
    "The Desktop Head Unit (DHU) is a tool provided in the Android SDK that simulates "
    "an Android Auto car head unit on your development computer. It connects to the "
    "Android Auto app on your test phone over USB (or via the Android emulator), "
    "projects your Car App Library UI onto a desktop window, and lets you interact with "
    "it using your mouse and keyboard — simulating the car touchscreen, steering wheel "
    "buttons, and voice commands. It is the primary testing tool for Android Auto development."
))
story.append(SP(4))
story.append(WhyUse(
    "Real car testing is expensive, slow, and often impractical during development. "
    "The DHU lets you test every aspect of your Android Auto app — templates, navigation, "
    "media controls, voice commands, screen sizes, day/night mode — entirely on your laptop. "
    "It is essential for iterating quickly on your UI and catching template violations "
    "before they cause crashes in a real car."
))
story.append(SP(5))
story.append(SecTitle("DHU Setup — Step by Step"))
story.append(Code(
    "Step 1: Install DHU via Android SDK Manager\n"
    "    Android Studio → SDK Manager → SDK Tools\n"
    "    ✓ Check: Android Auto Desktop Head Unit emulator\n"
    "    Path: $ANDROID_HOME/extras/google/auto/\n\n"
    "Step 2: Enable Developer Mode in Android Auto on your phone\n"
    "    Phone → Android Auto app → tap version number 10 times\n"
    "    → Developer settings unlocked → Enable 'Unknown sources'\n\n"
    "Step 3: Start the DHU server on your phone\n"
    "    Android Auto app → Developer settings → Start head unit server\n"
    "    (Phone now listens for DHU connection)\n\n"
    "Step 4: Connect phone to computer via USB\n"
    "    adb forward tcp:5277 tcp:5277\n\n"
    "Step 5: Launch the DHU on your computer\n"
    "    cd $ANDROID_HOME/extras/google/auto/\n"
    "    ./desktop-head-unit          (macOS/Linux)\n"
    "    desktop-head-unit.exe        (Windows)"
))
story.append(SecTitle("DHU Controls & Keyboard Shortcuts"))
dhu = [
    ["Action",             "Keyboard / Method",     "What it tests"],
    ["Tap",                "Mouse click",           "Touch input on templates"],
    ["Back button",        "Esc",                   "ScreenManager.pop()"],
    ["Voice input",        "Ctrl+M (microphone)",   "MEDIA_PLAY_FROM_SEARCH"],
    ["Media play/pause",   "Space bar",             "MediaSession onPlay/onPause"],
    ["Media next",         "N",                     "MediaSession onSkipToNext"],
    ["Media previous",     "P",                     "MediaSession onSkipToPrevious"],
    ["Day mode",           "D",                     "Light theme rendering"],
    ["Night mode",         "Ctrl+N",                "Dark theme rendering"],
    ["Wide screen",        "W",                     "Test wide car display layout"],
    ["Simulate steering wheel btns","S + arrow",   "Hardware media button handling"],
]
story.append(Table2(dhu, [38*mm, 40*mm, None], "#1B5E20",
    [colors.HexColor("#E8F5E9"), colors.white]))
story.append(SP(4))
story.append(Tip(
    "Run your app in debug mode (adb debugging enabled) while using the DHU. "
    "Logcat filters for 'SmartAAOS' will show all your Log.d() calls — "
    "this is the fastest way to debug template rendering issues."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 14 — Emulator Testing
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(14, "Android Auto Emulator Testing"))
story.append(SP(6))
story.append(WhatIs(
    "Android Auto can also be tested using the Android Emulator (AVD) without a physical phone. "
    "You create an AVD with the Android Auto system image or use the Automotive OS system image "
    "to test AAOS. The emulator approach is useful for CI/CD pipelines or when you don't have "
    "a physical Android device with the Android Auto app installed. "
    "For AAOS, there is a dedicated 'Automotive' hardware profile in Android Studio."
))
story.append(SP(4))
story.append(WhyUse(
    "Physical device + DHU setup requires a compatible phone with Android Auto installed, "
    "which may not always be available in a CI environment. The emulator allows automated "
    "UI tests to run headlessly. The AAOS emulator (automotive hardware profile) also lets "
    "you test AAOS-specific behavior (vehicle sensors, system integrations) without owning "
    "an AAOS car."
))
story.append(SP(5))
story.append(SecTitle("Creating an AAOS Emulator (Android Studio)"))
story.append(Code(
    "Step 1: Open AVD Manager\n"
    "    Android Studio → Tools → Device Manager → Create Virtual Device\n\n"
    "Step 2: Select Hardware Profile\n"
    "    Category: Automotive\n"
    "    Profile: Automotive (1024p landscape)  ← standard AAOS display\n"
    "             OR: Automotive Portrait        ← portrait AAOS display\n\n"
    "Step 3: Select System Image\n"
    "    Tab: Other Images\n"
    "    Release: Android 12L or 13 Automotive (AAOS)\n"
    "    ABI: x86_64\n"
    "    Note: Download if not already present (~3GB)\n\n"
    "Step 4: Configure AVD\n"
    "    Name: AAOS_Test\n"
    "    RAM: 4096 MB minimum\n"
    "    Storage: 8192 MB\n"
    "    Graphics: Hardware - GLES 2.0\n\n"
    "Step 5: Launch and test\n"
    "    Run → Select AAOS_Test as deployment target\n"
    "    App installs and launches in the AAOS emulator"
))
story.append(SecTitle("Android Auto Emulator (phone-based)"))
story.append(Code(
    "For Android Auto (phone projection testing) using an emulator:\n\n"
    "Step 1: Create a standard phone AVD (Pixel 6, API 33+)\n\n"
    "Step 2: Install Android Auto APK on the emulator\n"
    "    adb install android_auto.apk\n\n"
    "Step 3: Use the DHU to connect to the emulator\n"
    "    adb -e forward tcp:5277 tcp:5277    (-e = emulator)\n"
    "    ./desktop-head-unit\n\n"
    "Step 4: In the emulator, start head unit server\n"
    "    Android Auto app → Developer settings → Start head unit server"
))
story.append(Note(
    "The AAOS emulator is the most accurate way to test AAOS-specific features. "
    "However, it does not simulate real vehicle sensor data (speed, RPM) — "
    "you need to mock these in your VehicleRepository as SmartAAOS does with VehicleDataService."
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 15 — Code Differences: AAOS vs Android Auto
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(15, "AAOS vs Android Auto — Code Differences"))
story.append(SP(6))
story.append(WhatIs(
    "While the Car App Library allows most code to be shared between AAOS and Android Auto, "
    "there are key differences in manifest setup, Gradle dependencies, permissions, "
    "and available APIs. Understanding exactly what changes (and what stays the same) "
    "is critical when supporting both platforms from one codebase."
))
story.append(SP(5))
story.append(SecTitle("Side-by-Side Comparison"))
code_diff = [
    ["What",                   "Android Auto (app/)",                         "AAOS (automotive/)"],
    ["Gradle artifact",        "androidx.car.app:app",                        "androidx.car.app:app-automotive"],
    ["minSdk",                 "23 (recommended: 26)",                        "29 (required)"],
    ["uses-feature",           "None required",                               "android.hardware.type.automotive REQUIRED"],
    ["Vehicle permissions",    "None — no vehicle sensor access",             "CAR_SPEED, CAR_ENGINE_DETAILED, etc."],
    ["CarAppActivity",         "Declared in manifest (phone launcher)",        "Declared in manifest (car launcher)"],
    ["HostValidator",          "Validate Android Auto host certificate",       "Validate OEM host certificate"],
    ["VehicleDataService",     "Not used — no vehicle sensor IPC",            "AIDL service for sensor data"],
    ["CarPropertyManager",     "Not available",                               "Available for real OEM sensor data"],
    ["MediaBrowserService",    "Required — AA host uses it for media",        "Required — same API"],
    ["Testing tool",           "DHU (Desktop Head Unit)",                     "AAOS Emulator or AAOS car"],
    ["Play Store listing",     "Regular app listing (appears for AA users)",  "Automotive section listing"],
]
story.append(Table2(code_diff, [42*mm, 60*mm, None], "#0D47A1",
    [colors.HexColor("#E8EAF6"), colors.white]))
story.append(SP(5))
story.append(SecTitle("What is Identical in Both"))
story += [
    Bullet("CarAppService class and its lifecycle methods"),
    Bullet("Session class and onCreateScreen / onNewIntent"),
    Bullet("All Screen subclasses (HomeScreen, PlayerScreen, etc.)"),
    Bullet("All Template classes (ListTemplate, PaneTemplate, etc.)"),
    Bullet("ScreenManager navigation (push, pop, back)"),
    Bullet("MediaBrowserServiceCompat and MediaSession APIs"),
    Bullet("ExoPlayer integration and audio focus management"),
    Bullet("Foreground service and MediaStyle notification"),
    Bullet("Voice control via MEDIA_PLAY_FROM_SEARCH"),
    Bullet("All Car App Library UI components (Row, Action, CarIcon, etc.)"),
    SP(4),
]
story.append(SecTitle("Shared Module Pattern — Best Practice"))
story.append(Body(
    "When supporting both platforms, extract shared code into a :shared module to avoid duplication:"
))
story.append(Code(
    "// settings.gradle.kts\n"
    "include(\":app\")        // Android Auto\n"
    "include(\":automotive\") // AAOS\n"
    "include(\":shared\")     // shared CarAppService, Screens, MusicService\n\n"
    "// app/build.gradle.kts\n"
    "dependencies {\n"
    "    implementation(project(\":shared\"))\n"
    "    implementation(libs.androidx.car.app)              // Auto artifact\n"
    "}\n\n"
    "// automotive/build.gradle.kts\n"
    "dependencies {\n"
    "    implementation(project(\":shared\"))\n"
    "    implementation(libs.androidx.car.app.automotive)   // AAOS artifact\n"
    "}"
))

story.append(PageBreak())

# ════════════════════════════════════════════════════════════════════════════
# TOPIC 16 — Publishing to Play Store
# ════════════════════════════════════════════════════════════════════════════
story.append(Banner(16, "Publishing Android Auto Apps to Play Store"))
story.append(SP(6))
story.append(WhatIs(
    "Publishing an Android Auto app to the Play Store requires additional steps beyond a "
    "standard Android app submission. Google reviews all Android Auto apps manually for "
    "driver safety compliance before they appear in the Android Auto section. "
    "Your app must pass the Driver Distraction Guidelines review — which checks template usage, "
    "driving mode restrictions, voice support, and overall UX safety."
))
story.append(SP(4))
story.append(WhyUse(
    "Unlike regular Android apps that go live automatically after review, "
    "Android Auto apps require explicit approval from Google's Android Auto team. "
    "This process can take 1-3 weeks for the initial review. Understanding "
    "what reviewers check allows you to build compliant apps from the start "
    "and avoid rejection cycles."
))
story.append(SP(5))
story.append(SecTitle("Publishing Checklist"))
checklist = [
    ["Step", "Action",                              "Details"],
    ["1", "Test with DHU",                          "Test all screens, voice commands, driving restrictions"],
    ["2", "Check template compliance",              "Verify max items, actions, text lines per template"],
    ["3", "Verify driving restrictions",            "No complex taps/inputs allowed at speed > 5 km/h"],
    ["4", "Test voice commands",                    "MEDIA_PLAY_FROM_SEARCH must work with common queries"],
    ["5", "Check HostValidator",                    "Use production HostValidator (not ALLOW_ALL) for release"],
    ["6", "Sign the APK",                           "Use release signing key, not debug key"],
    ["7", "Update Play Console",                    "App content → Android Auto section → fill required fields"],
    ["8", "Submit for review",                      "Google manually reviews Android Auto apps"],
    ["9", "Address review feedback",                "May request video demo of the app in a real car or DHU"],
    ["10","Publish",                                "App appears in Play Store automotive section after approval"],
]
story.append(Table2(checklist, [8*mm, 46*mm, None], "#1565C0",
    [colors.HexColor("#E3F2FD"), colors.white]))
story.append(SP(5))
story.append(SecTitle("Driver Distraction Guidelines — Key Rules"))
story += [
    Bullet("<b>No text entry while driving</b> — disable text input fields when speed > 0"),
    Bullet("<b>Max 6 list items</b> at Car App API 1 (more allowed at higher API levels)"),
    Bullet("<b>Max 2 actions</b> on PaneTemplate — no more than 2 decision points per screen"),
    Bullet("<b>Voice must work</b> — onPlayFromSearch() must return a result for common music queries"),
    Bullet("<b>No custom drawing</b> unless navigation category with SurfaceContainer"),
    Bullet("<b>App must be usable with one hand</b> — no multi-touch gestures"),
    Bullet("<b>Content must update without user interaction</b> — no manual refresh required"),
    SP(4),
]
story.append(Warn(
    "Apps that add interaction triggers while driving (e.g., allowing song changes at 60 km/h) "
    "will fail Google's safety review. Always check vehicle speed or use the "
    "isCarMoving flag before enabling any non-essential interaction."
))
story.append(Key(
    "For AAOS apps, the review process goes through the car OEM's certification process "
    "in addition to (or instead of) Google's Play Store review. Contact the OEM's developer "
    "program for AAOS-specific certification requirements."
))

# ── SUMMARY ──────────────────────────────────────────────────────────────────
story.append(PageBreak())
story.append(Paragraph("Complete Android Auto Architecture Summary", section_title))
story.append(SP(4))
story.append(Code(
    "Android Auto — Complete Architecture Map\n"
    "\n"
    "┌───────────────────────────────────────────────────────────────────┐\n"
    "│  PHONE (app/ module)                                              │\n"
    "│                                                                   │\n"
    "│  ┌─────────────────────────────────────────────────────────────┐  │\n"
    "│  │  ENTRY: CarAppService (exports to Android Auto host)        │  │\n"
    "│  │    HostValidator → restricts who can connect                │  │\n"
    "│  │    onCreateSession() → SmartSession                         │  │\n"
    "│  └──────────────────────────┬──────────────────────────────────┘  │\n"
    "│                             │                                      │\n"
    "│  ┌──────────────────────────▼──────────────────────────────────┐  │\n"
    "│  │  SESSION: SmartSession                                      │  │\n"
    "│  │    onCreateScreen() → HomeScreen                            │  │\n"
    "│  │    onNewIntent() → voice handling                           │  │\n"
    "│  └──────────────────────────┬──────────────────────────────────┘  │\n"
    "│                             │                                      │\n"
    "│  ┌──────────────────────────▼──────────────────────────────────┐  │\n"
    "│  │  UI SCREENS (Car App Templates — projected to car)          │  │\n"
    "│  │    HomeScreen   → ListTemplate (songs)                      │  │\n"
    "│  │    PlayerScreen → PaneTemplate (now playing)                │  │\n"
    "│  │    Navigation: ScreenManager.push() / pop()                 │  │\n"
    "│  │    Refresh: invalidate() on state change                    │  │\n"
    "│  └──────────────────────────┬──────────────────────────────────┘  │\n"
    "│                             │                                      │\n"
    "│  ┌──────────────────────────▼──────────────────────────────────┐  │\n"
    "│  │  MUSIC ENGINE                                               │  │\n"
    "│  │    SmartMusicService (MediaBrowserServiceCompat)            │  │\n"
    "│  │    ExoPlayer → stream audio from URL                        │  │\n"
    "│  │    MediaSession → receive commands from car host            │  │\n"
    "│  │    AudioFocus → GAIN/LOSS/DUCK handling                     │  │\n"
    "│  │    ForegroundService + MediaStyle notification              │  │\n"
    "│  │    Voice: MEDIA_PLAY_FROM_SEARCH → fuzzy search             │  │\n"
    "│  └─────────────────────────────────────────────────────────────┘  │\n"
    "│                                                                    │\n"
    "│  ↑↓ USB or Wireless Android Auto connection                        │\n"
    "└───────────────────────────────────────────────────────────────────┘\n"
    "                              ↓\n"
    "┌───────────────────────────────────────────────────────────────────┐\n"
    "│  CAR HEAD UNIT                                                    │\n"
    "│    Android Auto host app                                          │\n"
    "│    Renders Car App Library templates                              │\n"
    "│    Sends touch/button/voice events back to phone                  │\n"
    "└───────────────────────────────────────────────────────────────────┘"
))
story += [SP(10), HR(), SP(4),
    Paragraph("Android Auto Deep Learning Notes  |  SmartAAOS Project  |  Swapnil Patil  |  2026", footer_style)]

# ── Build ────────────────────────────────────────────────────────────────────
doc.build(story)
print(f"PDF saved: {OUTPUT}")
