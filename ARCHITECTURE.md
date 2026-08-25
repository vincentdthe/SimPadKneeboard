# Architecture Document: SimPad Kneeboard

## 1. System Overview

**SimPad Kneeboard** is a Client/Server ecosystem engineered specifically for flight simulation pilots. It provides a real-time, interactive physical kneeboard experience on an Android tablet (optimized for active styluses like the Samsung S-Pen), driven by a lightweight Windows background server that interfaces with flight simulators.

- **Desktop Server:** **SimPad Kneeboard Server** (Windows PC, C# .NET 9 / ASP.NET Core)
- **Mobile Client:** **SimPad Kneeboard** (Android Tablet with S-Pen active inking & palm rejection)

```
+-----------------------------------------------------------------------------------+
|                        SIMPAD KNEEBOARD SERVER (WINDOWS PC)                       |
|                                                                                   |
|  +--------------------+     +------------------------+     +-------------------+  |
|  |     DCS World      |     |       Falcon BMS       |     |   Future Sims     |  |
|  |   (SimPad_Export)  |     |    (Shared Memory)     |     |  (MSFS / IL-2)    |  |
|  +---------+----------+     +-----------+------------+     +---------+---------+  |
|            | UDP / JSON                 | MemoryMappedFile           | SimConnect |
|            v                            v                            v            |
|  +-----------------------------------------------------------------------------+  |
|  |                    Telemetry & Sim Integration Engine                       |  |
|  |  - ActiveSimManager                                                         |  |
|  |  - DcsTelemetryProvider (UDP listener on port 17290)                        |  |
|  |  - FalconBmsTelemetryProvider (Win32 MMap 'FalconSharedMemoryArea[2]')      |  |
|  |  - MockTelemetryProvider (Offline testing & development test harness)      |  |
|  +--------------------------------------+--------------------------------------+  |
|                                         |                                         |
|                                         v                                         |
|  +-----------------------------------------------------------------------------+  |
|  |                       Kneeboard Document & Hierarchy Engine                 |  |
|  |  - Fallback Hierarchy: Global -> Simulator -> Aircraft/Module               |  |
|  |  - Document Scanner: PDF (paged index & streaming) / PNG / JPG              |  |
|  |  - Profile & Custom Folder Manager (`profiles.json`)                       |  |
|  |  - Dynamic Tab Providers (Radio, Live Telemetry, Live Map / WebView)        |  |
|  +--------------------------------------+--------------------------------------+  |
|                                         |                                         |
|                                         v                                         |
|  +-----------------------------------------------------------------------------+  |
|  |                     ASP.NET Core Server Layer (Kestrel)                     |  |
|  |  - REST API: Profiles, Tabs, Document Metadata, Media Streaming             |  |
|  |  - WebSocket Hub: `/ws/telemetry` (10-30 Hz Push, State Changes)            |  |
|  +--------------------------------------+--------------------------------------+  |
+-----------------------------------------|-----------------------------------------+
                                          | Local Network (Wi-Fi / LAN)
                                          v
+-----------------------------------------------------------------------------------+
|                        SIMPAD KNEEBOARD (ANDROID TABLET APP)                      |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                         Kneeboard Display Engine                            |  |
|  |  - Tab Navigation (Global, Sim, Module, Quick Notes, WebViews)              |  |
|  |  - Page Renderer (PDF & Image viewer with hardware acceleration)            |  |
|  |  - Embedded WebView (VAICOM PRO Kneeboard Out, DCS Web Editor Live Map)     |  |
|  |  - Dynamic Telemetry Display (Frequencies, Bullseye, Flight Data)           |  |
|  +--------------------------------------+--------------------------------------+  |
|                                         |                                         |
|                                         v                                         |
|  +-----------------------------------------------------------------------------+  |
|  |                   Transparent Inking & Annotation Engine                    |  |
|  |  - Active Stylus (S-Pen / USI Pen) with Pressure & Native Palm Rejection   |  |
|  |  - Per-Page Vector Stroke Persistence & Local Cache                         |  |
|  |  - Note Tools: Pen (Black/Red/Green/Yellow), Highlighter, Eraser, Clear All |  |
|  +-----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 2. Telemetry Extraction Architecture

### 2.1. DCS World Integration
- **Mechanism:** Lua hook placed in `%USERPROFILE%\Saved Games\DCS\Scripts\Hooks\SimPad_Hook.lua` or registered in `Export.lua`.
- **Protocol:** UDP packets over localhost (`127.0.0.1:17290`) encoded as JSON.
- **Data Exported:**
  - `sim`: `"DCS"`
  - `aircraft`: Module name (e.g., `"FA-18C_hornet"`, `"F-16C_50"`, `"A-10C_2"`, `"AH-64D_BLK_II"`)
  - `theater`: Map terrain (e.g., `"Caucasus"`, `"PersianGulf"`, `"Nevada"`, `"Syria"`, `"Sinai"`, `"Kola"`)
  - `mission_title`: Current mission name
  - `coordinates`: Latitude, Longitude, Altitude (meters / feet)
  - `heading`, `pitch`, `roll`, `ias_knots`, `mach`
  - `radios`: COM1 / COM2 active frequencies and channel presets
  - `bullseye`: Distance (nm) and bearing (degrees) to coalition bullseye

### 2.2. Falcon BMS Integration
- **Mechanism:** Windows Shared Memory (`MemoryMappedFile`) accessing:
  - `FalconSharedMemoryArea` (`FlightData` struct)
  - `FalconSharedMemoryArea2` (`FlightData2` struct with detailed radios, TACAN, ILS, OSB data)
- **C# Native Interop:** Zero-copy pointers to sequential C structs matching BMS `FlightData.h`:
  - Position: `x`, `y`, `z`, `pitch`, `roll`, `yaw` (converted to Lat/Lon/Alt)
  - Speeds: `kias`, `mach`
  - Avionics & Radios: `UHF_Frequency`, `VHF_Frequency`, `UhfPreset`, `VhfPreset`, `NavMode`, `tacanChannel`

### 2.3. Modular Extensibility (`ITelemetryProvider`)
The telemetry system exposes a common interface:
```csharp
public interface ITelemetryProvider : IDisposable
{
    string SimulatorName { get; }
    bool IsConnected { get; }
    TelemetryData? CurrentTelemetry { get; }
    event EventHandler<TelemetryData>? TelemetryUpdated;
    event EventHandler<SimStateChangedEventArgs>? SimStateChanged;
    Task StartAsync(CancellationToken cancellationToken);
    Task StopAsync(CancellationToken cancellationToken);
}
```

---

## 3. Folder Hierarchy & Document Fallback Logic

The folder hierarchy implements a 4-level precedence system:

```
[Level 1: GLOBAL]
  └── %USERPROFILE%\Saved Games\SimPadKneeboard\Global\
      ├── 01_General_Checklists.pdf
      └── Inflight_Guide.png

[Level 2: SIMULATOR]
  └── %USERPROFILE%\Saved Games\DCS\KNEEBOARD\
      └── DCS_General_Procedures.pdf

[Level 3: MODULE / AIRCRAFT]
  ├── (User Saved Games): %USERPROFILE%\Saved Games\DCS\KNEEBOARD\<Module>\
  │   └── FA-18C_hornet\
  │       ├── CASE_I_Recovery.pdf
  │       └── Weapon_Profiles.png
  └── (Game Installation): <DCS_Install>\Mods\aircraft\<Module>\Cockpit\KNEEBOARD\pages\
      └── 01_default_checklist.png

[Level 4: CUSTOM PROFILE / WEBVIEWS]
  ├── Custom user folder paths (configured in Profile)
  └── WebViews: DCS Web Editor Live Map, VAICOM PRO Out
```

---

## 4. API Endpoints Specification

### 4.1. REST Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/status` | Server health, active sim, connected clients |
| `GET` | `/api/telemetry` | Instantaneous telemetry snapshot |
| `POST` | `/api/telemetry/mock` | Enable/disable mock telemetry stream for development & testing |
| `GET` | `/api/profiles` | List configured profiles |
| `GET` | `/api/profiles/active` | Get active profile |
| `GET` | `/api/profiles/{id}` | Get specific profile details |
| `POST` | `/api/profiles` | Create or update profile |
| `POST` | `/api/profiles/active/{id}` | Switch active profile |
| `DELETE` | `/api/profiles/{id}` | Remove profile |
| `GET` | `/api/kneeboard/tabs` | Get resolved tabs for current sim/module context (or specified `?profileId=...`) |
| `GET` | `/api/kneeboard/tabs/{tabId}` | Get specific tab metadata |
| `GET` | `/api/kneeboard/pages/content` | Stream raw file (PDF/Image) with HTTP range support |
| `GET` | `/api/kneeboard/pages/info` | Get file metadata & total PDF pages count |
| `GET` | `/api/sims/dcs/hook-script` | Returns the DCS Lua hook script content |
| `POST` | `/api/sims/dcs/install-hook` | Automatically copies Lua hook to detected DCS Saved Games folder |
| `GET` | `/api/sims/bms/status` | Checks Falcon BMS shared memory connection status |

### 4.2. WebSocket Endpoint: `/ws/telemetry`

- **Direction:** Bidirectional (Server -> Client push at 20 Hz; Client -> Server control messages)
- **Payload Events:**
  - `connected`: Initial greeting with server status and current sim data.
  - `telemetry_frame`: Live flight data, frequencies, coordinates.
  - `sim_state_changed`: Simulator started, stopped, aircraft module changed.
  - `tabs_invalidated`: Signal that active aircraft changed and client should refresh `/api/kneeboard/tabs`.
  - `ping` / `pong`: Connection heartbeat.
