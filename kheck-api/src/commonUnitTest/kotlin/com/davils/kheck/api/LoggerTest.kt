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

package com.davils.kheck.api

import com.davils.kheck.api.record.LogEvent
import com.davils.kheck.api.record.LogMarker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val MESSAGE: String = "connected"
private const val CALLS: Int = 3

class LoggerTest : FunSpec({
    context("the logger that logs nothing") {
        test("is disabled for every level, takes events and hands itself out as its child") {
            LogLevel.entries.forEach { level -> Logger.None.isEnabled(level) shouldBe false }
            Logger.None.emit(LogLevel.Error, LogEvent(MESSAGE))
            Logger.None.name shouldBe LoggerName.ROOT
            Logger.None.child("pool") shouldBeSameInstanceAs Logger.None
        }

        test("never evaluates the event of a call") {
            val evaluated = mutableListOf<String>()
            Logger.None.error {
                evaluated += MESSAGE
                MESSAGE
            }
            evaluated.shouldBeEmpty()
        }
    }

    context("a call below the threshold") {
        test("never evaluates its event") {
            val logger = CollectingLogger(LogLevel.Info)
            val evaluated = mutableListOf<String>()
            logger.trace {
                evaluated += "trace"
                MESSAGE
            }
            logger.debug {
                evaluated += "debug"
                MESSAGE
            }
            logger.info {
                evaluated += "info"
                MESSAGE
            }
            evaluated shouldBe listOf("info")
            logger.levels shouldBe listOf(LogLevel.Info)
        }
    }

    context("a call at or above the threshold") {
        test("emits one event per level with its fields, its cause and its markers") {
            val logger = CollectingLogger(LogLevel.Trace)
            val failure = IllegalStateException("refused")
            logger.trace { MESSAGE }
            logger.debug { MESSAGE }
            logger.info {
                field("attempts", CALLS)
                marker(LogMarker.AUDIT)
                MESSAGE
            }
            logger.warn { MESSAGE }
            logger.error {
                cause(failure)
                MESSAGE
            }
            logger.log(LogLevel.Off) { MESSAGE }
            logger.levels shouldBe listOf(LogLevel.Trace, LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error)
            val audited = logger.events[2]
            audited.fields.keys shouldBe setOf("attempts")
            audited.markers shouldBe setOf(LogMarker.AUDIT)
            logger.events.last().cause shouldBe failure
        }
    }
})
