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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A color of a terminal: an entry of the 256-color palette or an RGB color.
 *
 * The companion names the 16 basic colors, which every color terminal shows.
 *
 * @since 1.0.0
 */
public sealed interface Color {
    /**
     * An entry of the 256-color palette; 0 to 15 are the basic colors.
     *
     * @property index the entry, from 0 to 255.
     * @throws IllegalArgumentException if [index] lies outside 0 to 255.
     * @since 1.0.0
     */
    public class Indexed(public val index: Int) : Color {
        init {
            require(index in 0..MAX_COMPONENT) { "A palette index lies between 0 and 255, but was $index" }
        }

        /**
         * Returns whether [other] is the same entry.
         *
         * @param other the object to compare with.
         * @return `true` if both name the same entry.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Indexed && other.index == index

        /**
         * Returns the hash code of the entry.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = index

        /**
         * Shows the entry, such as `Indexed(9)`.
         *
         * @return the color as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Indexed($index)"
    }

    /**
     * A 24-bit RGB color.
     *
     * @property red the red component, from 0 to 255.
     * @property green the green component, from 0 to 255.
     * @property blue the blue component, from 0 to 255.
     * @throws IllegalArgumentException if a component lies outside 0 to 255.
     * @since 1.0.0
     */
    public class Rgb(public val red: Int, public val green: Int, public val blue: Int) : Color {
        init {
            require(red in 0..MAX_COMPONENT) { "The red component lies between 0 and 255, but was $red" }
            require(green in 0..MAX_COMPONENT) { "The green component lies between 0 and 255, but was $green" }
            require(blue in 0..MAX_COMPONENT) { "The blue component lies between 0 and 255, but was $blue" }
        }

        /**
         * Returns whether [other] is the same color.
         *
         * @param other the object to compare with.
         * @return `true` if all components are equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is Rgb && other.red == red && other.green == green && other.blue == blue

        /**
         * Returns the color packed into one number.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = red shl RED_SHIFT or (green shl GREEN_SHIFT) or blue

        /**
         * Shows the components, such as `Rgb(255, 128, 0)`.
         *
         * @return the color as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Rgb($red, $green, $blue)"
    }

    /**
     * The 16 basic colors and the parsing of hex colors.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val MAX_COMPONENT: Int = 255
        private const val HEX_DIGITS: Int = 6
        private const val HEX_RADIX: Int = 16
        private const val RED_SHIFT: Int = 16
        private const val GREEN_SHIFT: Int = 8
        private const val BYTE_MASK: Int = 0xFF
        private const val BRIGHT_RED: Int = 9
        private const val BRIGHT_GREEN: Int = 10
        private const val BRIGHT_YELLOW: Int = 11
        private const val BRIGHT_BLUE: Int = 12
        private const val BRIGHT_MAGENTA: Int = 13
        private const val BRIGHT_CYAN: Int = 14
        private const val BRIGHT_WHITE: Int = 15

        /**
         * Black, entry 0.
         *
         * @since 1.0.0
         */
        public val Black: Color = Indexed(0)

        /**
         * Red, entry 1.
         *
         * @since 1.0.0
         */
        public val Red: Color = Indexed(1)

        /**
         * Green, entry 2.
         *
         * @since 1.0.0
         */
        public val Green: Color = Indexed(2)

        /**
         * Yellow, entry 3.
         *
         * @since 1.0.0
         */
        public val Yellow: Color = Indexed(3)

        /**
         * Blue, entry 4.
         *
         * @since 1.0.0
         */
        public val Blue: Color = Indexed(4)

        /**
         * Magenta, entry 5.
         *
         * @since 1.0.0
         */
        public val Magenta: Color = Indexed(5)

        /**
         * Cyan, entry 6.
         *
         * @since 1.0.0
         */
        public val Cyan: Color = Indexed(6)

        /**
         * White, entry 7.
         *
         * @since 1.0.0
         */
        public val White: Color = Indexed(7)

        /**
         * Bright black, a gray, entry 8.
         *
         * @since 1.0.0
         */
        public val BrightBlack: Color = Indexed(8)

        /**
         * Bright red, entry 9.
         *
         * @since 1.0.0
         */
        public val BrightRed: Color = Indexed(BRIGHT_RED)

        /**
         * Bright green, entry 10.
         *
         * @since 1.0.0
         */
        public val BrightGreen: Color = Indexed(BRIGHT_GREEN)

        /**
         * Bright yellow, entry 11.
         *
         * @since 1.0.0
         */
        public val BrightYellow: Color = Indexed(BRIGHT_YELLOW)

        /**
         * Bright blue, entry 12.
         *
         * @since 1.0.0
         */
        public val BrightBlue: Color = Indexed(BRIGHT_BLUE)

        /**
         * Bright magenta, entry 13.
         *
         * @since 1.0.0
         */
        public val BrightMagenta: Color = Indexed(BRIGHT_MAGENTA)

        /**
         * Bright cyan, entry 14.
         *
         * @since 1.0.0
         */
        public val BrightCyan: Color = Indexed(BRIGHT_CYAN)

        /**
         * Bright white, entry 15.
         *
         * @since 1.0.0
         */
        public val BrightWhite: Color = Indexed(BRIGHT_WHITE)

        /**
         * Parses a hex color such as `#FF8800` or `ff8800`.
         *
         * @param text six hex digits, optionally after a `#`.
         * @return the RGB color.
         * @throws IllegalArgumentException if [text] is not six hex digits.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        public fun hex(text: String): Color {
            val digits = text.removePrefix("#")
            val isHex = digits.length == HEX_DIGITS && digits.all { character -> isHexDigit(character) }
            require(isHex) { "A hex color has six hex digits, but was ${SafeText.quoted(text)}" }
            val value = digits.toInt(HEX_RADIX)
            return Rgb(
                red = value shr RED_SHIFT and BYTE_MASK,
                green = value shr GREEN_SHIFT and BYTE_MASK,
                blue = value and BYTE_MASK,
            )
        }

        private fun isHexDigit(character: Char): Boolean {
            val lower = character.lowercaseChar()
            return character in '0'..'9' || lower in 'a'..'f'
        }
    }
}
