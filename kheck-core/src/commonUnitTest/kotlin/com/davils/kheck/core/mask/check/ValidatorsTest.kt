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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val TOO_MANY_DIGITS: Int = 32

class ValidatorsTest : FunSpec({
    context("Luhn") {
        test("accepts a valid number and refuses wrong lengths") {
            Luhn.isMatch("4111-1111-1111-1111") shouldBe true
            Luhn.isMatch("411111111111") shouldBe false
            Luhn.isMatch("41111111111111111111") shouldBe false
        }
    }

    context("IBAN") {
        test("accepts a valid IBAN in lower case and refuses wrong lengths") {
            Iban.isMatch("gb82 west 1234 5698 7654 32") shouldBe true
            Iban.isMatch("DE89370400") shouldBe false
            Iban.isMatch("DE89" + "0".repeat(TOO_MANY_DIGITS)) shouldBe false
        }
    }
})
