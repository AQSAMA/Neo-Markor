package com.aqsama.neomarkor.domain.parser

import org.junit.Assert.*
import org.junit.Test

class MarkdownHighlighterTest {

    @Test
    fun `highlight returns annotated string with same text`() {
        val input = "# Hello\nSome **bold** text."
        val result = MarkdownHighlighter.highlight(input)
        assertEquals(input, result.text)
    }

    @Test
    fun `highlight adds spans for headings`() {
        val input = "# Heading 1\n## Heading 2\n### Heading 3"
        val result = MarkdownHighlighter.highlight(input)
        // The text should remain unchanged
        assertEquals(input, result.text)
        // Should have annotation spans applied (non-empty)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun `highlight handles empty string`() {
        val result = MarkdownHighlighter.highlight("")
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `highlight handles plain text without markdown`() {
        val input = "Just plain text with no markdown."
        val result = MarkdownHighlighter.highlight(input)
        assertEquals(input, result.text)
        // Plain text should have no style spans
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `highlight detects inline code`() {
        val input = "Use `println()` to print."
        val result = MarkdownHighlighter.highlight(input)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun `highlight detects bold text`() {
        val input = "This is **bold** text."
        val result = MarkdownHighlighter.highlight(input)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun `highlight detects wiki-links`() {
        val input = "See [[My Note]] for details."
        val result = MarkdownHighlighter.highlight(input)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun `highlight detects task list markers`() {
        val input = "- [x] Done task\n- [ ] Pending task"
        val result = MarkdownHighlighter.highlight(input)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun `highlight detects links`() {
        val input = "Visit [Google](https://google.com) now."
        val result = MarkdownHighlighter.highlight(input)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }
}
