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

import com.davils.kontinuity.circuitbreaker.CircuitState
import kotlin.time.Duration

/**
 * What one sink did since the logging started, as one consistent snapshot of counters.
 *
 * @property written the lines the sink wrote.
 * @property failed the lines the sink failed to write after every attempt.
 * @property dropped the records lost because the queue of the sink was full.
 * @property queued the records waiting for the sink now.
 * @property batches the batches the sink wrote.
 * @property writeTime the time the sink took for all batches.
 * @property maxWriteTime the longest time the sink took for one batch.
 * @property isFailing whether the last attempt of the sink failed.
 * @property rejected the lines a stage of the sink refused, such as an open circuit breaker, and handed to the
 *   fallback.
 * @property circuit the state of the circuit breaker among the stages of the sink, or `null` without one.
 * @since 1.0.0
 */
public class SinkStatistics internal constructor(
    public val written: Long,
    public val failed: Long,
    public val dropped: Long,
    public val queued: Long,
    public val batches: Long,
    public val writeTime: Duration,
    public val maxWriteTime: Duration,
    public val isFailing: Boolean,
    public val rejected: Long = 0L,
    public val circuit: CircuitState? = null,
) {
    private val values: List<Any?>
        get() = listOf(written, failed, dropped, queued, batches, writeTime, maxWriteTime, isFailing, rejected, circuit)

    /**
     * Compares by value: two [SinkStatistics] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [SinkStatistics].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is SinkStatistics && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the counters.
     *
     * @return the statistics as text.
     * @since 1.0.0
     */
    override fun toString(): String = "SinkStatistics(written=$written, failed=$failed, dropped=$dropped, " +
        "queued=$queued, batches=$batches, writeTime=$writeTime, maxWriteTime=$maxWriteTime, isFailing=$isFailing, " +
        "rejected=$rejected, circuit=${circuit ?: NONE})"

    private companion object {
        private const val NONE: String = "none"
    }
}
