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

package com.davils.kheck.core.format.json.encoding

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.Limits
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith

private const val QUOTE: String = "\""
private const val BACKSLASH: String = "\\"
private const val EMOJI: String = "\uD83D\uDE00"

private fun written(block: JsonWriter.() -> Unit): String {
    val out = StringBuilder()
    JsonWriter(out).block()
    return out.toString()
}

class JsonWriterTest : FunSpec({
    context("strings") {
        test("escape quotes, backslashes and controls the short way and the rest as unicode") {
            val controls = "a" + QUOTE + "b" + BACKSLASH + "c\nd\re\tf\bg\u000Ch\u0001"

            written { string(controls) } shouldBe """"a\"b\\c\nd\re\tf\bg\fh\u0001""""
            written { string("x\u202Ey\u2028") } shouldBe """"x\u202ey\u2028""""
        }

        test("keep pairs of surrogates and escape lone ones") {
            written { string(EMOJI) } shouldBe QUOTE + EMOJI + QUOTE
            written { string(HostileFragments.LONE_HIGH_SURROGATE) } shouldBe """"\ud800""""
            written { string(HostileFragments.TAG_LATIN_A) } shouldBe """"\udb40\udc41""""
        }

        test("are safe to show for every hostile fragment") {
            HostileFragments.all.forEach { fragment -> isSafeToShow(written { string(fragment) }) shouldBe true }
        }

        test("shorten text above the limit") {
            written { string("a".repeat(Limits.MAX_TEXT_LENGTH + 2)) } shouldEndWith "a…(+2 chars)" + QUOTE
        }
    }

    context("values") {
        test("write numbers, absent fields and nested values") {
            written {
                beginObject()
                field(name = "absent", value = null as String?)
                field(name = "count", value = null as Long?)
                name("nan")
                number(Double.NaN)
                name("values")
                value(LogValue.Items(listOf(LogValue.Bool(true), LogValue.Entries(mapOf("k" to LogValue.Null)))))
                strings(name = "none", values = emptyList())
                fields(name = "empty", values = emptyMap())
                endObject()
            } shouldBe """{"nan":"NaN","values":[true,{"k":null}]}"""
        }
    }
})
