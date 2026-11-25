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

package com.davils.kheck.kollect

import com.davils.kheck.api.info
import com.davils.kheck.core.Logging
import com.davils.kheck.core.LoggingBuilder
import com.davils.kheck.core.health.HealthRule
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.core.health.LoggingHealth
import com.davils.kheck.core.logging
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import com.davils.kheck.testing.RecordingDiagnostics
import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.SumPoint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import com.davils.kollect.core.health.HealthStatus as KollectStatus

private const val CAPTURE: String = "capture"
private const val FAMILIES: Int = 14

private fun TestScope.started(block: LoggingBuilder.() -> Unit = {}): Logging =
    logging(backgroundScope, StandardTestDispatcher(testScheduler)) {
        diagnostics = RecordingDiagnostics()
        block()
    }

private fun MetricsSnapshot.values(name: String): List<Double> {
    val family = family(MetricName.of(name)) ?: return emptyList()
    return family.points.map { point -> (point as? SumPoint)?.value ?: (point as GaugePoint).value }
}

class LoggingBindingsTest : FunSpec({
    context("meters") {
        test("report the statistics of the logging when the registry takes a snapshot") {
            runTest {
                val logging = started { sink(CAPTURE, CapturingSink()) }
                val registry = meterRegistry()
                logging.logger("a").info { "password=hunter2" }
                logging.flush()

                registry.bindLogging(logging).use { metrics ->
                    metrics.registrations.size shouldBe FAMILIES
                    metrics.toString() shouldContain "14 families"
                }
                registry.snapshot().values(LoggingMetricNames.RECORDS) shouldBe emptyList()
                val snapshot = registry.bindLogging(logging).use { registry.snapshot() }
                snapshot.values(LoggingMetricNames.RECORDS).sum() shouldBe 1.0
                snapshot.values(LoggingMetricNames.SINK_WRITTEN) shouldBe listOf(1.0)
                snapshot.values(LoggingMetricNames.SINK_FAILING) shouldBe listOf(0.0)
                snapshot.values(LoggingMetricNames.MASKED).sum() shouldBe 1.0
                snapshot.values(LoggingMetricNames.DROPPED) shouldBe listOf(0.0, 0.0)
                snapshot.values(LoggingMetricNames.INTAKE_SATURATION) shouldBe listOf(0.0)
            }
        }

        test("report a failing sink") {
            runTest {
                val sink = CapturingSink()
                val logging = started { sink(CAPTURE, sink) }
                val registry = meterRegistry()
                sink.fail(IllegalStateException("down"))

                logging.logger("a").info { "lost" }
                logging.flush()

                val snapshot = registry.bindLogging(logging).use { registry.snapshot() }
                snapshot.values(LoggingMetricNames.SINK_FAILING) shouldBe listOf(1.0)
            }
        }
    }

    context("health") {
        test("reports every status of the logging") {
            runTest {
                val up = started { healthRules = emptyList() }
                val degraded = started { healthRules = listOf(rule(HealthStatus.Degraded)) }
                val down = started { healthRules = listOf(rule(HealthStatus.Down)) }

                up.healthIndicator().check().status shouldBe KollectStatus.Up
                degraded.healthIndicator().check().status shouldBe KollectStatus.Degraded
                down.healthIndicator().check().status shouldBe KollectStatus.Down
            }
        }
    }
})

private fun rule(status: HealthStatus): HealthRule = HealthRule { LoggingHealth(status, listOf("judged")) }
