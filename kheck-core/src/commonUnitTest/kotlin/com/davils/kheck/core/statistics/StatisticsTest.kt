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

package com.davils.kheck.core.statistics

import com.davils.kheck.api.LogLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.seconds

private const val QUEUED: Long = 3L
private const val CAPACITY: Int = 4

class StatisticsTest : FunSpec({
    context("snapshots") {
        test("compute the saturation and the failing sinks and show their totals") {
            val failing = SinkStatistics(
                written = 1L,
                failed = 1L,
                dropped = 0L,
                queued = 0L,
                batches = 1L,
                writeTime = 1.seconds,
                maxWriteTime = 1.seconds,
                isFailing = true,
            )
            val statistics = LoggingStatistics(
                recorded = mapOf(LogLevel.Info to 2L),
                suppressed = mapOf(Suppression.Sampled to 1L),
                intakeQueued = QUEUED,
                intakeCapacity = CAPACITY,
                intakeDropped = 0L,
                droppedAfterClose = 0L,
                reentrant = 0L,
                masked = emptyMap(),
                failedListeners = 0L,
                sinks = mapOf("disk" to failing),
            )

            statistics.intakeSaturation shouldBe QUEUED.toDouble() / CAPACITY
            statistics.sinksFailing shouldBe 1
            statistics.toString() shouldContain "recorded=2, suppressed=1"
            failing.toString() shouldContain "isFailing=true"
        }
    }
})
