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

package com.davils.kheck.core.record

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.internal.orderedCopy
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.api.record.LogValue
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.time.Instant

/**
 * A record as sinks receive it: an event of a caller with its level, time, source, context and captured cause.
 *
 * Records are immutable and safe to share between threads; the collections they are given are copied.
 *
 * @property sequence the number of the record, increasing in the order the logging accepted records.
 * @property timestamp when the record was logged.
 * @property level the level of the record; never [LogLevel.Off].
 * @property source where the record comes from.
 * @property message the message, foreign text that every layout escapes.
 * @param fields the fields of the event, copied.
 * @param context the fields of the log context and of the enrichment, copied.
 * @param markers the markers of the event, copied.
 * @property cause the captured throwable, if any.
 * @property omitted how many fields, values and markers were left out because they exceeded the limits of kheck.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LogRecord(
    public val sequence: Long,
    public val timestamp: Instant,
    public val level: LogLevel,
    public val source: LogSource,
    public val message: String,
    fields: Map<String, LogValue> = emptyMap(),
    context: Map<String, LogValue> = emptyMap(),
    markers: Set<LogMarker> = emptySet(),
    public val cause: LogCause? = null,
    public val omitted: Int = 0,
) {
    /**
     * The fields of the event, in the order they were added; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val fields: Map<String, LogValue> = fields.orderedCopy()

    /**
     * The fields of the log context and of the enrichment; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val context: Map<String, LogValue> = context.orderedCopy()

    /**
     * The markers of the event; the set cannot be changed.
     *
     * @since 1.0.0
     */
    public val markers: Set<LogMarker> = markers.orderedCopy()

    internal fun copied(
        message: String = this.message,
        fields: Map<String, LogValue> = this.fields,
        context: Map<String, LogValue> = this.context,
        cause: LogCause? = this.cause,
    ): LogRecord = LogRecord(
        sequence = sequence,
        timestamp = timestamp,
        level = level,
        source = source,
        message = message,
        fields = fields,
        context = context,
        markers = markers,
        cause = cause,
        omitted = omitted,
    )

    /**
     * Shows the sequence, level, logger and escaped message of the record.
     *
     * @return the record as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "LogRecord(sequence=$sequence, level=$level, logger=${source.logger}, message=${SafeText.quoted(message)})"
}
