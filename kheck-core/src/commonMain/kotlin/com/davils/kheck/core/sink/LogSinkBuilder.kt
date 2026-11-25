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
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.core.filter.LogFilter
import com.davils.kheck.core.format.LogFormat
import com.davils.kheck.core.format.plain.plain
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration

/**
 * The options every sink shares, set inside the block of a sink such as `console { }`.
 *
 * @param C the configuration of the sink.
 * @since 1.0.0
 */
@DavilsDsl
public abstract class LogSinkBuilder<C : LogSinkConfiguration> : ValidatingBuilder<C>() {
    /**
     * The lowest level the sink writes; [LogLevel.Trace] by default, so the loggers decide.
     *
     * @since 1.0.0
     */
    public var level: LogLevel = LogLevel.Trace

    /**
     * How the sink encodes records; a [plain] layout by default.
     *
     * @since 1.0.0
     */
    public var format: LogFormat = plain()

    /**
     * What happens to a record when the queue of the sink is full; [LogOverflow.DropOldest] by default.
     *
     * @since 1.0.0
     */
    public var overflow: LogOverflow = LogOverflow.DropOldest

    /**
     * The most records waiting for the sink; 8 192 by default.
     *
     * @since 1.0.0
     */
    public var capacity: Int = DEFAULT_CAPACITY

    /**
     * The most records written in one batch; 512 by default.
     *
     * @since 1.0.0
     */
    public var maxBatch: Int = DEFAULT_MAX_BATCH

    /**
     * The markers of which a record must carry one to be written; empty for every record.
     *
     * @since 1.0.0
     */
    public var markers: Set<LogMarker> = emptySet()

    /**
     * Whether only loggers that name the sink route to it; `false` by default, so every logger does.
     *
     * @since 1.0.0
     */
    public var isExclusive: Boolean = false

    /**
     * The name of the sink that takes the batches this sink failed to write, or `null` for none.
     *
     * @since 1.0.0
     */
    public var fallback: String? = null

    internal val filters = mutableListOf<LogFilter>()
    internal var retry: SinkRetry = SinkRetry.NONE

    /**
     * Adds a filter that a record must pass to be written.
     *
     * @param filter the filter.
     * @since 1.0.0
     */
    public fun filter(filter: LogFilter) {
        checkOpen()
        filters += filter
    }

    /**
     * Retries a failed batch, as [SinkRetry] describes.
     *
     * @param attempts the attempts in total, from 1 to 100.
     * @param backoff the wait before the second attempt.
     * @param maxBackoff the longest wait between two attempts.
     * @since 1.0.0
     */
    public fun retry(attempts: Int, backoff: Duration, maxBackoff: Duration = backoff) {
        checkOpen()
        retry = SinkRetry(attempts = attempts, backoff = backoff, maxBackoff = maxBackoff)
    }

    /**
     * Returns the options set so far, for the configuration of a sink.
     *
     * @return the options.
     * @since 1.0.0
     */
    protected fun options(): LogSinkOptions = LogSinkOptions(this)

    /**
     * The defaults of every sink.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default of [capacity].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_CAPACITY: Int = 8_192

        /**
         * The default of [maxBatch].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_MAX_BATCH: Int = 512
    }
}
