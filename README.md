# Yupi - User Guide & Documentation

Yupi is an Android application that estimates the number of words you speak every day, complete with speech pattern metrics. All processing happens locally on your device. Audio is never recorded to a file and is never sent anywhere.

---

## Table of Contents
- [Key Features](#key-features)
- [How It Works](#how-it-works)
- [Device Requirements](#device-requirements)
- [App Permissions](#app-permissions)
- [User Guide](#user-guide)
  - [1. Voice Registration](#1-voice-registration)
  - [2. Starting and Stopping Tracking](#2-starting-and-stopping-tracking)
  - [3. Understanding the Dashboard](#3-understanding-the-dashboard)
  - [4. Preventing Background Termination](#4-preventing-background-termination)
- [Privacy and Data](#privacy-and-data)
- [Limitations](#limitations)
- [Troubleshooting](#troubleshooting)
- [Disclaimer](#disclaimer)

---

## Key Features
- **Daily Word Count**: Specifically detects the owner's voice. Other people's voices, TV sounds, and background music are ignored.
- **Voice Registration**: Voice profile setup is performed directly on your device.
- **Speech Pattern Metrics**: Measures your speaking duration, listener speaking duration, speaker turns, dialog vs. monologue duration, and interactivity score.
- **Summary Chart**: Displays a donut chart comparing your speaking duration against others.
- **Runs in the Background**: Remains active as a foreground service with a persistent notification while tracking is active.
- **Local Storage**: All data stays on your phone without requiring internet access or permissions.

---

## How It Works
1. The microphone detects audio using a Voice Activity Detection system.
2. Silence and background noise are discarded.
3. Speech is collected until a silence of approximately 0.6 seconds occurs.
4. The system verifies whether the voice matches your stored voice profile.
5. If it is not your voice, the speech is logged as the listener's speaking duration.
6. If verified as your voice, the system counts syllables to estimate the word count and saves it to the local database.

---

## Device Requirements
- **Operating System**: Android supported by your device configuration.
- **Microphone**: Must be functional.
- **Memory**: 4 GB RAM or more recommended for smooth processing.
- **Storage**: Sufficient space for the app and internal data models.

---

## App Permissions

| Permission | Purpose | Status |
| --- | --- | --- |
| **Microphone** | Listen to audio for voice registration and tracking | Required |
| **Foreground Service** | Run tracking in the background when the app is closed | Required |
| **Foreground Service Microphone** | Access the microphone in the background on Android 14+ | Required |
| **Notifications** | Display the active status notification on Android 13+ | Recommended |

The app does not request permissions for internet, contacts, location, camera, or external storage. If you deny microphone permission, tracking cannot function. You can enable it again in **Settings > Apps > Yupi > Permissions > Microphone**.

---

## User Guide

### 1. Voice Registration
- Open the app and grant Microphone and Notification permissions when prompted.
- Tap **Register Voice** in the top-right corner.
- Tap **Start Recording** and follow the on-screen instructions. Speak continuously during recording without pausing at the start.
- Wait a few seconds for processing to complete until the status updates to successfully saved.

**Registration Tips**:
- Speak at your normal volume and distance from your phone.
- Perform registration in a quiet room.
- If tracking frequently misses your voice, re-register your profile.

### 2. Starting and Stopping Tracking
- **Starting**: On the main screen, tap the large center button. The **Yupi Active** notification will appear. You can turn off the screen or switch to other apps; tracking will continue.
- **Stopping**: Tap the large button again. The notification will disappear and tracking will stop.

### 3. Understanding the Dashboard
- **Voice Profile**: Displays the voice registration status.
- **Today**: Total estimated words spoken today (resets automatically at midnight).
- **Speaking Duration**: Comparison of your speaking time versus the listener's time, accompanied by a donut chart.
- **Turns**: Frequency of speaker switches in conversation.
- **Interactivity**: Average speaker turns per minute.
- **Dialog vs. Monologue**: Ratio of two-way conversation time to single-speaker monologue time.

### 4. Preventing Background Termination
Battery optimization settings on devices like Tecno, Xiaomi, Oppo, Vivo, or Samsung often terminate background apps. To keep Yupi running:
- Go to **Settings > Apps > Yupi > Battery**.
- Select **Unrestricted**.
- Avoid force-closing the app from the task manager.

---

## Privacy and Data
- **No Audio Recording**: Audio is never saved to a file. It only exists in temporary memory for a maximum of 10 seconds per utterance before being discarded.
- **Stored Data**: Only your daily word count and a numeric representation of your voice profile are saved in the app's private storage.
- **No External Connectivity**: No data is transmitted to external servers because the app has no internet permissions.
- **Deleting Data**: You can clear all history and your voice profile via **Settings > Apps > Yupi > Storage > Clear Data**.
- **Ethical Considerations**: An active microphone may catch surrounding conversations. Inform others when using the app in shared spaces and adhere to local privacy regulations.

---

## Limitations
- The word count is an estimation derived from syllable peaks. Speech speed, language, and audio clarity affect accuracy.
- Short utterances under 1.5 seconds are ignored by the system.
- Low volume, far distance, or noisy environments can reduce verification accuracy.
- Similar voices (such as close family members or voice recordings) may trigger false verifications. This app is not designed for security or identity authentication.
- Continuous microphone operation and background processing will impact battery consumption.

---

## Troubleshooting

| Issue | Possible Cause and Solution |
| --- | --- |
| **Tracking button disabled** | Voice profile not registered. Open the **Register Voice** menu. |
| **Microphone permission prompt appears** | Enable microphone permission in your device's app settings. |
| **Notification does not appear** | Grant notification permission in Android system settings. |
| **Word count does not increase** | Ensure your voice is registered. Try re-registering in a quieter room. |
| **Other people's words are counted** | Re-register your voice profile with a cleaner recording free from background noise. |
| **Tracking stops automatically** | Battery optimization is active. Set battery settings for Yupi to **Unrestricted**. |
| **App is slow when starting tracking** | This is normal; the app takes about 2 seconds to load the internal models. |

---

## Disclaimer
This application is provided "as is" without guarantees of absolute accuracy. Estimated word counts and speech pattern metrics are indicative only and are not intended for medical, psychological, or forensic purposes.
