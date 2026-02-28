package com.aqsama.neomarkor.domain.model

import org.junit.Assert.*
import org.junit.Test

class FolderTest {

    @Test
    fun `default folder has gray color`() {
        val folder = Folder(id = "1", name = "Test")
        assertEquals(Folder.DEFAULT_FOLDER_COLOR, folder.color)
    }

    @Test
    fun `folder with no parent is a root folder`() {
        val folder = Folder(id = "1", name = "Root", parentId = null)
        assertNull(folder.parentId)
    }

    @Test
    fun `folder with parentId is a child folder`() {
        val folder = Folder(id = "2", name = "Child", parentId = "1")
        assertEquals("1", folder.parentId)
    }

    @Test
    fun `folder copy preserves all fields`() {
        val original = Folder(
            id = "1",
            name = "Test",
            color = 0xFFFF0000,
            parentId = "parent",
            order = 5,
            noteCount = 3,
        )
        val copy = original.copy(name = "Renamed")
        assertEquals("Renamed", copy.name)
        assertEquals(original.id, copy.id)
        assertEquals(original.color, copy.color)
        assertEquals(original.parentId, copy.parentId)
        assertEquals(original.order, copy.order)
        assertEquals(original.noteCount, copy.noteCount)
    }

    @Test
    fun `FolderColors has 11 predefined colors`() {
        assertEquals(11, FolderColors.colors.size)
    }

    @Test
    fun `FolderColors first color is Gray`() {
        assertEquals("Gray", FolderColors.colors.first().first)
    }

    @Test
    fun `root folders can be filtered from mixed list`() {
        val folders = listOf(
            Folder(id = "1", name = "Root 1", parentId = null, order = 0),
            Folder(id = "2", name = "Root 2", parentId = null, order = 1),
            Folder(id = "3", name = "Child 1", parentId = "1", order = 0),
            Folder(id = "4", name = "Child 2", parentId = "1", order = 1),
        )
        val roots = folders.filter { it.parentId == null }.sortedBy { it.order }
        assertEquals(2, roots.size)
        assertEquals("Root 1", roots[0].name)
        assertEquals("Root 2", roots[1].name)
    }

    @Test
    fun `child folders can be filtered by parentId`() {
        val folders = listOf(
            Folder(id = "1", name = "Root", parentId = null, order = 0),
            Folder(id = "2", name = "Child A", parentId = "1", order = 0),
            Folder(id = "3", name = "Child B", parentId = "1", order = 1),
            Folder(id = "4", name = "Other", parentId = "5", order = 0),
        )
        val childrenOf1 = folders.filter { it.parentId == "1" }.sortedBy { it.order }
        assertEquals(2, childrenOf1.size)
        assertEquals("Child A", childrenOf1[0].name)
        assertEquals("Child B", childrenOf1[1].name)
    }

    @Test
    fun `descendant ids can be collected recursively`() {
        val folders = listOf(
            Folder(id = "1", name = "Root", parentId = null),
            Folder(id = "2", name = "Child", parentId = "1"),
            Folder(id = "3", name = "Grandchild", parentId = "2"),
            Folder(id = "4", name = "Unrelated", parentId = null),
        )

        fun collectDescendantIds(parentId: String): Set<String> {
            val children = folders.filter { it.parentId == parentId }
            val result = mutableSetOf<String>()
            for (child in children) {
                result.add(child.id)
                result.addAll(collectDescendantIds(child.id))
            }
            return result
        }

        val descendants = collectDescendantIds("1")
        assertEquals(setOf("2", "3"), descendants)
    }

    @Test
    fun `folder serialization preserves data`() {
        val folder = Folder(
            id = "test-id",
            name = "My Folder",
            color = 0xFF4CAF50,
            parentId = null,
            order = 2,
            noteCount = 5,
        )
        val json = kotlinx.serialization.json.Json.encodeToString(Folder.serializer(), folder)
        val decoded = kotlinx.serialization.json.Json.decodeFromString(Folder.serializer(), json)
        assertEquals(folder, decoded)
    }

    @Test
    fun `folder list serialization round-trip`() {
        val folders = listOf(
            Folder(id = "1", name = "Folder A", color = 0xFFE53935, parentId = null, order = 0),
            Folder(id = "2", name = "Folder B", color = 0xFF2196F3, parentId = "1", order = 0),
        )
        val serializer = kotlinx.serialization.builtins.ListSerializer(Folder.serializer())
        val json = kotlinx.serialization.json.Json.encodeToString(serializer, folders)
        val decoded = kotlinx.serialization.json.Json.decodeFromString(serializer, json)
        assertEquals(folders, decoded)
    }
}
