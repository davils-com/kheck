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

package com.davils.kheck.knot

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.info
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.knot.fixture.HangingSink
import com.davils.kheck.knot.fixture.started
import com.davils.kheck.testing.CapturingSink
import com.davils.knot.failure.FailureOrigin
import com.davils.knot.failure.failures
import com.davils.knot.shutdown.ShutdownRegistration
import com.davils.knot.shutdown.report.TaskOutcome
import com.davils.knot.shutdown.shutdown
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

private const val CAPTURE: String = "capture"

class KnotBindingsTest : FunSpec({
    context("shutdown") {
        test("closes the logging as the last task of a shutdown") {
            runTest {
                val sink = CapturingSink()
                val logging = started { sink(CAPTURE, sink) }
                logging.logger("app").info { "bye" }

                val report = shutdown(backgroundScope) { logging(logging) }.initiate()

                logging.isClosed shouldBe true
                sink.messages shouldBe listOf("bye")
                val task = report.tasks.single()
                task.name.value shouldBe "logging-close"
                task.outcome.shouldBeInstanceOf<TaskOutcome.Completed>()
            }
        }

        test("joins a shutdown that runs already") {
            runTest {
                val logging = started { sink(CAPTURE, CapturingSink()) }
                val shutdown = shutdown(backgroundScope)

                val registration = shutdown.adopt(logging)
                registration.shouldBeInstanceOf<ShutdownRegistration.Registered>()
                val report = shutdown.initiate()

                logging.isClosed shouldBe true
                val task = report.tasks.single()
                task.name.value shouldBe "logging-close"
            }
        }

        test("does not let a sink that hangs hold the shutdown") {
            runTest {
                val logging = started { sink("slow", HangingSink()) }
                logging.logger("app").info { "stuck" }

                val report = shutdown(backgroundScope) { logging(logging = logging, timeout = 1.seconds) }.initiate()

                logging.isClosed shouldBe true
                val task = report.tasks.single()
                task.outcome.shouldBeInstanceOf<TaskOutcome.Completed>()
            }
        }
    }

    context("failures") {
        test("writes an unexpected failure as an error record") {
            runTest {
                val sink = CapturingSink()
                val logging = started { sink(CAPTURE, sink) }
                val origin = FailureOrigin.Component(kind = "worker", name = "indexer")

                failures(backgroundScope) { sinks { sink("log", logging.failureSink()) } }.use { handler ->
                    handler.report(IllegalStateException("boom"), origin)
                    handler.drain()
                }
                logging.flush()

                val record = sink.records.single()
                record.level shouldBe LogLevel.Error
                record.message shouldBe "Unexpected failure"
                record.source.logger.value shouldBe "failures"
                record.cause?.message shouldBe "boom"
                record.fields["failure.origin"] shouldBe LogValue.Text("worker:indexer")
                record.fields["failure.sink"] shouldBe LogValue.Text("log")
            }
        }
    }
})
