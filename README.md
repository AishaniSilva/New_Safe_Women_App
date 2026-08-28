# SAFE Women 🛡️
> Background-Activated Women's Safety Reporting Application for Bus Passengers in Sri Lanka.

**Author:** U.L.S.A. Silva (Index: 8421)  
**Degree:** BSc (Hons) in Management Information System  
**Institution:** KIU – Faculty of Computer Science & Engineering  
**Supervisor:** Ms. Thanuja Irugalbandara  

---

## Overview
**SAFE Women** is a native Android safety application engineered to protect female commuters in public transit. Grounded in the **Dual-Process Theory of Cognitive Overload**, the system eliminates the "app-open" usability bottleneck by allowing passengers to trigger emergency alerts silently via hardware buttons without unlocking their smartphone or opening the UI.

---

## Core Features
* 🔴 **Dual Hardware Triggers:** 
  * **Method 1:** 3.0-second continuous press and hold of the physical Volume Down button.
  * **Method 2:** Rapid triple-click (3 presses within 1.5 seconds) for quick panic dispatch.
* 📍 **Automated GPS Capture:** High-precision location fix via Google Play Services `FusedLocationProviderClient` with automatic fallback to Colombo Transit Hub (`6.9271° N, 79.8612° E`).
* 📡 **Zero-Data Cellular SMS Broadcast:** Simultaneous SMS transmission with clickable Google Maps coordinates to all registered guardians via native `SmsManager` (operates without mobile internet/data).
* 🔐 **Security & PIN Access Control:** 4-digit numeric keypad with SHA-256 password fallback protecting contact directories and settings.
* 📇 **1-Tap Contact Picker:** Direct address book integration (`ContactsContract.CommonDataKinds.Phone`) with multi-guardian priority management.
* 💬 **In-App Guardian Incident Feed:** Real-time emergency activity log and guardian chat coordination.
* 📞 **National Hotlines:** One-tap direct dialers for Police (`119`) and National Transport Commission (`1933`).

---

## Tech Stack
* **Language:** Kotlin 2.0+ (Java 17)
* **UI Toolkit:** Jetpack Compose (Material 3 Dark Luxury Theme)
* **Background Architecture:** Android `AccessibilityService` (`flagRequestFilterKeyEvents`) + `ForegroundService` + `WakeLock`
* **Local Persistence:** Room SQLite Database (Version 2)
* **Target SDK:** Android 15 (API 35) | **Min SDK:** Android 8.0 (API 26)

---

## Quick Start / Build
```bash
# Clone repository
git clone https://github.com/aishanisilva/safe-women-android.git
cd safe-women-android

# Build Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

Or open the project directly in **Android Studio** and click **Run** (`Shift + F10`).

---

## License & Academic Disclaimer
Developed solely for academic research under module **COM4901 Final Year Individual Project** at **KIU Sri Lanka**. All rights reserved.
