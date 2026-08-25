package com.simpad.kneeboard.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TelemetryData(
    val simulator: String = "None",
    val aircraft: String = "Unknown",
    val theater: String = "Unknown",
    val missionTitle: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val altitudeFeet: Double = 0.0,
    val aglMeters: Double = 0.0,
    val aglFeet: Double = 0.0,
    val headingDeg: Double = 0.0,
    val pitchDeg: Double = 0.0,
    val bankDeg: Double = 0.0,
    val iasKnots: Double = 0.0,
    val tasKnots: Double = 0.0,
    val mach: Double = 0.0,
    val radios: Map<String, String> = emptyMap(),
    val bullseye: BullseyeData? = null,
    val status: String = "Disconnected",
    val lastUpdatedUtc: String = ""
)

@Serializable
data class BullseyeData(
    val bearingDeg: Double = 0.0,
    val distanceNm: Double = 0.0
)

@Serializable
data class SimStateChangedEvent(
    val simulatorName: String = "",
    val isRunning: Boolean = false,
    val aircraftName: String = "",
    val theaterName: String = "",
    val previousAircraftName: String? = null
)

@Serializable
data class WebSocketEventFrame(
    val event: String,
    val timestamp: Long = 0L,
    val payload: JsonElement? = null
)
