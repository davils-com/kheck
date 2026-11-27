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

import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.error
import com.davils.kheck.api.info
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.api.warn
import com.davils.kheck.core.context.LogContext
import com.davils.kheck.core.context.logContext
import com.davils.kheck.core.context.withLogContext
import com.davils.kheck.core.fixture.BrokenTrace
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.TIMESTAMP
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.maskedTotal
import com.davils.kheck.core.fixture.recorded
import com.davils.kheck.core.fixture.suppressedBy
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.mask.MaskingRule
import com.davils.kheck.core.sample.engine.Deduplicator
import com.davils.kheck.core.statistics.Suppression
import com.davils.kheck.testing.CapturingSink
import com.davils.knot.environment.Environment
import com.davils.knot.environment.environment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

private const val MASKED: Long = 3L
private const val REPEATS: Int = 3
private const val TENANT: String = "tenant"
private const val ACME: String = "acme"
private const val BROKEN: String = "broken"
private const val HOST: String = "host"

class LoggingPipelineTest : FunSpec({
    context("records") {
        test("stamps sequence, time, thread and logger") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                logging.logger(SERVICE).info { "first" }
                logging.logger(SERVICE).info { "second" }
                logging.flush()

                val first = sink.records.first()
                sink.records.map { record -> record.sequence } shouldBe listOf(1L, 2L)
                first.timestamp shouldBe TIMESTAMP
                first.source.logger shouldBe LoggerName(SERVICE)
                first.source.thread.isNotBlank() shouldBe true
            }
        }

        test("carries fields, markers and the captured cause") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                logging.logger(SERVICE).error {
                    field(name = "rack", value = "r1")
                    cause(IllegalStateException(BROKEN, IllegalArgumentException("inner")))
                    "failed"
                }
                logging.flush()

                val record = sink.records.single()
                record.fields shouldBe mapOf("rack" to LogValue.Text("r1"))
                record.cause?.type shouldBe "java.lang.IllegalStateException"
                record.cause?.cause?.message shouldBe "inner"
            }
        }

        test("adds the caller when the logger asks for it") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    logger(SERVICE) { isCallerIncluded = true }
                }

                logging.logger(SERVICE).info { "located" }
                logging.flush()

                val source = sink.records.single().source
                source.caller.shouldNotBeNull().className shouldContain "LoggingPipelineTest"
            }
        }
    }

    context("context") {
        test("adds the enrichment and the context of the coroutine") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    enrich {
                        field(name = "application", value = "kheck-client")
                        hostname()
                        user()
                        processId()
                    }
                }

                withLogContext({ field(name = TENANT, value = ACME) }) {
                    withLogContext({ field("request", 1) }) {
                        logging.logger(SERVICE).info { "scoped" }
                    }
                }
                logging.flush()

                val context = sink.records.single().context
                context["application"] shouldBe LogValue.Text("kheck-client")
                context[HOST] shouldBe LogValue.Text("rack-01")
                context["user"] shouldBe LogValue.Text("alice")
                context[TENANT] shouldBe LogValue.Text(ACME)
                context["request"] shouldBe LogValue.Integer(1L)
                (context["pid"] is LogValue.Integer) shouldBe true
            }
        }

        test("takes the computer name on Windows") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    environment = environment { variables(mapOf("COMPUTERNAME" to "WS-01")) }
                    capture(sink)
                    enrich { hostname() }
                }

                logging.logger(SERVICE).info { HOST }
                logging.flush()

                sink.records.single().context[HOST] shouldBe LogValue.Text("WS-01")
            }
        }

        test("names the hostname unknown when the environment has none") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    environment = Environment.EMPTY
                    capture(sink)
                    enrich { hostname() }
                }

                logging.logger(SERVICE).info { HOST }
                logging.flush()

                sink.records.single().context[HOST] shouldBe LogValue.Text("unknown")
            }
        }

        test("describes the context element by its keys") {
            val element = logContext(mapOf(TENANT to LogValue.Text(ACME)))[LogContext].shouldNotBeNull()

            element.toString() shouldBe "LogContext(fields=[tenant])"
        }

        test("binds the fields of a log context to every thread of the coroutine") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                val executor = Executors.newSingleThreadExecutor()
                executor.asCoroutineDispatcher().use { other ->
                    withContext(logContext { field(name = TENANT, value = ACME) }) {
                        withContext(other) {
                            logging.logger(SERVICE).info { "elsewhere" }
                        }
                    }
                }
                logging.flush()

                sink.records.single().context[TENANT] shouldBe LogValue.Text(ACME)
            }
        }
    }

    context("masking") {
        test("masks secrets before any sink sees them") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                logging.logger(SERVICE).info {
                    secret("token")
                    field(name = "password", value = "hunter2")
                    "connect with password=hunter2"
                }
                logging.flush()

                val record = sink.records.single()
                record.message shouldBe "connect with password=****"
                record.fields.values.toSet() shouldBe setOf(LogValue.Text("****"))
                logging.maskedTotal() shouldBe MASKED
            }
        }

        test("drops a record when masking fails") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    mask {
                        rule(
                            MaskingRule(name = BROKEN, pattern = Regex("x"), validator = { error(BROKEN) }),
                        )
                    }
                }

                logging.logger(SERVICE).info { "x" }
                logging.flush()

                sink.messages shouldBe emptyList()
                logging.recorded.messages.single() shouldContain "masking failed"
            }
        }
    }

    context("sampling") {
        test("summarises repeated records when the window ends") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    sample { dedupe(window = 1.seconds) }
                }
                val logger = logging.logger(SERVICE)

                repeat(REPEATS) { logger.warn { "disk almost full" } }
                logging.flush()
                advanceTimeBy(2.seconds)
                logging.close()

                sink.records shouldHaveSize 2
                sink.records.last().fields shouldBe mapOf(Deduplicator.REPEATED to LogValue.Integer(2L))
                logging.suppressedBy(Suppression.Deduplicated) shouldBe 2L
            }
        }

        test("counts records sampled away") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    sample { rate = 0.0 }
                }

                logging.logger(SERVICE).info { "dropped" }
                logging.flush()

                sink.messages shouldBe emptyList()
                logging.suppressedBy(Suppression.Sampled) shouldBe 1L
            }
        }
    }

    context("failures") {
        test("reports a record whose cause cannot be read") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                logging.logger(SERVICE).error {
                    cause(BrokenTrace(IllegalStateException("no trace")))
                    "unreadable"
                }
                logging.flush()

                sink.messages shouldBe emptyList()
                logging.recorded.messages.single() shouldContain "could not be processed"
            }
        }
    }
})
