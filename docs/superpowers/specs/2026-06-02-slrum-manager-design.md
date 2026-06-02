# SLURM Manager Android App — Design Spec

**Date:** 2026-06-02  
**Status:** Approved  

---

## Overview

A native Android app (Kotlin + Jetpack Compose + Material 3) for managing and monitoring SLURM HPC jobs across multiple clusters. Connects via SSH directly to login nodes. Supports full job control, configurable background polling, push notifications, and Android Live Updates for actively running jobs.

---

## Architecture

### Layer Structure (Clean Architecture)

```
presentation/  — Compose screens, ViewModels, UI state
domain/        — use cases, domain models, repository interfaces
data/          — Room DB, SSH client, WorkManager workers, repository implementations
```

### Key Libraries

| Library | Purpose |
|---------|---------|
| SSHJ | SSH client (preferred over JSch for active maintenance) |
| Room | Local job/server snapshot database |
| WorkManager | Background polling (survives reboots, battery-friendly) |
| Hilt | Dependency injection |
| Compose Navigation | Screen navigation |
| DataStore | Preferences (polling intervals, notification settings) |
| EncryptedSharedPreferences | Credential storage |
| Android Keystore | SSH private key protection |
| Android Live Updates API (API 36+) | Real-time job progress in notification shade |

---

## Navigation Structure

```
Onboarding (first launch only)
  └─ Welcome → Add Server → Test Connection

Bottom Navigation Bar (3 tabs):
├─ Dashboard (unified all-server view)
│     └─ Job detail (bottom sheet → full screen)
├─ Servers
│     ├─ Add server (FAB → onboarding flow reused)
│     ├─ Server detail
│     │     ├─ Jobs tab
│     │     ├─ History tab
│     │     └─ Settings tab (polling interval, alert rules)
│     └─ (per-server settings accessible from detail)
└─ Settings (global: notification prefs, theme, app info)
```

---

## Data Models

### Server

```kotlin
data class Server(
    val id: UUID,
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val authMethod: AuthMethod,        // PASSWORD | SSH_KEY
    val pollingIntervalMinutes: Int = 5,
    val alertRules: List<AlertRule>
)
```

### Job

```kotlin
data class Job(
    val jobId: String,
    val serverId: UUID,
    val name: String,
    val state: JobState,               // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT, NODE_FAIL, ...
    val partition: String,
    val nodes: Int,
    val cpus: Int,
    val memoryMb: Long,
    val submitTime: Instant,
    val startTime: Instant?,
    val endTime: Instant?,
    val workDir: String,
    val exitCode: Int?,
    val nodelist: String,
    val priority: Int,
    val queuePosition: Int?
)
```

### AlertRule

```kotlin
data class AlertRule(
    val type: AlertType,               // QUEUE_WAIT_EXCEEDED | RUNTIME_EXCEEDED | NODE_FAILURE | PARTITION_DOWN
    val thresholdMinutes: Int?
)
```

### Credential Storage

- **Passwords:** `EncryptedSharedPreferences` backed by Android Keystore
- **SSH private keys:** Raw key material encrypted under a Keystore-protected symmetric key; public key stored in plaintext for display

---

## SSH Layer

### Commands Per Poll

1. `squeue --user=$USER --noheader --format="%i|%j|%T|%P|%D|%C|%m|%V|%S|%e|%Z|%p|%Q"` — running/pending jobs
2. `sacct --user=$USER --starttime=now-7days --noheader --format=JobID,JobName,State,Partition,NNodes,NCPUS,ReqMem,Submit,Start,End,WorkDir,ExitCode` — recent completed/failed jobs

### Session Management

- One `SshSession` per server, created lazily by `PollWorker`
- Sessions are not persisted across worker invocations (WorkManager processes are short-lived)
- Connection timeout: 30s; command timeout: 60s
- Errors surfaced as `ServerStatus.UNREACHABLE` in UI

---

## Background Polling

### WorkManager Strategy

- Intervals ≥ 15 min: `PeriodicWorkRequest` per server, tagged by `serverId`
- Intervals < 15 min: `OneTimeWorkRequest` that re-enqueues itself on completion
- Constraint: `NetworkType.CONNECTED`

### PollWorker Flow

```
1. Open SSH session
2. Run squeue + sacct
3. Parse → List<Job>
4. Upsert into Room (replace strategy)
5. Diff against previous Room snapshot → List<StateChangeEvent>
6. Evaluate AlertRules → List<AlertEvent>
7. Send notifications via NotificationEngine
8. Update Live Update tokens for RUNNING jobs
9. Re-enqueue if interval < 15 min
```

