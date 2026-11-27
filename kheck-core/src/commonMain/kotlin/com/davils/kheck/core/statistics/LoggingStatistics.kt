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
import com.davils.kheck.api.internal.orderedCopy
import com.davils.kontinuity.circuitbreaker.CircuitState
import com.davils.kore.core.annotation.DavilsInternal

/**
 * What a logging did since it started: a snapshot of its counters, for dashboards, health and tests.
 *
 * The counters only grow, except the gauges [intakeQueued] and the queues of the sinks. The snapshot is immutable;
 * each counter is read atomically, the snapshot as a whole is not, so two counters may differ by records that were
 * in flight. [Logging.bindTo][com.davils.kheck.core.Logging.bindTo] exposes the statistics as meters of kollect.
 *
 * @param recorded the records the pipeline processed, per level.
 * @param suppressed the records dropped on purpose, per reason.
 * @property intakeQueued the records waiting in the intake now.
 * @property intakeCapacity the capacity of the intake.
 * @property intakeDropped the records lost because the intake was full.
 * @property droppedAfterClose the records logged after the logging was closed, or abandoned in the intake by it.
 * @property reentrant the records a sink logged while it wrote, which are dropped so a sink cannot feed itself.
 * @param masked the values the masking replaced, per rule; `key`, `secret` and `depth` name the masking of sensitive
 * names, of secrets and of values nested too deep.
 * @property failedListeners the calls of event listeners that threw.
 * @param sinks the statistics of each sink, per name.
 * @property eventsRejected the events the topic of the bus refused or that could not be offered.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LoggingStatistics
@Suppress("LongParameterList")
internal constructor(
    recorded: Map<LogLevel, Long>,
    suppressed: Map<Suppression, Long>,
    public val intakeQueued: Long,
    public val intakeCapacity: Int,
    public val intakeDropped: Long,
    public val droppedAfterClose: Long,
    public val reentrant: Long,
    masked: Map<String, Long>,
    public val failedListeners: Long,
    sinks: Map<String, SinkStatistics>,
    public val eventsRejected: Long = 0L,
) {
    /**
     * The records the pipeline processed, per level; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val recorded: Map<LogLevel, Long> = recorded.orderedCopy()

    /**
     * The records dropped on purpose, per reason; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val suppressed: Map<Suppression, Long> = suppressed.orderedCopy()

    /**
     * The values the masking replaced, per rule; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val masked: Map<String, Long> = masked.orderedCopy()

    /**
     * The statistics of each sink, per name; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val sinks: Map<String, SinkStatistics> = sinks.orderedCopy()

    /**
     * The share of the intake in use, from 0 to 1.
     *
     * @since 1.0.0
     */
    public val intakeSaturation: Double get() = intakeQueued.toDouble() / intakeCapacity

    /**
     * How many sinks failed at their last attempt.
     *
     * @since 1.0.0
     */
    public val sinksFailing: Int get() = sinks.values.count { sink -> sink.isFailing }

    /**
     * How many sinks have a circuit breaker that refuses calls now, open or forced open.
     *
     * @since 1.0.0
     */
    public val circuitsOpen: Int get() = sinks.values.count { sink -> sink.circuit in REFUSING }

    private val values: List<Any?>
        get() = listOf(
            intakeQueued,
            intakeCapacity,
            intakeDropped,
            droppedAfterClose,
            reentrant,
            failedListeners,
            recorded,
            suppressed,
            masked,
            sinks,
            eventsRejected,
        )

    /**
     * Compares by value: two [LoggingStatistics] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [LoggingStatistics].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is LoggingStatistics && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the totals.
     *
     * @return the statistics as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LoggingStatistics(recorded=${recorded.values.sum()}, " +
        "suppressed=${suppressed.values.sum()}, intakeQueued=$intakeQueued, intakeDropped=$intakeDropped, " +
        "sinks=${sinks.keys})"

    private companion object {
        private val REFUSING: Set<CircuitState> = setOf(CircuitState.Open, CircuitState.ForcedOpen)
    }
}
