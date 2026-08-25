--[[
=============================================================================
  SimPad Kneeboard - DCS World Telemetry Export Script
=============================================================================
  This script exports DCS World flight telemetry, active aircraft module,
  theater/terrain, mission info, coordinates, bullseye, and radio frequencies
  to the SimPad Kneeboard Server via UDP (Port 17290).

  INSTALLATION:
  Option A (Recommended - Export.lua):
    Copy this file or append its content to:
    %USERPROFILE%\Saved Games\DCS\Scripts\Export.lua
    (or %USERPROFILE%\Saved Games\DCS.openbeta\Scripts\Export.lua)

  Option B (GUI Hook):
    Place this file into:
    %USERPROFILE%\Saved Games\DCS\Scripts\Hooks\SimPad_Hook.lua
=============================================================================
--]]

local simpad = {
    host = "127.0.0.1",
    port = 17290,
    socket = nil,
    udp = nil,
    sendRate = 0.05, -- 20 Hz
    lastSendTime = 0,
    prevAircraft = nil,
    prevTheater = nil,
}

-- Safe JSON serializer for DCS environment
local function toJson(val)
    if val == nil then return "null" end
    local t = type(val)
    if t == "number" or t == "boolean" then
        return tostring(val)
    elseif t == "string" then
        return string.format("%q", val)
    elseif t == "table" then
        local isArray = #val > 0
        local parts = {}
        if isArray then
            for _, v in ipairs(val) do
                table.insert(parts, toJson(v))
            end
            return "[" .. table.concat(parts, ",") .. "]"
        else
            for k, v in pairs(val) do
                table.insert(parts, string.format("%q:%s", tostring(k), toJson(v)))
            end
            return "{" .. table.concat(parts, ",") .. "}"
        end
    end
    return "null"
end

local function initSocket()
    if simpad.udp then return true end
    local status, socket = pcall(require, "socket")
    if not status or not socket then
        -- Try loading from DCS LuaSocket path
        package.cpath = package.cpath .. ";.\\LuaSocket\\?.dll;"
        status, socket = pcall(require, "socket")
    end

    if status and socket then
        simpad.socket = socket
        simpad.udp = socket.udp()
        if simpad.udp then
            simpad.udp:setpeername(simpad.host, simpad.port)
            simpad.udp:settimeout(0)
            return true
        end
    end
    return false
end

local function sendPacket(payload)
    if not initSocket() then return end
    local jsonStr = toJson(payload)
    pcall(function()
        simpad.udp:send(jsonStr)
    end)
end

-- =========================================================================
-- Export.lua Interface Handlers
-- =========================================================================

function LuaExportStart()
    initSocket()
    local startPayload = {
        sim = "DCS",
        event = "sim_started",
        timestamp = os.time(),
        install_path = (lfs and lfs.currentdir and lfs.currentdir()) or "",
        saved_games_path = (lfs and lfs.writedir and lfs.writedir()) or "",
    }
    sendPacket(startPayload)
end

function LuaExportBeforeNextFrame()
    -- Reserved for input injection
end

function LuaExportAfterNextFrame()
    local curTime = LoGetModelTime() or 0
    if (curTime - simpad.lastSendTime) < simpad.sendRate then
        return
    end
    simpad.lastSendTime = curTime

    local selfData = LoGetSelfData()
    if not selfData then
        return
    end

    local aircraftName = selfData.Name or "Unknown"
    local latLongAlt = selfData.LatLongAlt or {}
    local lat = latLongAlt.Lat or 0
    local lon = latLongAlt.Long or 0
    local altMeters = latLongAlt.Alt or 0
    local altFeet = altMeters * 3.28084

    local headingRad = selfData.Heading or 0
    local headingDeg = (headingRad * 180 / math.pi) % 360
    local pitchRad = selfData.Pitch or 0
    local pitchDeg = pitchRad * 180 / math.pi
    local bankRad = selfData.Bank or 0
    local bankDeg = bankRad * 180 / math.pi

    local iasMps = LoGetIndicatedAirSpeed() or 0
    local iasKnots = iasMps * 1.94384
    local tasMps = LoGetTrueAirSpeed() or 0
    local tasKnots = tasMps * 1.94384
    local mach = LoGetMachNumber() or 0
    local aglMeters = LoGetAltitudeAboveGroundLevel() or 0
    local aglFeet = aglMeters * 3.28084

    -- Mission & Theater detection
    local theater = (LoGetTerrainName and LoGetTerrainName()) or "Unknown"
    local missionTitle = (LoGetMissionTitle and LoGetMissionTitle()) or "Mission"

    -- Radios & Bullseye
    local radioStatus = (LoGetRadioBeaconsStatus and LoGetRadioBeaconsStatus()) or {}
    
    local packet = {
        sim = "DCS",
        event = "telemetry",
        timestamp = curTime,
        aircraft = aircraftName,
        theater = theater,
        mission_title = missionTitle,
        coordinates = {
            latitude = lat,
            longitude = lon,
            altitude_meters = altMeters,
            altitude_feet = altFeet,
            agl_meters = aglMeters,
            agl_feet = aglFeet,
        },
        attitude = {
            heading_deg = headingDeg,
            pitch_deg = pitchDeg,
            bank_deg = bankDeg,
        },
        airspeed = {
            ias_knots = iasKnots,
            tas_knots = tasKnots,
            mach = mach,
        },
        radios = radioStatus,
        status = "Active",
    }

    sendPacket(packet)

    if aircraftName ~= simpad.prevAircraft or theater ~= simpad.prevTheater then
        simpad.prevAircraft = aircraftName
        simpad.prevTheater = theater
        local changeEvent = {
            sim = "DCS",
            event = "aircraft_changed",
            aircraft = aircraftName,
            theater = theater,
        }
        sendPacket(changeEvent)
    end
end

function LuaExportStop()
    local stopPayload = {
        sim = "DCS",
        event = "sim_stopped",
        timestamp = os.time(),
    }
    sendPacket(stopPayload)
    if simpad.udp then
        pcall(function() simpad.udp:close() end)
        simpad.udp = nil
    end
end

-- =========================================================================
-- DCS GUI Hook Callbacks (if loaded in Scripts/Hooks/)
-- =========================================================================
local simpadHook = {}

function simpadHook.onSimulationStart()
    LuaExportStart()
end

function simpadHook.onSimulationStop()
    LuaExportStop()
end

function simpadHook.onMissionLoadEnd()
    initSocket()
    local missionInfo = (DCS and DCS.getCurrentMission and DCS.getCurrentMission()) or {}
    local terrainName = (DCS and DCS.getCurrentMission and missionInfo.mission and missionInfo.mission.theatre) or "Unknown"
    sendPacket({
        sim = "DCS",
        event = "mission_loaded",
        theater = terrainName,
    })
end

if DCS and DCS.setUserCallbacks then
    DCS.setUserCallbacks(simpadHook)
end
