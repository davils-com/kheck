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

package com.davils.kheck.core.mask

import com.davils.kore.testing.isSafeToShow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val KEPT: Int = 2

private const val DIGITS: String = "digits"
private const val NUMBERS: String = "[0-9]+"

class MaskValuesTest : FunSpec({
    context("masks") {
        test("compare by content and show their names") {
            Mask.Partial(keepStart = KEPT, keepEnd = 1) shouldBe Mask.Partial(keepStart = KEPT, keepEnd = 1)
            Mask.Partial(keepStart = KEPT, keepEnd = 1).hashCode() shouldBe
                Mask.Partial(keepStart = KEPT, keepEnd = 1).hashCode()
            Mask.Partial(keepStart = KEPT, keepEnd = 1) shouldNotBe Mask.Partial(keepStart = 1, keepEnd = 1)
            Mask.Partial(keepStart = KEPT, keepEnd = 1) shouldNotBe Mask.Partial(keepStart = KEPT, keepEnd = KEPT)
            Mask.Partial(keepStart = KEPT, keepEnd = 1) shouldNotBe Mask.Full
            Mask.Fixed("x") shouldBe Mask.Fixed("x")
            Mask.Fixed("x").hashCode() shouldBe "x".hashCode()
            Mask.Fixed("x") shouldNotBe Mask.Remove
            listOf(Mask.Full, Mask.Hash, Mask.Remove).map(Mask::toString) shouldBe listOf("Full", "Hash", "Remove")
            Mask.Partial(keepStart = KEPT, keepEnd = 1).toString() shouldBe "Partial(keepStart=2, keepEnd=1)"
            isSafeToShow(Mask.Fixed("\u001B[2J").toString()) shouldBe true
        }
    }

    context("rules") {
        test("show only their name") {
            MaskingRules.EMAIL.toString() shouldBe "MaskingRule(name=email)"
        }
    }

    context("equality") {
        test("compares rules by name, pattern, mask, hints and validator") {
            val rule = MaskingRule(name = DIGITS, pattern = Regex(NUMBERS), hints = listOf("1"))
            val same = MaskingRule(name = DIGITS, pattern = Regex(NUMBERS), hints = listOf("1"))

            rule shouldBe same
            rule.hashCode() shouldBe same.hashCode()
            rule shouldNotBe MaskingRule(name = DIGITS, pattern = Regex(NUMBERS, RegexOption.IGNORE_CASE))
            rule shouldNotBe MaskingRule(name = DIGITS, pattern = Regex("[0-9]*"), hints = listOf("1"))
        }
    }

    context("foreign values") {
        test("are never equal to a value of another type") {
            MaskingRules.EMAIL.equals(DIGITS) shouldBe false
        }
    }
})
