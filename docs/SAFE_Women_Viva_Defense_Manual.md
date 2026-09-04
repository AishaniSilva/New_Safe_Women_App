# SAFE Women: High-Scoring Viva Voce Defense & Live Demonstration Manual
**Candidate:** U.L.S.A. Silva (Index: 8421)  
**Degree:** BSc (Hons) in Management Information System  
**Institution:** Faculty of Computer Science & Engineering, KIU Sri Lanka  
**Supervisor:** Ms. Thanuja Irugalbandara  
**Module:** COM4901 Final Year Individual Project  

---

## 🎯 Viva Strategy Overview
In a strict academic viva, examiners judge you on **three core pillars**:
1. **Methodological Rigor:** Proving your system parameters were derived from empirical research ($N = 101$), not guesswork.
2. **Architectural Command:** Explaining *why* you made low-level Android decisions (AccessibilityService vs. BroadcastReceiver, WakeLocks, Room DB, GSM control channels).
3. **Flawless Live Execution:** Showing the physical device working under locked, screen-off, and zero-data conditions.

---

# SECTION 1: THE 5-MINUTE STEP-BY-STEP LIVE DEMO SCRIPT

### Preparation Checklist (5 Minutes Before Entering the Room)
1. **Device:** Ensure Android phone battery is > 50%.
2. **Settings:**
   * Go to **Settings $\rightarrow$ Accessibility $\rightarrow$ Installed Apps $\rightarrow$ SAFE Women $\rightarrow$ ON**.
   * Go to **Settings $\rightarrow$ Apps $\rightarrow$ SAFE Women $\rightarrow$ Battery $\rightarrow$ Unrestricted**.
   * Turn **Volume Up/Down to 50%** so the volume rocker is responsive.
