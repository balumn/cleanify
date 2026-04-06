# Cleanify

**Quiet the noise in your background.** Cleanify helps you **force-stop** selected Android apps on **non-rooted** devices by using the Accessibility Service to walk through the system **App info → Force stop** flow—similar to what you could tap manually, but batched for apps you choose.

| | |
|---|---|
| **License** | [MIT](LICENSE) |
| **Privacy** | [Privacy Policy](docs/PRIVACY.md) |
| **Terms** | [Terms of Use](docs/TERMS.md) |
| **Min SDK** | 26 (Android 8.0) |
| **Package** | `com.balumn.cleanify` |

## Features

- **Material 3 + Jetpack Compose** UI with simple navigation between dashboard and app selection.
- **Dashboard** with sections for apps **running in the background** vs **already stopped**, plus expandable lists.
- **App picker** with **user** and **system** app groupings, **select all** / **deselect all**, and **Save**.
- **Persisted selection** via **DataStore** (survives restarts).
- **Clean Up** runs the full queue of selected apps (when Accessibility is enabled).
- **Toolbar actions:** force-clean **only selected running** apps, or **remove selected** apps from your list (e.g. apps you cannot or should not kill).
- **Onboarding** for enabling the Accessibility Service and a **quick start** checklist.
- **Loading / indexing** states while the app reconciles package state with the system.

## Screenshots

_Add screenshots here after GA if you like._

## Safety and disclaimer

Force-stopping apps can affect notifications, background sync, alarms, or unsaved state. **Do not rely on Cleanify for apps that must keep running** (e.g. critical messaging, accessibility, banking, or device administration) unless you know what you are doing. Read the [Terms of Use](docs/TERMS.md). **This project is maintained by volunteers; use at your own risk.**

## Requirements

- Android **8.0+**
- **Accessibility** enabled for Cleanify when you want automated force-stop.
- On Android **11+**, broad package visibility may require the declared `QUERY_ALL_PACKAGES` usage (see manifest).

## Build (release)

1. **JDK 17**
2. Create a release keystore (once) and configure signing:
   - Copy `keystore.properties.example` to `keystore.properties` in the **project root** (this file is gitignored).
   - Generate a keystore, for example:
     ```bash
     keytool -genkeypair -v -storetype PKCS12 -keystore release.keystore \
       -alias cleanifyRelease -keyalg RSA -keysize 2048 -validity 10000
     ```
   - Fill in `storeFile`, `storePassword`, `keyPassword`, and `keyAlias` in `keystore.properties`.
3. Build:
   ```bash
   ./gradlew :app:assembleRelease
   ```
4. Output APK: `app/build/outputs/apk/release/app-release.apk`

If `keystore.properties` is missing, the release build is signed with the **debug** key (fine for local testing only). For a **public** APK, always create `keystore.properties` and a release keystore as below.

Release builds use **R8** (code shrinking, obfuscation, and **resource shrinking**).

## Install via ADB (replace debug with release)

```bash
adb uninstall com.balumn.cleanify
adb install app/build/outputs/apk/release/app-release.apk
```

If you previously installed a **debug** build, you usually **must uninstall** before installing a **release**-signed APK.

## Contributing

Issues and pull requests are welcome on GitHub ([@balumn](https://github.com/balumn)). By contributing, you agree your contributions are licensed under the same [MIT License](LICENSE) as the project.

## Publishing a GitHub release

Use [docs/GITHUB_RELEASE_v1.0.0.md](docs/GITHUB_RELEASE_v1.0.0.md) as a starting point for release description text; adjust the repo URL after you create it on GitHub.

## Open source

Cleanify is open source under the **MIT License**. You may use, modify, and distribute it broadly; see [LICENSE](LICENSE) for the full text and warranty disclaimer.
