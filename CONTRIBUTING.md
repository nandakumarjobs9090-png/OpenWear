# How to Contribute to OpenWear

Thank you for your interest in contributing to OpenWear! We welcome all contributions, from bug fixes and UI improvements to new watch driver implementations.

---

## 🐛 Reporting Bugs

1. Check the [Issues](../../issues) page first — your bug may already be reported.
2. Open a **New Issue** using the `bug` label.
3. Include:
   - Android version and device model.
   - Watch model and firmware version (if known).
   - Steps to reproduce the issue.
   - **BLE log files** (see Developer Debug Tools in the app) if the issue is connectivity related.

---

## 💡 Proposing Features

Open an issue with the `enhancement` label describing:
- The feature / new watch you want to support.
- Any BLE packet captures or research logs you have already done.

---

## 🔧 Submitting Pull Requests

1. **Fork** the repository.
2. Create a **feature branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes following the code style below.
4. Run the build to verify it compiles:
   ```powershell
   .\gradlew clean assembleDebug
   ```
5. **Submit a Pull Request** against the `main` branch. Include a clear description of the changes.

---

## 🌐 Adding a New Watch Driver

OpenWear uses a modular driver architecture. To add support for a new watch:

1. Create a new file in `app/src/main/kotlin/com/openwear/ble/drivers/` named `<BrandModel>Driver.kt`.
2. Implement the planned `WatchDriver` interface (coming soon).
3. Add the watch's BLE UUIDs to a new companion `object` similar to `DizoUuids` in `BleModels.kt`.
4. Document any reverse-engineered packet structures in the `README.md` under a new section for your watch.
5. Include at least one sample packet CSV log in `docs/packet-logs/` for the community's reference.

---

## ✅ Code Style

- Follow standard Kotlin conventions.
- Use `Log.d(TAG, ...)` for debug logging — use the module-specific `TAG` constant.
- All new screens must be Jetpack Compose `@Composable` functions.
- Add `@HiltViewModel` for all new ViewModels.
- New database entities must be added to `Entities.kt` with a Room migration.