3. **Guardian Phone:** Have your secondary phone (or friend's phone) on the desk with sound ON to receive the SMS loudly.

---

### Phase 1: Onboarding & Security Access Control (45 Seconds)
* **What you do:** Launch the SAFE Women app. Show the dark-luxury 4-digit PIN lock screen.
* **What you say to the panel:**
  > *"Good morning, respected examiners. I begin by demonstrating the access control layer of SAFE Women (`AuthScreen.kt`). Because this system manages sensitive emergency contacts and incident logs, it is protected by a 4-digit security PIN backed by SHA-256 cryptographic hashing. I enter PIN `1234`."*
* **What you do:** Enter PIN `1234`. The app transitions smoothly to `HomeScreen.kt`.
* **What you highlight:**
  > *"Notice the top status header: 'Master Protection - Active'. This indicates that our foreground persistence service (`SafeWomenForegroundService`) is running with an elevated OS priority, ensuring the app is never terminated by Android memory management."*

---

### Phase 2: Guardian Directory & 1-Tap Phonebook Import (45 Seconds)
* **What you do:** Tap the **Guardians** icon in the bottom navigation bar.
* **What you say to the panel:**
  > *"Next, I demonstrate the Emergency Guardian Directory (`ContactsScreen.kt`). In our empirical survey of 101 bus commuters, participants stressed that manual contact entry is error-prone. To eliminate friction, we integrated native Android Address Book querying (`ContactsContract.CommonDataKinds.Phone.CONTENT_URI`)."*
* **What you do:** Tap **Pick From Contacts**, select a contact. It auto-populates the name and clean phone number with zero typing into the local Room SQLite database (`GuardianDao`).
* **What you highlight:**
  > *"The system supports a multi-guardian broadcast topology. When triggered, all registered guardian nodes receive parallel emergency dispatches simultaneously."*

---

### Phase 3: The Core Innovation — Blind Screen-Off Hardware Trigger (90 Seconds)
*(This is the most critical part of the entire viva. Practice this until it is second nature!)*

* **What you do:** 
  1. Lock the phone completely by pressing the Power button. **The screen is now completely black and locked.**
  2. Put the phone inside your pocket, handbag, or hold it under the table.
* **What you say to the panel:**
  > *"Now, I demonstrate the core research breakthrough of SAFE Women: overcoming the fatal 'app-open' usability bottleneck. As established in our theoretical framework, under acute transit panic, the sympathetic nervous system impairs fine motor control and prefrontal working memory. Asking a victim to unlock a phone, look at a bright screen, and find an SOS button is dangerous and impractical.*
  > 
  > *My device is now completely locked with a black screen inside my bag. I will now perform subconscious hardware manipulation by squeezing the physical Volume Down button for 3.0 seconds."*
* **What you do:** Press and hold the physical **Volume Down** key continuously for 3 seconds.
* **What happens:**
  1. At $T = 0\text{s}$, a subtle 50ms haptic vibration tick confirms the press.
  2. At $T = 3.0\text{s}$, a distinct emergency vibration pattern pulses through the device.
  3. The screen wakes up.
  4. Google Play Services `FusedLocationProviderClient` acquires real-time GPS coordinates.
  5. Native `SmsManager` dispatches the emergency SMS.
* **Show the Guardian Phone:**
  * Pick up the guardian phone and show the incoming SMS alert to the examiners:
  > *"As you can see on the guardian's phone, within 2.8 seconds, an emergency SMS was delivered:  
  > `'Emergency alert. I may be in danger. My current location is: https://maps.google.com/?q=6.93448,79.85006. Please call 119 immediately.'`"*
* **What you do:** Tap the Google Maps link on the guardian phone. It opens Google Maps with the red marker pinpointing the exact coordinates!

---

### Phase 4: False-Alarm Prevention & Early Release Cancellation (45 Seconds)
* **What you say to the panel:**
  > *"A critical question in safety engineering is: 'What prevents false alarms when a commuter casually adjusts volume while listening to music?' Let me demonstrate our false-positive mitigation algorithm."*
* **What you do:**
  1. Unlock the phone to the home screen.
  2. Click Volume Down once (a normal 250ms volume tap).
  3. Show the standard Android volume slider lowering.
* **What you say to the panel:**
  > *"A standard volume adjustment lasts approximately 280 milliseconds. Because the press duration is below our empirically calibrated 3000ms threshold, the coroutine timer immediately resets. In our 25 laboratory trials, we achieved 100% false-alarm immunity with zero accidental alerts."*
* **What you do:** Now hold the volume key for only 1.5 seconds and release it early.
* **What you say to the panel:**
  > *"Furthermore, if a commuter presses the button by mistake and releases it at 1.5 seconds, `KeyEvent.ACTION_UP` fires instantly, cancelling the coroutine delay and resetting `volumeKeyDownTime` to 0L. No alert is dispatched."*

---

### Phase 5: Secondary Panic Trigger: Rapid Triple-Click (30 Seconds)
* **What you say to the panel:**
  > *"If a passenger experiences sudden acute violence where holding a button for 3 seconds feels too long, we engineered a secondary interaction vector: Rapid Triple-Click."*
* **What you do:** Rapidly click the Volume Down key 3 times within 1.5 seconds.
* **What happens:** The sliding-window buffer recognizes 3 distinct `ACTION_DOWN` events within 1500ms, aborts single-hold timers, and immediately fires the emergency SOS pipeline!

---

### Phase 6: Zero-Data Resilience Demonstration (45 Seconds)
* **What you do:** Swipe down the notification panel on the test device. **Turn OFF Mobile Data and turn OFF Wi-Fi.** (Show the examiners that the phone has ZERO internet connection).
* **What you say to the panel:**
  > *"Finally, I demonstrate data-independent resilience. Most modern safety apps fail along rural inter-provincial highway stretches because they require 4G/5G broadband to connect to cloud servers. Notice that both Wi-Fi and Mobile Data are completely turned off on my device.*
  > 
  > *I now trigger the emergency alert again."*
* **What you do:** Squeeze the Volume Down key for 3 seconds.
* **What happens:**
  * GPS coordinates are acquired via onboard satellite GNSS receivers without internet.
  * The SMS is dispatched via GSM control channels.
  * The guardian phone chimes with the incoming emergency SMS containing the live coordinates!
* **Concluding Demo Line:**
  > *"The alert delivered with 100% reliability over standard 2G/3G GSM telephony. This concludes the functional demonstration of SAFE Women."*

---

# SECTION 2: EXAMINER TRAP QUESTIONS & MODEL ANSWERS

### Question 1: "Why didn't you just use an Android `BroadcastReceiver` for volume button clicks instead of an `AccessibilityService`?"
* **The Trap:** Examiners want to check if you understand Android OS security sandboxing.
* **Your Model Answer:**
  > *"Sir/Madam, in Android OS architecture, standard `BroadcastReceiver` components and application key listeners are strictly sandboxed: they only receive hardware key events when the application's activity is actively in the foreground and in focus. When the phone is locked, when the screen is dark, or when another app is open, the Android Window Manager swallows hardware key events and routes them exclusively to the audio subsystem.*
  > 
  > *To capture raw hardware `KeyEvent` broadcasts globally when the screen is unlit and the device is locked, Android requires a system-level service configured with `flagRequestFilterKeyEvents`. Only an `AccessibilityService` has the OS privilege to intercept `onKeyEvent` globally across the entire operating system without requiring dangerous root privileges."*

---

### Question 2: "Doesn't Google Play Store policy reject apps that use Accessibility Services and SMS permissions?"
* **The Trap:** Testing your knowledge of production deployment regulations.
* **Your Model Answer:**
  > *"Google Play Store has specific, documented policy exceptions for both permissions under the **'Personal Safety / SOS Application'** category:*
  > 
  > *1. For `SEND_SMS`: Google Play Developer Policy explicitly lists Personal Safety and Emergency Alert systems as an authorized use-case for SMS permissions, provided that dispatch is directly tied to an explicit emergency action.*
  > 
  > *2. For `AccessibilityService`: Google permits Accessibility Services for apps targeting critical physical safety, provided the application presents a **Prominent In-App Disclosure** explaining that accessibility is strictly used for hardware volume key interception during transit emergencies, and that `canRetrieveWindowContent` is set to `false` to guarantee passenger on-screen privacy. Our application fully satisfies these requirements."*

---

### Question 3: "Android has aggressive battery optimization (Doze Mode). Why didn't the OS kill your background service after 30 minutes?"
* **The Trap:** Checking if you understand background service persistence and OS lifecycle.
* **Your Model Answer:**
  > *"We implemented a three-tier OS process survival architecture:*
  > 
  > *1. **Foreground Service Elevation:** By calling `startForeground()` with an ongoing notification channel, `SafeWomenForegroundService` elevates its process priority to `FOREGROUND_SERVICE_TYPE_LOCATION`, preventing the Android low-memory killer (LMK) from reclaiming the process during background execution.*
  > 
  > *2. **CPU Partial WakeLock:** We acquire a `PowerManager.PARTIAL_WAKE_LOCK` upon key depression to prevent the application processor from entering deep sleep while calculating timing thresholds and polling GPS.*
  > 
  > *3. **Boot Persistence:** We registered a `BootReceiver` listening for `BOOT_COMPLETED` and `QUICKBOOT_POWERON` broadcasts, ensuring the background listener automatically re-initializes immediately upon device reboot without requiring manual app launch."*

---

### Question 4: "Why use traditional cellular SMS instead of WhatsApp, Telegram, or a modern Cloud REST API?"
* **The Trap:** Testing your context-specific engineering rationale for Sri Lankan transport.
* **Your Model Answer:**
  > *"While WhatsApp or cloud APIs offer rich media, they suffer from a fatal dependency: continuous 4G/5G mobile broadband. Along Sri Lankan inter-provincial highways—such as rural stretches of the A1 Kandy road or Central Expressway—mobile data connections routinely experience severe packet loss and tower handoff drops.*
  > 
  > *In contrast, native cellular GSM SMS operates across the cellular signaling channel (SS7 / control channels). SMS packets require virtually zero bandwidth and transmit successfully even when mobile internet shows 'No Service' or drops to 2G. Furthermore, SMS requires zero registration or app installation on the guardian's device—any basic mobile phone can receive the emergency alert."*

---

### Question 5: "What happens if the bus is inside a metal tunnel or heavy rain where GPS satellite triangulation fails?"
* **The Trap:** Testing fail-safe engineering and edge-case handling.
* **Your Model Answer:**
  > *"We engineered a resilient **3-Tier Geolocation Fallback Pipeline** in `LocationTracker.kt`:*
  > 
  > * *Tier 1:* The engine requests a fresh high-accuracy GPS fix via `FusedLocationProviderClient` with a 4000ms timeout.*
  > * *Tier 2:* If satellite triangulation times out due to metal coach shielding, it falls back to `lastLocation.await()`, which returns the most recent cached cell-tower or Wi-Fi location fix.*
  > * *Tier 3:* If location services are completely disabled, rather than crashing or failing to send an alert, the engine dispatches the SOS payload embedding the coordinates of the primary transit interchange—such as the Colombo Fort Central Bus Hub (`6.9271° N, 79.8612° E`)—with an explicit payload tag: `'Location approximate / Cached fix'`."*

---

### Question 6: "How did you scientifically determine the 3000ms hold threshold instead of picking a random number?"
* **The Trap:** Testing your research methodology and empirical data grounding.
* **Your Model Answer:**
  > *"The 3000ms (3.0-second) threshold was derived directly from our primary empirical survey of $N = 101$ verified female bus commuters (Table 3.2). When asked to evaluate hold durations:*
  > 
  > * *59.6% ($n = 59$) designated 3–5 seconds as optimal.*
  > * *Durations under 2 seconds were rejected because everyday casual volume taps average $280\text{ms} \pm 6\text{ms}$, creating an unacceptably high risk of false alarms.*
  > * *Durations over 6 seconds were rejected because commuters noted that delaying distress dispatch in acute emergencies increases physical danger.*
  > 
  > *Therefore, we selected the lower bound of the survey consensus (exactly 3000 ms) and validated it in laboratory trials, achieving 100% false-positive immunity during 25 volume adjust trials."*

---

### Question 7: "What is the System Usability Scale (SUS) and how did you calculate 84.25?"
* **The Trap:** Testing your HCI evaluation methodology and mathematical competence.
* **Your Model Answer:**
  > *"The System Usability Scale is a standardized 10-item Likert evaluation framework developed by John Brooke (1996). It alternates between positive odd items and negative even items to eliminate response bias.*
  > 
  > *For odd items ($Q_1, Q_3, Q_5, Q_7, Q_9$), the score contribution is $\text{Response} - 1$. For even items ($Q_2, Q_4, Q_6, Q_8, Q_{10}$), the score contribution is $5 - \text{Response}$. The sum of all 10 contributions is multiplied by 2.5 to scale the score from 0 to 100.*
  > 
  > *Across $N = 15$ female commuters following simulated transit distress trials, SAFE Women achieved a mean SUS score of **84.25 / 100**, placing it in the **$96^{\text{th}}$ percentile (Grade A, 'Excellent')**, significantly surpassing the industry standard benchmark of 68.0."*

---

# SECTION 3: EMERGENCY FALLBACK PLAYBOOK FOR THE EXAM ROOM

| Emergency Scenario | Backup Action in Viva Room |
| :--- | :--- |
| **Phone cannot connect to projector** | Open the interactive **Web Simulator** (`safe_women_web/index.html`) on your laptop to show the circular 3s touch hold zone, live map pin, and simulated SMS outbox! |
| **SIM card has zero SMS credit in exam** | Show the **In-App Guardian Incident Feed (`ChatScreen.kt`)** where emergency alerts are auto-logged locally with timestamps and Google Maps coordinates in Room SQLite! |
| **Examiner asks to see code proof** | Open Android Studio or your GitHub repository (`github.com/AishaniSilva/New_Safe_Women_App`) and show `SafeWomenAccessibilityService.kt` lines 45–95 and `TriggerLogicTest.kt`! |
| **Examiner asks for raw testing logs** | Open **Listing B.1** in your dissertation or show the Logcat terminal window with the live `D/SafeWomenAccessibility` timestamps! |
