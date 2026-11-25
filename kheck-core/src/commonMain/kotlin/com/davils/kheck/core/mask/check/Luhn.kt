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

internal object Luhn : MaskingValidator {
    private const val MIN_DIGITS: Int = 13
    private const val MAX_DIGITS: Int = 19
    private const val DECIMAL: Int = 10

    override fun isMatch(candidate: String): Boolean {
        val digits = candidate.filter(Char::isDigit)
        if (digits.length !in MIN_DIGITS..MAX_DIGITS) return false
        val reversed = digits.reversed()
        val sum = reversed.withIndex().sumOf { (index, character) -> weighted(index, character.digitToInt()) }
        return sum % DECIMAL == 0
    }

    private fun weighted(index: Int, digit: Int): Int {
        if (index % 2 == 0) return digit
        val doubled = digit * 2
        return doubled / DECIMAL + doubled % DECIMAL
    }
}
