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

import com.davils.kheck.api.error
import com.davils.kheck.api.info
import com.davils.kheck.core.filter.LogFilter
import com.davils.kheck.core.fixture.BrokenTrace
import com.davils.kheck.core.fixture.Crashes
import com.davils.kheck.core.fixture.Fatal
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.crashingLogging
import com.davils.kheck.core.health.HealthRule
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.core.mask.MaskingRule
import com.davils.kheck.testing.CapturingSink
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException

private const val CANCELLED: String = "cancelled"

class LoggingCrashTest : FunSpec({
    context("errors of the virtual machine") {
        test("stop the pipeline when a global filter throws one") {
            runTest {
                val crashes = Crashes()
                val sink = CapturingSink()
                val logging = crashingLogging(crashes) {
                    capture(sink)
                    filter(LogFilter { throw Fatal() })
                }

                logging.logger(SERVICE).info { "lost" }
                logging.flush()

                sink.messages shouldBe emptyList()
                crashes.failures.single().shouldBeInstanceOf<Fatal>()
            }
        }

        test("stop the pipeline when masking throws one") {
            runTest {
                val crashes = Crashes()
                val logging = crashingLogging(crashes) {
                    capture(CapturingSink())
                    mask { rule(MaskingRule(name = "fatal", pattern = Regex("x"), validator = { throw Fatal() })) }
                }

                logging.logger(SERVICE).info { "x" }
                logging.flush()

                crashes.failures.single().shouldBeInstanceOf<Fatal>()
            }
        }

        test("stop the pipeline when reading a cause throws one") {
            runTest {
                val crashes = Crashes()
                val logging = crashingLogging(crashes) { capture(CapturingSink()) }

                logging.logger(SERVICE).error {
                    cause(BrokenTrace(Fatal()))
                    "broken"
                }
                logging.flush()

                crashes.failures.single().shouldBeInstanceOf<Fatal>()
            }
        }

        test("end the pipeline quietly when reading a cause cancels") {
            runTest {
                val crashes = Crashes()
                val logging = crashingLogging(crashes) { capture(CapturingSink()) }

                logging.logger(SERVICE).error {
                    cause(BrokenTrace(CancellationException(CANCELLED)))
                    "cancelled"
                }
                logging.flush()
                logging.close()

                crashes.failures shouldBe emptyList()
            }
        }

        test("stop a sink when its filter throws one") {
            runTest {
                val crashes = Crashes()
                val logging = crashingLogging(crashes) { capture(CapturingSink()) { filter { throw Fatal() } } }

                logging.logger(SERVICE).info { "lost" }
                logging.flush()

                crashes.failures.single().shouldBeInstanceOf<Fatal>()
            }
        }

        test("stop a sink when it throws one while writing") {
            runTest {
                val crashes = Crashes()
                val sink = CapturingSink()
                val logging = crashingLogging(crashes) { capture(sink) }
                sink.fail(Fatal())

                logging.logger(SERVICE).info { "lost" }
                logging.flush()

                crashes.failures shouldHaveSize 1
            }
        }

        test("stop a sink quietly when its write is cancelled") {
            runTest {
                val crashes = Crashes()
                val sink = CapturingSink()
                val logging = crashingLogging(crashes) { capture(sink) }
                sink.fail(CancellationException(CANCELLED))

                logging.logger(SERVICE).info { "lost" }
                logging.flush()
                logging.close()

                crashes.failures shouldBe emptyList()
            }
        }
    }

    context("health") {
        test("passes errors of the virtual machine on and degrades on other failures") {
            runTest {
                val crashes = Crashes()
                val fatal = crashingLogging(crashes) { healthRules = listOf(HealthRule { throw Fatal() }) }
                val failing = crashingLogging(crashes) { healthRules = listOf(HealthRule { error(CANCELLED) }) }

                shouldThrow<Fatal> { fatal.health() }
                failing.health().status shouldBe HealthStatus.Degraded
            }
        }
    }
})
