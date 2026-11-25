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
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.sink.LogOverflow
import com.davils.kheck.testing.CapturingSink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

private const val BURST: Int = 3

class LoggingEventsTest : FunSpec({
    context("sink events") {
        test("announces failures, recoveries and losses of a sink") {
            runTest {
                val events = mutableListOf<LoggingEvent>()
                val sink = CapturingSink()
                val logging = testLogging {
                    onEvent { event -> events += event }
                    capture(sink) {
                        capacity = 1
                        maxBatch = 1
                        overflow = LogOverflow.DropNewest
                    }
                }

                sink.fail(IllegalStateException("disk full"))
                logging.logger(SERVICE).info { "failed" }
                logging.flush()
                sink.recover()
                repeat(BURST) { logging.logger(SERVICE).info { "burst" } }
                logging.flush()

                events.map { event -> event::class.simpleName } shouldBe
                    listOf("SinkFailed", "RecordsDropped", "SinkRecovered")
            }
        }
    }

    context("listeners") {
        test("count a listener that throws and go on") {
            runTest {
                val events = mutableListOf<LoggingEvent>()
                val logging = testLogging {
                    onEvent { error("broken listener") }
                    onEvent { event -> events += event }
                }

                logging.reconfigure {
                    onEvent { error("broken listener") }
                    onEvent { event -> events += event }
                }

                logging.statistics().failedListeners shouldBe 1L
                events shouldBe listOf(LoggingEvent.ConfigurationApplied)
            }
        }

        test("show the events without their foreign text") {
            LoggingEvent.SinkFailed("disk", IllegalStateException("secret")).toString() shouldBe
                "SinkFailed(sink=disk, cause=IllegalStateException)"
            LoggingEvent.SinkRecovered("disk").toString() shouldBe "SinkRecovered(sink=disk)"
            LoggingEvent.RecordsDropped("disk", 2L).toString() shouldBe "RecordsDropped(sink=disk, count=2)"
            LoggingEvent.ConfigurationApplied.toString() shouldBe "ConfigurationApplied"
        }
    }
})
