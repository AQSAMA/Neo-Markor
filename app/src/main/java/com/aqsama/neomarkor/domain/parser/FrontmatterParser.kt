package com.aqsama.neomarkor.domain.parser

import com.aqsama.neomarkor.domain.model.Frontmatter

/**
 * Lightweight YAML frontmatter parser.
 * Extracts the `---` delimited block at the top of Markdown files
 * and parses simple key: value pairs, including list values for tags.
 */
object FrontmatterParser {

    private val FRONTMATTER_REGEX = Regex("""^---\s*\n(.*?)\n---\s*\n""", RegexOption.DOT_MATCHES_ALL)

    /** Returns the parsed [Frontmatter] and the body text after the frontmatter block. */
    fun parse(content: String): Pair<Frontmatter?, String> {
        val match = FRONTMATTER_REGEX.find(content) ?: return null to content
        val yamlBlock = match.groupValues[1]
        val body = content.substring(match.range.last + 1)
        val frontmatter = parseYamlBlock(yamlBlock)
        return frontmatter to body
    }

    /** Returns only the body content with frontmatter stripped. */
    fun stripFrontmatter(content: String): String = parse(content).second

    /** Returns only the [Frontmatter] or null if none present. */
    fun extractFrontmatter(content: String): Frontmatter? = parse(content).first

    private fun parseYamlBlock(yaml: String): Frontmatter {
        var title: String? = null
        var date: String? = null
        var pinned = false
        val tags = mutableListOf<String>()
        val extra = mutableMapOf<String, String>()

        var currentListKey: String? = null
        for (line in yaml.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // List item continuation (e.g. "  - item")
            if (trimmed.startsWith("- ") && currentListKey != null) {
                val value = trimmed.removePrefix("- ").trim()
                if (currentListKey == "tags") tags.add(value)
                continue
            }

            val colonIndex = trimmed.indexOf(':')
            if (colonIndex < 0) continue
            currentListKey = null

            val key = trimmed.substring(0, colonIndex).trim().lowercase()
            val value = trimmed.substring(colonIndex + 1).trim()

            if (value.isEmpty()) {
                // Could be a list key — next lines may have "- item"
                currentListKey = key
                continue
            }

            // Inline list: tags: [a, b, c]
            if (value.startsWith("[") && value.endsWith("]")) {
                val items = value.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                    .filter { it.isNotEmpty() }
                if (key == "tags") tags.addAll(items)
                continue
            }

            val unquoted = value.removeSurrounding("\"").removeSurrounding("'")
            when (key) {
                "title" -> title = unquoted
                "date" -> date = unquoted
                "pinned" -> pinned = unquoted.equals("true", ignoreCase = true)
                "tags" -> tags.add(unquoted)
                else -> extra[key] = unquoted
            }
        }

        return Frontmatter(
            title = title,
            tags = tags.toList(),
            date = date,
            pinned = pinned,
            extra = extra.toMap(),
        )
    }
}
