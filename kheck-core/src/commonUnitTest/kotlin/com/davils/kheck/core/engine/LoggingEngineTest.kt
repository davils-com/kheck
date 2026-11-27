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

package com.davils.kheck.core.engine

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.core.fixture.loggingConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

private const val REMEMBERED: String = "com.example.Remembered"
private const val FORGOTTEN: String = "com.example.Forgotten"

class LoggingEngineTest : FunSpec({
    context("loggers beyond the limit") {
        test("follow runtime levels although they are not remembered") {
            runTest {
                val engine = LoggingEngine(
                    scope = backgroundScope,
                    dispatcher = StandardTestDispatcher(testScheduler),
                    initial = loggingConfiguration { },
                    maxLoggers = 1,
                )
                val remembered = engine.logger(LoggerName(REMEMBERED))
                val forgotten = engine.logger(LoggerName(FORGOTTEN))

                engine.levels.set(FORGOTTEN, LogLevel.Trace)
                engine.levels.set(REMEMBERED, LogLevel.Error)

                forgotten.isEnabled(LogLevel.Trace) shouldBe true
                remembered.isEnabled(LogLevel.Warn) shouldBe false
            }
        }

        test("follow a reconfiguration although they are not remembered") {
            runTest {
                val engine = LoggingEngine(
                    scope = backgroundScope,
                    dispatcher = StandardTestDispatcher(testScheduler),
                    initial = loggingConfiguration { },
                    maxLoggers = 0,
                )
                val forgotten = engine.logger(LoggerName(FORGOTTEN))
                forgotten.isEnabled(LogLevel.Debug) shouldBe false

                engine.reconfigure(loggingConfiguration { level = LogLevel.Debug })

                forgotten.isEnabled(LogLevel.Debug) shouldBe true
            }
        }
    }
})
