# 🚗 Smart AAOS — Android Automotive Music Player

A native **Android Automotive OS** music player app built with
Kotlin and Car App Library. Runs directly on the car's
infotainment screen — no phone required.

---

## 📱 Screenshots

> Add emulator screenshots here

---

## ✨ Features

- 🎵 Song list with artist and album info
- ▶️ Real audio playback via ExoPlayer
- ⏸ Play / Pause / Next controls
- 📊 Live progress bar with real-time position
- 🎤 Voice control — "Hey Google play Kesariya"
- 🔊 Background playback via Foreground Service
- 🟢 Now Playing screen with track info
- 🔵 Highlighted currently playing song

---

## 🛠 Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Android Automotive OS | Target platform |
| Car App Library | UI templates |
| MediaBrowserServiceCompat | Music library |
| MediaSession | Playback controls |
| ExoPlayer (Media3) | Audio engine |
| Foreground Service | Background playback |
| MVVM Architecture | Code structure |

---

## 🏗 Architecture
```
Car Screen
    ↓
HomeScreen (ListTemplate)
    ↓ tap song
PlayerScreen (PaneTemplate)
    ↓
SmartMusicService (MediaBrowserService)
    ↓
ExoPlayer
    ↓
Car Speakers 🔊
```

---

## 📁 Project Structure
```
smart-aaos/
├── app/                          # Phone module (future use)
└── automotive/                   # Car module
    └── src/main/java/
        ├── MusicData.kt          # Song data
        ├── NavigationCallback.kt # Voice navigation
        ├── SmartCarAppService.kt # Car entry point
        ├── SmartMusicService.kt  # Music engine
        ├── SmartSession.kt       # Display handler
        └── screens/
            ├── HomeScreen.kt     # Song list UI
            └── PlayerScreen.kt   # Now playing UI
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Bumblebee or newer
- Android Automotive OS Emulator (API 33)
- Android Automotive with Google APIs x86_64 system image

### Setup
1. Clone the repository
```bash
git clone https://github.com/SwapnilCrafts/smart-aaos.git
```

2. Open in Android Studio

3. Sync Gradle

4. Select `automotive` run configuration

5. Select your AAOS emulator

6. Click Run ▶

---

## 🎤 Testing Voice Control
```bash
adb shell am startservice \
  -a android.media.action.MEDIA_PLAY_FROM_SEARCH \
  -n com.swapnil.smart.aaos/.SmartMusicService \
  --es query 'Kesariya'
```

---

## 📋 AAOS Constraints Learned

- Max 2 actions per PaneTemplate
- Max 2 text lines per Row
- Max 6 items in GridTemplate
- No free-form UI — must use Car App Library templates
- ExoPlayer must run on main thread

---

## 🗺 Roadmap

- [x] Song list UI
- [x] Audio playback
- [x] Play/Pause/Next controls
- [x] Progress bar
- [x] Voice control
- [x] Background playback
- [ ] Album art
- [ ] Vehicle speed lock
- [ ] Local device songs
- [ ] Equalizer
- [ ] Android Auto support
- [ ] Play Store submission

---

## 👨‍💻 Developer

**Swapnil Patil**
- GitHub: [@SwapnilCrafts](https://github.com/SwapnilCrafts)

---

## 📄 License
```
MIT License — feel free to use and modify
```