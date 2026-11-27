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

package com.davils.kheck.core.mask.engine

import com.davils.kheck.core.mask.Mask
import com.davils.kheck.core.mask.MaskingBuilder
import com.davils.kheck.core.mask.MaskingConfiguration
import com.davils.kheck.core.mask.MaskingRule
import com.davils.kheck.core.mask.fixture.secretKey
import com.davils.krypt.primitive.Crypto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun masker(block: MaskingBuilder.() -> Unit): Masker {
    val builder = MaskingBuilder()
    builder.block()
    return Masker(MaskingConfiguration(builder), Crypto()) { }
}

class MaskerEdgesTest : FunSpec({
    context("secret groups") {
        test("mask the whole match when the group took no part in it") {
            val rule = MaskingRule(name = "pin", pattern = Regex("pin(?:=(?<secret>\\d+))?"))
            val masker = masker {
                isDefaultsIncluded = false
                rule(rule)
            }

            masker.text("pin=1234") shouldBe "pin=****"
            masker.text("pin") shouldBe "****"
        }
    }

    context("hashes") {
        test("fall back to the mask once the secret key is destroyed") {
            val key = secretKey("a-key-of-16-bytes")
            val masker = masker {
                isDefaultsIncluded = false
                default = Mask.Hash
                hashKey = key
                rule(MaskingRule(name = "pin", pattern = Regex("\\d{4}")))
            }
            key.destroy()

            masker.text("1234") shouldBe "****"
        }
    }
})
