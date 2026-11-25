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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations a configuration of kheck reports, all below `kheck.logging`.
 *
 * Codes are a contract: once released, a code keeps its meaning.
 *
 * @since 1.0.0
 */
public object LoggingViolationCodes {
    /**
     * A width of a segment is negative, or the greatest width lies below the least.
     *
     * @since 1.0.0
     */
    public val INVALID_WIDTH: ViolationCode = code("invalid-width")

    /**
     * The abbreviation of a logger is negative.
     *
     * @since 1.0.0
     */
    public val INVALID_ABBREVIATION: ViolationCode = code("invalid-abbreviation")

    /**
     * A limit of frames or causes is negative.
     *
     * @since 1.0.0
     */
    public val INVALID_FRAMES: ViolationCode = code("invalid-frames")

    /**
     * A plain layout has no segment.
     *
     * @since 1.0.0
     */
    public val EMPTY_LAYOUT: ViolationCode = code("empty-layout")

    /**
     * A plain layout has no message; a warning.
     *
     * @since 1.0.0
     */
    public val MISSING_MESSAGE: ViolationCode = code("missing-message")

    /**
     * A capacity is below 1 or above the limit.
     *
     * @since 1.0.0
     */
    public val INVALID_CAPACITY: ViolationCode = code("invalid-capacity")

    /**
     * A batch is below 1 or larger than the capacity.
     *
     * @since 1.0.0
     */
    public val INVALID_BATCH: ViolationCode = code("invalid-batch")

    /**
     * A blocking overflow waits no finite, positive time.
     *
     * @since 1.0.0
     */
    public val INVALID_WAIT: ViolationCode = code("invalid-wait")

    /**
     * The name of a sink or a logger is blank.
     *
     * @since 1.0.0
     */
    public val BLANK_NAME: ViolationCode = code("blank-name")

    /**
     * A logger or a fallback names a sink that is not declared.
     *
     * @since 1.0.0
     */
    public val UNKNOWN_SINK: ViolationCode = code("unknown-sink")

    /**
     * A sink falls back to itself, directly or through a cycle.
     *
     * @since 1.0.0
     */
    public val SELF_FALLBACK: ViolationCode = code("self-fallback")

    /**
     * An exclusive sink that no logger routes to; a warning.
     *
     * @since 1.0.0
     */
    public val UNROUTED_SINK: ViolationCode = code("unrouted-sink")

    /**
     * A configuration that logs but has no sink; a warning.
     *
     * @since 1.0.0
     */
    public val NO_SINKS: ViolationCode = code("no-sinks")

    /**
     * A partial mask keeps a negative count of characters.
     *
     * @since 1.0.0
     */
    public val INVALID_PARTIAL: ViolationCode = code("invalid-partial")

    /**
     * A hash mask without a masking key.
     *
     * @since 1.0.0
     */
    public val MISSING_HASH_KEY: ViolationCode = code("missing-hash-key")

    /**
     * The longest text the masking scans is too short or too long.
     *
     * @since 1.0.0
     */
    public val INVALID_SCAN_LENGTH: ViolationCode = code("invalid-scan-length")

    /**
     * A depth lies outside its range.
     *
     * @since 1.0.0
     */
    public val INVALID_DEPTH: ViolationCode = code("invalid-depth")

    /**
     * A sampling rate lies outside 0 to 1.
     *
     * @since 1.0.0
     */
    public val INVALID_RATE: ViolationCode = code("invalid-rate")

    /**
     * A window or a period of time is not positive.
     *
     * @since 1.0.0
     */
    public val INVALID_WINDOW: ViolationCode = code("invalid-window")

    /**
     * A count of a limit or a retry is below its least value or above the limit.
     *
     * @since 1.0.0
     */
    public val INVALID_LIMIT: ViolationCode = code("invalid-limit")

    /**
     * A reconfiguration changes the intake, which cannot change while the logging runs.
     *
     * @since 1.0.0
     */
    public val INTAKE_CHANGED: ViolationCode = code("intake-changed")

    /**
     * A configuration declares more loggers, sinks or filters than the limit.
     *
     * @since 1.0.0
     */
    public val TOO_MANY: ViolationCode = code("too-many")

    private fun code(name: String): ViolationCode = ViolationCode("kheck.logging.$name")
}
