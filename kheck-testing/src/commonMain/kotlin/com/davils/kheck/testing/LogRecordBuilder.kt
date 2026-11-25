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

package com.davils.kheck.testing

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogFieldsBuilder
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.record.LogSource
import com.davils.kheck.core.record.toLogCause
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import kotlin.time.Instant

/**
 * Builds a [LogRecord] for a test of a format or a sink inside [logRecord], starting from fixed values.
 *
 * @since 1.0.0
 */
@DavilsDsl
@OptIn(DavilsInternal::class)
public class LogRecordBuilder internal constructor() {
    /**
     * The sequence number; 1 by default.
     *
     * @since 1.0.0
     */
    public var sequence: Long = 1L

    /**
     * The time; [TIMESTAMP] by default.
     *
     * @since 1.0.0
     */
    public var timestamp: Instant = TIMESTAMP

    /**
     * The level; [LogLevel.Info] by default.
     *
     * @since 1.0.0
     */
    public var level: LogLevel = LogLevel.Info

    /**
     * The name of the logger; [LOGGER] by default.
     *
     * @since 1.0.0
     */
    public var logger: String = LOGGER

    /**
     * The name of the thread; [THREAD] by default.
     *
     * @since 1.0.0
     */
    public var thread: String = THREAD

    /**
     * The message; [MESSAGE] by default.
     *
     * @since 1.0.0
     */
    public var message: String = MESSAGE

    /**
     * The frame that logged, if any.
     *
     * @since 1.0.0
     */
    public var caller: LogFrame? = null

    /**
     * The throwable, captured when the record is built, if any.
     *
     * @since 1.0.0
     */
    public var cause: Throwable? = null

    /**
     * The markers.
     *
     * @since 1.0.0
     */
    public var markers: Set<LogMarker> = emptySet()

    /**
     * The count of omitted parts.
     *
     * @since 1.0.0
     */
    public var omitted: Int = 0

    private var fields: Map<String, LogValue> = emptyMap()
    private var context: Map<String, LogValue> = emptyMap()

    /**
     * Sets the fields of the event.
     *
     * @param block adds the fields.
     * @since 1.0.0
     */
    public fun fields(block: LogFieldsBuilder.() -> Unit) {
        fields = collected(block)
    }

    /**
     * Sets the fields of the context.
     *
     * @param block adds the fields.
     * @since 1.0.0
     */
    public fun context(block: LogFieldsBuilder.() -> Unit) {
        context = collected(block)
    }

    internal fun build(): LogRecord = LogRecord(
        sequence = sequence,
        timestamp = timestamp,
        level = level,
        source = LogSource(logger = LoggerName(logger), thread = thread, caller = caller),
        message = message,
        fields = fields,
        context = context,
        markers = markers,
        cause = cause?.toLogCause(),
        omitted = omitted,
    )

    private fun collected(block: LogFieldsBuilder.() -> Unit): Map<String, LogValue> {
        val builder = LogFieldsBuilder()
        builder.block()
        return builder.collected()
    }

    /**
     * The fixed values a record starts from.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default name of the logger.
         *
         * @since 1.0.0
         */
        public const val LOGGER: String = "com.example.inventory.ServerService"

        /**
         * The default name of the thread.
         *
         * @since 1.0.0
         */
        public const val THREAD: String = "main"

        /**
         * The default message.
         *
         * @since 1.0.0
         */
        public const val MESSAGE: String = "loaded the servers"

        /**
         * The default time, `2026-11-10T08:15:30.250Z`.
         *
         * @since 1.0.0
         */
        public val TIMESTAMP: Instant = Instant.parse("2026-11-10T08:15:30.250Z")
    }
}
