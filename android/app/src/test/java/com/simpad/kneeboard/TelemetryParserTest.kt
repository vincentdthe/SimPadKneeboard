package com.simpad.kneeboard

import com.simpad.kneeboard.data.models.TelemetryData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TelemetryParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testTelemetryDataDeserialization() {
        val rawJson = """
            {
                "simulator": "DCS",
                "aircraft": "FA-18C_hornet",
                "theater": "Caucasus",
                "missionTitle": "Target Strike",
                "latitude": 42.1234,
                "longitude": 41.5678,
                "altitudeFeet": 18500.0,
                "aglFeet": 16200.0,
                "headingDeg": 285.0,
                "iasKnots": 380.0,
                "mach": 0.78,
                "radios": {
                    "PRI_COM1": "305.000 MHz",
                    "AUX_COM2": "127.500 MHz"
                },
                "bullseye": {
                    "bearingDeg": 180.0,
                    "distanceNm": 42.5
                },
                "status": "Active"
            }
        """.trimIndent()

        val data = json.decodeFromString<TelemetryData>(rawJson)
        assertEquals("DCS", data.simulator)
        assertEquals("FA-18C_hornet", data.aircraft)
        assertEquals("Caucasus", data.theater)
        assertEquals(18500.0, data.altitudeFeet, 0.1)
        assertEquals(0.78, data.mach, 0.01)
        assertEquals("305.000 MHz", data.radios["PRI_COM1"])
        assertNotNull(data.bullseye)
        assertEquals(180.0, data.bullseye!!.bearingDeg, 0.1)
        assertEquals(42.5, data.bullseye!!.distanceNm, 0.1)
    }
}
