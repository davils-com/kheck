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

package com.davils.kheck.core.mask.check

import com.davils.kheck.core.mask.MaskingValidator

internal object Iban : MaskingValidator {
    private const val MIN_LENGTH: Int = 15
    private const val MAX_LENGTH: Int = 34
    private const val HEADER: Int = 4
    private const val MODULUS: Int = 97
    private const val DECIMAL: Int = 10
    private const val LETTER_OFFSET: Int = 10

    override fun isMatch(candidate: String): Boolean {
        val compact = candidate.filterNot(Char::isWhitespace)
        if (compact.length !in MIN_LENGTH..MAX_LENGTH) return false
        val rearranged = compact.drop(HEADER) + compact.take(HEADER)
        val remainder = rearranged.fold(0) { remainder, character -> folded(remainder, character) }
        return remainder == 1
    }

    private fun folded(remainder: Int, character: Char): Int {
        if (character.isDigit()) return (remainder * DECIMAL + character.digitToInt()) % MODULUS
        val value = character.uppercaseChar() - 'A' + LETTER_OFFSET
        return (remainder * DECIMAL * DECIMAL + value) % MODULUS
    }
}
