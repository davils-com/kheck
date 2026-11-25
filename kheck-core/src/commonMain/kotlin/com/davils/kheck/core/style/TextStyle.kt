/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kheck.core.style

/**
 * How a piece of text is drawn on a terminal: colors and attributes.
 *
 * Styles are written only to a terminal with colors; every other sink writes the text alone.
 *
 * @property foreground the color of the text, or `null` for the color of the terminal.
 * @property background the color behind the text, or `null` for the color of the terminal.
 * @property isBold whether the text is bold.
 * @property isDim whether the text is dimmed.
 * @property isItalic whether the text is italic.
 * @property isUnderline whether the text is underlined.
 * @property isStrikethrough whether the text is struck through.
 * @since 1.0.0
 */
public class TextStyle(
    public val foreground: Color? = null,
    public val background: Color? = null,
    public val isBold: Boolean = false,
    public val isDim: Boolean = false,
    public val isItalic: Boolean = false,
    public val isUnderline: Boolean = false,
    public val isStrikethrough: Boolean = false,
) {
    private val flags: Int
        get() {
            val attributes = listOf(isBold, isDim, isItalic, isUnderline, isStrikethrough)
            return attributes.foldIndexed(0) { index, packed, flag -> packed or (flag.bit() shl index) }
        }

    private fun Boolean.bit(): Int {
        if (this) return 1
        return 0
    }

    /**
     * Returns whether [other] draws text the same way.
     *
     * @param other the object to compare with.
     * @return `true` if colors and attributes are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is TextStyle) return false
        return foreground == other.foreground && background == other.background && flags == other.flags
    }

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int {
        var result = foreground?.hashCode() ?: 0
        result = HASH_FACTOR * result + (background?.hashCode() ?: 0)
        return HASH_FACTOR * result + flags
    }

    /**
     * Shows the colors and the attributes that are set.
     *
     * @return the style as text.
     * @since 1.0.0
     */
    override fun toString(): String {
        val attributes = listOf(
            BOLD to isBold,
            DIM to isDim,
            ITALIC to isItalic,
            UNDERLINE to isUnderline,
            STRIKETHROUGH to isStrikethrough,
        )
        val shown = attributes.filter { (_, isSet) -> isSet }.map { (name, _) -> name }
        return "TextStyle(foreground=${foreground ?: NONE_COLOR}, background=${background ?: NONE_COLOR}, " +
            "attributes=$shown)"
    }

    /**
     * The style that draws nothing special.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val HASH_FACTOR: Int = 31
        private const val NONE_COLOR: String = "none"
        private const val BOLD: String = "bold"
        private const val DIM: String = "dim"
        private const val ITALIC: String = "italic"
        private const val UNDERLINE: String = "underline"
        private const val STRIKETHROUGH: String = "strikethrough"

        /**
         * No color and no attribute.
         *
         * @since 1.0.0
         */
        public val NONE: TextStyle = TextStyle()
    }
}
