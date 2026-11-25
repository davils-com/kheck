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

package com.davils.kheck.core.terminal

import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.ColorMode

internal class ColorDepthDetection(
    private val environment: Map<String, String>,
    private val terminal: Terminal,
    private val isWindows: Boolean,
) {
    fun resolve(mode: ColorMode, target: ConsoleTarget): ColorDepth = when (mode) {
        ColorMode.Never -> ColorDepth.None
        ColorMode.Always -> maxOf(a = detected(target), b = ColorDepth.Ansi16)
        ColorMode.Auto -> detected(target)
    }

    fun detected(target: ConsoleTarget): ColorDepth {
        val isDisabled = !environment[NO_COLOR].isNullOrEmpty()
        if (isDisabled) return ColorDepth.None
        return forced() ?: interactive(target)
    }

    private fun forced(): ColorDepth? {
        val value = environment[FORCE_COLOR] ?: return null
        return FORCED_LEVELS[value.lowercase()] ?: ColorDepth.Ansi16
    }

    private fun interactive(target: ConsoleTarget): ColorDepth {
        val term = environment[TERM]
        val isPlain = !terminal.isInteractive(target) || term == DUMB
        if (isPlain) return ColorDepth.None
        return trueColor() ?: palette(term)
    }

    private fun trueColor(): ColorDepth? {
        val colorTerm = environment[COLOR_TERM]?.lowercase()
        val isTrueColorTerm = colorTerm in TRUE_COLOR_TERMS
        val isTrueColorProgram = environment[TERM_PROGRAM] in TRUE_COLOR_PROGRAMS
        val isWindowsTerminal = environment[WINDOWS_TERMINAL] != null
        val isTrueColor = isTrueColorTerm || isTrueColorProgram || isWindowsTerminal
        if (!isTrueColor) return null
        return ColorDepth.TrueColor
    }

    private fun palette(term: String?): ColorDepth {
        if (term?.contains(EXTENDED_PALETTE) == true) return ColorDepth.Ansi256
        return basic(term)
    }

    private fun basic(term: String?): ColorDepth {
        val isLegacyConsole = isWindows && term == null
        if (isLegacyConsole) return ColorDepth.None
        return ColorDepth.Ansi16
    }

    private companion object {
        private const val NO_COLOR: String = "NO_COLOR"
        private const val FORCE_COLOR: String = "FORCE_COLOR"
        private const val TERM: String = "TERM"
        private const val COLOR_TERM: String = "COLORTERM"
        private const val TERM_PROGRAM: String = "TERM_PROGRAM"
        private const val WINDOWS_TERMINAL: String = "WT_SESSION"
        private const val DUMB: String = "dumb"
        private const val EXTENDED_PALETTE: String = "256color"
        private val TRUE_COLOR_TERMS: Set<String> = setOf("truecolor", "24bit")
        private val TRUE_COLOR_PROGRAMS: Set<String> = setOf("iTerm.app", "WezTerm", "vscode", "ghostty")
        private val FORCED_LEVELS: Map<String, ColorDepth> = mapOf(
            "0" to ColorDepth.None,
            "false" to ColorDepth.None,
            "2" to ColorDepth.Ansi256,
            "3" to ColorDepth.TrueColor,
        )
    }
}
