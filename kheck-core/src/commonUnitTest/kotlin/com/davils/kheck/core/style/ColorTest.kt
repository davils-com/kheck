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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val OUT_OF_RANGE: Int = 256
private const val BLACK: Int = 0
private const val WHITE: Int = 7
private const val BRIGHT_BLUE: Int = 12
private const val BRIGHT_WHITE: Int = 15

class ColorTest : FunSpec({
    context("hex") {
        test("reads six hex digits with or without hash") {
            Color.hex("#1E90FF") shouldBe Color.Rgb(red = 30, green = 144, blue = 255)
            Color.hex("000000") shouldBe Color.Rgb(red = 0, green = 0, blue = 0)
        }

        test("refuses a wrong length or a letter that is no hex digit") {
            shouldThrow<IllegalArgumentException> { Color.hex("#FFF") }
            shouldThrow<IllegalArgumentException> { Color.hex("#GGGGGG") }
        }
    }

    context("ranges") {
        test("refuses components and indices outside of a byte") {
            shouldThrow<IllegalArgumentException> { Color.Indexed(OUT_OF_RANGE) }
            shouldThrow<IllegalArgumentException> { Color.Indexed(-1) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = -1, green = 0, blue = 0) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = 0, green = OUT_OF_RANGE, blue = 0) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = 0, green = 0, blue = -1) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = OUT_OF_RANGE, green = 0, blue = 0) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = 0, green = -1, blue = 0) }
            shouldThrow<IllegalArgumentException> { Color.Rgb(red = 0, green = 0, blue = OUT_OF_RANGE) }
        }

        test("names the sixteen colors of every terminal") {
            val named = listOf(Color.Black, Color.White, Color.BrightBlue, Color.BrightWhite)

            named.map { color -> (color as Color.Indexed).index } shouldBe listOf(
                BLACK,
                WHITE,
                BRIGHT_BLUE,
                BRIGHT_WHITE,
            )
        }
    }

    context("styles") {
        test("builds a text style") {
            textStyle {
                foreground = Color.Red
                isBold = true
            } shouldBe TextStyle(foreground = Color.Red, isBold = true)
        }

        test("inherits the base style in a level style") {
            val style = segmentStyle {
                isBold = true
                byLevel { error { foreground = Color.Red } }
            }

            style.levels shouldBe mapOf(LogLevel.Error to TextStyle(foreground = Color.Red, isBold = true))
        }
    }
})
