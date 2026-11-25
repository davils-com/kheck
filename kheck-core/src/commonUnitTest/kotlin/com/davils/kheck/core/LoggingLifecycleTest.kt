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

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.davils.kheck.core

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.debug
import com.davils.kheck.api.info
import com.davils.kheck.api.warn
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.recordedTotal
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.health.HealthRule
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.testing.CapturingSink
import com.davils.kheck.testing.SinkCall
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

private const val CAPACITY: Int = 64

class LoggingLifecycleTest : FunSpec({
    context("reconfiguration") {
        test("swaps the sinks after writing what the old ones received") {
            runTest {
                val old = CapturingSink()
                val new = CapturingSink()
                val logging = testLogging { capture(old) }

                logging.logger(SERVICE).info { "before" }
                val result = logging.reconfigure {
                    level = LogLevel.Debug
                    capture(new)
                }
                logging.logger(SERVICE).debug { "after" }
                logging.flush()

                result shouldBe Reconfiguration.Applied
                old.messages shouldBe listOf("before")
                old.history.last() shouldBe SinkCall.Close
                new.messages shouldBe listOf("after")
                logging.configuration.level shouldBe LogLevel.Debug
            }
        }

        test("rejects an invalid configuration and keeps the old one") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                val result = logging.reconfigure { logger(SERVICE) { sinks("missing") } }
                logging.logger(SERVICE).info { "kept" }
                logging.flush()

                result.shouldBeInstanceOf<Reconfiguration.Rejected>().report.isValid shouldBe false
                sink.messages shouldBe listOf("kept")
            }
        }

        test("rejects a change of the intake") {
            runTest {
                val logging = testLogging { }

                val result = logging.reconfigure { intake { capacity = CAPACITY } }

                val rejection = result.shouldBeInstanceOf<Reconfiguration.Rejected>()
                rejection.report.errors.map { violation -> violation.code } shouldBe
                    listOf(LoggingViolationCodes.INTAKE_CHANGED)
            }
        }

        test("refuses to reconfigure after closing") {
            runTest {
                val logging = testLogging { }
                logging.close()

                logging.reconfigure { } shouldBe Reconfiguration.Closed
            }
        }

        test("publishes applied and rejected configurations") {
            runTest {
                val events = mutableListOf<LoggingEvent>()
                val logging = testLogging { onEvent { event -> events += event } }

                logging.reconfigure { onEvent { event -> events += event } }
                logging.reconfigure { logger(SERVICE) { sinks("missing") } }

                events.first() shouldBe LoggingEvent.ConfigurationApplied
                events.last().shouldBeInstanceOf<LoggingEvent.ConfigurationRejected>()
            }
        }
    }

    context("closing") {
        test("writes everything, closes the sinks and drops later records") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                logging.logger(SERVICE).info { "last" }
                logging.close()
                logging.logger(SERVICE).info { "too late" }
                logging.flush()
                logging.close()

                logging.isClosed shouldBe true
                sink.messages shouldBe listOf("last")
                sink.history shouldContain SinkCall.Close
                logging.statistics().droppedAfterClose shouldBe 1L
            }
        }

        test("counts records that arrived while the intake closed") {
            runTest {
                val logging = logging(backgroundScope, StandardTestDispatcher(testScheduler)) { }

                logging.logger(SERVICE).info { "racing" }
                logging.close()

                logging.recordedTotal() shouldBe 1L
            }
        }
    }

    context("telemetry") {
        test("binds its meters to a registry") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                logging.logger(SERVICE).warn { "counted" }
                logging.flush()

                val statistics = logging.statistics()
                statistics.recorded.getValue(LogLevel.Warn) shouldBe 1L
                statistics.sinks.getValue("capture").written shouldBe 1L
            }
        }

        test("is healthy without rules") {
            runTest {
                testLogging { healthRules = emptyList() }.health().status shouldBe HealthStatus.Up
            }
        }

        test("degrades when a rule cannot be evaluated") {
            runTest {
                val logging = testLogging { healthRules = listOf(HealthRule { error("broken") }) }

                logging.health().status shouldBe HealthStatus.Degraded
            }
        }
    }
})
