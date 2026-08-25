# SimPad Kneeboard

**SimPad Kneeboard** is an interactive physical kneeboard system for flight simulation (DCS World, Falcon BMS, MSFS), shifting the kneeboard display and inking experience onto an Android tablet (optimized for active styluses such as Samsung S-Pen with palm rejection) via a local Windows background server.

---

## Project Structure

```
OpenKneeboardClone/
├── ARCHITECTURE.md                  # Detailed Architecture, Protocols & API Specifications
├── project_requirements.md          # Original Product Requirements Document (PRD)
├── prompt.md                        # Task specifications
├── README.md                        # Project state & quickstart guide
│
├── dcs-hook/
│   └── SimPad_Export.lua            # DCS World Telemetry Export script (UDP Port 17290)
│
└── server/
    └── src/
        └── SimPadServer/            # Windows Backend Server (C# .NET 9 ASP.NET Core)
            ├── SimPadServer.csproj
            ├── Program.cs
            ├── appsettings.json
            ├── Models/              # Telemetry, Tab, Page, Profile, and WebSocket DTOs
            ├── Telemetry/           # DCS (UDP), Falcon BMS (Shared Memory), Mock provider, ActiveSimManager
            ├── Services/            # DocumentService (PDF/Image), ProfileService, KneeboardHierarchyService
            ├── WebSockets/          # Real-time WebSocket Hub (/ws/telemetry)
            └── Endpoints/           # REST API Route Handlers (/api/*)
```

---

## Current Status

- [x] **Backend Server Architecture & API Specification** (`ARCHITECTURE.md`)
- [x] **DCS World Telemetry Export Hook** (`dcs-hook/SimPad_Export.lua`)
- [x] **DCS Telemetry UDP Receiver** (`DcsTelemetryProvider.cs`)
- [x] **Falcon BMS Win32 Shared Memory Reader** (`FalconBmsTelemetryProvider.cs`)
- [x] **Mock Flight Simulation Harness** (`MockTelemetryProvider.cs`)
- [x] **Folder Fallback Hierarchy Engine** (`KneeboardHierarchyService.cs`: Global > Sim > Module > Profile)
- [x] **Document & PDF Page Streaming** (`DocumentService.cs`)
- [x] **Profile Persistence & Custom Folders** (`ProfileService.cs`)
- [x] **REST API Surface & Real-time WebSocket Hub** (`/api/*`, `/ws/telemetry`)
- [ ] **Android Tablet Client (`SimPad Kneeboard`)** *(Next Step)*

---

## How to Resume & Run

### 1. Run the Backend Server
```powershell
cd "g:\My Drive\Sims\Flight\OpenKneeboardClone\server\src\SimPadServer"
dotnet run
```
The server will start listening on `http://0.0.0.0:8090` and DCS UDP port `17290`.

### 2. Next Phase: Android Tablet Client
When resuming, proceed to build the native Android tablet app (`SimPad Kneeboard`) with:
- S-Pen active inking layer with palm rejection and vector stroke persistence.
- Document viewer for streamed PDF/image pages.
- Dynamic telemetry pages and WebViews (VAICOM PRO / DCS Web Editor Live Map).
