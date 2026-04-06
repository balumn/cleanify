# Cleanify

**Quiet the noise in your background.** Cleanify helps you **force-stop** selected Android apps on **non-rooted** devices by using the Accessibility Service to walk through the system **App info → Force stop** flow—similar to what you could tap manually, but batched for apps you choose.


|             |                                   |
| ----------- | --------------------------------- |
| **License** | [MIT](LICENSE)                    |
| **Privacy** | [Privacy Policy](docs/PRIVACY.md) |
| **Terms**   | [Terms of Use](docs/TERMS.md)     |
| **Min SDK** | 26 (Android 8.0)                  |
| **Package** | `com.balumn.cleanify`             |


## Features

- **Material 3 + Jetpack Compose** UI with simple navigation between dashboard and app selection.
- **Dashboard** with sections for apps **running in the background** vs **already stopped**, plus expandable lists.
- **App picker** with **user** and **system** app groupings, **select all** / **deselect all**, and **Save**.
- **Persisted selection** via **DataStore** (survives restarts).
- **Clean Up** runs the full queue of selected apps (when Accessibility is enabled).
- **Toolbar actions:** force-clean **only selected running** apps, or **remove selected** apps from your list (e.g. apps you cannot or should not kill).
- **Onboarding** for enabling the Accessibility Service and a **quick start** checklist.
- **Loading / indexing** states while the app reconciles package state with the system.
- **No ads, No telemetry, Privacy first** forever

## Safety and disclaimer

Force-stopping apps can affect notifications, background sync, alarms, or unsaved state. **Do not rely on Cleanify for apps that must keep running** (e.g. critical messaging, accessibility, banking, or device administration) unless you know what you are doing. Read the [Terms of Use](docs/TERMS.md). **This project is maintained by volunteers; use at your own risk.**

## Requirements

- Android **8.0+**
- **Accessibility** enabled for Cleanify when you want automated force-stop.
- On Android **11+**, broad package visibility may require the declared `QUERY_ALL_PACKAGES` usage (see manifest).

## Install via ADB

```bash
adb uninstall com.balumn.cleanify
adb install app/build/outputs/apk/release/app-release.apk
```

## Contributing

Issues and pull requests are welcome with open hearts. By contributing, you agree your contributions are licensed under the same [MIT License](LICENSE) as the project.

## Open source

Cleanify is open source under the **MIT License**. You may use, modify, and distribute it broadly; see [LICENSE](LICENSE) for the full text and warranty disclaimer.