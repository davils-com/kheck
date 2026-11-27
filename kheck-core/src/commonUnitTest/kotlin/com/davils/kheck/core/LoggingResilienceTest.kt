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
import com.davils.kheck.core.fixture.CAPTURE
import com.davils.kheck.core.fixture.HangingOpenSink
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.SlowSink
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.loggingCodes
import com.davils.kheck.core.fixture.recorded
import com.davils.kheck.core.fixture.sinkStatistics
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import com.davils.kheck.testing.SinkCall
import com.davils.kontinuity.circuitbreaker.CircuitState
import com.davils.kontinuity.circuitbreaker.circuitBreaker
import com.davils.kontinuity.circuitbreaker.countBased
import com.davils.kontinuity.core.Environment
import com.davils.kontinuity.core.backoff.exponential
import com.davils.kontinuity.core.backoff.fixed
import com.davils.kontinuity.core.environment
import com.davils.kontinuity.policy.resilience
import com.davils.kontinuity.retry.retry
import com.davils.kontinuity.testing.RefusingStage
import com.davils.kontinuity.timelimiter.timeLimiter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val FALLBACK: String = "fallback"
private const val SLOW: String = "slow"
private const val REFUSED: String = "refused"
private const val RETRIED: String = "retried"
private const val RETRY: String = "kheck.test.retry"
private const val BUSY: String = "busy"
private const val ATTEMPTS: Int = 3
private const val WAITED_MILLIS: Long = 3_000L
private val MAX_BACKOFF: Duration = 4.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LoggingResilienceTest : FunSpec({
    context("retries") {
        test("retry a failed batch with a growing backoff before the sink gives up") {
            runTest {
                val sink = CapturingSink()
                val retry = retry(name = RETRY, environment = virtual()) {
                    maxAttempts = ATTEMPTS
                    backoff = exponential(initial = 1.seconds, maximum = MAX_BACKOFF)
                }
                val logging = testLogging { capture(sink) { stages(retry) } }
                sink.fail(IllegalStateException(BUSY))

                logging.logger(SERVICE).info { RETRIED }
                logging.flush()

                sink.history.count { call -> call is SinkCall.Write } shouldBe ATTEMPTS
                logging.sinkStatistics(CAPTURE).failed shouldBe 1L
                testScheduler.currentTime shouldBe WAITED_MILLIS
            }
        }

        test("write a batch that succeeds on a later attempt") {
            runTest {
                val sink = CapturingSink()
                val retry = retry(name = RETRY, environment = virtual()) {
                    maxAttempts = 2
                    backoff = fixed(1.seconds)
                }
                val logging = testLogging { capture(sink) { stages(retry) } }
                sink.fail(IllegalStateException(BUSY))
                logging.logger(SERVICE).info { RETRIED }
                runCurrent()
                sink.recover()

                logging.flush()

                sink.messages shouldBe listOf(RETRIED)
                logging.health().status shouldBe HealthStatus.Up
            }
        }
    }

    context("circuit breakers") {
        test("refuse batches of a failing sink, hand them to the fallback and recover through a probe") {
            runTest {
                val primary = CapturingSink()
                val fallback = CapturingSink()
                val events = mutableListOf<LoggingEvent>()
                val breaker = circuitBreaker(name = "kheck.test.breaker", environment = virtual()) {
                    slidingWindow = countBased(2)
                    minimumNumberOfCalls = 2
                    permittedCallsInHalfOpenState = 1
                    waitInOpenState = fixed(10.seconds)
                }
                val logging = testLogging {
                    capture(primary) {
                        this.fallback = FALLBACK
                        stages(breaker)
                    }
                    capture(fallback, name = FALLBACK) { isExclusive = true }
                    onEvent { event -> events += event }
                }
                val logger = logging.logger(SERVICE)
                primary.fail(IllegalStateException("down"))

                logger.info { "first" }
                logging.flush()
                logger.info { "second" }
                logging.flush()
                logger.info { REFUSED }
                logging.flush()

                fallback.messages shouldBe listOf("first", "second", REFUSED)
                logging.sinkStatistics(CAPTURE).rejected shouldBe 1L
                logging.sinkStatistics(CAPTURE).failed shouldBe 2L
                logging.sinkStatistics(CAPTURE).circuit shouldBe CircuitState.Open
                logging.statistics().circuitsOpen shouldBe 1
                logging.health().status shouldBe HealthStatus.Degraded
                val rejections = events.filterIsInstance<LoggingEvent.SinkRejected>()
                rejections.single().toString() shouldBe "SinkRejected(sink=$CAPTURE, count=1)"

                primary.recover()
                advanceTimeBy(11.seconds)
                logger.info { "probe" }
                logging.flush()

                primary.messages shouldBe listOf("probe")
                logging.sinkStatistics(CAPTURE).circuit shouldBe CircuitState.Closed
            }
        }

        test("are found inside a policy") {
            runTest {
                val environment = virtual()
                val breaker = circuitBreaker(name = "kheck.test.breaker", environment = environment)
                val policy = resilience(name = "kheck.test.policy", environment = environment) {
                    retry(retry(name = RETRY, environment = environment) { maxAttempts = 2 })
                    circuitBreaker(breaker)
                }
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) { stages(policy) } }

                logging.logger(SERVICE).info { "through" }
                logging.flush()

                sink.messages shouldBe listOf("through")
                logging.sinkStatistics(CAPTURE).circuit shouldBe CircuitState.Closed
                logging.sinkStatistics(CAPTURE).toString() shouldContain "circuit=Closed"
            }
        }

        test("count a refusal of any stage as rejected without a fallback") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) { stages(RefusingStage()) } }

                logging.logger(SERVICE).info { REFUSED }
                logging.flush()

                sink.messages shouldBe emptyList()
                logging.sinkStatistics(CAPTURE).rejected shouldBe 1L
                logging.sinkStatistics(CAPTURE).isFailing shouldBe false
            }
        }
    }

    context("time limiters") {
        test("end a write that hangs and hand the batch to the fallback") {
            runTest {
                val fallback = CapturingSink()
                val limiter = timeLimiter(name = "kheck.test.limiter", environment = virtual()) { timeout = 1.seconds }
                val logging = testLogging {
                    sink(SLOW, SlowSink()) {
                        this.fallback = FALLBACK
                        stages(limiter)
                    }
                    capture(fallback, name = FALLBACK) { isExclusive = true }
                }

                logging.logger(SERVICE).info { "late" }
                logging.flush()

                fallback.messages shouldBe listOf("late")
                logging.sinkStatistics(SLOW).failed shouldBe 1L
                logging.recorded.messages.single() shouldContain "CallTimeoutException"
            }
        }

        test("end an opening that hangs and still write afterwards") {
            runTest {
                val sink = HangingOpenSink()
                val limiter = timeLimiter(name = "kheck.test.limiter", environment = virtual()) { timeout = 1.seconds }
                val logging = testLogging { sink(SLOW, sink) { stages(limiter) } }

                logging.logger(SERVICE).info { "written" }
                logging.flush()

                sink.written shouldBe listOf("written")
                logging.recorded.messages.first() shouldContain "CallTimeoutException"
            }
        }
    }

    context("validation") {
        test("refuses a stage given to two sinks") {
            val shared = RefusingStage()

            loggingCodes {
                capture(CapturingSink()) { stages(shared) }
                capture(CapturingSink(), name = FALLBACK) { stages(shared) }
            } shouldBe listOf(LoggingViolationCodes.SHARED_STAGE)
        }

        test("accepts different stages on different sinks") {
            loggingCodes {
                capture(CapturingSink()) { stages(RefusingStage()) }
                capture(CapturingSink(), name = FALLBACK) { stages(RefusingStage()) }
            } shouldBe emptyList()
        }

        test("refuses a stage given twice to one sink") {
            val shared = RefusingStage()

            loggingCodes { capture(CapturingSink()) { stages(shared, shared) } } shouldBe
                listOf(LoggingViolationCodes.SHARED_STAGE)
        }
    }
})

private fun TestScope.virtual(): Environment = environment { timeSource = testScheduler.timeSource }
