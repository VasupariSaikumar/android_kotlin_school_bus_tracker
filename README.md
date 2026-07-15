# 🚌 School Bus GPS Tracker

An Android Kotlin app for school bus drivers that tracks GPS location in the background and uploads it to Firebase Firestore — with configurable schedules, stop-based proximity tracking, and dynamic Firebase project support.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔔 **Daily Reminder** | Configurable alarm (default 7 AM, Mon–Sat) that auto-starts GPS tracking |
| 📍 **Background GPS** | Foreground service with persistent notification shares location at a configurable interval (default 5 min) |
| 🚏 **Bus Stop Management** | Save current GPS as a named stop; view and delete stops from the UI |
| 🎯 **Proximity Mode** | When stops exist, location is sent **once per stop per day** when the bus is within 150 m — instead of every N minutes |
| 🔥 **Firebase Integration** | Location & stop data saved to Firestore with anonymous authentication |
| ⚙️ **Dynamic Firebase Config** | Optionally enter custom Firebase credentials (API Key, Project ID, App ID) in Settings to connect to a different project at runtime |
| 🔄 **Boot Persistence** | Alarm and tracking survive device reboots |
| 🎨 **Modern UI** | Dark-themed Jetpack Compose UI with bus-yellow accents, pulsing status indicator, and settings bottom sheet |

---

## 📱 Screenshots

> *Coming soon — run the app to see the dark-themed Compose UI with tracking controls, stop management, and settings.*

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                   MainActivity                       │
│              (Jetpack Compose UI)                     │
│         ┌──────────┴──────────┐                      │
│    MainViewModel        BusTrackerScreen             │
│         │                                            │
│    ┌────┴─────────────┐                              │
│    │  Repositories    │                              │
│    │  • AuthRepository│                              │
│    │  • StopRepository│                              │
│    │  • LocationRepo  │                              │
│    │  • BusRepository │                              │
│    └────┬─────────────┘                              │
│         │                                            │
│    FirebaseManager ──► Firestore (default / custom)  │
└─────────────────────────────────────────────────────┘

