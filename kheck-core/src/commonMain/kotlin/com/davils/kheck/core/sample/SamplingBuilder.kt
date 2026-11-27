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
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration

/**
 * Configures the sampling inside `sample { }` of the logging.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class SamplingBuilder internal constructor() : ValidatingBuilder<SamplingConfiguration>() {
    /**
     * Names the sampling in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "sampling"

    /**
     * The share of records below [alwaysFrom] that is written, from 0 to 1; 1 by default.
     *
     * @since 1.0.0
     */
    public var rate: Double = 1.0

    /**
     * The level from which every record is written, whatever the rate and the limit; [LogLevel.Warn] by default.
     *
     * @since 1.0.0
     */
    public var alwaysFrom: LogLevel = LogLevel.Warn

    internal var deduplication: Deduplication? = null
    internal var limit: RateLimit? = null

    /**
     * Suppresses repeated records, as [Deduplication] describes.
     *
     * @param window how long a record suppresses its repetitions.
     * @param maxEntries the most distinct records remembered at once.
     * @since 1.0.0
     */
    public fun dedupe(window: Duration, maxEntries: Int = DEFAULT_MAX_ENTRIES) {
        checkOpen()
        deduplication = Deduplication(window, maxEntries)
    }

    /**
     * Limits the records below [alwaysFrom] per period, as [RateLimit] describes, through a keyed rate limiter of
     * kontinuity with a token bucket per key.
     *
     * At most 4,096 keys are counted at once; a key unused for the period, and at least ten minutes, may be forgotten
     * once that many are in use, and the records of a new key are limited while every key is in use, so a flood of
     * distinct keys cannot lift the limit.
     *
     * @param records the records allowed per period, 1 to 1,000,000.
     * @param per the period, positive and at most one day.
     * @param key what is counted separately.
     * @since 1.0.0
     */
    public fun limit(records: Int, per: Duration, key: RateLimitKey = RateLimitKey.Global) {
        checkOpen()
        limit = RateLimit(records, per, key)
    }

    /**
     * Creates the configuration.
     *
     * @return the configuration, validated afterwards.
     * @since 1.0.0
     */
    override fun assemble(): SamplingConfiguration = SamplingConfiguration(this)

    /**
     * The defaults of the sampling.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default of the most entries of a deduplication.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_MAX_ENTRIES: Int = 1_024
    }
}
