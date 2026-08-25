Product Requirements Document (PRD): "Android Kneeboard Sync"

1\. Executive Summary \& Core Concept

The fundamental goal of this project is to create an Android-based clone of the OpenKneeboard client. While OpenKneeboard traditionally displays its kneeboard client on a 2D monitor or inside a VR headset, this project aims to shift that exact client experience onto a physical Android tablet.



The architecture relies on a Client/Server model:



Desktop Server (Windows): Runs in the background on the simulation PC, extracting data and serving files.



Mobile Client (Android): A native app optimized for tablets with active stylus support (e.g., Samsung Galaxy Tab S9 FE), acting as the pilot's interactive physical kneeboard.



2\. Server-Side Requirements (Windows PC)

The server must handle data extraction, file organization, and network streaming to the Android client over a local network (Wi-Fi/LAN).



2.1. Simulator Integration \& Telemetry

Native Support: Must support DCS World and Falcon BMS out of the box.



Data Extraction: Needs to pull active simulation data (e.g., radio frequencies, coordinates, aircraft status) using the exact same logic as OpenKneeboard.



Modular Architecture: The extraction logic should be plugin-based to easily allow future integration with other simulators (e.g., MSFS 2020/2024, IL-2).



2.2. File Management \& Folder Structure

Supported Formats: Must read and serve PDF, PNG, and JPG files from user-defined local directories.



OpenKneeboard Hierarchy: Must strictly replicate the OpenKneeboard folder fallback and priority logic:



Global: Pages visible across all simulators and modules.



Game-specific: Pages visible only for a specific simulator (e.g., DCS only).



Module-specific: Pages visible only when flying a specific aircraft (e.g., F/A-18C, F-16C).



2.3. Profile System

Users must be able to create and manage multiple Profiles.



Profiles can represent a specific simulator setup or a distinct aircraft module.



Each profile must allow the user to designate custom source folders for PDFs/Images, ensuring complete organizational flexibility.



2.4. API \& Networking

The server must expose a local API (REST or WebSockets) to communicate seamlessly with the Android app, stream media files, and push real-time telemetry data.



3\. Client-Side Requirements (Android Tablet App)

The mobile application acts as the physical, interactive kneeboard in the virtual pilot's hands.



3.1. Hardware Optimization \& Stylus Support

Primary Target: Android Tablets (specifically tested for Samsung Galaxy Tab S series, like the S9 FE).



Active Stylus Integration: The app must natively support active pens (like the S-Pen). It must differentiate between touch input (used for swiping/changing pages or zooming) and pen input (strictly reserved for writing).



Palm Rejection: Flawless palm rejection is mandatory so the user can rest their hand on the screen while writing.



3.2. Kneeboard Viewer

The UI must allow smooth navigation through different document categories:



Static Pages: Rendering PDFs and Images served by the PC server, respecting the folder hierarchy of the active profile.



Dynamic Pages: Displaying text/graphic screens generated from the simulator's real-time data.



3.3. Web View Integration

The app must include an embedded browser module (WebView) that acts as a standard kneeboard page. This is critical for:



Displaying the OpenKneeboard Out feature provided by VAICOM PRO.



Accessing the Live Map and Live Kneeboard from the DCS Web Editor project.



3.4. Inking \& Annotation Engine

Transparent Layer: Over every single page (PDF, Image, Dynamic Data, or WebView), there must be a transparent, always-active annotation layer.



Quick Notes: The user must be able to take notes on the fly (e.g., copying down a JTAC 9-line, Bullseye coordinates, or radio frequencies).



Required Tools: Pen (basic colors: Red, Black, Green), Eraser, and a "Clear All" button for the current page.



Persistence: Annotations must anchor to their specific page. If the user swipes to the next page and returns, the notes must still be there.



4\. Technical Notes for Developers (Antigravity)

Open Source Reference: Developers are strongly encouraged to analyze the official OpenKneeboard GitHub repository (primarily C++/C#). The goal is not to copy the code, but to understand the data retrieval engineering from DCS/BMS (e.g., how the Export.lua and Shared Memory work) and the specific folder hierarchy logic, saving time on reverse-engineering simulator behaviors.



Suggested Tech Stack:



Backend (Windows): C# (.NET Core/WPF or MAUI) to maintain similarity with the existing OKB ecosystem, or Python/Go/Node for rapid API development.



Frontend (Android): Native Kotlin or a cross-platform framework like Flutter, provided the chosen framework can perfectly handle robust inking layers and native stylus APIs for palm rejection.

