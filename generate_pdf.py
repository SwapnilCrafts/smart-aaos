from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    HRFlowable, PageBreak, KeepTogether
)
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_JUSTIFY

OUTPUT = "/Users/swapnilpatil/Documents/AndroidProjects/SmartAAOS/AAOS_DeepLearning_Notes.pdf"

styles = getSampleStyleSheet()

# ── Style Definitions ────────────────────────────────────────────────────────
title_style = ParagraphStyle("DocTitle", parent=styles["Title"],
    fontSize=24, textColor=colors.HexColor("#0D1B2A"), spaceAfter=6, alignment=TA_CENTER)

subtitle_style = ParagraphStyle("DocSub", parent=styles["Normal"],
    fontSize=11, textColor=colors.HexColor("#5C6BC0"), spaceAfter=3, alignment=TA_CENTER)

topic_banner = ParagraphStyle("TopicBanner", parent=styles["Normal"],
    fontSize=14, textColor=colors.white, backColor=colors.HexColor("#1565C0"),
    spaceBefore=16, spaceAfter=2, leftIndent=-6, rightIndent=-6,
    borderPad=8, fontName="Helvetica-Bold")

section_title = ParagraphStyle("SectionTitle", parent=styles["Normal"],
    fontSize=11, textColor=colors.HexColor("#0D47A1"), spaceBefore=10, spaceAfter=3,
    fontName="Helvetica-Bold", borderPad=2)

what_is_style = ParagraphStyle("WhatIs", parent=styles["Normal"],
    fontSize=9.5, leading=15, spaceAfter=5, textColor=colors.HexColor("#1A237E"),
    backColor=colors.HexColor("#E8EAF6"), borderColor=colors.HexColor("#7986CB"),
    borderWidth=1, borderPad=7, leftIndent=0)

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
    fontSize=9.5, leading=14, spaceAfter=3, leftIndent=14,
    textColor=colors.HexColor("#212121"))

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

footer_style = ParagraphStyle("Footer", parent=styles["Normal"],
    fontSize=8, textColor=colors.HexColor("#9E9E9E"), alignment=TA_CENTER)

# ── Helper Functions ─────────────────────────────────────────────────────────
def Banner(num, title):
    return Paragraph(f"  TOPIC {num}   {title}", topic_banner)

def SecTitle(text):
    return Paragraph(text, section_title)

def WhatIs(text):
    return Paragraph(f"<b>What is it?</b>  {text}", what_is_style)

def WhyUse(text):
    return Paragraph(f"<b>Why do we use it?</b>  {text}", why_style)

def Body(text):
    return Paragraph(text, body_style)

def Bullet(text):
    return Paragraph(f"&nbsp;&nbsp;&nbsp;• &nbsp;{text}", bullet_style)

def Code(text):
    t = text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br/>").replace(" ","&nbsp;")
    return Paragraph(f'<font name="Courier" size="7.8">{t}</font>', code_style)

def Note(text):
    return Paragraph(f"<b>Note:</b>  {text}", note_style)

def Tip(text):
    return Paragraph(f"<b>Tip:</b>  {text}", tip_style)

def Key(text):
    return Paragraph(f"<b>Key Point:</b>  {text}", key_style)

def SP(h=5):
    return Spacer(1, h)

def HR():
    return HRFlowable(width="100%", thickness=0.5, color=colors.HexColor("#BDBDBD"), spaceAfter=4)

def make_table(data, col_widths, header_color="#1565C0", row_colors=None):
    if row_colors is None:
        row_colors = [colors.HexColor("#EEF2FF"), colors.white]
    t = Table(data, colWidths=col_widths, hAlign="LEFT")
    t.setStyle(TableStyle([
        ("BACKGROUND",     (0,0), (-1,0),  colors.HexColor(header_color)),
        ("TEXTCOLOR",      (0,0), (-1,0),  colors.white),
        ("FONTNAME",       (0,0), (-1,0),  "Helvetica-Bold"),
        ("FONTSIZE",       (0,0), (-1,-1), 8.5),
        ("ROWBACKGROUNDS", (0,1), (-1,-1), row_colors),
        ("GRID",           (0,0), (-1,-1), 0.4, colors.HexColor("#BDBDBD")),
        ("VALIGN",         (0,0), (-1,-1), "MIDDLE"),
        ("LEFTPADDING",    (0,0), (-1,-1), 6),
        ("RIGHTPADDING",   (0,0), (-1,-1), 6),
        ("TOPPADDING",     (0,0), (-1,-1), 4),
        ("BOTTOMPADDING",  (0,0), (-1,-1), 4),
    ]))
    return t

# ═════════════════════════════════════════════════════════════════════════════
doc = SimpleDocTemplate(OUTPUT, pagesize=A4,
    leftMargin=18*mm, rightMargin=18*mm, topMargin=18*mm, bottomMargin=18*mm)
story = []

# ─── COVER PAGE ──────────────────────────────────────────────────────────────
story += [
    SP(25),
    Paragraph("Android Automotive OS (AAOS)", title_style),
    Paragraph("&amp; Android Auto", title_style),
    Paragraph("Complete Deep Learning Notes", subtitle_style),
    SP(5),
    HR(),
    SP(5),
    Paragraph("Based on <b>SmartAAOS Project</b> — A full-featured native AAOS music player", subtitle_style),
    Paragraph("by <b>Swapnil Patil</b>", subtitle_style),
    SP(12),
    Body(
        "This document is a comprehensive study guide covering every concept needed to build "
        "production-grade Android Automotive OS and Android Auto applications. Each topic includes "
        "a clear explanation of what it is, why it is used in car apps, how it is implemented in "
        "the SmartAAOS project, and working code examples drawn directly from the source code."
    ),
    SP(6),
    Body(
        "SmartAAOS is a native AAOS music player that integrates real audio streaming, "
        "voice commands (Hey Google), vehicle telemetry via AIDL, real-time alerts, "
        "driving mode safety restrictions, and a full MVVM architecture — making it an "
        "ideal reference project to learn every layer of AAOS development."
    ),
    SP(10),
]

# Table of Contents
toc_data = [
    ["#", "Topic", "Core Concept"],
    ["1",  "AAOS vs Android Auto",              "Platform differences, Car App Library"],
    ["2",  "Project Module Structure",           "app vs automotive module, manifest setup"],
    ["3",  "Car App Library Entry Points",       "CarAppService, Session, HostValidator"],
    ["4",  "Template-Based UI System",           "ListTemplate, PaneTemplate, Row, Action"],
    ["5",  "Screen Navigation",                  "ScreenManager, back stack, NavigationCallback"],
    ["6",  "MediaBrowserServiceCompat",          "Music catalogue, onGetRoot, onLoadChildren"],
    ["7",  "MediaSession — Playback API",        "Callback, Metadata, PlaybackState"],
    ["8",  "MediaBrowserCompat — Client Side",   "Connect to service, TransportControls"],
    ["9",  "ExoPlayer (Media3)",                 "Audio playback, states, error handling"],
    ["10", "Audio Focus Management",             "Request, GAIN/LOSS/DUCK, abandon"],
    ["11", "Foreground Service + Notification",  "MediaStyle, FOREGROUND_SERVICE_TYPE"],
    ["12", "Voice Control",                      "MEDIA_PLAY_FROM_SEARCH, fuzzy search"],
    ["13", "AIDL — IPC for Vehicle Data",        "Interface, Stub, ServiceConnection, binding"],
    ["14", "MVVM Architecture",                  "CarViewModelStore, VehicleViewModel, LiveData"],
    ["15", "Driving Mode Restrictions",          "Safety rules, speed checks, UX constraints"],
    ["16", "Alert System",                       "AlertRepository, thresholds, severity levels"],
    ["17", "Progress Bar + Handler Timer",       "Unicode bar, Handler, periodic invalidate()"],
    ["18", "Build Configuration",                "Gradle, minSdk 29, Version Catalog, deps"],
]
story.append(make_table(toc_data, [10*mm, 62*mm, None], "#0D47A1",
    [colors.HexColor("#EEF2FF"), colors.white]))
story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 1 — AAOS vs Android Auto
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(1, "AAOS vs Android Auto — Core Concepts"))
story.append(SP(6))
story.append(WhatIs(
    "Android Automotive OS (AAOS) and Android Auto are two separate Google platforms for "
    "bringing Android apps to car dashboards. Despite similar names, they work completely differently. "
    "Android Auto requires a phone to be connected and projects the app onto the car screen. "
    "Android Automotive OS is a full Android operating system embedded directly in the car's infotainment hardware — "
    "no phone needed."
))
story.append(SP(4))
story.append(WhyUse(
    "Car manufacturers like Volvo, Polestar, Renault, and GM ship cars with Android Automotive OS built in. "
    "This means your app runs as a first-class citizen on the car — persistent, always available, using the "
    "car's own internet connection and hardware. Android Auto is easier to target (any Android phone user), "
    "while AAOS gives a deeper, more integrated in-car experience."
))
story.append(SP(6))

comp_data = [
    ["Feature",           "Android Auto",                   "Android Automotive OS (AAOS)"],
    ["Runs on",           "Phone → projected to car screen", "Natively on car's infotainment CPU"],
    ["Requires phone?",   "Yes — always needed",             "No — car is independent"],
    ["Project module",    "app/",                           "automotive/"],
    ["Operating System",  "Android on phone",               "Android on car ECU"],
    ["Internet source",   "Phone's data connection",        "Car's built-in SIM / WiFi"],
    ["App updates",       "Via Play Store on phone",        "Via Play Store on car / OTA"],
    ["Car permissions",   "Not applicable",                 "CAR_SPEED, CAR_ENGINE_DETAILED, etc."],
    ["OEM integration",   "Limited",                        "Deep — sensors, HVAC, displays"],
]
story.append(make_table(comp_data, [38*mm, 62*mm, None], "#0D47A1",
    [colors.HexColor("#E8EAF6"), colors.white]))
