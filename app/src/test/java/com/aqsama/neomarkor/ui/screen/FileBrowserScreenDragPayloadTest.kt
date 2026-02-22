package com.aqsama.neomarkor.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileBrowserScreenDragPayloadTest {

    @Test
    fun `encode and decode drag payload round trips`() {
        val encoded = encodeDragPayload("source-uri", "parent-uri", true)
        val decoded = decodeDragPayload(encoded)

        assertNotNull(decoded)
        assertEquals("source-uri", decoded?.sourceUriString)
        assertEquals("parent-uri", decoded?.sourceParentUriString)
        assertTrue(decoded?.sourceIsDirectory == true)
    }

    @Test
    fun `cannot drop node on same folder or itself`() {
        val payload = DragNodePayload(
            sourceUriString = "content://source",
            sourceParentUriString = "content://parent",
            sourceIsDirectory = false
        )

        assertFalse(canDropOnTarget(payload, "content://source"))
        assertFalse(canDropOnTarget(payload, "content://parent"))
    }

    @Test
    fun `cannot drop directory into its own descendants`() {
        val payload = DragNodePayload(
            sourceUriString = "content://tree/root/dir",
            sourceParentUriString = "content://tree/root",
            sourceIsDirectory = true
        )

        assertFalse(canDropOnTarget(payload, "content://tree/root/dir/child"))
        assertTrue(canDropOnTarget(payload, "content://tree/root/other"))
    }
}
