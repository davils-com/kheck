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

package com.davils.kheck.core.sink

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.internal.orderedCopy
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kheck.core.filter.LogFilter
import com.davils.kheck.core.format.LogFormat
import com.davils.kheck.core.integrity.IntegrityOptions
import com.davils.kontinuity.core.Stage
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The options every sink shares, as [LogSinkBuilder] describes them.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LogSinkOptions internal constructor(builder: LogSinkBuilder<*>) : Validatable {
    /**
     * The lowest level the sink writes.
     *
     * @since 1.0.0
     */
    public val level: LogLevel = builder.level

    /**
     * How the sink encodes records.
     *
     * @since 1.0.0
     */
    public val format: LogFormat = builder.format

    /**
     * What happens to a record when the queue is full.
     *
     * @since 1.0.0
     */
    public val overflow: LogOverflow = builder.overflow

    /**
     * The most records waiting.
     *
     * @since 1.0.0
     */
    public val capacity: Int = builder.capacity

    /**
     * The most records written in one batch.
     *
     * @since 1.0.0
     */
    public val maxBatch: Int = builder.maxBatch

    /**
     * The markers of which a record must carry one; the set cannot be changed.
     *
     * @since 1.0.0
     */
    public val markers: Set<LogMarker> = builder.markers.orderedCopy()

    /**
     * Whether only loggers that name the sink route to it.
     *
     * @since 1.0.0
     */
    public val isExclusive: Boolean = builder.isExclusive

    /**
     * The name of the fallback, if any.
     *
     * @since 1.0.0
     */
    public val fallback: String? = builder.fallback

    /**
     * The filters a record must pass; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val filters: List<LogFilter> = builder.filters.immutableList()

    /**
     * The resilience stages every write passes, the first outermost; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val stages: List<Stage> = builder.stages.immutableList()

    /**
     * The integrity chain of the sink, if any.
     *
     * @since 1.0.0
     */
    public val integrity: IntegrityOptions? = builder.integrity

    private val values: List<Any?>
        get() = listOf(
            level,
            format,
            overflow,
            capacity,
            maxBatch,
            markers,
            isExclusive,
            fallback,
            filters,
            stages,
            integrity,
        )

    /**
     * Checks the capacity, the batch, the overflow and the integrity.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        checkCapacity(::capacity, capacity)
        val isBatchInRange = maxBatch in 1..capacity.coerceAtLeast(1)
        if (!isBatchInRange) {
            fail(
                property = ::maxBatch,
                message = "must lie between 1 and the capacity, but is $maxBatch",
                code = LoggingViolationCodes.INVALID_BATCH,
            )
        }
        checkOverflow(::overflow, overflow)
        checkIntegrity(::integrity, integrity)
    }

    /**
     * Compares by value: two [LogSinkOptions] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [LogSinkOptions].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is LogSinkOptions && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the options.
     *
     * @return the options as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "LogSinkOptions(level=$level, format=$format, overflow=$overflow, capacity=$capacity, maxBatch=$maxBatch, " +
            "markers=$markers, isExclusive=$isExclusive, fallback=${fallback ?: NONE}, " +
            "stages=${stages.size}, integrity=${integrity ?: NONE})"

    private companion object {
        private const val NONE: String = "none"
    }
}
