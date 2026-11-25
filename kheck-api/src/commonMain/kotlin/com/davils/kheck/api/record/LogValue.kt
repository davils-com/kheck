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

import com.davils.kheck.api.internal.orderedCopy
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * The typed value of a field of a record.
 *
 * Every layout writes a value according to its type: a number stays a number in JSON, a timestamp is written in the
 * format of the layout, and a [Secret] is never written at all. Values are immutable and safe to share between
 * threads; [Items] and [Entries] copy what they are given.
 *
 * @since 1.0.0
 */
public sealed interface LogValue {
    /**
     * A piece of text, escaped by every layout where it is written.
     *
     * @property value the text.
     * @since 1.0.0
     */
    public class Text(public val value: String) : LogValue {
        /**
         * Returns whether [other] is a [Text] with the same text.
         *
         * @param other the value to compare with.
         * @return `true` if both hold the same text.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Text && other.value == value

        /**
         * Returns the hash code of the text.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Shows the text escaped and in quotes.
         *
         * @return the text as it may be shown.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = SafeText.quoted(value)
    }

    /**
     * A whole number.
     *
     * @property value the number.
     * @since 1.0.0
     */
    public class Integer(public val value: Long) : LogValue {
        /**
         * Returns whether [other] is an [Integer] with the same number.
         *
         * @param other the value to compare with.
         * @return `true` if both hold the same number.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Integer && other.value == value

        /**
         * Returns the hash code of the number.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Shows the number.
         *
         * @return the number in decimal.
         * @since 1.0.0
         */
        override fun toString(): String = value.toString()
    }

    /**
     * A floating point number; layouts write `NaN` and the infinities as text, because JSON has no such numbers.
     *
     * @property value the number.
     * @since 1.0.0
     */
    public class Decimal(public val value: Double) : LogValue {
        /**
         * Returns whether [other] is a [Decimal] with the same number, `NaN` included.
         *
         * @param other the value to compare with.
         * @return `true` if both hold the same number.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Decimal && other.value.equals(value)

        /**
         * Returns the hash code of the number.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Shows the number.
         *
         * @return the number in decimal.
         * @since 1.0.0
         */
        override fun toString(): String = value.toString()
    }

    /**
     * A truth value.
     *
     * @property isTrue the value.
     * @since 1.0.0
     */
    public class Bool(public val isTrue: Boolean) : LogValue {
        /**
         * Returns whether [other] is a [Bool] with the same value.
         *
         * @param other the value to compare with.
         * @return `true` if both hold the same value.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Bool && other.isTrue == isTrue

        /**
         * Returns the hash code of the value.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = isTrue.hashCode()

        /**
         * Shows `true` or `false`.
         *
         * @return the value.
         * @since 1.0.0
         */
        override fun toString(): String = isTrue.toString()
    }

    /**
     * A point in time.
     *
     * @property instant the point in time.
     * @since 1.0.0
     */
    public class Timestamp(public val instant: Instant) : LogValue {
        /**
         * Returns whether [other] is a [Timestamp] of the same point in time.
         *
         * @param other the value to compare with.
         * @return `true` if both hold the same point in time.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Timestamp && other.instant == instant

        /**
         * Returns the hash code of the point in time.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = instant.hashCode()

        /**
         * Shows the point in time in ISO 8601.
         *
         * @return the point in time.
         * @since 1.0.0
         */
        override fun toString(): String = instant.toString()
    }

    /**
     * A span of time, such as how long a call took.
     *
     * @property duration the span of time.
     * @since 1.0.0
     */
    public class Elapsed(public val duration: Duration) : LogValue {
        /**
         * Returns whether [other] is an [Elapsed] of the same span.
         *
         * @param other the value to compare with.
         * @return `true` if both hold the same span.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Elapsed && other.duration == duration

        /**
         * Returns the hash code of the span.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = duration.hashCode()

        /**
         * Shows the span, such as `1.5s`.
         *
         * @return the span.
         * @since 1.0.0
         */
        override fun toString(): String = duration.toString()
    }

    /**
     * A value that must never be written, such as a password or a token; layouts write [SafeText.REDACTED] instead.
     *
     * It holds nothing: the secret itself is never stored in a record, so no sink, buffer or heap dump of the logging
     * can reveal it, and not even its length is known.
     *
     * @since 1.0.0
     */
    public object Secret : LogValue {
        /**
         * Shows the mask that stands in for every secret.
         *
         * @return [SafeText.REDACTED].
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = SafeText.REDACTED
    }

    /**
     * A list of values.
     *
     * @param values the values, copied.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public class Items(values: List<LogValue>) : LogValue {
        /**
         * The values, in order; the list cannot be changed.
         *
         * @since 1.0.0
         */
        public val values: List<LogValue> = values.immutableList()

        /**
         * Returns whether [other] is an [Items] with equal values in the same order.
         *
         * @param other the value to compare with.
         * @return `true` if both hold equal values.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Items && other.values == values

        /**
         * Returns the hash code of the values.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the values, each as its own `toString` shows it.
         *
         * @return the values in brackets.
         * @since 1.0.0
         */
        override fun toString(): String = values.toString()
    }

    /**
     * A group of named values, such as the fields of an object.
     *
     * @param values the named values, copied in their order.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public class Entries(values: Map<String, LogValue>) : LogValue {
        /**
         * The named values, in the order they were given; the map cannot be changed.
         *
         * @since 1.0.0
         */
        public val values: Map<String, LogValue> = values.orderedCopy()

        /**
         * Returns whether [other] is an [Entries] with equal named values.
         *
         * @param other the value to compare with.
         * @return `true` if both hold equal named values.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Entries && other.values == values

        /**
         * Returns the hash code of the named values.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the named values with their names escaped.
         *
         * @return the named values in braces.
         * @since 1.0.0
         */
        override fun toString(): String = values.entries.joinToString(prefix = "{", postfix = "}") { entry ->
            SafeText.text(entry.key) + "=" + entry.value
        }
    }

    /**
     * The absence of a value, such as a field whose source was `null`.
     *
     * @since 1.0.0
     */
    public object Null : LogValue {
        /**
         * Shows `null`.
         *
         * @return `null`.
         * @since 1.0.0
         */
        override fun toString(): String = "null"
    }
}
