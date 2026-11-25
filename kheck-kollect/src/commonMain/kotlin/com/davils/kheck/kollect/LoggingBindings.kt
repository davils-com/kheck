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

import com.davils.kheck.core.Logging
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.core.statistics.LoggingStatistics
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Registration
import com.davils.kollect.core.declare.FamilyBuilder
import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthIndicator
import com.davils.kollect.core.health.ReasonCode
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.ObservationSink

private const val NANOS_PER_SECOND: Double = 1_000_000_000.0

/**
 * Declares the statistics of [logging] as observed meters of this registry, named as [LoggingMetricNames] lists them.
 *
 * The meters read [Logging.statistics] whenever the registry takes a snapshot, on the thread that takes it; the logging
 * itself is never slowed down. Counters of a sink that a reconfiguration removed disappear with it.
 *
 * @param logging the logging to observe.
 * @return the declared meters; closing them removes them again.
 * @since 1.0.0
 */
public fun MeterRegistry.bindLogging(logging: Logging): LoggingMetrics {
    val read: () -> LoggingStatistics = logging::statistics
    val registrations = listOf(
        observedCounterOf(name = LoggingMetricNames.RECORDS, key = LoggingMetricNames.LEVEL) {
            read().recorded.forEach { (level, count) -> observe(count.toDouble(), level.name.lowercase()) }
        },
        observedCounterOf(name = LoggingMetricNames.SUPPRESSED, key = LoggingMetricNames.REASON) {
            read().suppressed.forEach { (reason, count) -> observe(count.toDouble(), reason.name.lowercase()) }
        },
        observedCounterOf(name = LoggingMetricNames.DROPPED, key = LoggingMetricNames.SINK) { observeDropped(read()) },
        observedCounterOf(name = LoggingMetricNames.DROPPED_AFTER_CLOSE) {
            observe(read().droppedAfterClose.toDouble())
        },
        observedCounterOf(name = LoggingMetricNames.REENTRANT) { observe(read().reentrant.toDouble()) },
        observedCounterOf(name = LoggingMetricNames.MASKED, key = LoggingMetricNames.RULE) {
            read().masked.forEach { (rule, count) -> observe(count.toDouble(), rule) }
        },
        observedCounterOf(name = LoggingMetricNames.FAILED_LISTENERS) { observe(read().failedListeners.toDouble()) },
        observedGaugeOf(name = LoggingMetricNames.INTAKE_QUEUED) { observe(read().intakeQueued.toDouble()) },
        observedGaugeOf(name = LoggingMetricNames.INTAKE_SATURATION, unit = MeasureUnit.RATIO) {
            observe(read().intakeSaturation)
        },
        observedCounterOf(name = LoggingMetricNames.SINK_WRITTEN, key = LoggingMetricNames.SINK) {
            read().sinks.forEach { (name, sink) -> observe(sink.written.toDouble(), name) }
        },
        observedCounterOf(name = LoggingMetricNames.SINK_FAILED, key = LoggingMetricNames.SINK) {
            read().sinks.forEach { (name, sink) -> observe(sink.failed.toDouble(), name) }
        },
        observedGaugeOf(name = LoggingMetricNames.SINK_QUEUED, key = LoggingMetricNames.SINK) {
            read().sinks.forEach { (name, sink) -> observe(sink.queued.toDouble(), name) }
        },
        observedCounterOf(
            name = LoggingMetricNames.SINK_WRITE_TIME,
            key = LoggingMetricNames.SINK,
            unit = MeasureUnit.SECONDS,
        ) {
            read().sinks.forEach { (name, sink) ->
                observe(sink.writeTime.inWholeNanoseconds / NANOS_PER_SECOND, name)
            }
        },
        observedGaugeOf(name = LoggingMetricNames.SINK_FAILING, key = LoggingMetricNames.SINK) {
            read().sinks.forEach { (name, sink) -> observe(flag(sink.isFailing), name) }
        },
    )
    return LoggingMetrics(registrations)
}

/**
 * Returns a health indicator of kollect that reports the [health][Logging.health] of this logging.
 *
 * A degraded or failed logging is reported with [ReasonCode.Reported], because its rules judged it.
 *
 * @return the indicator.
 * @since 1.0.0
 */
public fun Logging.healthIndicator(): HealthIndicator = HealthIndicator {
    when (health().status) {
        HealthStatus.Up -> Health.Up
        HealthStatus.Degraded -> Health.degraded(ReasonCode.Reported)
        HealthStatus.Down -> Health.down(ReasonCode.Reported)
    }
}

private fun ObservationSink.observeDropped(statistics: LoggingStatistics) {
    observe(statistics.intakeDropped.toDouble(), LoggingMetricNames.INTAKE)
    statistics.sinks.forEach { (name, sink) -> observe(sink.dropped.toDouble(), name) }
}

private fun flag(isSet: Boolean): Double {
    if (isSet) return 1.0
    return 0.0
}

private fun MeterRegistry.observedCounterOf(
    name: String,
    key: String? = null,
    unit: MeasureUnit = MeasureUnit.NONE,
    observe: ObservationSink.() -> Unit,
): Registration<ObservedFamily> = observedCounter(name, described(key, unit), observe)

private fun MeterRegistry.observedGaugeOf(
    name: String,
    key: String? = null,
    unit: MeasureUnit = MeasureUnit.NONE,
    observe: ObservationSink.() -> Unit,
): Registration<ObservedFamily> = observedGauge(name, described(key, unit), observe)

private fun described(key: String?, unit: MeasureUnit): FamilyBuilder.() -> Unit = {
    this.unit = unit
    key?.let { name -> labelKeys(name) }
}
