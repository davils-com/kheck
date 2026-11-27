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

package com.davils.kheck.core.format.plain

import com.davils.kheck.core.format.TimestampFormat
import com.davils.kheck.core.style.SegmentStyle

/**
 * One part of the line a plain layout writes for a record, in the order the layout declares them.
 *
 * Every segment writes foreign text escaped, so no record can forge a line or control a terminal.
 *
 * @since 1.0.0
 */
public sealed interface PlainSegment {
    /**
     * Fixed text, such as a separator.
     *
     * @property value the text, written as it is.
     * @property style the style of the text.
     * @since 1.0.0
     */
    public class Text(public val value: String, public val style: SegmentStyle) : PlainSegment {
        private val values: List<Any?> get() = listOf(value, style)

        /**
         * Compares by value: two [Text] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Text].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Text && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Text(value=\"$value\", style=$style)"
    }

    /**
     * The time of the record.
     *
     * @property format how the time is written.
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Timestamp(
        public val format: TimestampFormat,
        public val frame: SegmentFrame,
        public val style: SegmentStyle,
    ) : PlainSegment {
        private val values: List<Any?> get() = listOf(format, frame, style)

        /**
         * Compares by value: two [Timestamp] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Timestamp].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Timestamp && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Timestamp(format=$format, frame=$frame, style=$style)"
    }

    /**
     * The level of the record, in capitals.
     *
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Level(public val frame: SegmentFrame, public val style: SegmentStyle) : PlainSegment {
        private val values: List<Any?> get() = listOf(frame, style)

        /**
         * Compares by value: two [Level] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Level].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Level && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Level(frame=$frame, style=$style)"
    }

    /**
     * The name of the thread that logged the record.
     *
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Thread(public val frame: SegmentFrame, public val style: SegmentStyle) : PlainSegment {
        private val values: List<Any?> get() = listOf(frame, style)

        /**
         * Compares by value: two [Thread] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Thread].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Thread && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Thread(frame=$frame, style=$style)"
    }

    /**
     * The name of the logger, shortened by abbreviating its leading packages to their first letter.
     *
     * @property abbreviation the length the name is shortened to, or 0 to keep it whole.
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Logger(
        public val abbreviation: Int,
        public val frame: SegmentFrame,
        public val style: SegmentStyle,
    ) : PlainSegment {
        private val values: List<Any?> get() = listOf(abbreviation, frame, style)

        /**
         * Compares by value: two [Logger] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Logger].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Logger && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Logger(abbreviation=$abbreviation, frame=$frame, style=$style)"
    }

    /**
     * The frame that logged the record, written only when the logger locates its callers.
     *
     * @property affix the text around the frame.
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Caller(
        public val affix: SegmentAffix,
        public val frame: SegmentFrame,
        public val style: SegmentStyle,
    ) : PlainSegment {
        private val values: List<Any?> get() = listOf(affix, frame, style)

        /**
         * Compares by value: two [Caller] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Caller].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Caller && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Caller(affix=$affix, frame=$frame, style=$style)"
    }

    /**
     * The markers of the record in brackets, written only when it has markers.
     *
     * @property affix the text around the markers.
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Markers(
        public val affix: SegmentAffix,
        public val frame: SegmentFrame,
        public val style: SegmentStyle,
    ) : PlainSegment {
        private val values: List<Any?> get() = listOf(affix, frame, style)

        /**
         * Compares by value: two [Markers] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Markers].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Markers && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Markers(affix=$affix, frame=$frame, style=$style)"
    }

    /**
     * The message of the record.
     *
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Message(public val frame: SegmentFrame, public val style: SegmentStyle) : PlainSegment {
        private val values: List<Any?> get() = listOf(frame, style)

        /**
         * Compares by value: two [Message] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Message].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Message && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Message(frame=$frame, style=$style)"
    }

    /**
     * The sequence number of the record.
     *
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Sequence(public val frame: SegmentFrame, public val style: SegmentStyle) : PlainSegment {
        private val values: List<Any?> get() = listOf(frame, style)

        /**
         * Compares by value: two [Sequence] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Sequence].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Sequence && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Sequence(frame=$frame, style=$style)"
    }

    /**
     * The fields of the event, and the count of omitted parts when the record lost any.
     *
     * @property layout how the fields are written.
     * @since 1.0.0
     */
    public class Fields(public val layout: EntriesLayout) : PlainSegment {
        private val values: List<Any?> get() = listOf(layout)

        /**
         * Compares by value: two [Fields] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Fields].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Fields && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Fields(layout=$layout)"
    }

    /**
     * The fields of the log context and of the enrichment.
     *
     * @property layout how the fields are written.
     * @since 1.0.0
     */
    public class Context(public val layout: EntriesLayout) : PlainSegment {
        private val values: List<Any?> get() = listOf(layout)

        /**
         * Compares by value: two [Context] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Context].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Context && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Context(layout=$layout)"
    }

    /**
     * The stack trace of the record, on the lines below it.
     *
     * @property layout how the stack trace is written.
     * @since 1.0.0
     */
    public class Cause(public val layout: CauseLayout) : PlainSegment {
        private val values: List<Any?> get() = listOf(layout)

        /**
         * Compares by value: two [Cause] segments are equal when every property is equal.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Cause].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Cause && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the properties of the segment.
         *
         * @return the segment as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Cause(layout=$layout)"
    }
}
