package com.aqsama.neomarkor.domain.parser

import org.junit.Assert.*
import org.junit.Test

class WikiLinkParserTest {

    @Test
    fun `findAll returns empty for text with no wiki-links`() {
        val result = WikiLinkParser.findAll("Hello world, no links here.")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAll detects simple wiki-link`() {
        val result = WikiLinkParser.findAll("See [[My Note]] for details.")
        assertEquals(1, result.size)
        assertEquals("My Note", result[0].target)
        assertEquals("My Note", result[0].display)
    }

    @Test
    fun `findAll detects wiki-link with display text`() {
        val result = WikiLinkParser.findAll("See [[My Note|this note]] for details.")
        assertEquals(1, result.size)
        assertEquals("My Note", result[0].target)
        assertEquals("this note", result[0].display)
    }

    @Test
    fun `findAll detects multiple wiki-links`() {
        val result = WikiLinkParser.findAll("Links: [[A]], [[B|Beta]], [[C]]")
        assertEquals(3, result.size)
        assertEquals("A", result[0].target)
        assertEquals("B", result[1].target)
        assertEquals("Beta", result[1].display)
        assertEquals("C", result[2].target)
    }

    @Test
    fun `findAll captures correct indices`() {
        val text = "Go to [[Target]] now."
        val result = WikiLinkParser.findAll(text)
        assertEquals(1, result.size)
        assertEquals(6, result[0].startIndex)
        assertEquals(17, result[0].endIndex)
        assertEquals("[[Target]]", text.substring(result[0].startIndex, result[0].endIndex))
    }

    @Test
    fun `containsWikiLinks returns false for plain text`() {
        assertFalse(WikiLinkParser.containsWikiLinks("No links here"))
    }

    @Test
    fun `containsWikiLinks returns true when wiki-link present`() {
        assertTrue(WikiLinkParser.containsWikiLinks("See [[Note]]"))
    }

    @Test
    fun `findAll handles wiki-link with spaces in target`() {
        val result = WikiLinkParser.findAll("[[  Spaced Note  ]]")
        assertEquals(1, result.size)
        assertEquals("Spaced Note", result[0].target)
    }
}
