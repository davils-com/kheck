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
import com.davils.kheck.core.format.text.codePointAt
import com.davils.kheck.core.format.text.isSafeCodePoint
import com.davils.kheck.core.format.text.widthOf
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

@OptIn(DavilsInternal::class)
internal class JsonWriter(private val out: StringBuilder) {
    private val firsts = ArrayList<Boolean>()

    fun beginObject() {
        separated()
        out.append('{')
        firsts += true
    }

    fun endObject() {
        firsts.removeAt(firsts.lastIndex)
        out.append('}')
    }

    fun beginArray() {
        separated()
        out.append('[')
        firsts += true
    }

    fun endArray() {
        firsts.removeAt(firsts.lastIndex)
        out.append(']')
    }

    fun name(name: String) {
        separated()
        quoted(name)
        out.append(':')
        firsts[firsts.lastIndex] = true
    }

    fun string(value: String) {
        separated()
        quoted(value)
    }

    fun number(value: Long) {
        separated()
        out.append(value)
    }

    fun number(value: Double) {
        if (!value.isFinite()) {
            string(value.toString())
            return
        }
        separated()
        out.append(value)
    }

    fun bool(value: Boolean) {
        separated()
        out.append(value)
    }

    fun nullValue() {
        separated()
        out.append(NULL)
    }

    fun value(value: LogValue) {
        when (value) {
            is LogValue.Text -> string(value.value)
            is LogValue.Integer -> number(value.value)
            is LogValue.Decimal -> number(value.value)
            is LogValue.Bool -> bool(value.isTrue)
            is LogValue.Timestamp -> string(value.instant.toString())
            is LogValue.Elapsed -> string(value.duration.toIsoString())
            LogValue.Secret -> string(SafeText.REDACTED)
            is LogValue.Items -> items(value.values)
            is LogValue.Entries -> entries(value.values)
            LogValue.Null -> nullValue()
        }
    }

    fun entries(values: Map<String, LogValue>) {
        beginObject()
        values.forEach { (name, item) ->
            name(name)
            value(item)
        }
        endObject()
    }

    fun field(name: String, value: String?) {
        val text = value ?: return
        name(name)
        string(text)
    }

    fun field(name: String, value: Long?) {
        val number = value ?: return
        name(name)
        number(number)
    }

    fun fields(name: String, values: Map<String, LogValue>) {
        if (values.isEmpty()) return
        name(name)
        entries(values)
    }

    fun strings(name: String, values: List<String>) {
        if (values.isEmpty()) return
        name(name)
        beginArray()
        values.forEach(::string)
        endArray()
    }

    private fun items(values: List<LogValue>) {
        beginArray()
        values.forEach(::value)
        endArray()
    }

    private fun separated() {
        if (firsts.isEmpty()) return
        val index = firsts.lastIndex
        if (firsts[index]) {
            firsts[index] = false
            return
        }
        out.append(',')
    }

    private fun quoted(text: String) {
        out.append('"')
        escaped(text)
        out.append('"')
    }

    private fun escaped(text: String) {
        val start = out.length
        var index = 0
        while (index < text.length) {
            if (out.length - start >= Limits.MAX_TEXT_LENGTH) {
                out.append("…(+")
                out.append(text.length - index)
                out.append(" chars)")
                return
            }
            val codePoint = codePointAt(text, index)
            val width = widthOf(codePoint)
            appendCodePoint(text = text, index = index, codePoint = codePoint, width = width)
            index += width
        }
    }

    private fun appendCodePoint(text: String, index: Int, codePoint: Int, width: Int) {
        val short = SHORT_ESCAPES[codePoint]
        if (short != null) {
            out.append(short)
            return
        }
        if (isSafeCodePoint(codePoint)) {
            out.append(text, index, index + width)
            return
        }
        for (offset in 0 until width) unicodeEscape(text[index + offset].code)
    }

    private fun unicodeEscape(code: Int) {
        out.append("\\u")
        out.append(code.toString(HEX_RADIX).padStart(HEX_WIDTH, '0'))
    }

    private companion object {
        private const val NULL: String = "null"
        private const val HEX_RADIX: Int = 16
        private const val HEX_WIDTH: Int = 4
        private val SHORT_ESCAPES: Map<Int, String> = mapOf(
            '"'.code to "\\\"",
            '\\'.code to "\\\\",
            '\n'.code to "\\n",
            '\r'.code to "\\r",
            '\t'.code to "\\t",
            '\b'.code to "\\b",
            '\u000C'.code to "\\f",
        )
    }
}
