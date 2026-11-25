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

package com.davils.kheck.core.sample

import com.davils.kheck.api.LogLevel
import com.davils.kheck.core.Limits
import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The sampling of a logging: deduplication, a rate limit and a rate, applied in that order to every record.
 *
 * Deduplication applies to every level. The rate limit and the rate apply only below [alwaysFrom]. The rate is
 * deterministic, not random: with a rate of 0.25 exactly every fourth record passes, so the share written is exact
 * and no randomness is needed. Created by `sample { }`.
 *
 * @since 1.0.0
 */
public class SamplingConfiguration internal constructor(builder: SamplingBuilder) : Validatable {
    /**
     * The share of records below [alwaysFrom] that is written.
     *
     * @since 1.0.0
     */
    public val rate: Double = builder.rate

    /**
     * The level from which every record is written.
     *
     * @since 1.0.0
     */
    public val alwaysFrom: LogLevel = builder.alwaysFrom

    /**
     * The deduplication, if any.
     *
     * @since 1.0.0
     */
    public val deduplication: Deduplication? = builder.deduplication

    /**
     * The rate limit, if any.
     *
     * @since 1.0.0
     */
    public val limit: RateLimit? = builder.limit

    /**
     * Checks the rate, the window and the counts.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        val isRateValid = rate in 0.0..1.0
        if (!isRateValid) fail(::rate, "must lie between 0 and 1, but is $rate", LoggingViolationCodes.INVALID_RATE)
        deduplication?.let { value -> within(::deduplication) { checkDeduplication(value) } }
        limit?.let { value -> within(::limit) { checkLimit(value) } }
    }

    /**
     * Shows the sampling.
     *
     * @return the sampling as text.
     * @since 1.0.0
     */
    override fun toString(): String = "SamplingConfiguration(rate=$rate, alwaysFrom=$alwaysFrom, " +
        "deduplication=${deduplication ?: NONE}, limit=${limit ?: NONE})"

    private fun ValidationScope.checkDeduplication(deduplication: Deduplication) {
        val isWindowValid = deduplication.window.isPositive() && deduplication.window.isFinite()
        if (!isWindowValid) {
            fail(Deduplication::window, "must be finite and positive", LoggingViolationCodes.INVALID_WINDOW)
        }
        if (deduplication.maxEntries in 1..Limits.MAX_CAPACITY) return
        fail(
            Deduplication::maxEntries,
            "must lie between 1 and ${Limits.MAX_CAPACITY}",
            LoggingViolationCodes.INVALID_LIMIT,
        )
    }

    private fun ValidationScope.checkLimit(limit: RateLimit) {
        if (limit.records < 1) {
            fail(RateLimit::records, "must allow at least 1 record", LoggingViolationCodes.INVALID_LIMIT)
        }
        if (limit.per.isPositive() && limit.per.isFinite()) return
        fail(RateLimit::per, "must be finite and positive", LoggingViolationCodes.INVALID_WINDOW)
    }

    private companion object {
        private const val NONE: String = "none"
    }
}
