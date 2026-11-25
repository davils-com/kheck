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
import com.davils.kheck.api.internal.orderedCopy
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * What a caller logs: a message, typed fields, an optional cause and markers.
 *
 * The level, the time and the logger are added by the logging when the event becomes a record. An event is
 * immutable; the collections it is given are copied.
 *
 * @param message the message, plain text that every layout escapes where it writes it.
 * @param fields the named values of the event, in the order they were added.
 * @param cause the throwable the event reports, if any.
 * @param markers the markers that classify the event.
 * @param omitted how many fields, values and markers were left out because they exceeded the [Limits]; every layout
 * writes this count when it is not zero.
 * @throws IllegalArgumentException if [omitted] is negative.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LogEvent(
    public val message: String,
    fields: Map<String, LogValue> = emptyMap(),
    public val cause: Throwable? = null,
    markers: Set<LogMarker> = emptySet(),
    public val omitted: Int = 0,
) {
    init {
        require(omitted >= 0) { NEGATIVE_OMITTED }
    }

    /**
     * The named values of the event, in the order they were added; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val fields: Map<String, LogValue> = fields.orderedCopy()

    /**
     * The markers that classify the event; the set cannot be changed.
     *
     * @since 1.0.0
     */
    public val markers: Set<LogMarker> = markers.orderedCopy()

    /**
     * Returns whether [other] is an event with the same message, fields, cause, markers and count of omitted parts.
     *
     * The cause is compared by identity, as throwables are.
     *
     * @param other the object to compare with.
     * @return `true` if both events are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogEvent) return false
        val isSameContent = message == other.message && fields == other.fields && cause == other.cause
        return isSameContent && markers == other.markers && omitted == other.omitted
    }

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int {
        var result = message.hashCode()
        result = HASH_FACTOR * result + fields.hashCode()
        result = HASH_FACTOR * result + (cause?.hashCode() ?: 0)
        result = HASH_FACTOR * result + markers.hashCode()
        return HASH_FACTOR * result + omitted
    }

    /**
     * Shows the event with its message escaped; the cause is shown by its type only, because its message is foreign
     * text.
     *
     * @return the event as text.
     * @since 1.0.0
     */
    override fun toString(): String {
        val causeType = cause?.let { throwable -> throwable::class.simpleName ?: ANONYMOUS } ?: NONE
        return "LogEvent(message=${SafeText.quoted(message)}, fields=$fields, cause=$causeType, " +
            "markers=$markers, omitted=$omitted)"
    }

    private companion object {
        private const val HASH_FACTOR: Int = 31
        private const val NONE: String = "none"
        private const val ANONYMOUS: String = "anonymous"
        private const val NEGATIVE_OMITTED: String = "The count of omitted parts must not be negative"
    }
}
