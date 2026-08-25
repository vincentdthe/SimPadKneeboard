package com.simpad.kneeboard

import com.simpad.kneeboard.data.models.InkPoint
import com.simpad.kneeboard.data.models.InkStroke
import com.simpad.kneeboard.data.models.PageInkingData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InkingSerializationTest {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    @Test
    fun testStrokeSerializationAndDeserialization() {
        val points = listOf(
            InkPoint(x = 100.5f, y = 200.5f, pressure = 0.8f, timestamp = 1700000000L),
            InkPoint(x = 105.0f, y = 208.0f, pressure = 0.85f, timestamp = 1700000010L)
        )

        val stroke = InkStroke(
            id = "stroke_1",
            colorArgb = 0xFF00E5FFL,
            strokeWidth = 3.5f,
            isHighlighter = false,
            points = points
        )

        val pageData = PageInkingData(
            pageId = "doc_checklist_p1",
            strokes = listOf(stroke),
            lastModifiedUtc = 1700000100L
        )

        val serialized = json.encodeToString(pageData)
        assertNotNull(serialized)

        val deserialized = json.decodeFromString<PageInkingData>(serialized)
        assertEquals("doc_checklist_p1", deserialized.pageId)
        assertEquals(1, deserialized.strokes.size)
        assertEquals(0xFF00E5FFL, deserialized.strokes[0].colorArgb)
        assertEquals(2, deserialized.strokes[0].points.size)
        assertEquals(100.5f, deserialized.strokes[0].points[0].x, 0.001f)
    }
}
