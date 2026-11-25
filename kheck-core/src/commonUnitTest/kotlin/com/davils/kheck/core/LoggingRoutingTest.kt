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

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.info
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.api.warn
import com.davils.kheck.core.filter.LogFilter
import com.davils.kheck.core.filter.LogFilters
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.recorded
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.testing.CapturingSink
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

private const val AUDIT_SINK: String = "audit"

private const val PASSED: String = "passed"
private const val LOGIN: String = "login"
private const val AUDIT_LOGGER: String = "audit.Login"

class LoggingRoutingTest : FunSpec({
    context("sinks") {
        test("routes every logger to every sink that is not exclusive") {
            runTest {
                val first = CapturingSink()
                val second = CapturingSink()
                val logging = testLogging {
                    capture(first, name = "first")
                    capture(second, name = "second")
                }

                logging.logger(SERVICE).info { "both" }
                logging.flush()

                first.messages shouldBe listOf("both")
                second.messages shouldBe listOf("both")
            }
        }

        test("routes to an exclusive sink only from the loggers that name it") {
            runTest {
                val main = CapturingSink()
                val audit = CapturingSink()
                val logging = testLogging {
                    capture(main)
                    capture(audit, name = AUDIT_SINK) { isExclusive = true }
                    logger(AUDIT_SINK) { sinks(AUDIT_SINK) }
                }

                logging.logger(AUDIT_LOGGER).info { LOGIN }
                logging.logger(SERVICE).info { "service" }
                logging.flush()

                audit.messages shouldBe listOf(LOGIN)
                main.messages shouldBe listOf(LOGIN, "service")
            }
        }

        test("stops at a logger that is not additive") {
            runTest {
                val main = CapturingSink()
                val audit = CapturingSink()
                val logging = testLogging {
                    capture(main)
                    capture(audit, name = AUDIT_SINK) { isExclusive = true }
                    logger(AUDIT_SINK) {
                        isAdditive = false
                        sinks(AUDIT_SINK)
                    }
                }

                logging.logger(AUDIT_LOGGER).info { LOGIN }
                logging.flush()

                audit.messages shouldBe listOf(LOGIN)
                main.messages shouldBe emptyList()
            }
        }
    }

    context("sink filters") {
        test("applies the level, the markers and the filters of a sink") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink) {
                        level = LogLevel.Warn
                        markers = setOf(LogMarker.AUDIT)
                        filter(LogFilters.outside("com.example.noisy"))
                    }
                }
                val logger = logging.logger(SERVICE)

                logger.warn { "unmarked" }
                logger.info {
                    marker(LogMarker.AUDIT)
                    "too low"
                }
                logger.warn {
                    marker(LogMarker.AUDIT)
                    "kept"
                }
                logging.logger("com.example.noisy.Poller").warn {
                    marker(LogMarker.AUDIT)
                    "filtered"
                }
                logging.flush()

                sink.messages shouldBe listOf("kept")
            }
        }

        test("lets a record pass a sink filter that fails") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) { filter { error("broken filter") } } }

                logging.logger(SERVICE).info { PASSED }
                logging.flush()

                sink.messages shouldBe listOf(PASSED)
                logging.recorded.messages shouldHaveSize 1
            }
        }
    }

    context("global filters") {
        test("drops records a global filter refuses") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    filter(LogFilters.unmarked(setOf(LogMarker.SECURITY)))
                    filter(LogFilters.atLeast(LogLevel.Info))
                }
                val logger = logging.logger(SERVICE)

                logger.info {
                    marker(LogMarker.SECURITY)
                    "secret"
                }
                logger.info { "open" }
                logging.flush()

                sink.messages shouldBe listOf("open")
            }
        }

        test("lets a record pass a global filter that fails") {
            runTest {
                val sink = CapturingSink()
                val failing = LogFilter { error("broken filter") }
                val logging = testLogging {
                    capture(sink)
                    filter(failing)
                }

                logging.logger(SERVICE).info { PASSED }
                logging.flush()

                sink.messages shouldBe listOf(PASSED)
                logging.recorded.messages shouldHaveSize 1
            }
        }

        test("selects records by markers and loggers") {
            val record = logRecord { markers = setOf(LogMarker.AUDIT) }

            LogFilters.marked(setOf(LogMarker.AUDIT)).accept(record) shouldBe true
            LogFilters.marked(setOf(LogMarker.SECURITY)).accept(record) shouldBe false
            LogFilters.within("com.example").accept(record) shouldBe true
            LogFilters.within("org").accept(record) shouldBe false
        }
    }
})
