package com.aqsama.neomarkor.domain.parser

import org.junit.Assert.*
import org.junit.Test

class FrontmatterParserTest {

    @Test
    fun `parse returns null frontmatter when no frontmatter block`() {
        val content = "# Hello World\nSome body text."
        val (fm, body) = FrontmatterParser.parse(content)
        assertNull(fm)
        assertEquals(content, body)
    }

    @Test
    fun `parse extracts title from frontmatter`() {
        val content = """
---
title: My Note
---
# Body
""".trimIndent()
        val (fm, body) = FrontmatterParser.parse(content)
        assertNotNull(fm)
        assertEquals("My Note", fm!!.title)
        assertTrue(body.contains("# Body"))
    }

    @Test
    fun `parse extracts inline tags`() {
        val content = """
---
tags: [kotlin, android, compose]
---
Body
""".trimIndent()
        val (fm, _) = FrontmatterParser.parse(content)
        assertNotNull(fm)
        assertEquals(listOf("kotlin", "android", "compose"), fm!!.tags)
    }

    @Test
    fun `parse extracts list-style tags`() {
        val content = """
---
tags:
  - note
  - journal
---
Body
""".trimIndent()
        val (fm, _) = FrontmatterParser.parse(content)
        assertNotNull(fm)
        assertEquals(listOf("note", "journal"), fm!!.tags)
    }

    @Test
    fun `parse extracts date and pinned`() {
        val content = """
---
date: 2026-01-15
pinned: true
---
Body
""".trimIndent()
        val (fm, _) = FrontmatterParser.parse(content)
        assertNotNull(fm)
        assertEquals("2026-01-15", fm!!.date)
        assertTrue(fm.pinned)
    }

    @Test
    fun `parse handles quoted values`() {
        val content = """
---
title: "My \"Quoted\" Title"
---
Body
""".trimIndent()
        val (fm, _) = FrontmatterParser.parse(content)
        assertNotNull(fm)
        assertEquals("My \\\"Quoted\\\" Title", fm!!.title)
    }

    @Test
    fun `parse stores unknown keys in extra map`() {
        val content = """
---
author: John
category: work
---
Body
""".trimIndent()
        val (fm, _) = FrontmatterParser.parse(content)
        assertNotNull(fm)
        assertEquals("John", fm!!.extra["author"])
        assertEquals("work", fm.extra["category"])
    }

    @Test
    fun `stripFrontmatter returns body only`() {
        val content = """
---
title: Test
---
Hello world
""".trimIndent()
        val body = FrontmatterParser.stripFrontmatter(content)
        assertFalse(body.contains("---"))
        assertTrue(body.contains("Hello world"))
    }

    @Test
    fun `extractFrontmatter returns frontmatter only`() {
        val content = """
---
title: Test
tags: [a, b]
---
Body
""".trimIndent()
        val fm = FrontmatterParser.extractFrontmatter(content)
        assertNotNull(fm)
        assertEquals("Test", fm!!.title)
        assertEquals(listOf("a", "b"), fm.tags)
    }

    @Test
    fun `parse ignores comment lines in frontmatter`() {
        val content = """
---
title: Test
# This is a comment
date: 2026-01-01
---
Body
""".trimIndent()
        val (fm, _) = FrontmatterParser.parse(content)
        assertNotNull(fm)
        assertEquals("Test", fm!!.title)
        assertEquals("2026-01-01", fm.date)
    }
}
