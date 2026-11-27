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

package com.davils.kheck.core.fixture

import com.davils.kheck.core.Logging
import com.davils.kheck.core.LoggingBuilder
import com.davils.kheck.core.format.plain.plain
import com.davils.kheck.core.logging
import com.davils.kheck.core.sink.custom.CustomSinkBuilder
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.core.statistics.SinkStatistics
import com.davils.kheck.core.statistics.Suppression
import com.davils.kheck.testing.CapturingSink
import com.davils.kheck.testing.RecordingDiagnostics
import com.davils.knot.environment.environment
import com.davils.kore.testing.TestClock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

internal const val CAPTURE: String = "capture"

internal val TIMESTAMP: Instant = Instant.parse("2026-11-10T08:15:30.250Z")

internal const val SERVICE: String = "com.example.inventory.ServerService"

internal fun TestScope.testLogging(block: LoggingBuilder.() -> Unit = {}): Logging =
    logging(backgroundScope, StandardTestDispatcher(testScheduler)) {
        clock = TestClock(TIMESTAMP)
        timeSource = testScheduler.timeSource
        environment = environment { variables(mapOf("HOSTNAME" to "rack-01", "USER" to "alice")) }
        diagnostics = RecordingDiagnostics()
        block()
    }

internal fun LoggingBuilder.capture(
    sink: CapturingSink,
    name: String = CAPTURE,
    block: CustomSinkBuilder.() -> Unit = {},
) {
    sink(name, sink) {
        format = plain {
            zone = TimeZone.UTC
            layout { message() }
        }
        block()
    }
}

internal val Logging.recorded: RecordingDiagnostics
    get() = configuration.diagnostics as RecordingDiagnostics

internal fun Logging.sinkStatistics(name: String): SinkStatistics {
    val statistics = statistics()
    return statistics.sinks.getValue(name)
}

internal fun Logging.suppressedBy(reason: Suppression): Long {
    val statistics = statistics()
    return statistics.suppressed.getValue(reason)
}

internal fun Logging.maskedTotal(): Long {
    val statistics = statistics()
    return statistics.masked.values.sum()
}

internal fun Logging.recordedTotal(): Long {
    val statistics = statistics()
    return statistics.recorded.values.sum()
}

internal fun Logging.failedTotal(): Long {
    val statistics = statistics()
    return statistics.sinks.values.sumOf { sink -> sink.failed }
}
