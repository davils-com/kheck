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

package com.davils.kheck.core

/**
 * The names and label keys of the meters [Logging.bindTo] declares.
 *
 * Names are a contract: once released, a name keeps its meaning.
 *
 * @since 1.0.0
 */
public object LoggingMetricNames {
    /**
     * The records the pipeline processed, labelled by [LEVEL]; a counter.
     *
     * @since 1.0.0
     */
    public const val RECORDS: String = "kheck.logging.records"

    /**
     * The records dropped on purpose, labelled by [REASON]; a counter.
     *
     * @since 1.0.0
     */
    public const val SUPPRESSED: String = "kheck.logging.suppressed"

    /**
     * The records lost because a queue was full, labelled by [SINK], where [INTAKE] names the intake; a counter.
     *
     * @since 1.0.0
     */
    public const val DROPPED: String = "kheck.logging.dropped"

    /**
     * The records logged after the logging was closed; a counter.
     *
     * @since 1.0.0
     */
    public const val DROPPED_AFTER_CLOSE: String = "kheck.logging.dropped_after_close"

    /**
     * The records a sink logged while it wrote; a counter.
     *
     * @since 1.0.0
     */
    public const val REENTRANT: String = "kheck.logging.reentrant"

    /**
     * The values the masking replaced, labelled by [RULE]; a counter.
     *
     * @since 1.0.0
     */
    public const val MASKED: String = "kheck.logging.masked"

    /**
     * The calls of event listeners that threw; a counter.
     *
     * @since 1.0.0
     */
    public const val FAILED_LISTENERS: String = "kheck.logging.failed_listeners"

    /**
     * The records waiting in the intake; a gauge.
     *
     * @since 1.0.0
     */
    public const val INTAKE_QUEUED: String = "kheck.logging.intake.queued"

    /**
     * The share of the intake in use; a gauge from 0 to 1.
     *
     * @since 1.0.0
     */
    public const val INTAKE_SATURATION: String = "kheck.logging.intake.saturation"

    /**
     * The lines a sink wrote, labelled by [SINK]; a counter.
     *
     * @since 1.0.0
     */
    public const val SINK_WRITTEN: String = "kheck.logging.sink.written"

    /**
     * The lines a sink failed to write, labelled by [SINK]; a counter.
     *
     * @since 1.0.0
     */
    public const val SINK_FAILED: String = "kheck.logging.sink.failed"

    /**
     * The records waiting for a sink, labelled by [SINK]; a gauge.
     *
     * @since 1.0.0
     */
    public const val SINK_QUEUED: String = "kheck.logging.sink.queued"

    /**
     * The time a sink took for its batches, in seconds, labelled by [SINK]; a counter.
     *
     * @since 1.0.0
     */
    public const val SINK_WRITE_TIME: String = "kheck.logging.sink.write_time"

    /**
     * Whether the last attempt of a sink failed, 1 or 0, labelled by [SINK]; a gauge.
     *
     * @since 1.0.0
     */
    public const val SINK_FAILING: String = "kheck.logging.sink.failing"

    /**
     * The timer of the writes of a sink, per sink, from which the distribution of write times follows.
     *
     * @since 1.0.0
     */
    public const val SINK_WRITE: String = "kheck.logging.sink.write"

    /**
     * The counter of the batches a sink wrote, per sink.
     *
     * @since 1.0.0
     */
    public const val SINK_BATCHES: String = "kheck.logging.sink.batches"

    /**
     * The gauge of the longest write of a sink, per sink, in seconds.
     *
     * @since 1.0.0
     */
    public const val SINK_MAX_WRITE_TIME: String = "kheck.logging.sink.max_write_time"

    /**
     * The counter of the lines a stage of a sink refused, per sink.
     *
     * @since 1.0.0
     */
    public const val SINK_REJECTED: String = "kheck.logging.sink.rejected"

    /**
     * The gauge of the sinks that fail.
     *
     * @since 1.0.0
     */
    public const val SINKS_FAILING: String = "kheck.logging.sinks.failing"

    /**
     * The gauge of the sinks whose circuit breaker refuses calls.
     *
     * @since 1.0.0
     */
    public const val CIRCUITS_OPEN: String = "kheck.logging.circuits.open"

    /**
     * The counter of the events the bus refused.
     *
     * @since 1.0.0
     */
    public const val EVENTS_REJECTED: String = "kheck.logging.events_rejected"

    /**
     * The label key of the level.
     *
     * @since 1.0.0
     */
    public const val LEVEL: String = "level"

    /**
     * The label key of the reason of a suppression.
     *
     * @since 1.0.0
     */
    public const val REASON: String = "reason"

    /**
     * The label key of the sink.
     *
     * @since 1.0.0
     */
    public const val SINK: String = "sink"

    /**
     * The label key of the masking rule.
     *
     * @since 1.0.0
     */
    public const val RULE: String = "rule"

    /**
     * The value of [SINK] that names the intake.
     *
     * @since 1.0.0
     */
    public const val INTAKE: String = "intake"
}
