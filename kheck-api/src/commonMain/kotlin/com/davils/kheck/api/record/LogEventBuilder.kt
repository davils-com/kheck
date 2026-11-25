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

package com.davils.kheck.api.record

import com.davils.kheck.api.Limits
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * Builds a [LogEvent] inside a call such as `logger.info { "Invoice sent" }`: the block adds fields, a cause and
 * markers and returns the message.
 *
 * Markers beyond [Limits.MAX_MARKERS] are left out and counted like the fields of [LogFieldsBuilder].
 *
 * @since 1.0.0
 */
@DavilsDsl
@OptIn(DavilsInternal::class)
public class LogEventBuilder @PublishedApi internal constructor() : LogFieldsBuilder() {
    private var captured: Throwable? = null
    private val markers = LinkedHashSet<LogMarker>()
    private var droppedMarkers: Int = 0

    /**
     * Sets the throwable the event reports; a second call replaces the first.
     *
     * @param throwable the cause.
     * @since 1.0.0
     */
    public fun cause(throwable: Throwable) {
        captured = throwable
    }

    /**
     * Adds a marker that classifies the event.
     *
     * @param marker the marker.
     * @since 1.0.0
     */
    public fun marker(marker: LogMarker) {
        val isFull = markers.size >= Limits.MAX_MARKERS && marker !in markers
        if (isFull) {
            droppedMarkers += 1
            return
        }
        markers += marker
    }

    @PublishedApi
    internal fun build(message: String): LogEvent = LogEvent(
        message = message,
        fields = collected(),
        cause = captured,
        markers = markers,
        omitted = omitted + droppedMarkers,
    )
}