story.append(SP(6))
story.append(Key(
    "The Car App Library (androidx.car.app) is the key abstraction — it provides template-based UI "
    "that works on both Android Auto and AAOS from a single codebase. You write the logic once; "
    "the car host renders it appropriately on each platform."
))
story.append(SP(4))
story.append(Tip(
    "In SmartAAOS, the automotive/ module is the full implementation. The app/ module exists for "
    "Android Auto compatibility but is currently minimal. Both modules share dependencies through "
    "the root build.gradle.kts."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 2 — Project Module Structure
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(2, "Project Module Structure"))
story.append(SP(6))
story.append(WhatIs(
    "An Android Automotive project uses a multi-module Gradle structure with separate modules for "
    "the phone (Android Auto) and the car (AAOS). The automotive/ module is the core — it contains "
    "all the car-specific code, resources, and manifest declarations. The app/ module targets "
    "Android Auto via a connected phone. Both modules can share common code through a shared library module."
))
story.append(SP(4))
story.append(WhyUse(
    "Separating modules keeps phone and car code isolated. The automotive module can declare "
    "car-only permissions, hardware features, and service types that would be rejected on a phone. "
    "It also lets you have different minSdk, dependencies, and build flavors for each platform."
))
story.append(SP(5))
story.append(Code(
    "SmartAAOS/\n"
    "├── app/                             ← Android Auto (phone module)\n"
    "│   └── build.gradle.kts             ← depends on car app 'app' artifact\n"
    "│\n"
    "├── automotive/                      ← AAOS (car module — primary)\n"
    "│   └── src/main/\n"
    "│       ├── AndroidManifest.xml      ← declares services, permissions, features\n"
    "│       ├── res/xml/\n"
    "│       │   └── automotive_app_desc.xml  ← declares media category\n"
    "│       └── java/com/swapnil/smart/aaos/\n"
    "│           ├── car/                 ← CarAppService, SmartSession\n"
    "│           ├── media/               ← SmartMusicService, MusicData, SongRepository\n"
    "│           ├── ui/screens/          ← HomeScreen, PlayerScreen, Dashboard, Diagnostics\n"
    "│           ├── vehicle/             ← VehicleDataService (AIDL), VehicleRepository\n"
    "│           ├── viewmodel/           ← VehicleViewModel, CarViewModelStore\n"
    "│           └── utils/               ← AlertRepository, AlbumArtLoader, VehicleAlert\n"
    "│\n"
    "└── gradle/\n"
    "    └── libs.versions.toml           ← centralized dependency version catalog"
))
story.append(SP(5))
story.append(SecTitle("Required Manifest Declarations for AAOS"))
story.append(Body(
    "The AndroidManifest.xml in the automotive module must declare hardware and software features "
    "that tell the system this app is built for AAOS. Without these, the app will not appear "
    "in the automotive Play Store or run on AAOS devices."
))
story.append(Code(
    "<!-- 1. Declare this is an automotive hardware app -->\n"
    "<uses-feature android:name=\"android.hardware.type.automotive\"\n"
    "              android:required=\"true\"/>\n\n"
    "<!-- 2. Requires the Car App templates host (AAOS runtime) -->\n"
    "<uses-feature android:name=\"android.software.car.templates_host\"\n"
    "              android:required=\"true\"/>\n\n"
    "<!-- 3. Link to automotive_app_desc.xml — declares app category -->\n"
    "<meta-data android:name=\"com.google.android.gms.car.application\"\n"
    "           android:resource=\"@xml/automotive_app_desc\"/>\n\n"
    "<!-- 4. Set minimum Car App API level -->\n"
    "<meta-data android:name=\"androidx.car.app.minCarApiLevel\"\n"
    "           android:value=\"1\"/>"
))
story.append(SecTitle("automotive_app_desc.xml — App Category"))
story.append(Body(
    "This XML file tells the system what kind of car app this is. The category affects where "
    "the app appears in the car launcher and what system integrations are available."
))
story.append(Code(
    "<!-- res/xml/automotive_app_desc.xml -->\n"
    "<automotiveApp>\n"
    "    <uses name=\"media\" />     <!-- music/audio app -->\n"
    "    <!-- Other options: navigation, notification, video -->\n"
    "</automotiveApp>"
))
story.append(Note(
    "Vehicle data permissions (CAR_SPEED, CAR_ENGINE_DETAILED, CAR_ENERGY, CAR_INFO) must also "
    "be declared in the manifest and are granted by the OEM — not user-granted like normal permissions."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 3 — Car App Library Entry Points
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(3, "Car App Library Entry Points"))
story.append(SP(6))
story.append(WhatIs(
    "Every Car App has two mandatory entry points provided by the Car App Library: "
    "CarAppService and Session. CarAppService is the Android Service that the car host binds to — "
    "it is the front door of your app. Session represents one active connection to a car display "
    "and is responsible for creating the first screen shown to the user."
))
story.append(SP(4))
story.append(WhyUse(
    "Car App Library abstracts away the complex communication between your app and the car host "
    "(the OEM system that renders the UI). You don't deal with raw IPC or AIDL calls to the host — "
    "CarAppService and Session handle that bridge. This is why every AAOS app must extend "
    "CarAppService instead of a plain Android Service."
))
story.append(SP(5))
story.append(SecTitle("CarAppService — The App Entry Point"))
story.append(Body(
    "CarAppService is to Car Apps what Application class is to regular Android apps. "
    "It's the service the car host discovers and binds to. You must declare it in the manifest "
    "with the correct intent filter and category so the system finds it."
))
story.append(Code(
    "// SmartCarAppService.kt\n"
    "class SmartCarAppService : CarAppService() {\n\n"
    "    // Controls which car hosts are allowed to connect\n"
    "    // ALLOW_ALL = any host (use in development)\n"
    "    // In production: use HostValidator with a curated list of OEM host certificates\n"
    "    override fun createHostValidator(): HostValidator {\n"
    "        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR\n"
    "    }\n\n"
    "    // Called each time a new car display connects — return a fresh Session\n"
    "    override fun onCreateSession(): Session {\n"
    "        return SmartSession()\n"
    "    }\n\n"
    "    // Clean up shared resources when service is destroyed\n"
    "    override fun onDestroy() {\n"
    "        super.onDestroy()\n"
    "        CarViewModelStore.clear()  // release all ViewModels\n"
    "    }\n"
    "}"
))
story.append(Code(
    "<!-- Manifest declaration -->\n"
    "<service android:name=\".car.SmartCarAppService\"\n"
    "         android:exported=\"true\">\n"
    "    <intent-filter>\n"
    "        <action android:name=\"androidx.car.app.CarAppService\"/>\n"
    "        <category android:name=\"androidx.car.app.category.MEDIA\"/>\n"
    "    </intent-filter>\n"
    "</service>"
))
story.append(SP(5))
story.append(SecTitle("Session — One Car Display Connection"))
story.append(Body(
    "Session manages the lifecycle of a single car display connection. It receives the intent "
    "that started the app and must return the first Screen to show. "
    "Session also handles new intents (like voice commands) that arrive while the session is alive. "
    "The carContext property gives you access to car-specific APIs throughout the session."
))
story.append(Code(
    "// SmartSession.kt\n"
    "class SmartSession : Session() {\n\n"
    "    // Called once when the session starts — return your first Screen\n"
    "    override fun onCreateScreen(intent: Intent): Screen {\n"
    "        return HomeScreen(carContext).also {\n"
    "            handleVoiceIntent(intent)   // check if launched via voice\n"
    "        }\n"
    "    }\n\n"
    "    // Called when a new intent arrives while the session is already running\n"
    "    // Example: user says \"Hey Google, play Kesariya\" while app is open\n"
    "    override fun onNewIntent(intent: Intent) {\n"
    "        handleVoiceIntent(intent)\n"
    "    }\n\n"
    "    private fun handleVoiceIntent(intent: Intent) {\n"
    "        if (intent.action != MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) return\n"
    "        val query = intent.getStringExtra(SearchManager.QUERY) ?: return\n"
    "        // Connect to music service and trigger search playback\n"
    "        mediaBrowser?.connect()\n"
    "    }\n"
    "}"
))
story.append(Key(
    "carContext (available in both Session and Screen) is the Car App equivalent of Android's Context. "
    "Use it to access car services, start services, and create screens. Never use applicationContext directly."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 4 — Template-Based UI System
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(4, "Template-Based UI System"))
story.append(SP(6))
story.append(WhatIs(
    "Car App Library uses a template system instead of custom Views, XML layouts, or Jetpack Compose. "
    "A template is a structured data model (like ListTemplate or PaneTemplate) that you build with a Builder pattern. "
    "The car host — not your app — is responsible for rendering it on screen. You describe what to show; "
    "the system decides how to render it based on the car's display capabilities."
))
story.append(SP(4))
story.append(WhyUse(
    "Car infotainment systems have strict safety requirements. Custom Views would let developers create "
    "complex, distracting UIs. Templates enforce driver-distraction guidelines automatically: "
    "limited text, limited actions, standardized layouts. The OEM also controls the visual theme, "
    "ensuring the app matches the car's design language. This is why you cannot draw arbitrary UI in AAOS."
))
story.append(SP(5))

tmpl_data = [
    ["Template",          "Used In",          "Purpose",                                  "Key Limit"],
    ["ListTemplate",      "HomeScreen",        "Scrollable list — songs, navigation rows", "Max 6 items (API 1)"],
    ["PaneTemplate",      "PlayerScreen",      "Content pane with actions",                "Max 2 Actions"],
    ["MessageTemplate",   "DashboardScreen",   "Text-based info display",                  "Max 2 actions"],
    ["MessageTemplate",   "DiagnosticsScreen", "Alert and engine status details",           "Max 2 actions"],
]
story.append(make_table(tmpl_data, [38*mm, 35*mm, 60*mm, None], "#1565C0",
    [colors.HexColor("#E3F2FD"), colors.white]))
story.append(SP(6))

story.append(SecTitle("ListTemplate — HomeScreen (song list + navigation)"))
story.append(Body(
    "ListTemplate is the most common template. It shows a scrollable list of Rows. "
    "Each Row has a title, up to 2 lines of text, an optional image, and an optional click action. "
    "The template also supports a header action (e.g., app icon) and an ActionStrip for global actions."
))
story.append(Code(
    "override fun onGetTemplate(): Template {\n"
    "    val listBuilder = ItemList.Builder()\n\n"
    "    // Add a row for each song\n"
    "    MusicData.songs.forEachIndexed { index, song ->\n"
    "        listBuilder.addItem(\n"
    "            Row.Builder()\n"
    "                .setTitle(song.title)                          // line 1\n"
    "                .addText(\"${song.artist} • ${song.album}\")  // line 2\n"
    "                .setImage(icon)                               // album art thumbnail\n"
    "                .setOnClickListener {\n"
    "                    screenManager.push(PlayerScreen(carContext, song, {}))\n"
    "                }\n"
    "                .build()\n"
    "        )\n"
    "    }\n\n"
    "    return ListTemplate.Builder()\n"
    "        .setTitle(\"Smart AAOS\")\n"
    "        .setHeaderAction(Action.APP_ICON)       // show app icon in header\n"
    "        .setSingleList(listBuilder.build())\n"
    "        .setActionStrip(\n"
    "            ActionStrip.Builder()\n"
    "                .addAction(driveAction)         // global Drive/Park toggle\n"
    "                .build()\n"
    "        )\n"
    "        .build()\n"
    "}"
))

story.append(SecTitle("PaneTemplate — PlayerScreen (Now Playing)"))
story.append(Body(
    "PaneTemplate is used for detail screens. It shows a Pane with Rows of information and "
    "up to 2 action buttons. In SmartAAOS, it shows the song info with a progress bar, "
    "album art, and Play/Next controls."
))
story.append(Code(
    "return PaneTemplate.Builder(\n"
    "    Pane.Builder()\n"
    "        .addRow(\n"
    "            Row.Builder()\n"
    "                .setTitle(song.title)\n"
    "                .addText(\"${song.artist} • ${song.album}\")\n"
    "                .addText(\"$progressBar  $progressText\")\n"
    "                .setImage(albumArtIcon)\n"
    "                .build()\n"
    "        )\n"
    "        .addAction(playPauseAction)   // Action 1 of max 2\n"
    "        .addAction(nextAction)        // Action 2 of max 2\n"
    "        .build()\n"
    ")\n"
    ".setTitle(\"Now Playing\")\n"
    ".setHeaderAction(Action.BACK)         // built-in back button\n"
    ".setActionStrip(ActionStrip.Builder().addAction(driveParkAction).build())\n"
    ".build()"
))

story.append(SecTitle("Template Constraints — Safety Rules"))
rules_data = [
    ["Component",          "Limit",        "Why this limit exists"],
    ["PaneTemplate actions","Max 2",       "Too many buttons = driver distraction"],
    ["Row text lines",      "Max 2",       "More text requires reading = eyes off road"],
    ["ListTemplate items",  "Max 6",       "Scrolling long lists while driving is unsafe"],
    ["ActionStrip",         "Max 4",       "Small header area, limited tap targets"],
    ["Row image size",      "1 per Row",   "Consistent visual scanning pattern"],
]
story.append(make_table(rules_data, [44*mm, 22*mm, None], "#B71C1C",
    [colors.HexColor("#FFEBEE"), colors.white]))
story.append(SP(4))
story.append(Note(
    "Violating template constraints causes a runtime IllegalArgumentException crash. "
    "The Car App Library enforces these rules to ensure your app passes OEM certification."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 5 — Screen Navigation
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(5, "Screen Navigation"))
story.append(SP(6))
story.append(WhatIs(
    "Navigation in Car App Library is managed by ScreenManager, which maintains a back stack "
    "of Screen instances. You push new screens onto the stack to navigate forward, and pop them "
    "to go back. Unlike Jetpack Navigation (which uses fragments and XML graphs), Car App navigation "
    "is entirely imperative — you manually push and pop screens in response to user actions."
))
story.append(SP(4))
story.append(WhyUse(
    "The Car App back stack integrates with the car's hardware back button and voice navigation commands. "
    "By using ScreenManager, your app gets correct back-navigation behavior for free, without needing "
    "to wire up back button handlers yourself. The screen lifecycle (onGetTemplate) is also automatically "
    "called when a screen becomes visible again after another screen is popped."
))
story.append(SP(5))
story.append(SecTitle("ScreenManager — Push & Pop"))
story.append(Code(
    "// Navigate to PlayerScreen\n"
    "screenManager.push(PlayerScreen(carContext, song, {}))\n\n"
    "// Navigate to DashboardScreen\n"
    "screenManager.push(DashboardScreen(carContext))\n\n"
    "// Pop current screen (go back)\n"
    "screenManager.pop()\n\n"
    "// Built-in back button in template header\n"
    "PaneTemplate.Builder(pane)\n"
    "    .setHeaderAction(Action.BACK)   // system handles pop automatically\n"
    "    .build()"
))
story.append(SP(5))
story.append(SecTitle("NavigationCallback Pattern — Decoupling Service from UI"))
story.append(Body(
    "A common challenge in Car Apps: the music service (SmartMusicService) needs to trigger "
    "screen navigation after a voice command, but services don't have access to ScreenManager. "
    "SmartAAOS solves this with a singleton object (NavigationCallback) that acts as a lightweight "
    "event bus between the service and the active screen."
))
story.append(Body(
    "The active screen registers its handlers in init {}, the service invokes them when needed. "
    "This avoids broadcasts, shared state, or complex dependency injection."
))
story.append(Code(
    "// NavigationCallback.kt — singleton event bus\n"
    "object NavigationCallback {\n"
    "    var onPlaySong: ((Song) -> Unit)? = null       // open PlayerScreen\n"
    "    var onOpenDashboard: (() -> Unit)? = null      // open DashboardScreen\n"
    "    var onPause: (() -> Unit)? = null              // pause from service\n"
    "    var onNext: (() -> Unit)? = null               // skip from service\n"
    "}\n\n"
    "// HomeScreen.init — register the handler\n"
    "NavigationCallback.onPlaySong = { song ->\n"
    "    screenManager.push(PlayerScreen(carContext, song, {}))\n"
    "}\n\n"
    "// SmartMusicService.onStartCommand — trigger from service (main thread!)\n"
    "handler.post {\n"
    "    NavigationCallback.onPlaySong?.invoke(song)\n"
    "}"
))
story.append(SP(5))
story.append(SecTitle("Screen Invalidation — Refreshing the UI"))
story.append(Body(
    "Because Car App templates are immutable data objects (not live views), you must call invalidate() "
    "to trigger a UI refresh. This causes onGetTemplate() to be called again, "
    "where you build a fresh template with updated data. SmartAAOS uses three triggers for invalidation:"
))
story.append(Code(
    "// Trigger 1: LiveData observer — fires when ViewModel state changes\n"
    "viewModel.isCarMoving.observeForever  { invalidate() }\n"
    "viewModel.currentAlert.observeForever { invalidate() }\n\n"
    "// Trigger 2: MediaController callback — fires when song/state changes\n"
    "mediaController?.registerCallback(object : MediaControllerCompat.Callback() {\n"
    "    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) { invalidate() }\n"
    "    override fun onMetadataChanged(metadata: MediaMetadataCompat?)   { invalidate() }\n"
    "})\n\n"
    "// Trigger 3: Handler timer — periodic refresh for progress bar\n"
    "handler.postDelayed({ invalidate() }, 1000L)"
))
story.append(Key(
    "invalidate() must always be called from the main thread. If you need to call it from a "
    "background thread or coroutine, use handler.post { invalidate() }."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 6 — MediaBrowserServiceCompat
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(6, "MediaBrowserServiceCompat — Music Service Foundation"))
story.append(SP(6))
story.append(WhatIs(
    "MediaBrowserServiceCompat is the base class for all media services in Android. "
    "It does two things: (1) it exposes a browsable catalogue of media items so the system "
    "and other apps can discover what content is available, and (2) it hosts the MediaSession "
    "that clients use to send playback commands. In AAOS, all media apps must extend this class "
    "so that the car's media controls and voice assistant can control your app."
))
story.append(SP(4))
story.append(WhyUse(
    "The Android media system uses the browser/service pattern to allow the car host, Google Assistant, "
    "and hardware media buttons to discover and control any media app uniformly. By extending "
    "MediaBrowserServiceCompat, your app automatically participates in this system — your songs "
    "appear in the car's media picker, voice commands route to your app, and hardware buttons "
    "trigger your callbacks. Without this, the car simply cannot control your audio."
))
story.append(SP(5))
story.append(SecTitle("Two Methods You Must Override"))
story.append(Body(
    "onGetRoot() acts as an access gate — return a BrowserRoot to allow connection, "
    "or null to deny it. onLoadChildren() returns the list of media items for a given parent ID, "
    "which is how the car system discovers your song catalogue."
))
story.append(Code(
    "class SmartMusicService : MediaBrowserServiceCompat() {\n\n"
    "    // Gate: who is allowed to browse this service?\n"
    "    // Return BrowserRoot to allow, null to deny\n"
    "    override fun onGetRoot(\n"
    "        clientPackageName: String,\n"
    "        clientUid: Int,\n"
    "        rootHints: Bundle?\n"
    "    ): BrowserRoot {\n"
    "        // Allow all clients in SmartAAOS (for development)\n"
    "        // Production: check clientPackageName for known clients only\n"
    "        return BrowserRoot(\"root\", null)\n"
    "    }\n\n"
    "    // Catalogue: return media items for the given parent\n"
    "    override fun onLoadChildren(\n"
    "        parentId: String,\n"
    "        result: Result<MutableList<MediaBrowserCompat.MediaItem>>\n"
    "    ) {\n"
    "        val items = MusicData.songs.map { song ->\n"
    "            val desc = MediaDescriptionCompat.Builder()\n"
    "                .setMediaId(song.id)         // unique ID used in playFromMediaId\n"
    "                .setTitle(song.title)\n"
    "                .setSubtitle(song.artist)\n"
    "                .build()\n"
    "            // FLAG_PLAYABLE = can be played directly\n"
    "            // FLAG_BROWSABLE = has children (e.g., a folder/album)\n"
    "            MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)\n"
    "        }.toMutableList()\n"
    "        result.sendResult(items)\n"
    "    }\n"
    "}"
))
story.append(SecTitle("Manifest Declaration"))
story.append(Code(
    "<service android:name=\".media.SmartMusicService\" android:exported=\"true\">\n"
    "    <intent-filter>\n"
    "        <!-- Required: makes this discoverable as a media browser service -->\n"
    "        <action android:name=\"android.media.browse.MediaBrowserService\"/>\n"
    "        <!-- Required: enables Hey Google voice search -->\n"
    "        <action android:name=\"android.media.action.MEDIA_PLAY_FROM_SEARCH\"/>\n"
    "    </intent-filter>\n"
    "</service>"
))
story.append(Note(
    "The sessionToken must be set in onCreate() before any client can connect: "
    "sessionToken = session.sessionToken. Without this, MediaBrowserCompat clients will fail to connect."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 7 — MediaSession
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(7, "MediaSession — Playback Control API"))
story.append(SP(6))
story.append(WhatIs(
    "MediaSessionCompat is the central control hub for media playback. It is a token-based API "
    "that any authorized client can use to send playback commands to your service — without knowing "
    "the service's implementation details. The session receives commands through its Callback "
    "and publishes the current state (playing, paused, position) and metadata (song title, artist, duration) "
    "back to all connected clients."
))
story.append(SP(4))
story.append(WhyUse(
    "MediaSession is what connects your music service to the entire Android media ecosystem: "
    "the car's steering wheel controls, the AAOS media notification, Google Assistant voice commands, "
    "Bluetooth headset buttons, and your own UI screens all communicate through the same session token. "
    "Without MediaSession, none of these external control surfaces could control your app. "
    "It is mandatory for any media app that wants system-level integration."
))
story.append(SP(5))
story.append(SecTitle("Setup in onCreate()"))
story.append(Code(
    "override fun onCreate() {\n"
    "    super.onCreate()\n\n"
    "    // Create the session with a debug tag\n"
    "    session = MediaSessionCompat(this, \"SmartMusicService\")\n\n"
    "    // CRITICAL: expose the token so clients can connect\n"
    "    sessionToken = session.sessionToken\n\n"
    "    // Register callback to receive commands\n"
    "    session.setCallback(callback)\n\n"
    "    // Enable media button + transport control handling\n"
    "    session.setFlags(\n"
    "        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or\n"
    "        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS\n"
    "    )\n"
    "}"
))
story.append(SecTitle("MediaSession.Callback — All Playback Commands"))
story.append(Body(
    "The Callback receives every playback command from any client. Each override corresponds "
    "to a user action or a transport control command. You implement the actual playback logic here."
))
story.append(Code(
    "val callback = object : MediaSessionCompat.Callback() {\n\n"
    "    override fun onPlay() {\n"
    "        if (requestAudioFocus()) {    // always check focus before playing\n"
    "            exoPlayer.play()\n"
    "            updatePlaybackState(STATE_PLAYING)\n"
    "            startForegroundService()  // keep running in background\n"
    "        }\n"
    "    }\n\n"
    "    override fun onPause() {\n"
    "        exoPlayer.pause()\n"
    "        stopProgressTimer()\n"
    "        abandonAudioFocus()           // release focus when paused\n"
    "        updatePlaybackState(STATE_PAUSED)\n"
    "    }\n\n"
    "    override fun onSkipToNext()     { currentIndex = (currentIndex+1) % songs.size; playSong(currentIndex) }\n"
    "    override fun onSkipToPrevious() { currentIndex = (currentIndex-1+songs.size) % songs.size; playSong(currentIndex) }\n"
    "    override fun onSeekTo(pos: Long){ exoPlayer.seekTo(pos); updatePlaybackState(STATE_PLAYING) }\n\n"
    "    // Called when user taps a specific song from the list\n"
    "    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {\n"
    "        val index = songs.indexOfFirst { it.id == mediaId }\n"
    "        if (index != -1) { currentIndex = index; playSong(currentIndex) }\n"
    "    }\n\n"
    "    // Called by voice assistant: \"Hey Google, play Kesariya\"\n"
    "    override fun onPlayFromSearch(query: String?, extras: Bundle?) {\n"
    "        val index = if (query.isNullOrEmpty()) 0 else findSongByQuery(query)\n"
    "        playSong(if (index != -1) index else 0)\n"
    "    }\n"
    "}"
))
story.append(SecTitle("Publishing Metadata & Playback State"))
story.append(Body(
    "After every state change (song loaded, play, pause, seek), you must update both "
    "the metadata and the playback state. This is what the car notification, lock screen, "
    "and client screens read to display current information."
))
story.append(Code(
    "// Metadata — song information (set when song changes)\n"
    "session.setMetadata(\n"
    "    MediaMetadataCompat.Builder()\n"
    "        .putString(METADATA_KEY_TITLE,  song.title)\n"
    "        .putString(METADATA_KEY_ARTIST, song.artist)\n"
    "        .putLong(METADATA_KEY_DURATION, song.durationMs)\n"
    "        .build()\n"
    ")\n\n"
    "// Playback state — status + position (update every second while playing)\n"
    "session.setPlaybackState(\n"
    "    PlaybackStateCompat.Builder()\n"
    "        .setState(STATE_PLAYING, exoPlayer.currentPosition, 1.0f)\n"
    "        .setActions(ACTION_PLAY or ACTION_PAUSE or ACTION_SKIP_TO_NEXT\n"
    "                    or ACTION_SKIP_TO_PREVIOUS or ACTION_SEEK_TO)\n"
    "        .build()\n"
    ")"
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 8 — MediaBrowserCompat Client
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(8, "MediaBrowserCompat — Client Side (Screen to Service)"))
story.append(SP(6))
story.append(WhatIs(
    "MediaBrowserCompat is the client-side counterpart to MediaBrowserServiceCompat. "
    "It is used by UI screens (like PlayerScreen) to establish a connection to the music service "
    "and obtain a MediaControllerCompat — the object through which all playback commands are sent. "
    "The connection is asynchronous: you provide a ConnectionCallback and get notified when "
    "the connection is established."
))
story.append(SP(4))
story.append(WhyUse(
    "Instead of calling the music service directly (which would couple the UI tightly to the service), "
    "MediaBrowserCompat provides a clean, system-managed connection. Once connected, the MediaControllerCompat "
    "it provides automatically routes commands through the MediaSession — meaning the service, the car host, "
    "voice assistant, and your UI all share the same control channel. "
    "This is the standard pattern for all Android media apps."
))
story.append(SP(5))
story.append(SecTitle("Connecting to the Service — PlayerScreen"))
story.append(Code(
    "// PlayerScreen.kt — init block\n"
    "mediaBrowser = MediaBrowserCompat(\n"
    "    carContext,\n"
    "    ComponentName(carContext, SmartMusicService::class.java),  // target service\n"
    "    object : MediaBrowserCompat.ConnectionCallback() {\n\n"
    "        override fun onConnected() {\n"
    "            // Get controller using the session token from the service\n"
    "            mediaController = MediaControllerCompat(\n"
    "                carContext, mediaBrowser!!.sessionToken\n"
    "            )\n\n"
    "            // Start playing the selected song immediately\n"
    "            mediaController?.transportControls?.playFromMediaId(song.id, null)\n"
    "            isPlaying = true\n\n"
    "            // Listen for state changes to update UI\n"
    "            mediaController?.registerCallback(object : MediaControllerCompat.Callback() {\n"
    "                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {\n"
    "                    isPlaying = state?.state == STATE_PLAYING\n"
    "                    currentPositionMs = state?.position ?: 0L\n"
    "                    invalidate()   // re-render the template\n"
    "                }\n"
    "                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {\n"
    "                    // Auto-next triggered in service — update which song is showing\n"
    "                    val newId = metadata?.getString(METADATA_KEY_MEDIA_ID)\n"
    "                    val newSong = songs.firstOrNull { it.id == newId }\n"
    "                    if (newSong != null && newSong.id != song.id) {\n"
    "                        song = newSong\n"
    "                        loadAlbumArt()\n"
    "                        invalidate()\n"
    "                    }\n"
    "                }\n"
    "            })\n"
    "        }\n"
    "    }, null\n"
    ")\n"
    "mediaBrowser?.connect()"
))
story.append(SecTitle("Transport Controls — Sending Commands"))
story.append(Code(
    "// All commands go through transportControls:\n"
    "mediaController?.transportControls?.play()\n"
    "mediaController?.transportControls?.pause()\n"
    "mediaController?.transportControls?.stop()\n"
    "mediaController?.transportControls?.skipToNext()\n"
    "mediaController?.transportControls?.skipToPrevious()\n"
    "mediaController?.transportControls?.seekTo(positionMs)\n"
    "mediaController?.transportControls?.playFromMediaId(song.id, null)\n"
    "mediaController?.transportControls?.playFromSearch(\"Kesariya\", null)"
))
story.append(Tip(
    "Keep mediaBrowser and mediaController as class-level fields (not local variables). "
    "If declared inside the callback lambda, the garbage collector may collect them before "
    "onConnected() fires, causing a null reference crash."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 9 — ExoPlayer
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(9, "ExoPlayer (Media3) — Audio Playback Engine"))
story.append(SP(6))
story.append(WhatIs(
    "ExoPlayer (now part of the Media3 library as androidx.media3.exoplayer) is Google's "
    "recommended media player for Android. It supports streaming audio/video from URLs (HLS, DASH, MP3, etc.), "
    "local files, and adaptive bitrate formats. It is far more powerful and extensible than Android's "
    "built-in MediaPlayer, and integrates directly with MediaSession for state synchronization."
))
story.append(SP(4))
story.append(WhyUse(
    "SmartAAOS streams 5 Bollywood songs from remote MP3 URLs. ExoPlayer handles all the "
    "complexity of network buffering, codec selection, and error recovery. "
    "It also fires state change events (STATE_ENDED, STATE_READY, onPlayerError) that SmartMusicService "
    "uses to auto-advance to the next song. The Media3 version is the latest stable iteration "
    "and replaces the older standalone ExoPlayer library."
))
story.append(SP(5))
story.append(Code(
    "// 1. Create the player (once, in onCreate)\n"
    "exoPlayer = ExoPlayer.Builder(this).build()\n\n"
    "// 2. Add a state listener\n"
    "exoPlayer.addListener(object : Player.Listener {\n\n"
    "    override fun onPlaybackStateChanged(playbackState: Int) {\n"
    "        when (playbackState) {\n"
    "            Player.STATE_IDLE      -> { /* Initial state, nothing loaded */ }\n"
    "            Player.STATE_BUFFERING -> { /* Loading/buffering data from network */ }\n"
    "            Player.STATE_READY     -> {\n"
    "                // Player is ready — if playWhenReady is true, it will play\n"
    "                if (exoPlayer.playWhenReady)\n"
    "                    updatePlaybackState(STATE_PLAYING)\n"
    "            }\n"
    "            Player.STATE_ENDED     -> {\n"
    "                // Song finished — skip to next automatically\n"
    "                onSkipToNext()\n"
    "            }\n"
    "        }\n"
    "    }\n\n"
    "    override fun onPlayerError(error: PlaybackException) {\n"
    "        // Network error, bad URL, codec issue — skip to next\n"
    "        Log.e(\"SmartAAOS\", \"Player error: ${error.message}\")\n"
    "        onSkipToNext()\n"
    "    }\n"
    "})\n\n"
    "// 3. Load and play a song (called for every song change)\n"
    "private fun playSong(index: Int) {\n"
    "    val song = MusicData.songs[index]\n"
    "    val mediaItem = MediaItem.fromUri(song.url)  // URL or local URI\n"
    "    exoPlayer.setMediaItem(mediaItem)\n"
    "    exoPlayer.prepare()    // start loading/buffering\n"
    "    if (requestAudioFocus()) {\n"
    "        exoPlayer.play()   // start playback\n"
    "    }\n"
    "    updateMetadata(index)\n"
    "    startForegroundService()\n"
    "    startProgressTimer()\n"
    "}\n\n"
    "// 4. Always release in onDestroy to free native resources\n"
    "override fun onDestroy() {\n"
    "    exoPlayer.release()\n"
    "    session.release()\n"
    "}"
))

states_data = [
    ["State",           "Meaning",                                "Typical Action"],
    ["STATE_IDLE",      "Player created, no media loaded",        "Call prepare() after setMediaItem()"],
    ["STATE_BUFFERING", "Loading media from network/disk",        "Show loading indicator"],
    ["STATE_READY",     "Buffered and ready to play/seek",        "Play if playWhenReady = true"],
    ["STATE_ENDED",     "Reached end of media",                   "Auto-skip to next song"],
]
story.append(make_table(states_data, [32*mm, 68*mm, None], "#2E7D32",
    [colors.HexColor("#E8F5E9"), colors.white]))
story.append(SP(4))
story.append(Note(
    "ExoPlayer is a main-thread component. Never call exoPlayer.play(), pause(), seekTo() "
    "from a background thread. Use Handler(Looper.getMainLooper()).post { ... } if needed."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 10 — Audio Focus
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(10, "Audio Focus Management"))
story.append(SP(6))
story.append(WhatIs(
    "Audio focus is Android's system for managing which app gets to play audio at any given moment. "
    "When your app wants to play music, it must request audio focus from the AudioManager. "
    "Other apps (navigation, phone calls, assistant) can take focus away temporarily or permanently. "
    "Your app must listen for these focus changes and respond appropriately — pausing, lowering volume, "
    "or resuming based on the type of focus change."
))
story.append(SP(4))
story.append(WhyUse(
    "In a car, multiple audio sources compete simultaneously: your music, turn-by-turn navigation, "
    "incoming calls, Google Assistant, and system alerts. Audio focus ensures they don't all "
    "play over each other. If your app ignores audio focus, it will continue playing during phone calls "
    "and Google Assistant queries — which is unacceptable for AAOS certification. "
    "Proper audio focus handling is a Play Store automotive policy requirement."
))
story.append(SP(5))
story.append(Code(
    "// Step 1: Build the AudioFocusRequest\n"
    "audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)\n"
    "    .setAudioAttributes(\n"
    "        AudioAttributes.Builder()\n"
    "            .setUsage(AudioAttributes.USAGE_MEDIA)              // music\n"
    "            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC) // audio content type\n"
    "            .build()\n"
    "    )\n"
    "    .setAcceptsDelayedFocusGain(true)   // wait for focus if currently unavailable\n"
    "    .setOnAudioFocusChangeListener(audioFocusListener)\n"
    "    .build()\n\n"
    "// Step 2: Request focus before every play() call\n"
    "private fun requestAudioFocus(): Boolean {\n"
    "    val result = audioManager.requestAudioFocus(audioFocusRequest)\n"
    "    return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED\n"
    "}\n\n"
    "// Step 3: Handle focus changes\n"
    "val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->\n"
    "    when (focusChange) {\n"
    "        AUDIOFOCUS_GAIN -> {\n"
    "            // Focus returned (e.g., call ended) — resume at full volume\n"
    "            exoPlayer.volume = 1f\n"
    "            exoPlayer.play()\n"
    "            updatePlaybackState(STATE_PLAYING)\n"
    "        }\n"
    "        AUDIOFOCUS_LOSS -> {\n"
    "            // Another app permanently took over (e.g., Spotify) — stop\n"
    "            exoPlayer.pause()\n"
    "            abandonAudioFocus()\n"
    "        }\n"
    "        AUDIOFOCUS_LOSS_TRANSIENT -> {\n"
    "            // Temporary loss (phone call, assistant) — just pause\n"
    "            exoPlayer.pause()\n"
    "        }\n"
    "        AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {\n"
    "            // Notification or brief sound — lower volume instead of pausing\n"
    "            exoPlayer.volume = 0.2f\n"
    "        }\n"
    "    }\n"
    "}\n\n"
    "// Step 4: Abandon focus when paused, stopped, or destroyed\n"
    "private fun abandonAudioFocus() {\n"
    "    if (::audioFocusRequest.isInitialized)\n"
    "        audioManager.abandonAudioFocusRequest(audioFocusRequest)\n"
    "}"
))
story.append(Key(
    "Always request audio focus before calling exoPlayer.play() and abandon it when you stop. "
    "Apps that hold audio focus without playing are rejected during AAOS certification."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 11 — Foreground Service + Notification
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(11, "Foreground Service + Media Notification"))
story.append(SP(6))
story.append(WhatIs(
    "A foreground service is an Android service that has user-visible presence via a notification. "
    "Unlike background services (which Android aggressively kills to save memory), foreground services "
    "are protected from being killed as long as they show a notification. "
    "A MediaStyle notification is a specialized notification that shows album art, track name, "
    "and playback buttons — styled by the system to match the platform's media UI."
))
story.append(SP(4))
story.append(WhyUse(
    "If SmartMusicService ran as a regular background service, Android would kill it when the user "
    "switches screens — cutting off music playback. Foreground service keeps music alive indefinitely. "
    "The MediaStyle notification is critical because: (1) it satisfies the foreground service visibility "
    "requirement, (2) it shows media controls on the car's notification shade and lock screen, "
    "and (3) it lets the car host discover your service for system-level media controls."
))
story.append(SP(5))
story.append(Code(
    "private fun startForegroundService() {\n"
    "    val song = MusicData.songs[currentIndex]\n\n"
    "    // Build Previous, Play/Pause, Next actions using MediaButtonReceiver\n"
    "    // MediaButtonReceiver automatically routes button taps to MediaSession\n"
    "    val prevAction = NotificationCompat.Action(\n"
    "        android.R.drawable.ic_media_previous, \"Previous\",\n"
    "        MediaButtonReceiver.buildMediaButtonPendingIntent(this, ACTION_SKIP_TO_PREVIOUS)\n"
    "    )\n"
    "    val playPauseAction = if (exoPlayer.isPlaying)\n"
    "        NotificationCompat.Action(android.R.drawable.ic_media_pause, \"Pause\",\n"
    "            MediaButtonReceiver.buildMediaButtonPendingIntent(this, ACTION_PAUSE))\n"
    "    else\n"
    "        NotificationCompat.Action(android.R.drawable.ic_media_play, \"Play\",\n"
    "            MediaButtonReceiver.buildMediaButtonPendingIntent(this, ACTION_PLAY))\n"
    "    val nextAction = NotificationCompat.Action(\n"
    "        android.R.drawable.ic_media_next, \"Next\",\n"
    "        MediaButtonReceiver.buildMediaButtonPendingIntent(this, ACTION_SKIP_TO_NEXT)\n"
    "    )\n\n"
    "    val notification = NotificationCompat.Builder(this, CHANNEL_ID)\n"
    "        .setContentTitle(song.title)\n"
    "        .setContentText(song.artist)\n"
    "        .setSubText(song.album)\n"
    "        .setSmallIcon(android.R.drawable.ic_media_play)\n"
    "        .setOngoing(true)                   // cannot be dismissed by user\n"
    "        .addAction(prevAction)\n"
    "        .addAction(playPauseAction)\n"
    "        .addAction(nextAction)\n"
    "        .setStyle(\n"
    "            // MediaStyle: connects notification to MediaSession\n"
    "            // System renders rich media controls (album art, progress)\n"
    "            androidx.media.app.NotificationCompat.MediaStyle()\n"
    "                .setMediaSession(session.sessionToken)\n"
    "                .setShowActionsInCompactView(0, 1, 2)  // show all 3 in compact\n"
    "        )\n"
    "        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // lock screen\n"
    "        .build()\n\n"
    "    // Android Q+ requires specifying the service type\n"
    "    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {\n"
    "        startForeground(NOTIFICATION_ID, notification,\n"
    "            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)\n"
    "    } else {\n"
    "        startForeground(NOTIFICATION_ID, notification)\n"
    "    }\n"
    "}"
))
story.append(Note(
    "A NotificationChannel must be created before building the notification on Android O (API 26+). "
    "Use IMPORTANCE_LOW to avoid the notification making a sound every time it updates."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 12 — Voice Control
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(12, "Voice Control — MEDIA_PLAY_FROM_SEARCH"))
story.append(SP(6))
story.append(WhatIs(
    'MEDIA_PLAY_FROM_SEARCH is an Android intent action fired by Google Assistant '
    'when a user makes a voice request like "Hey Google, play Kesariya" or '
    '"Hey Google, play songs by Arijit Singh". The intent carries the user\'s query text '
    'and is delivered to the media app\'s Session (via onNewIntent) or directly to the '
    'music service (via onStartCommand).'
))
story.append(SP(4))
story.append(WhyUse(
    "Voice control is a first-class feature for in-car apps. Drivers cannot safely type or tap "
    "to find a song. Voice search lets them request music without taking their eyes off the road. "
    "Implementing MEDIA_PLAY_FROM_SEARCH correctly means your app is fully voice-controllable "
    "via Google Assistant — a key requirement for AAOS media apps. "
    "SmartAAOS handles voice at two levels: in SmartSession (for navigation to PlayerScreen) "
    "and in SmartMusicService (for the actual playback)."
))
story.append(SP(5))
story.append(SecTitle("End-to-End Voice Flow"))
story.append(Code(
    "User: \"Hey Google, play Kesariya\"\n"
    "  ↓\n"
    "Google Assistant fires MEDIA_PLAY_FROM_SEARCH intent\n"
    "  ↓\n"
    "SmartSession.onNewIntent() received\n"
    "  ↓\n"
    "MediaBrowserCompat connects to SmartMusicService\n"
    "  ↓\n"
    "onConnected: mediaController.transportControls.playFromSearch(\"Kesariya\")\n"
    "  ↓\n"
    "SmartMusicService.callback.onPlayFromSearch(\"Kesariya\")\n"
    "  ↓\n"
    "findSongByQuery(\"kesariya\") → index = 2\n"
    "  ↓\n"
    "playSong(2) → ExoPlayer starts streaming\n"
    "  ↓\n"
    "handler.post { NavigationCallback.onPlaySong?.invoke(song) }\n"
    "  ↓\n"
    "HomeScreen pushes PlayerScreen → user sees Now Playing"
))
story.append(SP(5))
story.append(SecTitle("Fuzzy Search Algorithm"))
story.append(Body(
    "Because voice recognition may return partial matches or variations (\"kesariya\" vs \"Kesariya\"), "
    "SmartAAOS uses a 4-level fuzzy search — trying progressively looser matches:"
))
story.append(Code(
    "private fun findSongByQuery(query: String): Int {\n"
    "    val q = query.lowercase().trim()\n\n"
    "    // Level 1: exact title match (most specific)\n"
    "    var idx = songs.indexOfFirst { it.title.lowercase() == q }\n"
    "    if (idx != -1) return idx\n\n"
    "    // Level 2: title contains the query\n"
    "    idx = songs.indexOfFirst { it.title.lowercase().contains(q) }\n"
    "    if (idx != -1) return idx\n\n"
    "    // Level 3: artist name contains the query\n"
    "    idx = songs.indexOfFirst { it.artist.lowercase().contains(q) }\n"
    "    if (idx != -1) return idx\n\n"
    "    // Level 4: album name contains the query (least specific)\n"
    "    idx = songs.indexOfFirst { it.album.lowercase().contains(q) }\n"
    "    if (idx != -1) return idx\n\n"
    "    return -1  // no match — caller falls back to playing song[0]\n"
    "}"
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 13 — AIDL
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(13, "AIDL — IPC for Vehicle Data"))
story.append(SP(6))
story.append(WhatIs(
    "AIDL (Android Interface Definition Language) is Android's mechanism for Inter-Process Communication (IPC). "
    "It allows one process to call methods on an object that lives in a different process, as if it were local. "
    "You define an interface in a .aidl file, and Android generates the Java/Kotlin Stub and Proxy classes. "
    "The server process implements the Stub; client processes call through the Proxy transparently."
))
story.append(SP(4))
story.append(WhyUse(
    "In a real car, vehicle data (speed, RPM, fuel) comes from a separate OEM system service running "
    "in a different process with privileged permissions. Your app cannot directly access these "
    "sensors — it must IPC to the OEM's service. SmartAAOS simulates this architecture with "
    "VehicleDataService (the server) and VehicleRepository (the client) connected via AIDL. "
    "Understanding AIDL is essential for any app that integrates with real automotive sensor data."
))
story.append(SP(5))
story.append(SecTitle("Step 1 — Define the AIDL Interface"))
story.append(Body(
    "The .aidl file is the contract. Both server and client share this definition. "
    "Android Studio generates Stub and Proxy classes automatically when you build the project."
))
story.append(Code(
    "// automotive/src/main/aidl/com/swapnil/smart/aaos/vehicle/IVehicleDataService.aidl\n"
    "interface IVehicleDataService {\n"
    "    float getSpeed();             // current speed in km/h\n"
    "    float getRpm();               // engine RPM\n"
    "    float getFuelLevel();         // fuel percentage 0-100\n"
    "    String getGear();             // P, R, N, D, 1, 2, 3...\n"
    "    boolean isEngineOn();         // engine on/off\n"
    "    float getOdometer();          // total km driven\n"
    "    void simulateDriving(float speedKmh, float rpm, float fuel); // for testing\n"
    "    void simulateParked();        // reset to parked state\n"
    "}"
))
story.append(SecTitle("Step 2 — Implement the Server (VehicleDataService)"))
story.append(Code(
    "class VehicleDataService : Service() {\n\n"
    "    // Implement all AIDL methods in the Stub\n"
    "    private val binder = object : IVehicleDataService.Stub() {\n"
    "        override fun getSpeed() = currentSpeed\n"
    "        override fun getRpm()   = currentRpm\n"
    "        override fun getFuelLevel() = currentFuel\n"
    "        override fun getGear()  = currentGear\n"
    "        override fun isEngineOn() = engineOn\n"
    "        override fun getOdometer() = currentOdometer\n\n"
    "        override fun simulateDriving(speedKmh: Float, rpm: Float, fuel: Float) {\n"
    "            currentSpeed = speedKmh\n"
    "            currentRpm   = rpm\n"
    "            currentFuel  = fuel\n"
    "            currentGear  = \"D\"\n"
    "        }\n"
    "        override fun simulateParked() {\n"
    "            currentSpeed = 0f;  currentRpm = 800f;  currentGear = \"P\"\n"
    "        }\n"
    "    }\n\n"
    "    override fun onBind(intent: Intent): IBinder = binder\n"
    "}"
))
story.append(SecTitle("Step 3 — Bind from Client (VehicleRepository)"))
story.append(Code(
    "object VehicleRepository {\n"
    "    private var vehicleService: IVehicleDataService? = null\n\n"
    "    private val connection = object : ServiceConnection {\n"
    "        override fun onServiceConnected(name: ComponentName, binder: IBinder) {\n"
    "            // Convert raw IBinder to the typed AIDL interface\n"
    "            vehicleService = IVehicleDataService.Stub.asInterface(binder)\n"
    "        }\n"
    "        override fun onServiceDisconnected(name: ComponentName) {\n"
    "            vehicleService = null\n"
    "        }\n"
    "    }\n\n"
    "    fun connect(context: Context) {\n"
    "        context.bindService(\n"
    "            Intent(context, VehicleDataService::class.java),\n"
    "            connection,\n"
    "            Context.BIND_AUTO_CREATE\n"
    "        )\n"
    "    }\n\n"
    "    fun getSpeed(): Float = vehicleService?.getSpeed() ?: 0f\n"
    "    fun getRpm():   Float = vehicleService?.getRpm()   ?: 800f\n"
    "}"
))
story.append(Note(
    "Protect AIDL services with android:protectionLevel=\"signature\" in the Manifest so that "
    "only apps signed with the same key can bind. This prevents unauthorized apps from reading "
    "vehicle data."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 14 — MVVM Architecture
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(14, "MVVM Architecture in Car App"))
story.append(SP(6))
story.append(WhatIs(
    "MVVM (Model-View-ViewModel) separates the UI (Screen/Template) from business logic (ViewModel) "
    "and data (Repository/AIDL). The ViewModel holds LiveData observables — reactive state containers "
    "that automatically notify observers when values change. This is the recommended Android "
    "architecture pattern. However, Car App Library's Screen class is not a ViewModelStoreOwner, "
    "so standard ViewModelProvider doesn't work — SmartAAOS solves this with a custom CarViewModelStore."
))
story.append(SP(4))
story.append(WhyUse(
    "Without MVVM, every Screen would need to directly poll the AIDL service, manage coroutines, "
    "and track state locally. With MVVM: (1) all Screens share the same VehicleViewModel instance, "
    "so they always show consistent data; (2) the ViewModel survives screen pushes/pops, "
    "preserving state; (3) LiveData automatically triggers invalidate() when vehicle data changes, "
    "eliminating manual polling from each screen."
))
story.append(SP(5))
story.append(SecTitle("CarViewModelStore — Custom ViewModel Registry"))
story.append(Body(
    "Since Car App Screens don't have a ViewModelStore, SmartAAOS implements a singleton store "
    "that creates and caches ViewModel instances by class type. "
    "All screens get the same instance — state is naturally shared."
))
story.append(Code(
    "object CarViewModelStore {\n"
    "    private val store = mutableMapOf<Class<*>, Any>()\n\n"
    "    // Get existing ViewModel or create a new one\n"
    "    @Suppress(\"UNCHECKED_CAST\")\n"
    "    fun <T : Any> get(clazz: Class<T>): T {\n"
    "        return store.getOrPut(clazz) {\n"
    "            clazz.getDeclaredConstructor().newInstance()\n"
    "        } as T\n"
    "    }\n\n"
    "    // Called in CarAppService.onDestroy() to free everything\n"
    "    fun clear() = store.clear()\n"
    "}"
))
story.append(SecTitle("VehicleViewModel — State + Polling"))
story.append(Body(
    "VehicleViewModel owns all vehicle-related LiveData and polls the AIDL service every second "
    "in a background coroutine. It also feeds data to AlertRepository to check thresholds."
))
story.append(Code(
    "class VehicleViewModel : ViewModel() {\n\n"
    "    // LiveData — screens observe these\n"
    "    val speed       = MutableLiveData(0f)\n"
    "    val rpm         = MutableLiveData(800f)\n"
    "    val fuelLevel   = MutableLiveData(75f)\n"
    "    val gear        = MutableLiveData(\"P\")\n"
    "    val isCarMoving = MutableLiveData(false)  // speed > 2 km/h\n"
    "    val currentAlert = MutableLiveData<VehicleAlert?>()\n\n"
    "    private val scope = CoroutineScope(Dispatchers.IO + Job())\n\n"
    "    init {\n"
    "        scope.launch {\n"
    "            while (true) {\n"
    "                // Read from AIDL service on IO thread\n"
    "                val spd  = VehicleRepository.getSpeed()\n"
    "                val rpm  = VehicleRepository.getRpm()\n"
    "                val fuel = VehicleRepository.getFuelLevel()\n\n"
    "                // Update LiveData on main thread\n"
    "                withContext(Dispatchers.Main) {\n"
    "                    speed.value       = spd\n"
    "                    isCarMoving.value = spd > 2f\n"
    "                    AlertRepository.evaluate(spd, rpm, fuel)\n"
    "                    currentAlert.value = AlertRepository.currentAlert\n"
    "                }\n"
    "                delay(1000)  // poll every 1 second\n"
    "            }\n"
    "        }\n"
    "    }\n"
    "}"
))
story.append(SecTitle("Using in Screens"))
story.append(Code(
    "class HomeScreen(carContext: CarContext) : Screen(carContext) {\n\n"
    "    // Get shared instance — same object across all screens\n"
    "    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)\n\n"
    "    init {\n"
    "        // Observe and auto-refresh when state changes\n"
    "        viewModel.isCarMoving.observeForever  { invalidate() }\n"
    "        viewModel.currentAlert.observeForever { invalidate() }\n"
    "    }\n\n"
    "    override fun onGetTemplate(): Template {\n"
    "        val isMoving = viewModel.isCarMoving.value ?: false\n"
    "        // Build template based on current state...\n"
    "    }\n"
    "}"
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 15 — Driving Mode Restrictions
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(15, "Driving Mode Restrictions"))
story.append(SP(6))
story.append(WhatIs(
    "Driving mode restrictions are UX constraints that disable or limit certain app interactions "
    "while the vehicle is in motion. They exist to prevent driver distraction — a legal and safety "
    "requirement for automotive software. In SmartAAOS, interactions that require sustained attention "
    "(browsing song list, opening dashboard, changing next song) are disabled when speed exceeds 2 km/h."
))
story.append(SP(4))
story.append(WhyUse(
    "Car OEMs and Google Play Store's automotive policies require media apps to implement driver "
    "distraction guidelines. Apps that allow complex interactions while driving can be rejected "
    "from the automotive Play Store or cause OEM certification to fail. "
    "In real AAOS, the car host itself also enforces some restrictions — but apps are expected "
    "to self-enforce based on vehicle speed data, providing defence-in-depth."
))
story.append(SP(5))
story.append(Code(
    "// HomeScreen.onGetTemplate()\n"
    "val isMoving = viewModel.isCarMoving.value ?: false  // true when speed > 2 km/h\n\n"
    "// Disable song list item clicks while driving\n"
    "MusicData.songs.forEachIndexed { index, song ->\n"
    "    val rowBuilder = Row.Builder()\n"
    "        .setTitle(song.title)\n"
    "        .addText(\"${song.artist} • ${song.album}\")\n"
    "        .setImage(icon)\n\n"
    "    if (!isMoving) {  // only set click listener when PARKED\n"
    "        rowBuilder.setOnClickListener {\n"
    "            screenManager.push(PlayerScreen(carContext, song, {}))\n"
    "        }\n"
    "    }\n"
    "    // When driving: row is visible but not tappable\n"
    "    listBuilder.addItem(rowBuilder.build())\n"
    "}\n\n"
    "// Disable dashboard navigation while driving\n"
    "val dashboardRow = Row.Builder()\n"
    "    .setTitle(\"Vehicle Dashboard\")\n"
    "    .addText(\"Speed • RPM • Fuel • Gear\")\n"
    "if (!isMoving) {\n"
    "    dashboardRow.setOnClickListener { screenManager.push(DashboardScreen(carContext)) }\n"
    "}\n\n"
    "// PlayerScreen: check speed inline for Next button\n"
    "val nextAction = Action.Builder()\n"
    "    .setTitle(\"Next\")\n"
    "    .setOnClickListener {\n"
    "        val speed = VehicleRepository.getSpeed()\n"
    "        if (speed <= 2f) {   // only allowed when nearly stopped\n"
    "            mediaController?.transportControls?.skipToNext()\n"
    "        }\n"
    "    }.build()"
))
story.append(SecTitle("Title Changes Based on Drive State"))
story.append(Code(
    "// Header title reflects current state\n"
    "ListTemplate.Builder()\n"
    "    .setTitle(\n"
    "        if (isMoving) \"Smart AAOS — Driving\"\n"
    "        else          \"Smart AAOS — Parked\"\n"
    "    )"
))
story.append(Key(
    "The 2 km/h threshold (not 0) accounts for very slow parking maneuvers where the car "
    "is technically moving but the driver has full attention available."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 16 — Alert System
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(16, "Alert System — Real-Time Vehicle Warnings"))
story.append(SP(6))
story.append(WhatIs(
    "The alert system is a real-time monitoring layer that continuously evaluates vehicle telemetry "
    "against safety thresholds. When a threshold is exceeded (overspeed, high RPM, low fuel), "
    "an alert is raised and displayed on all active screens — both HomeScreen and PlayerScreen. "
    "Alerts have severity levels (LOW, MEDIUM, HIGH, CRITICAL) that indicate urgency."
))
story.append(SP(4))
story.append(WhyUse(
    "In a real production car app, the vehicle exposes safety events through the CarPropertyManager API. "
    "SmartAAOS simulates this with AlertRepository to demonstrate the pattern: a centralized monitoring "
    "object that evaluates rules and exposes the current alert via LiveData. "
    "The alert shows up on any active screen without requiring explicit routing — because "
    "VehicleViewModel.currentAlert is observed by all screens, an alert raised anywhere "
    "automatically appears everywhere."
))
story.append(SP(5))
story.append(Code(
    "// VehicleAlert.kt — data model\n"
    "data class VehicleAlert(\n"
    "    val message: String,\n"
    "    val severity: AlertSeverity\n"
    ")\n\n"
    "enum class AlertSeverity { LOW, MEDIUM, HIGH, CRITICAL }\n\n"
    "// AlertRepository.kt — threshold evaluation\n"
    "object AlertRepository {\n"
    "    var currentAlert: VehicleAlert? = null\n\n"
    "    fun evaluate(speed: Float, rpm: Float, fuel: Float) {\n"
    "        currentAlert = when {\n"
    "            speed > 100f  -> VehicleAlert(\"Overspeed: ${speed} km/h\",  AlertSeverity.HIGH)\n"
    "            rpm   > 5000f -> VehicleAlert(\"Engine stress: ${rpm} RPM\", AlertSeverity.MEDIUM)\n"
    "            fuel  < 10f   -> VehicleAlert(\"Low fuel: ${fuel}%\",        AlertSeverity.LOW)\n"
    "            else          -> null   // clear alert when all normal\n"
    "        }\n"
    "    }\n"
    "}"
))
story.append(SecTitle("Displaying Alerts on HomeScreen"))
story.append(Code(
    "// HomeScreen.onGetTemplate() — inject alert row at top of list\n"
    "viewModel.currentAlert.value?.let { alert ->\n"
    "    listBuilder.addItem(\n"
    "        Row.Builder()\n"
    "            .setTitle(\"${alert.message}\")\n"
    "            .addText(\"Severity: ${alert.severity} — Tap to view details\")\n"
    "            .setOnClickListener {\n"
    "                screenManager.push(DiagnosticsScreen(carContext))\n"
    "            }\n"
    "            .build()\n"
    "    )\n"
    "}"
))
story.append(SecTitle("Displaying Alerts on PlayerScreen"))
story.append(Code(
    "// PlayerScreen.onGetTemplate() — add alert row above song info\n"
    "AlertRepository.currentAlert?.let { alert ->\n"
    "    paneBuilder.addRow(\n"
    "        Row.Builder()\n"
    "            .setTitle(\"${alert.message}\")\n"
    "            .addText(\"Check vehicle status\")\n"
    "            .build()\n"
    "    )\n"
    "}"
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 17 — Progress Bar + Handler Timer
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(17, "Progress Bar + Handler Refresh Timer"))
story.append(SP(6))
story.append(WhatIs(
    "Car App Library templates do not provide a native progress bar widget. "
    "To display song playback progress, SmartAAOS builds a visual progress bar from Unicode "
    "block characters (█ for filled, ░ for empty) and renders it as a text line in the Row. "
    "To keep it updated in real time, a Handler running on the main thread calls invalidate() "
    "every second — causing onGetTemplate() to rebuild the template with fresh position data."
))
story.append(SP(4))
story.append(WhyUse(
    "Users need to know how far into a song they are, especially in a car context. "
    "The text-based progress bar is a creative workaround for the template system's limitations — "
    "it fits inside a Row's addText() line and updates smoothly every second. "
    "The Handler timer pattern (instead of a CoroutineScope timer) is used because ExoPlayer "
    "must be accessed from the main thread — Handler(Looper.getMainLooper()) guarantees this."
))
story.append(SP(5))
story.append(SecTitle("Building the Visual Progress Bar"))
story.append(Code(
    "// Builds: ████████░░░░░░░  (15 blocks total)\n"
    "private fun buildProgressBar(positionMs: Long, durationMs: Long): String {\n"
    "    if (durationMs <= 0) return \"░░░░░░░░░░░░░░░\"\n"
    "    val totalBlocks = 15\n"
    "    val filled = (positionMs.toFloat() / durationMs.toFloat() * totalBlocks).toInt()\n"
    "    val clamped = filled.coerceIn(0, totalBlocks)  // avoid overflow\n"
    "    return \"█\".repeat(clamped) + \"░\".repeat(totalBlocks - clamped)\n"
    "}\n\n"
    "// Builds: 1:23 / 3:45\n"
    "private fun buildProgressText(positionMs: Long, durationMs: Long): String {\n"
    "    return \"${formatTime(positionMs)} / ${formatTime(durationMs)}\"\n"
    "}\n\n"
    "private fun formatTime(ms: Long): String {\n"
    "    val seconds = ms / 1000\n"
    "    return \"%d:%02d\".format(seconds / 60, seconds % 60)\n"
    "}\n\n"
    "// Usage in onGetTemplate()\n"
    "val progressLine = \"${buildProgressBar(currentPositionMs, song.durationMs)}  \"\n"
    "                 + buildProgressText(currentPositionMs, song.durationMs)\n"
    "                 + \"  Track $songNumber of ${MusicData.songs.size}\"\n\n"
    "Row.Builder()\n"
    "    .setTitle(song.title)\n"
    "    .addText(\"${song.artist} • ${song.album}\")\n"
    "    .addText(progressLine)   // second text line = progress bar\n"
    "    .build()"
))
story.append(SecTitle("Handler Timer — Periodic Refresh"))
story.append(Code(
    "// Declared at class level — must be class-level, not local, to cancel it later\n"
    "private val handler = Handler(Looper.getMainLooper())\n\n"
    "private val refreshRunnable = object : Runnable {\n"
    "    override fun run() {\n"
    "        invalidate()                       // rebuild template with new position\n"
    "        handler.postDelayed(this, 1000L)   // schedule again in 1 second\n"
    "    }\n"
    "}\n\n"
    "// Start in init block\n"
    "init {\n"
    "    handler.post(refreshRunnable)\n"
    "}\n\n"
    "// PlayerScreen does NOT have onDestroy() — clean up via mediaBrowser\n"
    "// Service side: stop timer on pause/stop\n"
    "private fun stopProgressTimer() {\n"
    "    handler.removeCallbacks(progressRunnable)\n"
    "}"
))
story.append(Key(
    "The Runnable re-schedules itself using postDelayed(this, 1000L) — this is the standard "
    "Android pattern for periodic main-thread work. It is preferred over a background thread + runOnUiThread "
    "because it avoids threading overhead and keeps ExoPlayer calls safely on the main thread."
))

story.append(PageBreak())

# ═════════════════════════════════════════════════════════════════════════════
#  TOPIC 18 — Build Configuration
# ═════════════════════════════════════════════════════════════════════════════
story.append(Banner(18, "Build Configuration"))
story.append(SP(6))
story.append(WhatIs(
    "The build configuration for an AAOS project has specific requirements: minSdk 29 (required for "
    "AAOS), separate dependencies for the automotive vs phone module, and the Gradle Version Catalog "
    "(libs.versions.toml) for centralized dependency management. Getting the build configuration "
    "wrong is one of the most common causes of AAOS apps not appearing on the automotive Play Store."
))
story.append(SP(4))
story.append(WhyUse(
    "The automotive module uses app-automotive (not app) from the Car App Library — using the wrong "
    "artifact causes the app to build but not run on AAOS. The minSdk 29 requirement exists because "
    "Android 10 introduced automotive-specific APIs and the AAOS hardware requirement. "
    "The Version Catalog centralizes version numbers, preventing version conflicts across modules "
    "and making dependency upgrades a single-file change."
))
story.append(SP(5))
story.append(SecTitle("automotive/build.gradle.kts"))
story.append(Code(
    "android {\n"
    "    compileSdk = 36                  // Android 15 — always use latest\n\n"
    "    defaultConfig {\n"
    "        applicationId = \"com.swapnil.smart.aaos\"\n"
    "        minSdk    = 29               // Android 10 — mandatory for AAOS\n"
    "        targetSdk = 36               // Android 15\n"
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
    "    implementation(libs.androidx.car.app.automotive) // AAOS — NOT 'app'\n"
    "    implementation(libs.media3.exoplayer)            // ExoPlayer\n"
    "    implementation(libs.androidx.media)              // MediaBrowserServiceCompat\n"
    "    implementation(libs.glide)                       // album art from URL\n"
    "    implementation(libs.androidx.lifecycle.viewmodel)\n"
    "    implementation(libs.androidx.lifecycle.livedata)\n"
    "}"
))
story.append(SecTitle("gradle/libs.versions.toml — Version Catalog"))
story.append(Code(
    "[versions]\n"
    "agp                = \"8.13.2\"\n"
    "kotlin             = \"2.0.21\"\n"
    "carApp             = \"1.4.0\"\n"
    "media3             = \"1.3.0\"\n"
    "mediaBrowserCompat = \"1.7.0\"\n"
    "glide              = \"4.16.0\"\n"
    "lifecycle          = \"2.7.0\"\n\n"
    "[libraries]\n"
    "# AAOS module — use app-automotive artifact\n"
    "androidx-car-app-automotive = { group = \"androidx.car.app\",\n"
    "                                name  = \"app-automotive\",\n"
    "                                version.ref = \"carApp\" }\n\n"
    "# Android Auto phone module — use app artifact (not app-automotive)\n"
    "androidx-car-app = { group = \"androidx.car.app\",\n"
    "                     name  = \"app\",\n"
    "                     version.ref = \"carApp\" }\n\n"
    "media3-exoplayer = { group = \"androidx.media3\",\n"
    "                     name  = \"exoplayer\",\n"
    "                     version.ref = \"media3\" }"
))

deps_data = [
    ["Library",                 "Artifact",       "Used For",                          "Module"],
    ["Car App Library (AAOS)",  "app-automotive", "Templates, Session, Screen, CarIcon", "automotive"],
    ["Car App Library (Auto)",  "app",            "Same API, for Android Auto phone",    "app"],
    ["Media3 ExoPlayer",        "exoplayer",      "Audio playback engine",               "both"],
    ["AndroidX Media",          "media",          "MediaBrowserServiceCompat",           "both"],
    ["Lifecycle ViewModel",     "lifecycle-vm",   "ViewModel base class",                "both"],
    ["Lifecycle LiveData",      "lifecycle-ld",   "Reactive state (LiveData)",           "both"],
    ["Glide",                   "glide:glide",    "Load album art from URLs",            "automotive"],
]
story.append(make_table(deps_data, [36*mm, 28*mm, 58*mm, None], "#4A148C",
    [colors.HexColor("#F3E5F5"), colors.white]))
story.append(SP(4))
story.append(Note(
    "Using app-automotive vs app is the most critical dependency decision. "
    "app-automotive includes AAOS-specific classes (CarAppActivity, car host communication). "
    "Using app in the automotive module will cause crashes at runtime on AAOS hardware."
))

# ═════════════════════════════════════════════════════════════════════════════
#  SUMMARY PAGE
# ═════════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(Paragraph("Complete Architecture Summary", section_title))
story.append(SP(4))
story.append(Code(
    "SmartAAOS — Full Architecture Map\n"
    "\n"
    "┌─────────────────────────────────────────────────────────────────────┐\n"
    "│  ENTRY POINT                                                        │\n"
    "│  CarAppService → createHostValidator → onCreateSession → Session    │\n"
    "│  Session → onCreateScreen → HomeScreen (first visible screen)       │\n"
    "└───────────────────────────────┬─────────────────────────────────────┘\n"
    "                                │\n"
    "┌───────────────────────────────▼─────────────────────────────────────┐\n"
    "│  UI LAYER (Car App Templates)                                       │\n"
    "│  HomeScreen  → ListTemplate (songs + vehicle rows)                  │\n"
    "│  PlayerScreen → PaneTemplate (album art + progress + controls)      │\n"
    "│  DashboardScreen → MessageTemplate (speed, RPM, fuel, gear)         │\n"
    "│  DiagnosticsScreen → MessageTemplate (alerts + engine status)       │\n"
    "│  Navigation: ScreenManager.push() + NavigationCallback pattern      │\n"
    "│  Refresh: invalidate() triggered by LiveData / MediaController CB   │\n"
    "└───────────────────────────────┬─────────────────────────────────────┘\n"
    "                                │\n"
    "┌───────────────────────────────▼─────────────────────────────────────┐\n"
    "│  STATE LAYER (MVVM)                                                 │\n"
    "│  CarViewModelStore → VehicleViewModel                               │\n"
    "│  LiveData: speed, rpm, fuel, gear, isCarMoving, currentAlert        │\n"
    "│  Coroutine: polls AIDL every 1 second on IO thread                  │\n"
    "└──────────────┬────────────────────────────────────┬─────────────────┘\n"
    "               │                                    │\n"
    "┌──────────────▼──────────────┐   ┌─────────────────▼─────────────────┐\n"
    "│  MUSIC ENGINE               │   │  VEHICLE DATA (AIDL IPC)          │\n"
    "│  SmartMusicService          │   │  VehicleDataService (server)       │\n"
    "│   extends MediaBrowserSvc   │   │   implements IVehicleDataService   │\n"
    "│  ExoPlayer — stream audio   │   │  VehicleRepository (client)        │\n"
    "│  MediaSession — control API │   │   binds via ServiceConnection      │\n"
    "│  AudioFocus — gain/loss     │   │  AlertRepository — thresholds      │\n"
    "│  ForegroundService + notif  │   └────────────────────────────────────┘\n"
    "│  Voice: PLAY_FROM_SEARCH    │\n"
    "│  MediaBrowserCompat client  │\n"
    "└─────────────────────────────┘"
))
story.append(SP(10))
story.append(HR())
story.append(SP(4))
story.append(Paragraph(
    "Android Automotive OS (AAOS) &amp; Android Auto — Deep Learning Notes  |  SmartAAOS Project  |  Swapnil Patil  |  2026",
    footer_style
))

# ═════════════════════════════════════════════════════════════════════════════
doc.build(story)
print(f"PDF saved: {OUTPUT}")
