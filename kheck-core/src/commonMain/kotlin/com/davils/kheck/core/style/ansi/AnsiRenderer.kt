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

package com.davils.kheck.core.style.ansi

import com.davils.kheck.core.style.Color
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.TextStyle

internal class AnsiRenderer(private val depth: ColorDepth) {
    private val sequences = HashMap<TextStyle, String>()

    val isActive: Boolean get() = depth != ColorDepth.None

    fun open(style: TextStyle): String {
        if (!isActive) return ""
        return sequences.getOrPut(style) { sequenceOf(style) }
    }

    fun close(style: TextStyle): String {
        if (open(style).isEmpty()) return ""
        return RESET
    }

    private fun sequenceOf(style: TextStyle): String {
        val codes = buildList {
            if (style.isBold) add(BOLD)
            if (style.isDim) add(DIM)
            if (style.isItalic) add(ITALIC)
            if (style.isUnderline) add(UNDERLINE)
            if (style.isStrikethrough) add(STRIKETHROUGH)
            style.foreground?.let { color -> addAll(colorCodes(color, isBackground = false)) }
            style.background?.let { color -> addAll(colorCodes(color, isBackground = true)) }
        }
        if (codes.isEmpty()) return ""
        return codes.joinToString(separator = ";", prefix = PREFIX, postfix = SUFFIX)
    }

    private fun colorCodes(color: Color, isBackground: Boolean): List<String> {
        val channel = extendedChannel(isBackground)
        return when (val reduced = reduced(color)) {
            is Color.Rgb -> listOf(channel, TRUE_COLOR, "${reduced.red}", "${reduced.green}", "${reduced.blue}")
            is Color.Indexed -> indexedCodes(reduced.index, channel, isBackground)
        }
    }

    private fun indexedCodes(index: Int, channel: String, isBackground: Boolean): List<String> {
        if (index >= BASIC_COLORS) return listOf(channel, INDEXED, index.toString())
        return listOf(basicCode(index, isBackground).toString())
    }

    private fun reduced(color: Color): Color = when (color) {
        is Color.Rgb -> reducedRgb(color)
        is Color.Indexed -> reducedIndexed(color)
    }

    private fun reducedRgb(color: Color.Rgb): Color {
        if (depth == ColorDepth.TrueColor) return color
        return Color.Indexed(paletteIndex(color))
    }

    private fun paletteIndex(color: Color.Rgb): Int {
        if (depth == ColorDepth.Ansi256) return Palette.nearestIndexed(color)
        return Palette.nearestBasic(color)
    }

    private fun reducedIndexed(color: Color.Indexed): Color {
        val isKept = color.index < BASIC_COLORS || depth != ColorDepth.Ansi16
        if (isKept) return color
        return Color.Indexed(Palette.nearestBasic(Palette.extendedRgb(color.index)))
    }

    private fun extendedChannel(isBackground: Boolean): String {
        if (isBackground) return BACKGROUND_EXTENDED
        return FOREGROUND_EXTENDED
    }

    private fun basicCode(index: Int, isBackground: Boolean): Int {
        val isBright = index >= BRIGHT_START
        return basicBase(isBright, isBackground) + index % BRIGHT_START
    }

    private fun basicBase(isBright: Boolean, isBackground: Boolean): Int = BASES.getValue(isBright to isBackground)

    internal companion object {
        const val RESET: String = "\u001B[0m"
        private const val PREFIX: String = "\u001B["
        private const val SUFFIX: String = "m"
        private const val BOLD: String = "1"
        private const val DIM: String = "2"
        private const val ITALIC: String = "3"
        private const val UNDERLINE: String = "4"
        private const val STRIKETHROUGH: String = "9"
        private const val FOREGROUND_EXTENDED: String = "38"
        private const val BACKGROUND_EXTENDED: String = "48"
        private const val TRUE_COLOR: String = "2"
        private const val INDEXED: String = "5"
        private const val BASIC_COLORS: Int = 16
        private const val BRIGHT_START: Int = 8
        private const val NORMAL_FOREGROUND: Int = 30
        private const val BRIGHT_FOREGROUND: Int = 90
        private const val NORMAL_BACKGROUND: Int = 40
        private const val BRIGHT_BACKGROUND: Int = 100
        private val BASES: Map<Pair<Boolean, Boolean>, Int> = mapOf(
            Pair(first = false, second = false) to NORMAL_FOREGROUND,
            Pair(first = true, second = false) to BRIGHT_FOREGROUND,
            Pair(first = false, second = true) to NORMAL_BACKGROUND,
            Pair(first = true, second = true) to BRIGHT_BACKGROUND,
        )
    }
}
