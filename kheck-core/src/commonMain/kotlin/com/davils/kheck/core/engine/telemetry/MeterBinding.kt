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

package com.davils.kheck.core.engine.telemetry

import com.davils.kheck.core.LoggingMetricNames
import com.davils.kheck.core.statistics.LoggingStatistics
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Registration
import com.davils.kollect.core.declare.FamilyBuilder
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.ObservationSink
import com.davils.kollect.core.name.MetricName
import kotlin.time.Duration

internal class MeterBinding(
    private val registry: MeterRegistry,
    labels: Map<String, String>,
    private val read: () -> LoggingStatistics,
) {
    private val keys: List<String> = labels.keys.toList()
    private val values: List<String> = labels.values.toList()

    @Suppress("SpreadOperator")
    fun bound(telemetry: LoggingTelemetry): () -> Unit {
        val observed = observed()
        val timer = registry.timer(LoggingMetricNames.SINK_WRITE) { labelKeys(*keyed(LoggingMetricNames.SINK)) }
        val observer = WriteObserver { sink, elapsed -> timer.family.with(*valued(sink)).record(elapsed) }
        telemetry.observe(observer)
        return {
            telemetry.forget(observer)
            observed.forEach { registration -> registration.family.close() }
            if (timer is Registration.Declared) registry.remove(MetricName.of(LoggingMetricNames.SINK_WRITE))
        }
    }

    private fun observed(): List<Registration<ObservedFamily>> = listOf(
        counter(name = LoggingMetricNames.RECORDS, key = LoggingMetricNames.LEVEL) { statistics ->
            statistics.recorded.forEach { (level, count) -> labelled(count, level.name.lowercase()) }
        },
        counter(name = LoggingMetricNames.SUPPRESSED, key = LoggingMetricNames.REASON) { statistics ->
            statistics.suppressed.forEach { (reason, count) -> labelled(count, reason.name.lowercase()) }
        },
        counter(name = LoggingMetricNames.DROPPED, key = LoggingMetricNames.SINK) { statistics ->
            labelled(statistics.intakeDropped, LoggingMetricNames.INTAKE)
            statistics.sinks.forEach { (name, sink) -> labelled(sink.dropped, name) }
        },
        counter(name = LoggingMetricNames.DROPPED_AFTER_CLOSE) { statistics -> labelled(statistics.droppedAfterClose) },
        counter(name = LoggingMetricNames.REENTRANT) { statistics -> labelled(statistics.reentrant) },
        counter(name = LoggingMetricNames.MASKED, key = LoggingMetricNames.RULE) { statistics ->
            statistics.masked.forEach { (rule, count) -> labelled(count, rule) }
        },
        counter(name = LoggingMetricNames.FAILED_LISTENERS) { statistics -> labelled(statistics.failedListeners) },
        counter(name = LoggingMetricNames.EVENTS_REJECTED) { statistics -> labelled(statistics.eventsRejected) },
        gauge(name = LoggingMetricNames.INTAKE_QUEUED) { statistics -> labelled(statistics.intakeQueued) },
        gauge(name = LoggingMetricNames.INTAKE_SATURATION, unit = MeasureUnit.RATIO) { statistics ->
            labelled(statistics.intakeSaturation)
        },
        gauge(name = LoggingMetricNames.SINKS_FAILING) { statistics -> labelled(statistics.sinksFailing.toLong()) },
        gauge(name = LoggingMetricNames.CIRCUITS_OPEN) { statistics -> labelled(statistics.circuitsOpen.toLong()) },
        counter(name = LoggingMetricNames.SINK_WRITTEN, key = LoggingMetricNames.SINK) { statistics ->
            statistics.sinks.forEach { (name, sink) -> labelled(sink.written, name) }
        },
        counter(name = LoggingMetricNames.SINK_FAILED, key = LoggingMetricNames.SINK) { statistics ->
            statistics.sinks.forEach { (name, sink) -> labelled(sink.failed, name) }
        },
        counter(name = LoggingMetricNames.SINK_REJECTED, key = LoggingMetricNames.SINK) { statistics ->
            statistics.sinks.forEach { (name, sink) -> labelled(sink.rejected, name) }
        },
        counter(name = LoggingMetricNames.SINK_BATCHES, key = LoggingMetricNames.SINK) { statistics ->
            statistics.sinks.forEach { (name, sink) -> labelled(sink.batches, name) }
        },
        gauge(name = LoggingMetricNames.SINK_QUEUED, key = LoggingMetricNames.SINK) { statistics ->
            statistics.sinks.forEach { (name, sink) -> labelled(sink.queued, name) }
        },
        counter(
            name = LoggingMetricNames.SINK_WRITE_TIME,
            key = LoggingMetricNames.SINK,
            unit = MeasureUnit.SECONDS,
        ) { statistics ->
            statistics.sinks.forEach { (name, sink) -> labelled(sink.writeTime.seconds(), name) }
        },
        gauge(
            name = LoggingMetricNames.SINK_MAX_WRITE_TIME,
            key = LoggingMetricNames.SINK,
            unit = MeasureUnit.SECONDS,
        ) { statistics ->
            statistics.sinks.forEach { (name, sink) -> labelled(sink.maxWriteTime.seconds(), name) }
        },
        gauge(name = LoggingMetricNames.SINK_FAILING, key = LoggingMetricNames.SINK) { statistics ->
            statistics.sinks.forEach { (name, sink) -> labelled(flag(sink.isFailing), name) }
        },
    )

    private fun counter(
        name: String,
        key: String? = null,
        unit: MeasureUnit = MeasureUnit.NONE,
        observe: ObservationSink.(LoggingStatistics) -> Unit,
    ): Registration<ObservedFamily> = registry.observedCounter(name, described(key, unit)) { observe(read()) }

    private fun gauge(
        name: String,
        key: String? = null,
        unit: MeasureUnit = MeasureUnit.NONE,
        observe: ObservationSink.(LoggingStatistics) -> Unit,
    ): Registration<ObservedFamily> = registry.observedGauge(name, described(key, unit)) { observe(read()) }

    @Suppress("SpreadOperator")
    private fun described(key: String?, unit: MeasureUnit): FamilyBuilder.() -> Unit = {
        this.unit = unit
        val all = keyed(*listOfNotNull(key).toTypedArray())
        if (all.isNotEmpty()) labelKeys(*all)
    }

    private fun keyed(vararg own: String): Array<String> = (keys + own).toTypedArray()

    private fun valued(vararg own: String): Array<String> = (values + own).toTypedArray()

    @Suppress("SpreadOperator")
    private fun ObservationSink.labelled(value: Long, vararg own: String) {
        observe(value.toDouble(), *valued(*own))
    }

    @Suppress("SpreadOperator")
    private fun ObservationSink.labelled(value: Double, vararg own: String) {
        observe(value, *valued(*own))
    }

    private fun Duration.seconds(): Double = inWholeNanoseconds / NANOS_PER_SECOND

    private fun flag(isSet: Boolean): Long {
        if (isSet) return 1L
        return 0L
    }

    private companion object {
        private const val NANOS_PER_SECOND: Double = 1_000_000_000.0
    }
}
