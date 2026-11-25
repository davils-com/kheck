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
    public class Text(public val value: String, public val style: SegmentStyle) : PlainSegment

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
    ) : PlainSegment

    /**
     * The level of the record, in capitals.
     *
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Level(public val frame: SegmentFrame, public val style: SegmentStyle) : PlainSegment

    /**
     * The name of the thread that logged the record.
     *
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Thread(public val frame: SegmentFrame, public val style: SegmentStyle) : PlainSegment

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
    ) : PlainSegment

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
    ) : PlainSegment

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
    ) : PlainSegment

    /**
     * The message of the record.
     *
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Message(public val frame: SegmentFrame, public val style: SegmentStyle) : PlainSegment

    /**
     * The sequence number of the record.
     *
     * @property frame the width of the segment.
     * @property style the style of the segment.
     * @since 1.0.0
     */
    public class Sequence(public val frame: SegmentFrame, public val style: SegmentStyle) : PlainSegment

    /**
     * The fields of the event, and the count of omitted parts when the record lost any.
     *
     * @property layout how the fields are written.
     * @since 1.0.0
     */
    public class Fields(public val layout: EntriesLayout) : PlainSegment

    /**
     * The fields of the log context and of the enrichment.
     *
     * @property layout how the fields are written.
     * @since 1.0.0
     */
    public class Context(public val layout: EntriesLayout) : PlainSegment

    /**
     * The stack trace of the record, on the lines below it.
     *
     * @property layout how the stack trace is written.
     * @since 1.0.0
     */
    public class Cause(public val layout: CauseLayout) : PlainSegment
}
