# SAFE Women 🛡️
> Background-Activated Women's Safety Reporting Application for Semi-Luxury and Luxury Bus Passengers in Sri Lanka.

**Author:** U.L.S.A. Silva (Index: 8421)  
**Degree:** BSc (Hons) in Management Information System  
**Institution:** KIU Sri Lanka – Faculty of Computer Science & Engineering  
**Supervisor:** Ms. Thanuja Irugalbandara  
**Module:** COM4901 Final Year Individual Project  

---

## 🌟 Research Overview
**SAFE Women** is a native Android safety application engineered to protect female commuters in inter-provincial public transit. Grounded in the **Dual-Process Theory of Cognitive Overload Under Acute Stress**, the system eliminates the fatal "app-open" usability bottleneck by allowing passengers to trigger emergency alerts silently via physical hardware buttons without unlocking their smartphone or looking at a screen.

Empirically calibrated via a primary survey of **$N = 101$** verified bus commuters and evaluated through **$N = 15$** participant System Usability Scale (SUS) trials achieving a score of **84.25 / 100 (Grade A, "Excellent")**.

---

## 📱 Core Engineering Features
* 🔴 **Dual Hardware Triggers:** 
  * **Primary Vector:** 3.0-second continuous press and hold of the physical Volume Down button with screen locked/off.
  * **Secondary Vector:** Rapid triple-click (3 presses within 1.5 seconds) for instant emergency dispatch.
* 📍 **Automated GPS Pipeline:** High-precision coordinate fix via Google Play Services `FusedLocationProviderClient` with automatic fail-safe fallback to Colombo Fort Transit Hub (`6.9271° N, 79.8612° E`).
* 📡 **Zero-Data Cellular SMS Broadcast:** Simultaneous SMS transmission with clickable Google Maps coordinates to all registered guardians via native `SmsManager` (100% data-independent, runs without mobile internet).
* 🔐 **Security & PIN Access Control:** 4-digit numeric keypad (`AuthScreen.kt`) with SHA-256 cryptographic hashing protecting contact directories and incident logs.
* 📇 **1-Tap Contact Picker:** Direct address book integration (`ContactsContract.CommonDataKinds.Phone`) with multi-guardian priority broadcasting.
* 💬 **In-App Guardian Incident Feed:** Real-time emergency activity log and guardian chat coordination with 1-tap quick incident status chips.
* 📞 **National Emergency Hotlines:** Direct dialers for Sri Lanka Police (`119`) and National Transport Commission (`1933`).

---

## 💻 Tech Stack
* **Platform:** Native Android (Kotlin 2.0+, Java 17)
* **UI Framework:** Jetpack Compose (Material Design 3 Dark Luxury Theme)
* **Background Architecture:** Android `AccessibilityService` (`flagRequestFilterKeyEvents`) + `ForegroundService` + `PARTIAL_WAKE_LOCK`
* **Local Persistence:** Room SQLite Database (Version 2)
* **Target SDK:** Android 15 (API 35) | **Min SDK:** Android 8.0 (API 26)

---

## 🌐 Interactive Smartphone Hardware Simulator
For presentations, live defenses, and web demonstration without an Android device:
* Located in the [`simulator/`](./simulator/) directory.
* Open [`simulator/index.html`](./simulator/index.html) in any modern web browser.
* Features a realistic smartphone frame with **working tactile Side Volume Down button (3s hold)**, Power lock button, live guardian receiving phone, and real-time Android Logcat trace!

---

## 📚 Documentation & Research Artifacts
* 🎓 **Viva Voce Defense & Demo Manual:** [`docs/SAFE_Women_Viva_Defense_Manual.md`](./docs/SAFE_Women_Viva_Defense_Manual.md)
* 📊 **Chapter 5 Evaluation Datasets (.xlsx):** [`docs/evaluation_datasets/`](./docs/evaluation_datasets/)
  * `SAFE_Women_Chapter_5_Experimental_Evaluation_Datasets.xlsx` (Master workbook with live formulas)
  * `Table_5_2_Trigger_Recognition_Latency.xlsx` ($N = 10$ laboratory trials, Mean: 142.0 ms)
  * `Table_5_3_Cellular_SMS_Transmission_Latency.xlsx` (8 carrier route trials across Dialog, Mobitel, Airtel, Hutch)
  * `Table_5_4_GPS_Spatial_Accuracy_Deviation.xlsx` (Ground-truth surveyed landmarks, Mean drift: ±11.8 m)
  * `Table_5_5_System_Usability_Scale_SUS_Scores.xlsx` ($N = 15$ participant ratings, Mean SUS: 84.25)

---

## 🚀 Quick Start / Build
```bash
# Clone repository
git clone git@github.com:AishaniSilva/New_Safe_Women_App.git
cd New_Safe_Women_App

# Build Debug APK using Gradle wrapper
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```
Or open the project directly in **Android Studio** and click **Run** (`Shift + F10`).

---

## 📜 License & Academic Attribution
Developed solely for academic research under module **COM4901 Final Year Individual Project** at **KIU Sri Lanka**. All rights reserved.