---

## Notifications

### Channels

| Channel ID | Name | Priority | Triggers |
|------------|------|----------|---------|
| `job_state_changes` | Job State Changes | HIGH | COMPLETED, FAILED, CANCELLED, TIMEOUT |
| `job_alerts` | Job Alerts | DEFAULT | Queue wait exceeded, runtime exceeded |
| `cluster_events` | Cluster Events | HIGH | Node failure, partition down |

### Notification Actions

- **Running job:** `Cancel` (inline action → `scancel` via SSH)
- **Failed/completed job:** `View Details` (deep link to job detail screen)

### Android Live Updates (API 36+)

- Token issued when job transitions to RUNNING
- Updated each poll cycle: elapsed time, current state, progress bar (indeterminate for RUNNING, determinate for COMPLETING)
- Dismissed automatically when job reaches terminal state
- Graceful degradation on API < 36: standard ongoing notification with same content

---

## UI Screens (Material 3 Compose)

### Design System

- **Color:** `dynamicLightColorScheme` / `dynamicDarkColorScheme` on API 31+; static purple seed (`#6750A4`) below
- **Typography:** `MaterialTheme.typography` — no hardcoded sizes
- **Shapes:** `MaterialTheme.shapes` — no hardcoded corner radii
- **Dark mode:** full support via `isSystemInDarkTheme()`

### Onboarding (3-step `HorizontalPager`)

1. **Welcome** — app logo, description, `FilledButton` "Get Started"
2. **Add Server** — `OutlinedTextField` (hostname, port, username), `SegmentedButton` (Password / SSH Key), key import via `ActivityResultContracts.OpenDocument`
3. **Test Connection** — `CircularProgressIndicator` (indeterminate), success `Icon` + latency, failure `Card` with error message + Retry `FilledTonalButton`

Shown only on first launch. Re-accessible from Servers tab FAB for adding additional servers.

### Dashboard Screen

- `LargeTopAppBar` collapsing on scroll
- `FilterChip` row for server filtering
- `LazyColumn` of `ElevatedCard` job rows: job name, server `Badge`, state chip, elapsed time
- Jobs sorted: RUNNING → PENDING → COMPLETED → FAILED
- `FloatingActionButton` → Submit job bottom sheet
- `PullToRefreshBox` (Material3 Compose) for manual refresh

### Servers Screen

- `LazyColumn` of server `ElevatedCard`s: name, hostname, job counts (running / pending / failed), last poll time, online/offline indicator (`primary` vs `error` color)
- FAB → Add server flow

### Server Detail Screen

- `MediumTopAppBar` with server name + status indicator
- Summary `Card` row: running / pending / completed today
- `TabRow` (Primary tabs): **Jobs** | **History** | **Settings**
- **Jobs tab:** `LazyColumn` scoped to this server
- **History tab:** `LazyColumn` of completed jobs with `sacct` data, filterable by date range
- **Settings tab:** `Slider` (polling interval 1–60 min), `Switch` per alert type, `OutlinedTextField` for thresholds, `FilledButton` "Test Connection"

### Job Detail Screen

- Entry: bottom sheet swipe-up → expand to full screen
- State `AssistChip` with color from `JobState` (success/error/primary/secondary)
- Two-column resource grid: nodes, CPUs, memory, partition, work dir, exit code
- Timeline `Card`: submit → start → end with human-readable durations
- Action row: `FilledTonalButton` (Cancel / Requeue), `TextButton` (View script path)

### Submit Job Screen (bottom sheet)

- `OutlinedTextField` for script path or inline script editor (`BasicTextField` with monospace font)
- File picker for `.sh` / `.slurm` files via `ActivityResultContracts.OpenDocument`
- Resource override fields: partition, nodes, CPUs, memory, time limit
- `FilledButton` "Submit" → shows job ID on success

---

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />  <!-- API 33+ -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

---

## Testing Strategy

- **Unit tests:** use cases, `JobDiffer`, `AlertEngine`, SSH output parsers
- **Integration tests:** Room DAO operations, WorkManager with `TestDriver`
- **UI tests:** Compose `ComposeTestRule` for onboarding flow, job detail screen
- Target: 80% coverage on domain + data layers

---

## Out of Scope

- Web dashboard or desktop companion
- SLURM REST API support (SSH only)
- Job script editor with syntax highlighting (path display only for v1)
- Multi-user / shared credential profiles
