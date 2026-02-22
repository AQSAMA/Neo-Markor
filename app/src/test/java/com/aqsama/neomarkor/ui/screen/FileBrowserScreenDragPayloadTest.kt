package com.aqsama.neomarkor.ui.screen

import com.aqsama.neomarkor.domain.model.FileNode
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
    fun `drop validation allows non identical targets`() {
        val payload = DragNodePayload(
            sourceUriString = "content://tree/root/dir",
            sourceParentUriString = "content://tree/root",
            sourceIsDirectory = true
        )

        assertTrue(canDropOnTarget(payload, "content://tree/root/dir2"))
        assertTrue(canDropOnTarget(payload, "content://tree/root/other"))
    }

    @Test
    fun `directory cannot be moved into descendant based on tree map`() {
        val folderUri = "content://com.android.externalstorage.documents/tree/primary%3ANotes/document/primary%3ANotes%2Ffolder"
        val childUri = "content://com.android.externalstorage.documents/tree/primary%3ANotes/document/primary%3ANotes%2Ffolder%2Fchild"
        val tree = listOf(
            FileNode(
                name = "folder",
                uriString = folderUri,
                isDirectory = true,
                children = listOf(
                    FileNode(name = "child", uriString = childUri, isDirectory = true)
                )
            )
        )
        val descendants = buildDescendantUriMap(tree)
        val payload = DragNodePayload(
            sourceUriString = folderUri,
            sourceParentUriString = "content://com.android.externalstorage.documents/tree/primary%3ANotes/document/primary%3ANotes",
            sourceIsDirectory = true
        )

        assertFalse(canDropOnTarget(payload, childUri) { source, target ->
            descendants[source]?.contains(target) == true
        })
    }
}
