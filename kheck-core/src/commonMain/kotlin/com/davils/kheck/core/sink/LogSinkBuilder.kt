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
import com.davils.kheck.core.integrity.IntegrityBuilder
import com.davils.kheck.core.integrity.IntegrityOptions
import com.davils.kontinuity.core.Stage
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

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
    internal val stages = mutableListOf<Stage>()
    internal var integrity: IntegrityOptions? = null

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
     * Adds resilience stages of kontinuity, such as a retry, a circuit breaker, a time limiter or a whole policy, that
     * every write of a batch passes, the first stage outermost.
     *
     * A stage that refuses a batch, such as an open circuit breaker, hands it to the [fallback] without retrying it and
     * counts it as rejected; a write that fails after every stage is a failure of the sink. A time limiter
     * among the stages also bounds the opening, flushing and closing of the sink.
     *
     * A stage keeps state, so it belongs to one sink only; the same stage on two sinks or twice on one sink is refused
     * with [LoggingViolationCodes.SHARED_STAGE][com.davils.kheck.core.LoggingViolationCodes.SHARED_STAGE]. kheck never
     * builds a jittered backoff; the stages, their time source, events and meters come from the application.
     *
     * @param stages the stages, in order from the outermost.
     * @since 1.0.0
     */
    public fun stages(vararg stages: Stage) {
        checkOpen()
        this.stages += stages
    }

    /**
     * Seals every line of the sink in an integrity chain, so tampering with what the sink stored can be proven with
     * [IntegrityChain.verify][com.davils.kheck.core.integrity.IntegrityChain.verify]; each line reaches the sink with
     * its [LogSeal][com.davils.kheck.core.integrity.LogSeal] in [LogLine.seal].
     *
     * The chain advances only when a batch is written, so a batch that fails and goes to the fallback leaves no gap;
     * the fallback keeps its own chain. A configuration without a key, with a destroyed key or with an algorithm the
     * cryptography of the logging refuses is refused.
     *
     * @param block sets the key, the algorithm and the anchor.
     * @since 1.0.0
     */
    public fun integrity(block: IntegrityBuilder.() -> Unit) {
        checkOpen()
        val builder = IntegrityBuilder()
        builder.block()
        integrity = builder.options()
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
