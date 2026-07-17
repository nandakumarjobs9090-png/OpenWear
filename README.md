# ⌚ OpenWear

> An open-source, privacy-first companion app for abandoned smartwatches. Reverse-engineered BLE communication — no cloud, no discontinued apps, just your data.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

OpenWear is an open-source companion mobile application that communicates directly with smartwatches over Bluetooth Low Energy (BLE). It breathes new life into abandoned or unsupported smartwatches by bypassing discontinued official cloud services, giving users full ownership of their fitness data.

**Currently supported watch:** DIZO Watch R *(official app discontinued)*

---

## ✨ Features

| Screen | Description |
|--------|-------------|
| 🏠 **Today Dashboard** | Step count rings, battery, live heart rate, SpO2, and sleep summary |
| 🏃 **Fitness** | Activity rings, distance, calories, weekly step bar chart |
| 😴 **Sleep** | Radial arc canvas showing Awake / Light / Deep sleep stages with score |
| ❤️ **Health Vitals** | Live heart rate history graph, SpO2 monitoring, resting/avg/max stats |
| 📡 **Device Manager** | BLE scanning, auto-connect from paired device list, battery indicator |
| 🔬 **Developer Debug Tools** | Real-time BLE packet sniffer with CSV/JSON export for reverse engineering |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **DI** | Dagger Hilt |
| **Database** | Room (local, offline) |
| **Async** | Kotlin Coroutines & Flow |
| **BLE** | Android BluetoothGatt API |
| **Background** | Android Foreground Service |

---

## 🧠 DIZO Watch R — Reverse-Engineered Protocol

These specifications were discovered through active BLE packet analysis using the built-in sniffer.

### BLE Services

| UUID | Type | Purpose |
|------|------|---------|
| `000001ff-3c17-d293-8e48-14fe2e4da212` | Vendor | Primary command channel |
| `0000d0ff-3c17-d293-8e48-14fe2e4da212` | Vendor | Configuration & device info |
| `00006287-3c17-d293-8e48-14fe2e4da212` | Vendor | Watch face / OTA firmware |
| `0000fee7-0000-1000-8000-00805f9b34fb` | Standard FEE7 | Active heartbeat notifications |

### Key Characteristics

| Char UUID | Properties | Purpose |
|-----------|-----------|---------|
| `0000ff02-3c17-...` | Write | Send commands to watch |
| `0000ff03-3c17-...` | Notify | Receive command responses |
| `0000fea1-0000-...` | Notify | Live status (HR, steps, battery) |

### `FEA1` Status Packet Layout (10 bytes, type `0x07`)

```
[0]   [1]   [2]   [3]   [4]   [5]   [6]   [7]     [8]     [9]
 07    HR    00    00   BAT    00    00  STEP_LO  STEP_HI   00
```

| Field | Byte | Format | Example |
|-------|------|--------|---------|
| Packet Type | `[0]` | `0x07` = status frame | `07` |
| Heart Rate | `[1]` | Uint8, bpm | `3D` = 61 bpm |
| Battery | `[4]` | Uint8, percent | `2A` = 42% |
| Steps | `[7-8]` | Uint16 little-endian | `65 09` = 2405 steps |

---

## ⚡ Build Instructions

**No Android Studio needed!** Build entirely with CLI tools.

### Prerequisites

| Tool | Requirement |
|------|------------|
| Java JDK | v22 (`JAVA_HOME` set) |
| Android SDK CLI Tools | (`ANDROID_HOME` set) |
| ADB | On system `PATH` |

### Quick Start

```bash
# 1. Clone
git clone https://github.com/yourusername/OpenWear.git
cd OpenWear

# 2. Verify your environment
.\scripts\verify_env.ps1

# 3. Build
.\gradlew clean assembleDebug

# 4. Install on connected phone
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🕵️ Using the Built-in BLE Packet Sniffer

OpenWear includes a **Developer Debug Tools** screen to capture, view, and export live BLE packets from your watch — this is how the protocol was reverse-engineered.

1. Connect to your watch via the **Device** tab.
2. Scroll to the bottom and tap **Open Logs**.
3. Watch real-time hex packets stream in.
4. Tap **Export CSV** to save the log.
5. Pull logs to your PC via ADB:

```powershell
adb pull /sdcard/Android/data/com.openwear/files/OpenWear/logs/ ./logs
```

---

## 🤝 Contributing

We welcome contributions! Whether it's a bug fix, a new screen, or a driver for a new watch — all are welcome.

### Adding a New Watch Driver

1. Research the watch's BLE services using **nRF Connect** (safe, no modifications needed).
2. Use OpenWear's built-in sniffer to capture raw packets while performing actions on the watch.
3. Decode the protocol following the structure in `BleManager.kt`.
4. Open an issue using the [New Watch Driver template](.github/ISSUE_TEMPLATE/new_watch_driver.md).

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide.

### Planned Watch Support
- [ ] Amazfit Bip / GTR series
- [ ] Noise ColorFit series
- [ ] boAt Xtend series
- [ ] Fire-Boltt series
- [ ] Xiaomi Mi Band series

---

## 🗺️ Roadmap

- [x] BLE connection stack & service discovery
- [x] Auto-connect from OS-paired device list
- [x] Correct vendor base UUIDs (`-3c17-d293-8e48-14fe2e4da212`)
- [x] Decode live status packets (HR, battery, steps)
- [x] Real-time BLE packet sniffer with CSV/JSON export
- [ ] Historical data sync (sleep stages, past workouts) via `FF02` commands
- [ ] Watch face upload support
- [ ] Driver abstraction interface for community watch support
- [ ] Web dashboard for data visualization
- [ ] AI-powered weekly health insights

---

## ⚖️ License

Licensed under the **Apache 2.0 License** — see [LICENSE](LICENSE) for details.

---

## ⚠️ Disclaimer

This project is an independent, community-driven effort. It is **not affiliated with, endorsed by, or connected to** DIZO, Realme, or any watch manufacturer. All reverse engineering was performed legally through passive BLE observation without modifying any firmware. This software is provided as-is with no warranty.
