# SimPad Kneeboard

**SimPad Kneeboard** is an interactive physical kneeboard system for flight simulation (DCS World, Falcon BMS, MSFS), shifting the kneeboard display and inking experience onto an Android tablet (optimized for active styluses such as Samsung S-Pen with hardware palm rejection) via a local Windows background server.

---

## Project Structure

```text
SimPadKneeboard/
├── ARCHITECTURE.md                  # Detailed Architecture, Protocols & API Specifications
├── project_requirements.md          # Original Product Requirements Document (PRD)
├── README.md                        # Project state & quickstart guide
├── .gitignore                       # Git ignore rules for .NET & Android
│
├── dcs-hook/
│   └── SimPad_Export.lua            # DCS World Telemetry Export script (UDP Port 17290)
│
├── server/                          # Windows Backend Server (C# .NET 9 ASP.NET Core)
│   └── src/
│       └── SimPadServer/
│           ├── SimPadServer.csproj
│           ├── Program.cs
│           ├── appsettings.json
│           ├── Models/              # Telemetry, Tab, Page, Profile, and WebSocket DTOs
│           ├── Telemetry/           # DCS (UDP), Falcon BMS (Shared Memory), Mock provider, ActiveSimManager
│           ├── Services/            # DocumentService (PDF/Image), ProfileService, KneeboardHierarchyService
│           ├── WebSockets/          # Real-time WebSocket Hub (/ws/telemetry)
│           └── Endpoints/           # REST API Route Handlers (/api/*)
│
└── android/                         # Android Tablet Client (Kotlin / Jetpack Compose)
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── app/
        ├── build.gradle.kts
        └── src/main/java/com/simpad/kneeboard/
            ├── MainActivity.kt      # Fullscreen immersive cockpit activity
            ├── SimPadApplication.kt
            ├── data/
            │   ├── api/             # SimPadApiService & SimPadWebSocketClient
            │   ├── models/          # Telemetry, Tab, Page, and InkStroke models
            │   └── repository/      # InkingStorageRepository & KneeboardRepository
            ├── ui/
            │   ├── MainKneeboardScreen.kt
            │   ├── inking/          # InkingView & InkingCanvas (Hardware S-Pen & Palm Rejection)
            │   ├── pages/           # DocumentPageViewer, TelemetryHudPage, QuickNotesPage, WebViewPage
            │   ├── components/      # TopBar, TabNavigationBar, InkingToolbar, ConnectionDialog
            │   └── theme/           # Color, Type, Theme (Cockpit Dark, NVG Green, Red Light, Day)
            └── viewmodel/           # KneeboardViewModel & InkingViewModel
```

---

## Features

### 🖥️ Windows Backend Server
- **DCS World UDP Ingestion**: 20 Hz telemetry stream from `SimPad_Export.lua` (coordinates, attitude, speeds, COM frequencies, bullseye, module changes).
- **Falcon BMS Shared Memory**: Reads Win32 shared memory (`FalconSharedMemoryArea` / `FalconSharedMemoryArea2`) zero-copy structs.
- **OpenKneeboard Folder Fallback**: Global ➔ Sim ➔ Aircraft Module ➔ Custom Profile folder precedence.
- **Document Streaming**: Serves PDF and image pages with HTTP byte-range streaming.
- **Multi-Profile Manager**: Create, switch, and persist custom aircraft/sim profile folders in `profiles.json`.
- **Real-Time WebSocket Hub**: Broadcasts telemetry and module change events on `/ws/telemetry`.
- **Mock Simulator Test Harness**: Optional offline flight simulator for testing without launching DCS/BMS.

### 📱 Android Tablet Client (`SimPad Kneeboard`)
- **Active Stylus Inking & Hardware Palm Rejection**: S-Pen / Active pen support with pressure dynamics. Stylus draws on transparent overlay while fingers handle gestures (zoom, pan, page turning).
- **Per-Page Note Persistence**: Vector stroke notes are cached and stored per page in local internal storage.
- **Live Telemetry HUD**: Real-time display for COM1/COM2 radios, TACAN, ILS, Bullseye range/bearing, Altitude, Mach, and Coordinates.
- **Embedded WebViews**: Seamless support for DCS Web Editor Live Map and VAICOM PRO Kneeboard Out.
- **Cockpit Lighting Modes**:
  - 🌑 **Dark Cockpit**: Default tactical cockpit theme.
  - 🟢 **NVG Green**: Night-vision phosphor mode for night sorties.
  - 🔴 **Red Light Chart**: Aviation red-floodlight mode.
  - ☀️ **Day Mode**: High-contrast light theme for daytime flights.

---

## How to Run

### 1. Windows Backend Server
```powershell
cd "server/src/SimPadServer"
dotnet run
```
The server will listen on `http://0.0.0.0:8090` and DCS UDP port `17290`.

### 2. DCS World Hook Setup
Copy `dcs-hook/SimPad_Export.lua` into:
`%USERPROFILE%\Saved Games\DCS\Scripts\Hooks\SimPad_Hook.lua`
*(Or call `POST http://localhost:8090/api/sims/dcs/install-hook` for automatic installation)*.

### 3. Android Tablet Client
Open the `android/` directory in **Android Studio**:
- Connect your Android Tablet (e.g. Samsung Galaxy Tab S9/S8) via USB or Wi-Fi ADB.
- Click **Run `app`** (`Shift + F10`).
- In the app, tap the **Settings** icon on the top bar and enter your PC's local Wi-Fi IP address (e.g., `192.168.1.50:8090`).
