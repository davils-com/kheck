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
import com.davils.kheck.testing.FakeTerminal
import com.davils.knot.environment.environment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val OUTPUT: ConsoleTarget = ConsoleTarget.StandardOutput
private const val FORCE_COLOR: String = "FORCE_COLOR"
private const val TERM: String = "TERM"
private const val COLOR_TERM: String = "COLORTERM"

class ColorDepthDetectionTest : FunSpec({
    context("environment") {
        test("turns color off for NO_COLOR") {
            detected(mapOf("NO_COLOR" to "1", COLOR_TERM to "truecolor")) shouldBe ColorDepth.None
        }

        test("follows FORCE_COLOR even without a terminal") {
            detected(mapOf(FORCE_COLOR to "3"), isInteractive = false) shouldBe ColorDepth.TrueColor
            detected(mapOf(FORCE_COLOR to "2"), isInteractive = false) shouldBe ColorDepth.Ansi256
            detected(mapOf(FORCE_COLOR to "1"), isInteractive = false) shouldBe ColorDepth.Ansi16
            detected(mapOf(FORCE_COLOR to "false")) shouldBe ColorDepth.None
        }

        test("writes plain text when the output is no terminal or a dumb one") {
            detected(mapOf(TERM to "xterm-256color"), isInteractive = false) shouldBe ColorDepth.None
            detected(mapOf(TERM to "dumb")) shouldBe ColorDepth.None
        }

        test("recognises terminals with true color") {
            detected(mapOf(COLOR_TERM to "24bit")) shouldBe ColorDepth.TrueColor
            detected(mapOf("TERM_PROGRAM" to "WezTerm")) shouldBe ColorDepth.TrueColor
            detected(mapOf("WT_SESSION" to "1")) shouldBe ColorDepth.TrueColor
        }

        test("recognises the palette of 256 colors and falls back to 16 colors") {
            detected(mapOf(TERM to "screen-256color")) shouldBe ColorDepth.Ansi256
            detected(mapOf(TERM to "xterm")) shouldBe ColorDepth.Ansi16
            detected(emptyMap()) shouldBe ColorDepth.Ansi16
        }

        test("writes plain text to the legacy Windows console") {
            detected(emptyMap(), isWindows = true) shouldBe ColorDepth.None
            detected(mapOf(TERM to "cygwin"), isWindows = true) shouldBe ColorDepth.Ansi16
        }
    }

    context("modes") {
        test("never colors in mode never and always colors in mode always") {
            val detection = detection(emptyMap(), isInteractive = false)

            detection.resolve(ColorMode.Never, OUTPUT) shouldBe ColorDepth.None
            detection.resolve(ColorMode.Always, OUTPUT) shouldBe ColorDepth.Ansi16
            detection.resolve(ColorMode.Auto, OUTPUT) shouldBe ColorDepth.None
        }
    }
})

private fun detection(
    variables: Map<String, String>,
    isInteractive: Boolean = true,
    isWindows: Boolean = false,
): ColorDepthDetection {
    val terminal = FakeTerminal(setOfNotNull(OUTPUT.takeIf { isInteractive }))
    val environment = environment { variables(variables) }
    return ColorDepthDetection(environment = environment, terminal = terminal, isWindows = isWindows)
}

private fun detected(
    variables: Map<String, String>,
    isInteractive: Boolean = true,
    isWindows: Boolean = false,
): ColorDepth = detection(variables, isInteractive, isWindows).detected(OUTPUT)
