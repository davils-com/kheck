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

package com.davils.kheck.jul

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.Logging
import com.davils.kheck.core.LoggingBuilder
import com.davils.kheck.core.logging
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.logging.ErrorManager
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger

private const val LONG_NAME: Int = 600
private const val NAME: String = "com.example.Legacy"

@OptIn(ExperimentalCoroutinesApi::class)
class JulBridgeTest : FunSpec({
    context("records") {
        test("forwards formatted records with source and cause") {
            runTest {
                val sink = CapturingSink()
                val logging = bridged(sink)
                val record = LogRecord(Level.WARNING, "disk {0} is full")
                record.loggerName = NAME
                record.parameters = arrayOf("sda")
                record.sourceClassName = "Disk"
                record.sourceMethodName = "check"
                record.thrown = IllegalStateException("full")

                logging.bridgeJul(backgroundScope).use {
                    Logger.getLogger(NAME).log(record)
                    logging.flush()
                }

                val written = sink.records.single()
                written.message shouldBe "disk sda is full"
                written.level shouldBe LogLevel.Warn
                written.fields shouldBe mapOf(
                    "jul.class" to LogValue.Text("Disk"),
                    "jul.method" to LogValue.Text("check"),
                )
                written.cause?.message shouldBe "full"
            }
        }

        test("skips records below the level of kheck and forwards records without source") {
            runTest {
                val sink = CapturingSink()
                val logging = bridged(sink)
                logging.bridgeJul(backgroundScope).use {
                    val root = LogManager.getLogManager().getLogger("")
                    val handler = root.handlers.single()
                    handler.publish(LogRecord(Level.FINE, "hidden"))
                    handler.publish(LogRecord(Level.SEVERE, "anonymous"))
                    handler.flush()
                    handler.close()
                    logging.flush()
                }

                sink.records.map { record -> record.message } shouldBe listOf("anonymous")
                sink.records.single().fields shouldBe emptyMap()
            }
        }
    }

    context("failures") {
        test("reports a record it cannot hand over instead of throwing") {
            runTest {
                val logging = bridged(CapturingSink())
                val errors = mutableListOf<String>()
                logging.bridgeJul(backgroundScope).use {
                    val root = LogManager.getLogManager().getLogger("")
                    val handler = root.handlers.single()
                    handler.errorManager = object : ErrorManager() {
                        override fun error(msg: String?, ex: Exception?, code: Int) {
                            errors += msg.orEmpty()
                        }
                    }
                    val record = LogRecord(Level.SEVERE, "long")
                    record.loggerName = "n".repeat(LONG_NAME)
                    handler.publish(record)
                }

                errors.single() shouldContain "could not be handed"
            }
        }
    }

    context("levels") {
        test("maps every level of kheck to JUL") {
            LogLevel.entries.map { level -> level.toJulLevel() } shouldBe listOf(
                Level.FINEST,
                Level.FINE,
                Level.INFO,
                Level.WARNING,
                Level.SEVERE,
                Level.OFF,
            )
        }

        test("maps every level of JUL") {
            val levels = listOf(
                Level.SEVERE,
                Level.WARNING,
                Level.INFO,
                Level.CONFIG,
                Level.FINE,
                Level.FINER,
                Level.FINEST,
                Level.ALL,
            )

            levels.map { level -> level.toLogLevel() } shouldBe listOf(
                LogLevel.Error,
                LogLevel.Warn,
                LogLevel.Info,
                LogLevel.Debug,
                LogLevel.Debug,
                LogLevel.Trace,
                LogLevel.Trace,
                LogLevel.Trace,
            )
        }

        test("mirrors the configured and overridden levels into JUL and restores JUL on close") {
            runTest {
                val logging = bridged(CapturingSink()) {
                    logger(NAME) { level = LogLevel.Debug }
                    logger("com.example.Inherited") { isAdditive = true }
                }
                val root = LogManager.getLogManager().getLogger("")
                val before = root.level
                logging.bridgeJul(backgroundScope).use {
                    runCurrent()
                    val configured = Logger.getLogger(NAME).level

                    logging.levels.set("", LogLevel.Warn)
                    logging.levels.set("com.example.Other", LogLevel.Trace)
                    runCurrent()

                    root.level shouldBe Level.WARNING
                    configured shouldBe Level.FINE
                    Logger.getLogger("com.example.Other").level shouldBe Level.FINEST
                }
                root.level shouldBe before
                Logger.getLogger(NAME).level shouldBe null
            }
        }
    }
})

private fun TestScope.bridged(sink: CapturingSink, block: LoggingBuilder.() -> Unit = {}): Logging =
    logging(backgroundScope, StandardTestDispatcher(testScheduler)) {
        sink("capture", sink)
        block()
    }
