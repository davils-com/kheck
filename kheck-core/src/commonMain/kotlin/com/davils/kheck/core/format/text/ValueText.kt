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

@file:OptIn(DavilsInternal::class)

package com.davils.kheck.core.format.text

import com.davils.kheck.api.record.LogValue
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val QUOTE: Char = '"'
private const val NULL_TEXT: String = "null"

internal fun LogValue.rendered(): String {
    val out = StringBuilder()
    out.appendValue(this, isQuoted = false)
    return out.toString()
}

internal fun StringBuilder.appendValue(value: LogValue, isQuoted: Boolean): StringBuilder {
    when (value) {
        is LogValue.Text -> appendText(value.value, isQuoted)
        is LogValue.Integer -> append(value.value)
        is LogValue.Decimal -> append(value.value)
        is LogValue.Bool -> append(value.isTrue)
        is LogValue.Timestamp -> append(value.instant.toString())
        is LogValue.Elapsed -> append(value.duration.toString())
        LogValue.Secret -> append(SafeText.REDACTED)
        is LogValue.Items -> appendItems(value.values)
        is LogValue.Entries -> appendEntries(value.values)
        LogValue.Null -> append(NULL_TEXT)
    }
    return this
}

private fun StringBuilder.appendText(text: String, isQuoted: Boolean) {
    val isQuoteNeeded = isQuoted && text.isQuoteNeeded()
    if (!isQuoteNeeded) {
        appendSanitized(text)
        return
    }
    append(QUOTE)
    appendSanitized(text.replace(oldValue = "\"", newValue = "\\\""))
    append(QUOTE)
}

private fun StringBuilder.appendItems(values: List<LogValue>) {
    append('[')
    values.forEachIndexed { index, item ->
        if (index > 0) append(", ")
        appendValue(item, isQuoted = true)
    }
    append(']')
}

private fun StringBuilder.appendEntries(values: Map<String, LogValue>) {
    append('{')
    var index = 0
    values.forEach { (key, item) ->
        if (index > 0) append(", ")
        appendSanitized(key)
        append('=')
        appendValue(item, isQuoted = true)
        index++
    }
    append('}')
}

private fun String.isQuoteNeeded(): Boolean {
    if (isEmpty()) return true
    return any { character -> character.isWhitespace() || character == '=' || character == QUOTE }
}
