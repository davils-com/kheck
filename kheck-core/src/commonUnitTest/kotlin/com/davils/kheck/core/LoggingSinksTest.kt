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
import com.davils.kheck.api.record.LogEvent
import com.davils.kheck.core.fixture.BrokenFormat
import com.davils.kheck.core.fixture.MinimalSink
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.SlowSink
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.sinkStatistics
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.sink.LogOverflow
import com.davils.kheck.core.sink.custom.CustomSinkBuilder
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import com.davils.kheck.testing.SinkCall
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

private const val RECORDS: Int = 3

class LoggingSinksTest : FunSpec({
    context("custom sinks") {
        test("closes a sink that keeps the default lifecycle") {
            runTest {
                val sink = MinimalSink()
                val logging = testLogging { sink("minimal", sink) }

                logging.logger(SERVICE).info { "written" }
                logging.close()

                sink.written shouldBe listOf("written")
            }
        }

        test("writes nothing for a batch of records it cannot encode") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { sink("custom", sink) { format = BrokenFormat } }

                logging.logger(SERVICE).info { BrokenFormat.BROKEN }
                logging.flush()

                sink.history.none { call -> call is SinkCall.Write } shouldBe true
            }
        }

        test("drops a record when a blocked queue stays full") {
            runTest {
                val logging = testLogging {
                    sink("slow", SlowSink()) {
                        capacity = 1
                        maxBatch = 1
                        overflow = LogOverflow.Block(1.seconds)
                    }
                }

                repeat(RECORDS) { logging.logger(SERVICE).info { "record" } }
                logging.flush()

                logging.sinkStatistics("slow").dropped shouldBe 1L
            }
        }

        test("describes its configuration") {
            val configuration = CustomSinkBuilder(MinimalSink()).build()

            configuration.toString() shouldContain "options=LogSinkOptions"
        }
    }

    context("bridges") {
        test("skips an event emitted below the threshold") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                logging.logger(SERVICE).emit(LogLevel.Debug, LogEvent("hidden"))
                logging.logger(SERVICE).emit(LogLevel.Info, LogEvent("shown"))
                logging.flush()

                sink.messages shouldBe listOf("shown")
            }
        }
    }

    context("testing helpers") {
        test("clears captured lines and builds records with causes") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }
                logging.logger(SERVICE).info { "first" }
                logging.flush()

                sink.clear()

                sink.lines shouldBe emptyList()
                logRecord { cause = IllegalStateException("boom") }.cause?.message shouldBe "boom"
            }
        }

        test("starts logging without a configuration block") {
            runTest {
                val logging = logging(backgroundScope, StandardTestDispatcher(testScheduler))

                logging.configuration.sinks shouldBe emptyMap()
                logging.close()
            }
        }
    }
})
