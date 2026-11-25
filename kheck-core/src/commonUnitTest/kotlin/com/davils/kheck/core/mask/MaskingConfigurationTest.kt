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

import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kheck.core.mask.fixture.maskingConfiguration
import com.davils.kheck.core.mask.fixture.maskingReport
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val TOO_SHORT: Int = 10
private const val TOO_DEEP: Int = 65

class MaskingConfigurationTest : FunSpec({
    context("validation") {
        test("accepts the defaults") {
            maskingReport { }.isValid shouldBe true
        }

        test("refuses limits that are too small or too large") {
            val report = maskingReport {
                maxValueLength = TOO_SHORT
                maxDepth = TOO_DEEP
            }

            report.errors.map { violation -> violation.code } shouldBe listOf(
                LoggingViolationCodes.INVALID_SCAN_LENGTH,
                LoggingViolationCodes.INVALID_DEPTH,
            )
        }

        test("refuses a depth of zero") {
            maskingReport { maxDepth = 0 }.errors.single().code shouldBe LoggingViolationCodes.INVALID_DEPTH
        }

        test("refuses a partial mask that keeps a negative count at the end") {
            maskingReport { default = Mask.Partial(keepStart = 0, keepEnd = -1) }.isValid shouldBe false
        }

        test("refuses a partial mask that keeps a negative count") {
            maskingReport {
                default = Mask.Partial(
                    keepStart = -1,
                    keepEnd = 0,
                )
            }.errors.map { violation -> violation.code } shouldBe
                listOf(LoggingViolationCodes.INVALID_PARTIAL)
        }

        test("refuses a hash without a key on the default and on a rule") {
            val report = maskingReport {
                default = Mask.Hash
                rule(MaskingRule(name = "hashed", pattern = Regex("x"), mask = Mask.Hash))
            }

            report.errors.map { violation -> violation.code } shouldBe listOf(
                LoggingViolationCodes.MISSING_HASH_KEY,
                LoggingViolationCodes.MISSING_HASH_KEY,
            )
        }

        test("refuses a hash mask without a key and a key shorter than 16 bytes") {
            maskingReport { default = Mask.Hash }.isValid shouldBe false
            shouldThrow<IllegalArgumentException> { MaskingKey.of("short") }
        }
    }

    context("description") {
        test("lists the rules without revealing the hash key") {
            val configuration = maskingConfiguration {
                hashKey = MaskingKey.of("key-0123456789abcdef")
                keyPattern(Regex("pin"))
                keys(listOf("iban"))
                rules(MaskingRules.PERSONAL_DATA)
            }

            configuration.toString() shouldContain "card-number"
            configuration.keys.contains("iban") shouldBe true
            configuration.keyPatterns.size shouldBe 2
            MaskingRules.JWT.toString() shouldBe "MaskingRule(name=jwt)"
        }
    }
})
