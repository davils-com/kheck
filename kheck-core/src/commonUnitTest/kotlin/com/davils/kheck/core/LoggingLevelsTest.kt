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
import com.davils.kheck.api.Logger
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.debug
import com.davils.kheck.api.error
import com.davils.kheck.api.info
import com.davils.kheck.api.trace
import com.davils.kheck.api.warn
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.testing.CapturingSink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

private const val EXAMPLE: String = "com.example"
private const val POOL: String = "com.example.pool"

class LoggingLevelsTest : FunSpec({
    context("thresholds") {
        test("writes records at or above the level and skips the rest") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }
                val logger = logging.logger(SERVICE)

                logger.trace { "trace" }
                logger.debug { "debug" }
                logger.info { "info" }
                logger.warn { "warn" }
                logger.error { "error" }
                logging.flush()

                sink.messages shouldBe listOf("info", "warn", "error")
            }
        }

        test("evaluates no message of a disabled level") {
            runTest {
                val logging = testLogging { level = LogLevel.Off }
                var isEvaluated = false

                logging.logger(SERVICE).error {
                    isEvaluated = true
                    "never"
                }

                isEvaluated shouldBe false
                logging.logger(SERVICE).isEnabled(LogLevel.Off) shouldBe false
            }
        }

        test("takes the level of the nearest configured ancestor") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    logger(EXAMPLE) { level = LogLevel.Debug }
                    logger("com.example.inventory") { isAdditive = true }
                    logger("com.example.inventory.ServerService") { level = LogLevel.Error }
                }

                logging.logger("com.example.Other").debug { "other" }
                logging.logger("com.example.inventory.Rack").debug { "rack" }
                logging.logger(SERVICE).warn { "service" }
                logging.logger("org.vendor.Library").debug { "vendor" }
                logging.flush()

                sink.messages shouldBe listOf("other", "rack")
            }
        }
    }

    context("overrides") {
        test("changes levels at runtime and restores them") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }
                val logger = logging.logger(SERVICE)

                logging.levels.set(EXAMPLE, LogLevel.Debug)
                logger.debug { "first" }
                logging.levels.clear(EXAMPLE)
                logger.debug { "second" }
                logging.levels.set("", LogLevel.Trace)
                logger.trace { "third" }
                logging.levels.reset()
                logger.trace { "fourth" }
                logging.flush()

                sink.messages shouldBe listOf("first", "third")
                logging.levels.overrides.value shouldBe emptyMap()
            }
        }

        test("applies overrides to loggers created later") {
            runTest {
                val logging = testLogging { }

                logging.levels.set(SERVICE, LogLevel.Trace)

                logging.logger(SERVICE).isEnabled(LogLevel.Trace) shouldBe true
            }
        }
    }

    context("loggers") {
        test("names a logger after its class without the companion") {
            runTest {
                val logging = testLogging { }

                logging.logger<LoggingLevelsTest>().name.value shouldBe
                    "com.davils.kheck.core.LoggingLevelsTest"
                val companion = logging.logger(LoggerName.Companion::class)
                companion.name.value shouldBe "com.davils.kheck.api.LoggerName"
                logging.logger(SERVICE).toString() shouldBe "Logger($SERVICE)"
            }
        }

        test("returns the same logger for the same name") {
            runTest {
                val logging = testLogging { }

                (logging.logger(SERVICE) === logging.logger(SERVICE)) shouldBe true
            }
        }

        test("hands out the logger named below it as its child") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }

                val child = logging.logger(EXAMPLE).child("pool")
                child.info { "opened" }
                logging.flush()

                val record = sink.records.single()
                child.name shouldBe LoggerName(POOL)
                (child === logging.logger(POOL)) shouldBe true
                record.source.logger shouldBe child.name
            }
        }

        test("offers a logger that writes nothing") {
            Logger.None.isEnabled(LogLevel.Error) shouldBe false
            Logger.None.name shouldBe LoggerName.ROOT
            Logger.None.error { "ignored" }
        }
    }
})
