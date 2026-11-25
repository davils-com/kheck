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

package com.davils.kheck.core.sample.engine

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.sample.RateLimitKey
import com.davils.kheck.core.sample.SamplingBuilder
import com.davils.kheck.core.sample.fixture.samplingConfiguration
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private const val DRAWS: Int = 4
private const val BUCKETS: Int = 4_097
private const val TWICE: Long = 2L
private const val THREE: Int = 3
private const val HALF: Double = 0.5
private const val TENANT: String = "tenant"
private const val LOGGER: String = "logger"
private const val FIRST: String = "first"

class SamplerTest : FunSpec({
    context("deduplication") {
        test("passes the first record and suppresses repeats inside the window") {
            val time = TestTimeSource()
            val sampler = sampler(time) { dedupe(window = 1.seconds) }
            val record = logRecord()

            List(THREE) { sampler.decide(record) } shouldBe listOf(
                SamplingDecision.Keep,
                SamplingDecision.Deduplicated,
                SamplingDecision.Deduplicated,
            )
            sampler.expired() shouldBe emptyList()
        }

        test("summarises the repeats when the window ends") {
            val time = TestTimeSource()
            val sampler = sampler(time) { dedupe(window = 1.seconds) }
            val record = logRecord()
            repeat(THREE) { sampler.decide(record) }

            time += 1.seconds

            sampler.expired().single().fields shouldBe mapOf(Deduplicator.REPEATED to LogValue.Integer(TWICE))
            sampler.expired() shouldBe emptyList()
        }

        test("forgets a record that was not repeated") {
            val time = TestTimeSource()
            val sampler = sampler(time) { dedupe(window = 1.seconds) }
            sampler.decide(logRecord())

            time += 1.seconds

            sampler.expired() shouldBe emptyList()
        }

        test("starts a new window when a repeat arrives after the old one") {
            val time = TestTimeSource()
            val sampler = sampler(time) { dedupe(window = 1.seconds) }
            val record = logRecord()
            repeat(TWICE.toInt()) { sampler.decide(record) }

            time += 2.seconds

            sampler.decide(record) shouldBe SamplingDecision.Keep
            sampler.expired().size shouldBe 1
        }

        test("evicts the oldest entry when the table is full") {
            val time = TestTimeSource()
            val sampler = sampler(time) { dedupe(window = 1.seconds, maxEntries = 1) }
            repeat(TWICE.toInt()) { sampler.decide(logRecord { message = FIRST }) }

            sampler.decide(logRecord { message = "second" }) shouldBe SamplingDecision.Keep
            sampler.expired().single().message shouldBe FIRST
        }
    }

    context("rate limit") {
        test("allows the configured records per period") {
            val time = TestTimeSource()
            val sampler = sampler(time) { limit(records = 2, per = 1.seconds) }
            val record = logRecord()

            List(THREE) { sampler.decide(record) } shouldBe listOf(
                SamplingDecision.Keep,
                SamplingDecision.Keep,
                SamplingDecision.RateLimited,
            )

            time += 500.milliseconds

            sampler.decide(record) shouldBe SamplingDecision.Keep
        }

        test("keeps a bucket per logger") {
            val sampler = sampler(
                TestTimeSource(),
            ) { limit(records = 1, per = 1.seconds, key = RateLimitKey.PerLogger) }

            sampler.decide(logRecord { logger = FIRST }) shouldBe SamplingDecision.Keep
            sampler.decide(logRecord { logger = "second" }) shouldBe SamplingDecision.Keep
            sampler.decide(logRecord { logger = FIRST }) shouldBe SamplingDecision.RateLimited
        }

        test("keeps a bucket per level") {
            val sampler = sampler(
                TestTimeSource(),
            ) { limit(records = 1, per = 1.seconds, key = RateLimitKey.PerLevel) }

            sampler.decide(logRecord { level = LogLevel.Debug }) shouldBe SamplingDecision.Keep
            sampler.decide(logRecord { level = LogLevel.Info }) shouldBe SamplingDecision.Keep
            sampler.decide(logRecord { level = LogLevel.Debug }) shouldBe SamplingDecision.RateLimited
        }

        test("keeps a bucket per value of a field or of the context") {
            val sampler = sampler(TestTimeSource()) {
                limit(records = 1, per = 1.seconds, key = RateLimitKey.PerField(TENANT))
            }

            val first = logRecord { fields { field(name = TENANT, value = "a") } }
            val second = logRecord { context { field(name = TENANT, value = "b") } }

            sampler.decide(first) shouldBe SamplingDecision.Keep
            sampler.decide(second) shouldBe SamplingDecision.Keep
            sampler.decide(first) shouldBe SamplingDecision.RateLimited
        }

        test("shares one bucket between records without the field") {
            val sampler = sampler(
                TestTimeSource(),
            ) { limit(records = 1, per = 1.seconds, key = RateLimitKey.PerField("x")) }

            sampler.decide(logRecord()) shouldBe SamplingDecision.Keep
            sampler.decide(logRecord()) shouldBe SamplingDecision.RateLimited
        }

        test("starts over when too many buckets exist") {
            val sampler = sampler(
                TestTimeSource(),
            ) { limit(records = 1, per = 1.seconds, key = RateLimitKey.PerLogger) }

            val decisions = (0 until BUCKETS).map { index -> sampler.decide(logRecord { logger = "$LOGGER$index" }) }

            decisions.all { decision -> decision == SamplingDecision.Keep } shouldBe true
        }

        test("keeps a known bucket when the table is full") {
            val sampler = sampler(
                TestTimeSource(),
            ) { limit(records = 1, per = 1.seconds, key = RateLimitKey.PerLogger) }
            repeat(BUCKETS - 1) { index -> sampler.decide(logRecord { logger = "$LOGGER$index" }) }

            sampler.decide(logRecord { logger = "${LOGGER}0" }) shouldBe SamplingDecision.RateLimited
        }

        test("never limits warnings and errors") {
            val sampler = sampler(TestTimeSource()) { limit(records = 1, per = 1.seconds) }

            List(
                THREE,
            ) { sampler.decide(logRecord { level = LogLevel.Error }) }.toSet() shouldBe setOf(SamplingDecision.Keep)
        }
    }

    context("rate") {
        test("drops every record below the rate") {
            sampler(TestTimeSource()) { rate = 0.0 }.decide(logRecord()) shouldBe SamplingDecision.Sampled
        }

        test("keeps exactly the share of the rate, in a fixed order") {
            val sampler = sampler(TestTimeSource()) { rate = HALF }

            List(DRAWS) { sampler.decide(logRecord()) } shouldBe listOf(
                SamplingDecision.Sampled,
                SamplingDecision.Keep,
                SamplingDecision.Sampled,
                SamplingDecision.Keep,
            )
        }

        test("summarises nothing without deduplication") {
            sampler(TestTimeSource()) { }.expired() shouldBe emptyList()
        }

        test("keeps every record at full rate") {
            sampler(TestTimeSource()) { }.decide(logRecord()) shouldBe SamplingDecision.Keep
        }
    }
})

private fun sampler(time: TestTimeSource, block: SamplingBuilder.() -> Unit): Sampler =
    Sampler(samplingConfiguration(block), time)
