\# MISSION

You are an expert software architect and developer. Your goal is to build a Client/Server architecture that acts as an Android-based clone of the "OpenKneeboard" client. 



\## PROJECT REQUIREMENTS (PRD)

1\. \*\*Server (Windows PC):\*\* Runs in the background, extracts telemetry/data from DCS World and Falcon BMS, reads local PDF/PNG files, and serves them via local API.

2\. \*\*Client (Android Tablet):\*\* A native Android app (optimized for Samsung S-Pen/active stylus) that fetches data from the server, displays PDFs/images/dynamic data/WebViews, and includes a transparent, persistent inking layer for handwritten notes with palm rejection.



\## REFERENCE CODE \& CONTEXT

The original OpenKneeboard logic is open-source. Do NOT read the entire repository, as you will exceed your context limit with VR rendering code that we do not need for an Android app. 

Analyze the repository at `https://github.com/OpenKneeboard/OpenKneeboard` and focus \*\*EXCLUSIVELY\*\* on these components to understand the data extraction and folder logic:



1\. \*\*DCS Data Extraction:\*\* Inspect `src/dcs-hook/` (e.g., `OpenKneeboardDCSExt.lua`) to understand how telemetry and radio data are exported from DCS.

2\. \*\*Falcon BMS Extraction:\*\* Search the repo for how it reads the BMS Shared Memory.

3\. \*\*Folder Structure Logic:\*\* Find the C++ logic that dictates the fallback hierarchy (Global > Game-specific > Module-specific). 

4\. \*\*IGNORE:\*\* Completely ignore any code related to OpenXR, OpenVR, Direct3D rendering, Wacom tablet drivers, or the desktop UI.



\## YOUR FIRST TASK

Do not write the Android app yet. For your first output, write the architecture document and the base code for the \*\*Windows Backend Server\*\*. Decide whether C# (.NET) or Python/Go is the best tool for the server based on the data extraction needs. Show me the API endpoints you plan to expose to the Android client.

