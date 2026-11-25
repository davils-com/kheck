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

import com.davils.kheck.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val HEX_RADIX: Int = 16
private const val HEX_WIDTH: Int = 4
private const val SUPPLEMENTARY_START: Int = 0x10000
private const val HIGH_SURROGATE_SHIFT: Int = 10
private const val LOW_SURROGATE_MASK: Int = 0x3FF
private const val SHORTENED_PREFIX: String = "…(+"
private const val SHORTENED_SUFFIX: String = " chars)"
private val NAMED_ESCAPES: Map<Int, String> = mapOf('\n'.code to "\\n", '\r'.code to "\\r", '\t'.code to "\\t")

internal fun String.sanitized(maxLength: Int = Limits.MAX_TEXT_LENGTH): String {
    val out = StringBuilder(length.coerceAtMost(maxLength))
    out.appendSanitized(this, maxLength)
    return out.toString()
}

internal fun StringBuilder.appendSanitized(
    text: CharSequence,
    maxLength: Int = Limits.MAX_TEXT_LENGTH,
): StringBuilder {
    val isClean = text.length <= maxLength && text.all { character -> isPlain(character) }
    if (isClean) return append(text)
    appendEscaped(text, maxLength)
    return this
}

private fun StringBuilder.appendEscaped(text: CharSequence, maxLength: Int) {
    val start = length
    var index = 0
    while (index < text.length) {
        if (length - start >= maxLength) {
            shortened(text.length - index)
            return
        }
        index = appendCodePointAt(text, index)
    }
}

internal fun StringBuilder.appendEscapedCodePoint(codePoint: Int) {
    val named = namedEscape(codePoint)
    if (named != null) {
        append(named)
        return
    }
    append("\\u{")
    val hex = codePoint.toString(HEX_RADIX).uppercase()
    append(hex.padStart(HEX_WIDTH, '0'))
    append('}')
}

internal fun codePointAt(text: CharSequence, index: Int): Int {
    val high = text[index]
    val hasPair = high.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
    if (!hasPair) return high.code
    val low = text[index + 1]
    val highOffset = high.code - Char.MIN_HIGH_SURROGATE.code
    val upper = highOffset shl HIGH_SURROGATE_SHIFT
    val lowOffset = low.code - Char.MIN_LOW_SURROGATE.code
    return SUPPLEMENTARY_START + upper + (lowOffset and LOW_SURROGATE_MASK)
}

@OptIn(DavilsInternal::class)
internal fun isSafeCodePoint(codePoint: Int): Boolean {
    val isSurrogate = codePoint in Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code
    return !isSurrogate && SafeText.isSafe(codePoint)
}

private fun isPlain(character: Char): Boolean {
    val isAscii = character.code in ' '.code..'~'.code
    val isSafe = !character.isSurrogate() && isSafeCodePoint(character.code)
    return isAscii || isSafe
}

private fun StringBuilder.appendCodePointAt(text: CharSequence, index: Int): Int {
    val codePoint = codePointAt(text, index)
    val width = widthOf(codePoint)
    if (isSafeCodePoint(codePoint)) {
        append(text, index, index + width)
        return index + width
    }
    appendEscapedCodePoint(codePoint)
    return index + width
}

private fun StringBuilder.shortened(remaining: Int): StringBuilder {
    append(SHORTENED_PREFIX)
    append(remaining)
    append(SHORTENED_SUFFIX)
    return this
}

private fun namedEscape(codePoint: Int): String? = NAMED_ESCAPES[codePoint]

internal fun widthOf(codePoint: Int): Int {
    if (codePoint >= SUPPLEMENTARY_START) return 2
    return 1
}
