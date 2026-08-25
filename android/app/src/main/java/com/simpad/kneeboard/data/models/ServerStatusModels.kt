package com.simpad.kneeboard.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerStatus(
    val serverName: String = "SimPad Kneeboard Server",
    val version: String = "1.0.0",
    val httpPort: Int = 8090,
    val dcsUdpPort: Int = 17290,
    val activeSimulator: String = "None",
    val activeAircraft: String = "None",
    val activeTheater: String = "None",
    val activeProfileId: String? = null,
    val activeProfileName: String? = null,
    val connectedClientsCount: Int = 0,
    val startTimeUtc: String = "",
    val uptimeSeconds: Double = 0.0
)
