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

import com.davils.kheck.api.LogLevel
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val GREEN: Int = 128

class StyleValuesTest : FunSpec({
    context("colors") {
        test("compare by content and show it") {
            Color.Rgb(red = 1, green = 2, blue = 3) shouldBe Color.Rgb(red = 1, green = 2, blue = 3)
            Color.Rgb(red = 1, green = 2, blue = 3) shouldNotBe Color.Rgb(red = 1, green = 2, blue = 4)
            Color.Rgb(red = 1, green = 2, blue = 3) shouldNotBe Color.Rgb(red = 1, green = 3, blue = 3)
            Color.Rgb(red = 1, green = 2, blue = 3) shouldNotBe Color.Rgb(red = 2, green = 2, blue = 3)
            Color.Rgb(red = 1, green = 2, blue = 3) shouldNotBe Color.Red
            Color.Red shouldNotBe Color.Rgb(red = 1, green = 2, blue = 3)
            Color.Red.hashCode() shouldBe Color.Indexed(1).hashCode()
            Color.Red.equals("red") shouldBe false
            Color.hex("ABCDEF") shouldBe Color.hex("#abcdef")
            Color.Rgb(red = 255, green = GREEN, blue = 0).toString() shouldBe "Rgb(255, 128, 0)"
            Color.BrightRed.toString() shouldBe "Indexed(9)"
        }

        test("refuse components out of range") {
            shouldThrow<IllegalArgumentException> { Color.Indexed(-1) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = 256, green = 0, blue = 0) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = 0, green = 256, blue = 0) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = 0, green = 0, blue = 256) }
        }

        test("parse hex and refuse anything else with an escaped message") {
            Color.hex("#ff8000") shouldBe Color.Rgb(red = 255, green = GREEN, blue = 0)
            Color.hex("FF8000") shouldBe Color.Rgb(red = 255, green = GREEN, blue = 0)
            shouldThrow<IllegalArgumentException> { Color.hex("#ff800") }
            shouldThrow<IllegalArgumentException> { Color.hex("#gg8000") }
            val failure = shouldThrow<IllegalArgumentException> { Color.hex("\u001B[2J123") }
            isSafeToShow(failure.message.orEmpty()) shouldBe true
        }
    }

    context("text styles") {
        test("compare colors and attributes and show the attributes that are set") {
            val style = textStyle {
                foreground = Color.Red
                background = Color.Blue
                isBold = true
                isDim = true
                isItalic = true
                isUnderline = true
                isStrikethrough = true
            }
            val same = TextStyle(
                foreground = Color.Red,
                background = Color.Blue,
                isBold = true,
                isDim = true,
                isItalic = true,
                isUnderline = true,
                isStrikethrough = true,
            )

            style shouldBe same
            style.hashCode() shouldBe same.hashCode()
            style shouldNotBe TextStyle.NONE
            style shouldNotBe TextStyle(foreground = Color.Red)
            TextStyle(foreground = Color.Red) shouldNotBe TextStyle(background = Color.Red)
            style.equals("style") shouldBe false
            style.toString() shouldContain "attributes=[bold, dim, italic, underline, strikethrough]"
            TextStyle.NONE.toString() shouldBe "TextStyle(foreground=none, background=none, attributes=[])"
        }
    }

    context("segment styles") {
        test("pick the style of a level and fall back to the base") {
            val style = segmentStyle {
                isBold = true
                byLevel {
                    trace { isDim = true }
                    debug { isDim = true }
                    info { isItalic = true }
                    warn { foreground = Color.Yellow }
                    error { foreground = Color.Red }
                }
            }

            style.styleFor(LogLevel.Error).foreground shouldBe Color.Red
            style.styleFor(LogLevel.Error).isBold shouldBe true
            style.levels.keys shouldBe LogLevel.entries.toSet() - LogLevel.Off
            SegmentStyle.NONE.styleFor(LogLevel.Info) shouldBe TextStyle.NONE
            style shouldBe segmentStyle {
                isBold = true
                byLevel {
                    trace { isDim = true }
                    debug { isDim = true }
                    info { isItalic = true }
                    warn { foreground = Color.Yellow }
                    error { foreground = Color.Red }
                }
            }
            style.hashCode() shouldBe style.hashCode()
            style shouldNotBe SegmentStyle.NONE
            SegmentStyle(base = style.base) shouldNotBe style
            style.equals("style") shouldBe false
            style.toString() shouldContain "levels=[Trace, Debug, Info, Warn, Error]"
        }
    }
})
