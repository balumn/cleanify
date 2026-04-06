# Cleanify v1.0.0 — General availability / open source

Cleanify is **open source** under the [MIT License](../LICENSE). Use is subject to the [Terms of Use](TERMS.md) and [Privacy Policy](PRIVACY.md).

## Highlights

- **Batch force-stop** for apps you select, on **non-rooted** devices, via the **Accessibility Service** (automates the system App info → Force stop flow).
- **No network** usage for core functionality in this repository build.
- **Local-only** preferences (DataStore) for your app list.

## Features

- Material 3 + Jetpack Compose UI
- Dashboard: **running vs stopped** sections, expandable lists
- App selection: user / system groupings, select all / deselect all, Save
- **Clean Up** — full queue
- Toolbar: **force-clean selected running** only, **remove selected** from list
- Accessibility onboarding + quick start
- Loading / indexing while syncing with system package state

## Install

Download **app-release.apk** from this release. If upgrading from a build signed with a different key, uninstall the old app first (`adb uninstall com.balumn.cleanify`).

## Source

Repository: `https://github.com/balumn/cleanify` (create the repo and push if not live yet.)

## Credits

Maintainer: [@balumn](https://github.com/balumn)
