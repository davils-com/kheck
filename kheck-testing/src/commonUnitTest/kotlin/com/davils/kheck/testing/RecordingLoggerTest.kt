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

package com.davils.kheck.testing

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.info
import com.davils.kheck.api.record.LogEvent
import com.davils.kheck.api.warn
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val AREA: String = "kheck.redis"
private const val POOL: String = "kheck.redis.pool"
private const val OPENED: String = "opened"
private const val LOST: String = "lost"

class RecordingLoggerTest : FunSpec({
    context("recording") {
        test("keeps every event at or above its threshold in the order it was emitted") {
            val logger = RecordingLogger(LoggerName(AREA), LogLevel.Info)
            logger.emit(LogLevel.Debug, LogEvent("ignored"))
            logger.info { OPENED }
            logger.warn { LOST }

            logger.messages shouldBe listOf(OPENED, LOST)
            logger.levels shouldBe listOf(LogLevel.Info, LogLevel.Warn)
            logger.events.map { event -> event.message } shouldBe listOf(OPENED, LOST)
            logger.isEnabled(LogLevel.Debug) shouldBe false
            logger.toString() shouldBe "RecordingLogger(kheck.redis)"
        }

        test("forgets what it recorded when it is cleared") {
            val logger = RecordingLogger()
            logger.info { OPENED }
            logger.clear()

            logger.entries.shouldBeEmpty()
        }
    }

    context("children") {
        test("carry their own name and record into the journal of their parent") {
            val logger = RecordingLogger(LoggerName(AREA))
            val pool = logger.child("pool")
            pool.info { OPENED }
            logger.warn { LOST }

            pool.name shouldBe LoggerName(POOL)
            logger.entries.map { entry -> entry.logger.value } shouldBe listOf(POOL, AREA)
            logger.entries.first() shouldBe LoggedEvent(LogLevel.Info, LogEvent(OPENED), LoggerName(POOL))
        }

        test("keep the threshold of their parent") {
            val logger = RecordingLogger(threshold = LogLevel.Warn)
            logger.child("pool").info { OPENED }

            logger.entries.shouldBeEmpty()
        }
    }

    context("logged events") {
        test("compare level, event and logger") {
            val event = LoggedEvent(LogLevel.Info, LogEvent(OPENED), LoggerName(AREA))

            event.hashCode() shouldBe LoggedEvent(LogLevel.Info, LogEvent(OPENED), LoggerName(AREA)).hashCode()
            event shouldNotBe LoggedEvent(LogLevel.Warn, LogEvent(OPENED), LoggerName(AREA))
            event shouldNotBe LoggedEvent(LogLevel.Info, LogEvent(LOST), LoggerName(AREA))
            event shouldNotBe LoggedEvent(LogLevel.Info, LogEvent(OPENED), LoggerName(POOL))
            event.equals(OPENED) shouldBe false
            event.toString() shouldBe "LoggedEvent(level=Info, logger=kheck.redis, event=${LogEvent(OPENED)})"
        }
    }
})
