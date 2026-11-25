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

package com.davils.kheck.core.format.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val ESCAPE_DIGITS: Int = 4
private const val HEX_RADIX: Int = 16
private const val NULL: Int = 0x00
private const val ESCAPE: Int = 0x1B
private const val DELETE: Int = 0x7F
private const val NEXT_LINE: Int = 0x85
private const val OVERRIDE: Int = 0x202E
private const val ISOLATE: Int = 0x2066
private const val SEPARATOR: Int = 0x2028
private const val MARK: Int = 0x200F
private const val ARABIC_MARK: Int = 0x061C
private const val LONE_HIGH: Int = 0xD800
private const val SHORT: Int = 3
private const val BACKSLASH: String = "\\"

class SanitizingTest : FunSpec({
    context("forgeable characters") {
        test("keeps printable text as it is") {
            "rack 1 – ready ✓".sanitized() shouldBe "rack 1 – ready ✓"
        }

        test("names line breaks and tabs") {
            "a\nb\rc\td".sanitized() shouldBe "a${BACKSLASH}nb${BACKSLASH}rc${BACKSLASH}td"
        }

        test("escapes control, escape and delete characters") {
            val text = text(NULL, ESCAPE) + "[" + text(DELETE, NEXT_LINE)

            text.sanitized() shouldBe escaped(NULL) + escaped(ESCAPE) + "[" + escaped(DELETE) + escaped(NEXT_LINE)
        }

        test("escapes direction overrides and separators") {
            val codes = listOf(OVERRIDE, ISOLATE, SEPARATOR, MARK, ARABIC_MARK)

            val text = codes.joinToString(separator = "") { code -> Char(code).toString() }

            text.sanitized() shouldBe codes.joinToString(separator = "") { code -> escaped(code) }
        }

        test("keeps pairs of surrogates and escapes lone ones") {
            val emoji = "\uD83D\uDE00"
            val lone = Char(LONE_HIGH).toString()

            "a${emoji}b".sanitized() shouldBe "a${emoji}b"
            "a${lone}b".sanitized() shouldBe "a" + escaped(LONE_HIGH) + "b"
            lone.sanitized() shouldBe escaped(LONE_HIGH)
            "${lone}x".sanitized() shouldBe escaped(LONE_HIGH) + "x"
        }
    }

    context("length") {
        test("shortens long text and says how much it left out") {
            "abcdef".sanitized(maxLength = SHORT) shouldBe "abc…(+3 chars)"
            "ab\ncd".sanitized(maxLength = SHORT) shouldBe "ab${BACKSLASH}n…(+2 chars)"
        }
    }
})

private fun text(vararg codes: Int): String = codes.joinToString(separator = "") { code -> Char(code).toString() }

private fun escaped(code: Int): String {
    val hex = code.toString(HEX_RADIX).uppercase()
    return BACKSLASH + "u{" + hex.padStart(ESCAPE_DIGITS, '0') + "}"
}
