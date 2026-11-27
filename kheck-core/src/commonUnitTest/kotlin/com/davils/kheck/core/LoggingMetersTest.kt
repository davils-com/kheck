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

import com.davils.kheck.api.info
import com.davils.kheck.core.fixture.CAPTURE
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.health.HealthRule
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.core.health.LoggingHealth
import com.davils.kheck.testing.CapturingSink
import com.davils.kollect.core.health.ReasonCode
import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.konfetti.core.eventBus
import com.davils.konfetti.testing.EventRecorder
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import com.davils.kollect.core.health.HealthStatus as KollectStatus

private const val SERVICE_LABEL: String = "service"
private const val BILLING: String = "billing"

class LoggingMetersTest : FunSpec({
    context("meters") {
        test("declare the statistics with the labels of the application and time every write") {
            runTest {
                val logging = testLogging { capture(CapturingSink()) }
                val registry = meterRegistry()
                val labels = mapOf(SERVICE_LABEL to BILLING)

                val snapshot = logging.bindTo(registry, labels).use { meters ->
                    logging.logger(SERVICE).info { "first" }
                    logging.flush()
                    meters.toString() shouldBe "LoggingMeters"
                    registry.snapshot()
                }

                snapshot.values(LoggingMetricNames.SINK_WRITTEN) shouldBe listOf(1.0)
                snapshot.values(LoggingMetricNames.SINK_BATCHES) shouldBe listOf(1.0)
                snapshot.values(LoggingMetricNames.SINKS_FAILING) shouldBe listOf(0.0)
                snapshot.values(LoggingMetricNames.CIRCUITS_OPEN) shouldBe listOf(0.0)
                snapshot.values(LoggingMetricNames.EVENTS_REJECTED) shouldBe listOf(0.0)
                snapshot.values(LoggingMetricNames.SINK_REJECTED) shouldBe listOf(0.0)
                snapshot.values(LoggingMetricNames.SINK_MAX_WRITE_TIME).size shouldBe 1
                val write = snapshot.family(MetricName.of(LoggingMetricNames.SINK_WRITE)).shouldNotBeNull()
                val point = write.points.single() as HistogramPoint
                point.count shouldBe 1L
                point.labels.values shouldBe listOf(BILLING, CAPTURE)
                registry.snapshot().family(MetricName.of(LoggingMetricNames.SINK_WRITE)) shouldBe null
                registry.snapshot().values(LoggingMetricNames.RECORDS) shouldBe emptyList()
            }
        }
    }

    context("health indicator") {
        test("passes the codes of the reasons on") {
            runTest {
                val up = testLogging { healthRules = emptyList() }
                val degraded = testLogging { healthRules = listOf(HealthRule { error("broken") }) }
                val down = testLogging {
                    healthRules = listOf(
                        HealthRule {
                            LoggingHealth(HealthStatus.Down, listOf("gone"), listOf(ReasonCode.StateMatched))
                        },
                    )
                }

                up.healthIndicator().check().status shouldBe KollectStatus.Up
                degraded.healthIndicator().check().reasons shouldBe listOf(ReasonCode.CheckFailed)
                down.healthIndicator().check().status shouldBe KollectStatus.Down
                down.healthIndicator().check().reasons shouldBe listOf(ReasonCode.StateMatched)
                LoggingHealth(HealthStatus.Degraded, listOf("old")).codes shouldBe listOf(ReasonCode.Reported)
            }
        }
    }

    context("events") {
        test("publish the events of the logging on the topic of the bus") {
            runTest {
                val bus = eventBus(backgroundScope)
                bus.declare(LoggingEvent.TOPIC)
                val recorder = EventRecorder<LoggingEvent>()
                bus[LoggingEvent.TOPIC].consume(name = "test", owner = backgroundScope) {
                    handle { envelope -> recorder.record(envelope) }
                }
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    events(bus)
                }
                sink.fail(IllegalStateException("down"))

                logging.logger(SERVICE).info { "lost" }
                logging.flush()

                val envelopes = recorder.awaitCount(1)
                envelopes.single().payload.toString() shouldBe "SinkFailed(sink=$CAPTURE, cause=IllegalStateException)"
                logging.statistics().eventsRejected shouldBe 0L
            }
        }

        test("declare the topic and skip events while nobody consumes") {
            runTest {
                val bus = eventBus(backgroundScope)
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink)
                    events(bus)
                }
                sink.fail(IllegalStateException("down"))

                logging.logger(SERVICE).info { "lost" }
                logging.flush()

                (LoggingEvent.TOPIC in bus) shouldBe true
                logging.statistics().eventsRejected shouldBe 0L
            }
        }
    }

    context("meters without labels") {
        test("report a failing sink and share the families of a second binding") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) }
                val registry = meterRegistry()
                sink.fail(IllegalStateException("down"))
                logging.logger(SERVICE).info { "lost" }
                logging.flush()

                val snapshot = logging.bindTo(registry).use {
                    logging.bindTo(registry).use { registry.snapshot() }
                }

                snapshot.values(LoggingMetricNames.SINK_FAILING) shouldBe listOf(1.0)
                snapshot.values(LoggingMetricNames.SINKS_FAILING) shouldBe listOf(1.0)
            }
        }
    }
})

private fun MetricsSnapshot.values(name: String): List<Double> {
    val family = family(MetricName.of(name)) ?: return emptyList()
    return family.points.map { point -> (point as? SumPoint)?.value ?: (point as GaugePoint).value }
}
