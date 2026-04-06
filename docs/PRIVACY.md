# Privacy Policy — Cleanify

**Last updated:** April 6, 2026

**Maintainer:** [balumn](https://github.com/balumn) (open-source project; repository URL may vary — see the project README).

## Summary

Cleanify is designed to work **entirely on your device**. It does not require an account and, in the open-source version described here, does not include third-party analytics SDKs or advertising networks.

## What data is processed

- **Installed applications:** The app reads information about installed packages on your device so it can list apps and help you choose which ones may be force-stopped. On some Android versions this uses the `QUERY_ALL_PACKAGES` capability.
- **Your choices:** App selections and related preferences are stored **locally** on your device (via Android DataStore / preferences). This data is not transmitted to us by the app itself.
- **Accessibility:** To automate opening system app settings and confirming “Force stop” on **non-rooted** devices, Cleanify can use the Android **Accessibility Service** when you explicitly enable it in system settings. That service operates on your device to perform actions you initiate.

## Network

The application as published in this repository does not declare general internet access for its own functionality. We do not operate servers that receive your app lists or selections from Cleanify through the app code included here.

## Third parties

We do not sell your personal information. This policy describes the open-source app behavior; if you install a build from someone else, that distributor is responsible for their own practices.

## Children

Cleanify is not directed at children. The app performs administrative actions on your device and should be used only with appropriate understanding of those risks.

## Changes

We may update this policy when the app or repository changes materially. The “Last updated” date at the top will change when we do.

## Contact

Questions or requests related to this policy are best opened as **GitHub Issues** on the project repository (see README for the canonical link).
