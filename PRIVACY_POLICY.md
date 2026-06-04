# Privacy Policy

**SLURM Manager** — Android app by Tomás Palma  
**Effective date:** 2026-06-04  
**Last updated:** 2026-06-04

---

## 1. Overview

SLURM Manager is an open-source Android application that connects to your own HPC clusters via SSH to monitor and control Slurm jobs. **No data you enter or generate in this app is ever sent to the developer, any third-party server, analytics platform, or advertising network.** All data stays on your device and travels only between your device and the SSH servers you configure.

---

## 2. Data collected and where it lives

| Data | Where it is stored | Leaves your device? |
|---|---|---|
| SSH credentials (password or private key) | `EncryptedSharedPreferences` (AES-256, Android Keystore) | **Never** |
| Server hostnames, ports, usernames | Room database (local, on-device) | **Never** |
| Job data polled from your clusters | Room database (local, on-device) | **Never** |
| Notification and appearance preferences | Jetpack DataStore (local, on-device) | **Never** |

### 2.1 SSH credentials

Passwords and private keys are encrypted at rest using the Android Keystore system via `androidx.security:security-crypto`. They are read only at connection time and are never logged, exported, or transmitted to anyone other than the SSH server you configured.

### 2.2 Job and server data

Job lists, server status, and polling results are fetched directly from your SSH servers and stored locally in a Room (SQLite) database on your device. This data is never uploaded anywhere.

### 2.3 Notifications

Push notifications are delivered entirely on-device using Android's local notification system. No notification content is routed through any external push notification service controlled by the developer.

---

## 3. Data we do NOT collect

- No user accounts or registration
- No analytics or crash reporting SDK (no Firebase, no Sentry, no Mixpanel)
- No advertising identifiers or tracking pixels
- No usage telemetry
- No location data
- No camera or microphone access

---

## 4. Third-party services

The app has **no runtime dependency on any third-party service** controlled by the developer. The only network connections the app makes are SSH connections to servers that you explicitly configure.

Build-time dependencies (open-source libraries: sshj, BouncyCastle, Jetpack, etc.) are listed in the [README](README.md) and are subject to their own licenses. None of these libraries phone home at runtime.

---

## 5. Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | SSH connections to your clusters |
| `POST_NOTIFICATIONS` | Job state-change and alert notifications |
| `RECEIVE_BOOT_COMPLETED` | Restart background polling after device reboot |
| `FOREGROUND_SERVICE` | WorkManager polling tasks |

No permission is used for any purpose beyond what is described above.

---

## 6. Data retention and deletion

All data is stored locally on your device. You can delete all app data at any time via **Android Settings → Apps → SLURM Manager → Storage → Clear data**. Uninstalling the app removes all locally stored data.

---

## 7. Children's privacy

This app is not directed at children under 13 and does not knowingly collect any information from children.

---

## 8. Changes to this policy

If this policy changes materially, the updated version will be committed to the repository with a new **Last updated** date. Continued use of the app after a policy update constitutes acceptance of the new terms.

---

## 9. Contact

Questions or concerns? Open an issue at:  
**https://github.com/Tresillo2017/slurm-manager/issues**
