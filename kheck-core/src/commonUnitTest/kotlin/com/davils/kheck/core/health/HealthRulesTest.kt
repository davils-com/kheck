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

package com.davils.kheck.core.health

import com.davils.kheck.core.statistics.LoggingStatistics
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val LOW: Double = 1.0
private const val HIGH: Double = 2.0

private const val READING: String = "reading"

private fun bounded(value: Double): HealthRule =
    HealthRules.threshold(name = READING, degradedAt = LOW, downAt = HIGH, reading = reading(value))

private fun reading(value: Double): (LoggingStatistics) -> Double = { value }

class HealthRulesTest : FunSpec({
    context("thresholds") {
        test("are up below, degraded from and down from their limits") {
            val rule = bounded(0.0)
            val degraded = bounded(LOW)
            val down = bounded(HIGH)
            val never = HealthRules.threshold(name = READING, reading = reading(HIGH))
            val statistics = emptyStatistics()

            rule.evaluate(statistics).status shouldBe HealthStatus.Up
            degraded.evaluate(statistics).status shouldBe HealthStatus.Degraded
            down.evaluate(statistics).reasons shouldBe listOf("reading is 2.0")
            down.evaluate(statistics).status shouldBe HealthStatus.Down
            never.evaluate(statistics) shouldBe LoggingHealth.UP
        }
    }

    context("combination") {
        test("takes the worst status and every reason") {
            val degraded = LoggingHealth(HealthStatus.Degraded, listOf("a"))
            val down = LoggingHealth(HealthStatus.Down, listOf("b"))
            val combined = LoggingHealth.worstOf(listOf(degraded, down))

            combined.status shouldBe HealthStatus.Down
            combined.reasons shouldBe listOf("a", "b")
            LoggingHealth.worstOf(emptyList()) shouldBe LoggingHealth.UP
            combined.toString() shouldBe "LoggingHealth(status=Down, reasons=[a, b])"
        }
    }

    context("equality") {
        test("compares results by status and reasons") {
            val degraded = LoggingHealth(HealthStatus.Degraded, listOf("slow"))

            degraded shouldBe LoggingHealth(HealthStatus.Degraded, listOf("slow"))
            degraded.hashCode() shouldBe LoggingHealth(HealthStatus.Degraded, listOf("slow")).hashCode()
            degraded shouldNotBe LoggingHealth.UP
            degraded.equals("slow") shouldBe false
        }
    }
})

internal fun emptyStatistics(): LoggingStatistics = LoggingStatistics(
    recorded = emptyMap(),
    suppressed = emptyMap(),
    intakeQueued = 0L,
    intakeCapacity = 1,
    intakeDropped = 0L,
    droppedAfterClose = 0L,
    reentrant = 0L,
    masked = emptyMap(),
    failedListeners = 0L,
    sinks = emptyMap(),
)
