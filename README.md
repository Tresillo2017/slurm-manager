# SLURM Manager

> SSH-based HPC job monitor and controller for Android

[![Android](https://img.shields.io/badge/Platform-Android%2011%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.02-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%20You-Expressive-757575?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![License](https://img.shields.io/github/license/Tresillo2017/slurm-manager)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Tresillo2017/slurm-manager?include_prereleases)](https://github.com/Tresillo2017/slurm-manager/releases)

SLURM Manager connects to one or more HPC clusters over SSH and lets you monitor, filter, and control Slurm jobs directly from your phone or tablet. No VPN client or web portal required — just an SSH key or password.

---

## Screenshots

> _Coming soon_

---

## Features

- **Multi-cluster support** — add as many servers as you need; switch with a single filter chip
- **Live job list** — background polling via WorkManager keeps the job list fresh
- **Job detail** — resources, timeline, runtime, and one-tap cancel / requeue
- **Job submission** — pick a local sbatch script or type one inline, set partition / nodes / CPUs / time limit
- **Alert rules** — per-server threshold alerts for queue wait time, runtime exceeded, node failures, and partition down events
- **Push notifications** — state-change and threshold alerts delivered as Android notifications (with progress indicators for running jobs)
- **Adaptive layout** — two-pane list-detail on tablets and large phones; bottom nav on compact; rail on medium
- **Material You** — full dynamic color (Android 12+) with expressive spring motion
- **SSH auth** — password or private key (Ed25519, RSA, ECDSA); private keys stored in EncryptedSharedPreferences

---

## Architecture

```
app/src/main/java/com/tomasps/slurmmanager/
├── data/
│   ├── credential/      EncryptedSharedPreferences key store
│   ├── local/db/        Room database — Job + Server entities, DAOs
│   ├── notification/    NotificationEngine (state-change + alert notifications)
│   ├── remote/ssh/      SshClient (sshj), SlurmOutputParser (squeue output → domain models)
│   ├── repository/      JobRepositoryImpl, ServerRepositoryImpl
│   └── worker/          PollWorker (WorkManager), AlertEngine, JobDiffer
├── di/                  Hilt modules (AppModule, DataStoreModule, WorkerModule)
├── domain/
│   ├── model/           Job, Server, JobState, AlertRule, AlertType, AuthMethod, …
│   └── repository/      JobRepository, ServerRepository interfaces
└── presentation/
    ├── dashboard/        Job list with server filter chips, FAB, pull-to-refresh
    ├── jobdetail/        Resources, timeline, cancel/requeue actions
    ├── onboarding/       Add-server wizard with connection test
    ├── serverdetail/     Per-server jobs, history, polling/alert settings
    ├── servers/          Server list with swipe-to-delete
    ├── settings/         Global notification + appearance preferences
    ├── submit/           Modal bottom sheet for sbatch job submission
    ├── navigation/       NavigationSuiteScaffold + NavGraph with shared-axis transitions
    └── theme/            SlrumManagerTheme (dynamic color, MotionScheme.expressive())
```

**Stack summary:**

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 Expressive |
| Navigation | Navigation Compose 2.9 with `NavigationSuiteScaffold` |
| State | ViewModel + StateFlow |
| DI | Hilt 2.56 |
| Local DB | Room 2.7 |
| Preferences | Jetpack DataStore |
| Secure storage | androidx-security-crypto (`EncryptedSharedPreferences`) |
| Background work | WorkManager 2.10 |
| SSH | sshj 0.38 + BouncyCastle 1.78 (full, not Android-stripped) |
| Serialization | kotlinx.serialization 1.8 |

---

## Requirements

| | |
|---|---|
| **Android** | 11+ (API 30) |
| **Target SDK** | 36 |
| **Cluster** | Any host running Slurm with an SSH daemon |
| **Auth** | Password **or** private key (Ed25519 / RSA / ECDSA) |

---

## Building

1. Clone the repo:
   ```bash
   git clone https://github.com/Tresillo2017/slurm-manager.git
   cd slurm-manager
   ```

2. Open in Android Studio Meerkat (2025.1) or later.

3. Build and run on a device or emulator (API 30+):
   ```bash
   ./gradlew :app:assembleDebug
   ```

No API keys or local secrets are required — the app connects exclusively to servers you add.

---

## Adding a server

1. On first launch, the onboarding wizard starts automatically.
2. Enter **hostname**, **port** (default 22), and **username**.
3. Choose **Password** or **SSH Key** authentication.
   - For SSH Key: tap *Import Private Key* and select your `.pem` / private key file.
4. Tap **Test Connection** — the app runs a live SSH handshake.
5. Tap **Save Server** on success.

Additional servers can be added at any time from the Servers screen → **+** button.

---

## Polling & alerts

Polling is done via a `WorkManager` periodic task scheduled per server. The interval is configurable per server (1–60 minutes) from the server detail screen.

Alert rules are also per-server and support:

| Alert type | Trigger |
|---|---|
| `QUEUE_WAIT_EXCEEDED` | Job pending longer than threshold |
| `RUNTIME_EXCEEDED` | Job running longer than threshold |
| `JOB_FAILED` | Job transitions to FAILED / CANCELLED |
| `NODE_FAILURE` | Node reports down in sinfo |
| `PARTITION_DOWN` | Partition reports down in sinfo |

---

## Contributing

Pull requests are welcome. For large changes, open an issue first to discuss what you'd like to change.

```bash
# Run unit tests
./gradlew :app:test

# Run instrumented tests (requires a connected device/emulator)
./gradlew :app:connectedAndroidTest
```

---

## License

[MIT](LICENSE) © Tomás Palma
