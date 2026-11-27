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
import com.davils.kheck.core.mask.MaskingRule
import com.davils.kheck.core.mask.fixture.CountingMasker
import com.davils.kheck.core.mask.fixture.secretKey
import com.davils.krypt.core.algorithm.MacAlgorithm
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val MESSAGE: String = "Hi There"
private const val GREETING: String = "greeting"
private const val SHA256_TAG: String = "hash:b0344c61d8db3853"
private const val SHA384_TAG: String = "hash:afd03944d8489562"
private val KEY: ByteArray = ByteArray(20) { 0x0b }

class HasherTest : FunSpec({
    context("RFC 4231, test case 1") {
        test("hashes with HMAC-SHA-256 under a secret key") {
            val masker = CountingMasker {
                isDefaultsIncluded = false
                default = Mask.Hash
                hashKey = secretKey(KEY)
                rule(MaskingRule(name = GREETING, pattern = Regex(MESSAGE)))
            }

            masker.text(MESSAGE) shouldBe SHA256_TAG
        }

        test("hashes with HMAC-SHA-384 when chosen") {
            val masker = CountingMasker {
                isDefaultsIncluded = false
                default = Mask.Hash
                hashKey = secretKey(KEY)
                algorithm = MacAlgorithm.HmacSha384
                rule(MaskingRule(name = GREETING, pattern = Regex(MESSAGE)))
            }

            masker.text(MESSAGE) shouldBe SHA384_TAG
        }
    }
})
