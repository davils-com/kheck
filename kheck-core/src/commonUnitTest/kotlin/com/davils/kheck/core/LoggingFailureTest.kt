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

package com.davils.kheck.core

import com.davils.kheck.api.info
import com.davils.kheck.core.fixture.BrokenFormat
import com.davils.kheck.core.fixture.CAPTURE
import com.davils.kheck.core.fixture.FailingLifecycleSink
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.failedTotal
import com.davils.kheck.core.fixture.recorded
import com.davils.kheck.core.fixture.sinkStatistics
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import com.davils.kheck.testing.SinkCall
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val ATTEMPTS: Int = 3
private const val WAITED_MILLIS: Long = 3_000L
private const val RETRIED: String = "retried"
private const val BUSY: String = "busy"
private val MAX_BACKOFF: Duration = 4.seconds
private const val FALLBACK: String = "fallback"

private const val LIFECYCLE_REPORTS: Int = 3

@OptIn(ExperimentalCoroutinesApi::class)
class LoggingFailureTest : FunSpec({
    context("failing sinks") {
        test("hands the records of a failing sink to its fallback and reports the failure once") {
            runTest {
                val primary = CapturingSink()
                val fallback = CapturingSink()
                val logging = testLogging {
                    capture(primary) { this.fallback = FALLBACK }
                    capture(fallback, name = FALLBACK) { isExclusive = true }
                }
                primary.fail(IllegalStateException("disk full"))

                logging.logger(SERVICE).info { "first" }
                logging.flush()
                logging.logger(SERVICE).info { "second" }
                logging.flush()

                fallback.messages shouldBe listOf("first", "second")
                logging.recorded.messages.single() shouldContain "failed"
                logging.health().status shouldBe HealthStatus.Degraded
                logging.statistics().sinksFailing shouldBe 1
                logging.failedTotal() shouldBe 2L
            }
        }

        test("reports the recovery of a sink") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }
                sink.fail(IllegalStateException("disk full"))
                logging.logger(SERVICE).info { "lost" }
                logging.flush()

                sink.recover()
                logging.logger(SERVICE).info { "written" }
                logging.flush()

                sink.messages shouldBe listOf("written")
                logging.recorded.messages.last() shouldContain "recovered"
                logging.health().status shouldBe HealthStatus.Up
            }
        }

        test("keeps a forwarded record when the fallback fails as well") {
            runTest {
                val primary = CapturingSink()
                val fallback = CapturingSink()
                val logging = testLogging {
                    capture(primary) { this.fallback = FALLBACK }
                    capture(fallback, name = FALLBACK) {
                        isExclusive = true
                        this.fallback = CAPTURE
                    }
                }
                primary.fail(IllegalStateException("primary"))
                fallback.fail(IllegalStateException("fallback"))

                logging.logger(SERVICE).info { "lost" }
                logging.flush()

                logging.recorded.messages.size shouldBe 2
            }
        }

        test("retries a failed batch with a growing backoff before it gives up") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink) { retry(attempts = ATTEMPTS, backoff = 1.seconds, maxBackoff = MAX_BACKOFF) }
                }
                sink.fail(IllegalStateException(BUSY))

                logging.logger(SERVICE).info { RETRIED }
                logging.flush()

                sink.history.count { call -> call is SinkCall.Write } shouldBe ATTEMPTS
                logging.sinkStatistics(CAPTURE).failed shouldBe 1L
                testScheduler.currentTime shouldBe WAITED_MILLIS
            }
        }

        test("writes a batch that succeeds on a later attempt") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) { retry(attempts = 2, backoff = 1.seconds) } }
                sink.fail(IllegalStateException(BUSY))
                logging.logger(SERVICE).info { RETRIED }
                runCurrent()
                sink.recover()

                logging.flush()

                sink.messages shouldBe listOf(RETRIED)
                logging.health().status shouldBe HealthStatus.Up
            }
        }

        test("reports a sink that cannot open, flush or close") {
            runTest {
                val sink = FailingLifecycleSink()
                val logging = testLogging { sink("broken", sink) }

                logging.logger(SERVICE).info { "still written" }
                logging.flush()
                logging.close()

                sink.written.size shouldBe 1
                logging.recorded.messages.size shouldBe LIFECYCLE_REPORTS
            }
        }

        test("skips a record its format cannot encode") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { sink("custom", sink) { format = BrokenFormat } }

                logging.logger(SERVICE).info { BrokenFormat.BROKEN }
                logging.logger(SERVICE).info { "fine" }
                logging.flush()

                sink.texts shouldBe listOf("fine")
                logging.recorded.messages.single() shouldContain "could not encode"
            }
        }
    }
})