┌──────────────────────────────────────┐
│         Background Services          │
│                                      │
│  AlarmScheduler ──► AlarmReceiver    │
│                       │              │
│                  GPSTrackingService   │
│                  (Foreground Service) │
│                       │              │
│              FusedLocationProvider    │
│                                      │
│  BootReceiver ──► reschedule alarm   │
└──────────────────────────────────────┘
```

**Tech Stack:**
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt (Dagger)
- **Backend:** Firebase Firestore + Firebase Auth (anonymous)
- **Location:** Google Play Services FusedLocationProviderClient
- **Scheduling:** AlarmManager (exact alarms)
- **Min SDK:** 28 (Android 9)

---

## 📂 Project Structure

```
android_kotlin_school_bus_tracker/
├── app/
│   ├── build.gradle.kts                    # App-level dependencies
│   ├── google-services.json                # Firebase config (default project)
│   └── src/main/
│       ├── AndroidManifest.xml             # Permissions, services, receivers
│       └── java/com/example/android_kotlin_school_bus_tracker/
│           │
│           ├── SchoolBusTrackerApp.kt      # @HiltAndroidApp Application class
│           ├── MainActivity.kt             # Entry point, permissions, Compose host
│           ├── AppStartupViewModel.kt      # Auth bootstrap on app launch
│           │
│           ├── data/                       # Data layer
│           │   ├── Prefs.kt                # SharedPreferences wrapper (settings)
│           │   ├── FirebaseManager.kt      # Dynamic Firebase project initializer
│           │   ├── AuthRepository.kt       # Anonymous Firebase Auth
│           │   ├── BusRepository.kt        # Bus document CRUD
│           │   ├── StopRepository.kt       # Stop CRUD (Firestore)
│           │   └── LocationRepository.kt   # GPS location uploads (Firestore)
│           │
│           ├── domain/                     # Domain models
│           │   ├── Bus.kt                  # Bus data class
│           │   └── Stop.kt                 # Stop data class
│           │
│           ├── di/                         # Dependency Injection
│           │   └── provideFirebaseAuth.kt  # Hilt AppModule (all providers)
│           │
│           ├── receiver/                   # Broadcast receivers
│           │   ├── AlarmScheduler.kt       # Schedules exact daily alarms
│           │   ├── AlarmReceiver.kt        # Fires at alarm time → starts service
│           │   └── BootReceiver.kt         # Reschedules alarm after reboot
│           │
│           ├── service/                    # Background services
│           │   └── GPSTrackingService.kt   # Foreground service (periodic + proximity)
│           │
│           └── ui/                         # Presentation layer
│               ├── BusTrackerScreen.kt     # Full Compose UI (all screens)
│               ├── MainViewModel.kt        # UI state, tracking, stop management
│               └── theme/
│                   ├── Color.kt
│                   ├── Theme.kt
│                   └── Type.kt
│
├── build.gradle.kts                        # Root build config
├── settings.gradle.kts                     # Project settings
├── gradle/
│   └── libs.versions.toml                  # Version catalog
└── gradle.properties
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Hedgehog or newer
- **JDK 11+**
- A **Firebase project** with:
  - ✅ Anonymous Authentication enabled
  - ✅ Cloud Firestore created (start in test mode)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/VasupariSaikumar/android_kotlin_school_bus_tracker.git
   cd android_kotlin_school_bus_tracker
   ```

2. **Firebase setup**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a project (or use the existing one)
   - Enable **Authentication → Sign-in method → Anonymous**
   - Enable **Firestore Database** → Start in **test mode**
   - Download `google-services.json` and place it in `app/`

3. **Firestore Security Rules** (under Firestore → Rules tab)
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /buses/{busId}/{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

4. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and click ▶ Run.

5. **Grant permissions** on the device:
   - Location → "Precise" → "Allow all the time"
   - Notifications → "Allow"

---

## 🔥 Firestore Data Structure

```
buses/
  └── {busId}/                          # Auto-generated UID (anonymous auth)
        ├── id: "busId"
        ├── createdAt: 1720900000000
        │
        ├── stops/                      # Saved bus stops
        │     └── {stopId}/
        │           ├── id: "uuid"
        │           ├── name: "School Gate"
        │           ├── latitude: 17.385
        │           ├── longitude: 78.486
        │           └── createdAt: 1720900000000
        │
        └── locations/                  # GPS location uploads
              └── {auto-id}/
                    ├── latitude: 17.385
                    ├── longitude: 78.486
                    ├── accuracy: 12.5
                    ├── timestamp: 1720900300000
                    ├── busId: "busId"
                    └── nearStopId: "uuid"   # (only in proximity mode)
```

---

## ⚙️ Configuration

All settings are configurable from the in-app **Settings** bottom sheet:

| Setting | Default | Description |
|---|---|---|
| Notification Time | 07:00 | Daily alarm time to auto-start tracking |
| Active Days | Mon–Sat | Days when the alarm fires |
| Location Frequency | 5 min | GPS upload interval (periodic mode) |
| Proximity Radius | 150 m | Distance threshold for stop detection |
| Firebase API Key | *(blank)* | Custom Firebase project API key |
| Firebase Project ID | *(blank)* | Custom Firebase project ID |
| Firebase App ID | *(blank)* | Custom Firebase app ID |
| Firebase DB URL | *(blank)* | Custom Realtime Database URL |

> Leave the Firebase fields blank to use the default project from `google-services.json`.

---

## 📋 Tracking Modes

### Periodic Mode (default)
When **no stops** are saved, the app uploads the bus's GPS location to Firestore at the configured interval (every 5 minutes by default).

### Stop-Proximity Mode
When **one or more stops** are saved:
- GPS is polled every **30 seconds**
- When the bus is within **150 m** of a saved stop, the location is uploaded **once** for that stop
- The same stop won't trigger again until the next day
- This reduces Firebase writes and gives precise stop-arrival data

---

## 🔒 Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | High-accuracy GPS |
| `ACCESS_BACKGROUND_LOCATION` | Location updates when app is in background |
| `FOREGROUND_SERVICE_LOCATION` | Required for foreground service with location type |
| `POST_NOTIFICATIONS` | Show tracking and reminder notifications (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after device reboot |
| `SCHEDULE_EXACT_ALARM` | Precise daily reminder delivery |
| `INTERNET` | Firebase data sync |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m "Add my feature"`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
