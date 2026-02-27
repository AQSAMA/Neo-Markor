package com.aqsama.neomarkor.domain.parser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Provides lightweight syntax highlighting for Markdown source text.
 * Returns an [AnnotatedString] with color/style spans applied.
 */
object MarkdownHighlighter {

    // Heading: lines starting with # (up to 6 levels)
    private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.*)$""", RegexOption.MULTILINE)

    // Bold: **text** or __text__
    private val BOLD_REGEX = Regex("""\*\*(.+?)\*\*|__(.+?)__""")

    // Italic: *text* or _text_ (but not ** or __)
    private val ITALIC_REGEX = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)|(?<!_)_(?!_)(.+?)(?<!_)_(?!_)""")

    // Strikethrough: ~~text~~
    private val STRIKETHROUGH_REGEX = Regex("""~~(.+?)~~""")

    // Inline code: `code`
    private val INLINE_CODE_REGEX = Regex("""`([^`]+?)`""")

    // Code block fences: ``` or ~~~
    private val CODE_FENCE_REGEX = Regex("""^(```|~~~).*$""", RegexOption.MULTILINE)

    // Wiki-links: [[target]] or [[target|display]]
    private val WIKI_LINK_REGEX = Regex("""\[\[([^\]]+?)\]\]""")

    // Links: [text](url)
    private val LINK_REGEX = Regex("""\[([^\]]+?)\]\(([^)]+?)\)""")

    // Images: ![alt](url)
    private val IMAGE_REGEX = Regex("""!\[([^\]]*?)\]\(([^)]+?)\)""")

    // Blockquote: lines starting with >
    private val BLOCKQUOTE_REGEX = Regex("""^>\s?(.*)$""", RegexOption.MULTILINE)

    // Task lists: - [ ] or - [x]
    private val TASK_REGEX = Regex("""^(\s*-\s\[[ xX]\])\s""", RegexOption.MULTILINE)

    // Horizontal rule: --- or *** or ___
    private val HR_REGEX = Regex("""^([-*_]{3,})\s*$""", RegexOption.MULTILINE)

    fun highlight(
        text: String,
        headingColor: Color = Color(0xFF1565C0),
        boldColor: Color = Color(0xFF1B1B1B),
        italicColor: Color = Color(0xFF555555),
        codeColor: Color = Color(0xFFD32F2F),
        codeBgColor: Color = Color(0x1AD32F2F),
        linkColor: Color = Color(0xFF0277BD),
        wikiLinkColor: Color = Color(0xFF6A1B9A),
        blockquoteColor: Color = Color(0xFF757575),
        taskColor: Color = Color(0xFF2E7D32),
        hrColor: Color = Color(0xFFBDBDBD),
        strikethroughColor: Color = Color(0xFF9E9E9E),
    ): AnnotatedString = buildAnnotatedString {
        append(text)

        // Headings
        HEADING_REGEX.findAll(text).forEach { match ->
            val level = match.groupValues[1].length
            val fontSize = when (level) {
                1 -> 24.sp; 2 -> 20.sp; 3 -> 18.sp
                else -> 16.sp
            }
            addStyle(
                SpanStyle(
                    color = headingColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                ),
                match.range.first, match.range.last + 1,
            )
        }

        // Bold
        BOLD_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = boldColor),
                match.range.first, match.range.last + 1,
            )
        }

        // Italic
        ITALIC_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(fontStyle = FontStyle.Italic, color = italicColor),
                match.range.first, match.range.last + 1,
            )
        }

        // Strikethrough
        STRIKETHROUGH_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough, color = strikethroughColor),
                match.range.first, match.range.last + 1,
            )
        }

        // Inline code
        INLINE_CODE_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(
                    color = codeColor,
                    fontFamily = FontFamily.Monospace,
                    background = codeBgColor,
                ),
                match.range.first, match.range.last + 1,
            )
        }

        // Code fences
        CODE_FENCE_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = codeColor, fontFamily = FontFamily.Monospace),
                match.range.first, match.range.last + 1,
            )
        }

        // Wiki-links
        WIKI_LINK_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(
                    color = wikiLinkColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                ),
                match.range.first, match.range.last + 1,
            )
        }

        // Images (before links to avoid overlap)
        IMAGE_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = linkColor, fontStyle = FontStyle.Italic),
                match.range.first, match.range.last + 1,
            )
        }

        // Links
        LINK_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                match.range.first, match.range.last + 1,
            )
        }

        // Blockquotes
        BLOCKQUOTE_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = blockquoteColor, fontStyle = FontStyle.Italic),
                match.range.first, match.range.last + 1,
            )
        }

        // Task list markers
        TASK_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = taskColor, fontWeight = FontWeight.Bold),
                match.range.first, match.groups[1]!!.range.last + 1,
            )
        }

        // Horizontal rules
        HR_REGEX.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = hrColor),
                match.range.first, match.range.last + 1,
            )
        }
    }
}
