package com.aqsama.neomarkor.domain.parser

import com.aqsama.neomarkor.domain.model.WikiLink

/**
 * Parses [[wiki-links]] from Markdown content.
 * Supports [[target]] and [[target|display text]] formats.
 */
object WikiLinkParser {

    private val WIKI_LINK_REGEX = Regex("""\[\[([^\]]+?)(?:\|([^\]]+?))?\]\]""")

    /** Extracts all wiki-links from the given [content]. */
    fun findAll(content: String): List<WikiLink> =
        WIKI_LINK_REGEX.findAll(content).map { match ->
            val target = match.groupValues[1].trim()
            val display = match.groupValues[2].trim().ifEmpty { target }
            WikiLink(
                target = target,
                display = display,
                startIndex = match.range.first,
                endIndex = match.range.last + 1,
            )
        }.toList()

    /** Checks if the content contains any wiki-links. */
    fun containsWikiLinks(content: String): Boolean =
        WIKI_LINK_REGEX.containsMatchIn(content)
}
