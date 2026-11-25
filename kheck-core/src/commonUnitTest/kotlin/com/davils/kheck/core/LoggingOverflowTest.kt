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
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.engine.Notices
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.sinkStatistics
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.sink.LogOverflow
import com.davils.kheck.testing.CapturingSink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val RECORDS: Int = 4

private const val LOST: Long = 3L

class LoggingOverflowTest : FunSpec({
    context("sink queue") {
        test("drops the newest records and announces the loss") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink) {
                        capacity = 1
                        maxBatch = 1
                        overflow = LogOverflow.DropNewest
                    }
                }

                logAll(logging)
                logging.flush()
                logging.logger(SERVICE).info { "after" }
                logging.flush()

                sink.records.first().fields shouldBe mapOf(Notices.DROPPED to LogValue.Integer(LOST))
                sink.messages.drop(1) shouldBe listOf("record 0", "after")
                logging.sinkStatistics("capture").dropped shouldBe LOST
            }
        }

        test("drops the oldest records") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink) {
                        capacity = 1
                        maxBatch = 1
                        overflow = LogOverflow.DropOldest
                    }
                }

                logAll(logging)
                logging.flush()

                sink.messages.last() shouldBe "record 3"
            }
        }

        test("waits for room before it gives up") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink) {
                        capacity = 1
                        maxBatch = 1
                        overflow = LogOverflow.Block(1.seconds)
                    }
                }

                logAll(logging)
                logging.flush()

                sink.messages shouldBe List(RECORDS) { index -> "record $index" }
            }
        }
    }

    context("intake") {
        test("drops the newest records when the intake is full") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    intake {
                        capacity = 1
                        overflow = LogOverflow.DropNewest
                    }
                }

                logAll(logging)
                logging.flush()

                sink.messages shouldBe listOf("record 0")
                logging.statistics().intakeDropped shouldBe LOST
            }
        }

        test("keeps the newest records when the intake drops the oldest") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    intake { capacity = 1 }
                }

                logAll(logging)
                logging.flush()

                sink.messages shouldBe listOf("record 3")
            }
        }

        test("blocks a caller for a bounded time when the intake is full") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    intake {
                        capacity = 1
                        overflow = LogOverflow.Block(10.milliseconds)
                    }
                }

                logging.logger(SERVICE).info { "first" }
                logging.logger(SERVICE).info { "second" }
                logging.flush()

                sink.messages shouldBe listOf("first")
            }
        }
    }
})

private fun logAll(logging: Logging) {
    val logger = logging.logger(SERVICE)
    repeat(RECORDS) { index -> logger.info { "record $index" } }
}
