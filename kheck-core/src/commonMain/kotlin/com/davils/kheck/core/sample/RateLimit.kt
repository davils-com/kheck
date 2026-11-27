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

import kotlin.time.Duration

/**
 * The limit of how many records below the level of [SamplingConfiguration.alwaysFrom] are written per period, as a
 * token bucket that allows bursts of [records] and refills at [records] per [per].
 *
 * @property records the records allowed per period, and the size of a burst.
 * @property per the period.
 * @property key what is counted separately.
 * @since 1.0.0
 */
public class RateLimit(public val records: Int, public val per: Duration, public val key: RateLimitKey) {
    private val values: List<Any?> get() = listOf(records, per, key)

    /**
     * Compares by value: two [RateLimit] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [RateLimit].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is RateLimit && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the limit.
     *
     * @return the limit as text.
     * @since 1.0.0
     */
    override fun toString(): String = "RateLimit(records=$records, per=$per, key=$key)"
}
