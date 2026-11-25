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

package com.davils.kheck.core.sample

import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kheck.core.sample.fixture.samplingCodes
import com.davils.kheck.core.sample.fixture.samplingConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration

private const val TOO_HIGH: Double = 1.5

class SamplingConfigurationTest : FunSpec({
    context("validation") {
        test("refuses a rate outside of zero and one") {
            samplingCodes { rate = TOO_HIGH } shouldBe listOf(LoggingViolationCodes.INVALID_RATE)
            samplingCodes { rate = -TOO_HIGH } shouldBe listOf(LoggingViolationCodes.INVALID_RATE)
        }

        test("refuses an empty window and an empty table") {
            samplingCodes { dedupe(window = Duration.ZERO, maxEntries = 0) } shouldBe listOf(
                LoggingViolationCodes.INVALID_WINDOW,
                LoggingViolationCodes.INVALID_LIMIT,
            )
        }

        test("refuses a limit without records or period") {
            samplingCodes { limit(records = 0, per = Duration.ZERO) } shouldBe listOf(
                LoggingViolationCodes.INVALID_LIMIT,
                LoggingViolationCodes.INVALID_WINDOW,
            )
        }
    }

    context("description") {
        test("names the limit") {
            val configuration = samplingConfiguration { limit(records = 1, per = Duration.parse("1s")) }

            configuration.toString() shouldContain "deduplication=none, limit=RateLimit"
            samplingConfiguration { dedupe(Duration.parse("1s")) }.toString() shouldContain "limit=none"
        }
    }
})
