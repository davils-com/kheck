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

package com.davils.kheck.slf4j

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.Logging
import com.davils.kheck.core.LoggingBuilder
import com.davils.kheck.core.logging
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.MarkerFactory
import org.slf4j.event.DefaultLoggingEvent
import org.slf4j.event.Level
import org.slf4j.spi.LoggingEventAware
import kotlin.concurrent.withLock

private const val NAME: String = "com.example.Library"
private const val PENDING: Int = 1_025
private const val COUNT: Int = 3
private const val TOTAL: Long = 4L
private const val RATIO: Double = 0.5
private const val RACK: String = "rack"
private const val TENANT: String = "tenant"
private const val ACME: String = "acme"
private const val TRACE: String = "trace"
private const val USER: String = "user"
private const val ALICE: String = "alice"
private const val BLANK: String = "blank"
private const val UNKNOWN: String = "unknown"
private const val OUTER: String = "outer"
private const val INNER: String = "inner"

class Slf4jBridgeTest : FunSpec({
    afterEach { Slf4jBridge.detach() }

    context("binding") {
        test("serves its own loggers through SLF4J") {
            LoggerFactory.getLogger(NAME).shouldBeInstanceOf<BridgeLogger>()
            LoggerFactory.getLogger(NAME).name shouldBe NAME
        }

        test("keeps records of the startup and writes them once logging is attached") {
            runTest {
                val sink = CapturingSink()
                val logger = LoggerFactory.getLogger(NAME)

                logger.info("early {}", "bird")
                logger.debug("too detailed")
                val logging = bound(sink)
                logging.flush()

                sink.messages shouldBe listOf("early bird")
                Slf4jBridge.isAttached shouldBe true
                Slf4jBridge.detach()
                Slf4jBridge.isAttached shouldBe false
            }
        }

        test("counts the records it had to forget before attaching") {
            runTest {
                val lost = Slf4jBridge.lost
                val logger = LoggerFactory.getLogger(NAME)

                repeat(PENDING) { logger.warn("waiting") }
                val counted = Slf4jBridge.lost
                bound(CapturingSink())

                counted shouldBe lost + 1
            }
        }
    }

    context("races") {
        test("writes a record directly when logging is attached while the record waits") {
            runTest {
                val sink = CapturingSink()
                val logging = logging(backgroundScope, StandardTestDispatcher(testScheduler)) { sink("capture", sink) }
                val racer = Thread { LoggerFactory.getLogger(NAME).info("racing") }

                Slf4jBridge.lock.withLock {
                    racer.start()
                    while (!Slf4jBridge.lock.hasQueuedThreads()) Thread.onSpinWait()
                    Slf4jBridge.attach(logging)
                }
                racer.join()
                logging.flush()

                sink.messages shouldBe listOf("racing")
            }
        }
    }

    context("records") {
        test("maps levels, markers and causes") {
            runTest {
                val sink = CapturingSink()
                val logging = bound(sink) { level = LogLevel.Trace }
                val logger = LoggerFactory.getLogger(NAME)
                val marker = MarkerFactory.getMarker("AUDIT")

                logger.trace(marker, "t")
                logger.debug(marker, "d")
                logger.info(marker, "i")
                logger.warn(marker, "w")
                logger.error(marker, "e", IllegalStateException("broken"))
                logging.flush()

                sink.records.map { record -> record.level } shouldBe
                    listOf(
                        LogLevel.Trace,
                        LogLevel.Debug,
                        LogLevel.Info,
                        LogLevel.Warn,
                        LogLevel.Error,
                    )
                sink.records.last().markers shouldBe setOf(LogMarker.AUDIT)
                sink.records
                    .last()
                    .cause
                    ?.message shouldBe "broken"
            }
        }

        test("turns key value pairs of the fluent API into fields") {
            runTest {
                val sink = CapturingSink()
                val logging = bound(sink)
                val logger = LoggerFactory.getLogger(NAME)

                val pairs = listOf<Pair<String, Any?>>(
                    "count" to COUNT,
                    "total" to TOTAL,
                    "ratio" to RATIO,
                    "flag" to true,
                    "name" to RACK,
                    "missing" to null,
                )
                val marked = logger.atWarn().addMarker(MarkerFactory.getMarker("SECURITY"))
                val event = pairs.fold(marked) { builder, (key, value) -> builder.addKeyValue(key, value) }
                event.log("checked {}", RACK)
                val hidden = logger.atDebug().addKeyValue("hidden", 1)
                hidden.log("hidden")
                logging.flush()

                val record = sink.records.single()
                record.message shouldBe "checked rack"
                record.markers shouldBe setOf(LogMarker.SECURITY)
                record.fields shouldBe mapOf(
                    "count" to LogValue.Integer(COUNT.toLong()),
                    "total" to LogValue.Integer(TOTAL),
                    "ratio" to LogValue.Decimal(RATIO),
                    "flag" to LogValue.Bool(true),
                    "name" to LogValue.Text(RACK),
                    "missing" to LogValue.Null,
                )
            }
        }

        test("answers every level check with and without marker") {
            runTest {
                bound(CapturingSink()) { level = LogLevel.Warn }
                val logger = LoggerFactory.getLogger(NAME)
                val marker = MarkerFactory.getMarker("M")

                listOf(
                    logger.isTraceEnabled(marker),
                    logger.isDebugEnabled(marker),
                    logger.isInfoEnabled(marker),
                    logger.isWarnEnabled(marker),
                    logger.isErrorEnabled(marker),
                    logger.isEnabledForLevel(Level.ERROR),
                ) shouldBe listOf(false, false, false, true, true, true)
            }
        }
    }

    context("events") {
        test("handles events without pattern, pairs or markers and skips disabled ones") {
            runTest {
                val sink = CapturingSink()
                val logging = bound(sink)
                val logger = LoggerFactory.getLogger(NAME)
                val aware = logger as LoggingEventAware

                logger.info(null as String?)
                aware.log(DefaultLoggingEvent(Level.INFO, logger))
                aware.log(DefaultLoggingEvent(Level.DEBUG, logger))
                logging.flush()

                sink.messages shouldBe listOf("", "")
                sink.records.last().fields shouldBe emptyMap()
            }
        }
    }

    context("limits") {
        test("drop markers of SLF4J that kheck cannot carry and count them") {
            runTest {
                val sink = CapturingSink()
                val logging = bound(sink)
                val logger = LoggerFactory.getLogger(NAME)

                val marked = logger.atInfo().addMarker(MarkerFactory.getDetachedMarker(" "))
                marked.log("blank marker")
                logging.flush()

                sink.records.single().markers shouldBe emptySet()
                sink.records.single().omitted shouldBe 1
            }
        }

        test("keep a value of an unknown type as text") {
            runTest {
                val sink = CapturingSink()
                val logging = bound(sink)

                val builder = LoggerFactory.getLogger(NAME).atInfo()
                val withRack = builder.addKeyValue(RACK, LogValue.Bool(true))
                withRack.addKeyValue(USER, 'c').log("x")
                logging.flush()

                sink.records.single().fields shouldBe mapOf(RACK to LogValue.Bool(true), USER to LogValue.Text("c"))
            }
        }
    }

    context("MDC") {
        test("keeps the MDC in the context of records") {
            runTest {
                val sink = CapturingSink()
                val logging = bound(sink)
                val logger = LoggerFactory.getLogger(NAME)

                MDC.put(TENANT, ACME)
                MDC.put(BLANK, null)
                logger.info("scoped")
                val copy = MDC.getCopyOfContextMap()
                MDC.remove(BLANK)
                val tenant = MDC.get(TENANT)
                MDC.setContextMap(mapOf(USER to ALICE, "none" to null))
                val user = MDC.get(USER)
                MDC.clear()
                logging.flush()

                sink.records.single().context shouldBe mapOf(
                    TENANT to LogValue.Text(ACME),
                    BLANK to LogValue.Null,
                )
                copy shouldBe mapOf(TENANT to ACME, BLANK to null)
                tenant shouldBe ACME
                user shouldBe ALICE
                MDC.get(USER) shouldBe null
            }
        }

        test("keeps deques per key") {
            MDC.pushByKey(TRACE, OUTER)
            MDC.pushByKey(TRACE, INNER)

            val adapter = MDC.getMDCAdapter()

            adapter.getCopyOfDequeByKey(TRACE).toList() shouldBe listOf(OUTER, INNER)
            MDC.popByKey(TRACE) shouldBe INNER
            adapter.clearDequeByKey(TRACE)
            MDC.popByKey(TRACE) shouldBe null
            adapter.getCopyOfDequeByKey(UNKNOWN) shouldBe null
            adapter.clearDequeByKey(UNKNOWN)
            MDC.popByKey(UNKNOWN) shouldBe null
            MDC.clear()
        }
    }
})

private fun TestScope.bound(sink: CapturingSink, block: LoggingBuilder.() -> Unit = {}): Logging {
    val logging = logging(backgroundScope, StandardTestDispatcher(testScheduler)) {
        sink("capture", sink)
        block()
    }
    Slf4jBridge.attach(logging)
    return logging
}
