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

import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kheck.core.format.LogEncoder
import com.davils.kheck.core.format.LogFormat
import com.davils.kheck.core.format.plain.encoding.PlainEncoder
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.ColorMode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlinx.datetime.TimeZone

/**
 * The plain layout: one line of text per record, made of [segments], followed by the stack trace of its cause.
 *
 * Every piece of foreign text, such as the message, a field, the logger or the stack trace, is escaped where it is
 * written: a line break becomes `\n` and a control character `\u{…}`, so a record can neither forge a line of the
 * log nor control a terminal. A text longer than [Limits.MAX_TEXT_LENGTH][com.davils.kheck.core.Limits.MAX_TEXT_LENGTH]
 * is shortened and marked. Created by [plain].
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class PlainFormat internal constructor(builder: PlainFormatBuilder) : LogFormat, Validatable {
    /**
     * Whether the layout writes colors.
     *
     * @since 1.0.0
     */
    override val colorMode: ColorMode = builder.colorMode

    /**
     * The styles the segments start from.
     *
     * @since 1.0.0
     */
    public val theme: PlainTheme = builder.theme

    /**
     * The time zone of the local timestamps.
     *
     * @since 1.0.0
     */
    public val zone: TimeZone = builder.zone

    /**
     * The segments of a line, in order; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val segments: List<PlainSegment> = builder.segments().immutableList()

    private val values: List<Any?> get() = listOf(colorMode, theme, zone, segments)

    /**
     * Creates the encoder of one sink.
     *
     * @param depth the colors of the sink.
     * @return a new encoder.
     * @since 1.0.0
     */
    override fun encoder(depth: ColorDepth): LogEncoder = PlainEncoder(this, depth)

    /**
     * Checks that the layout has segments, a message and valid widths and limits.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (segments.isEmpty()) {
            fail(::segments, "must contain at least one segment", LoggingViolationCodes.EMPTY_LAYOUT)
            return
        }
        val hasMessage = segments.any { segment -> segment is PlainSegment.Message }
        if (!hasMessage) warn(::segments, "has no message segment", LoggingViolationCodes.MISSING_MESSAGE)
        each(::segments, segments) { segment -> checkSegment(segment) }
    }

    /**
     * Compares by value: two [PlainFormat] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [PlainFormat].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is PlainFormat && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the color mode, the zone and the count of segments.
     *
     * @return the format as text.
     * @since 1.0.0
     */
    override fun toString(): String = "PlainFormat(colorMode=$colorMode, zone=$zone, segments=${segments.size})"

    private fun ValidationScope.checkSegment(segment: PlainSegment) {
        when (segment) {
            is PlainSegment.Timestamp -> checkFrame(segment.frame)
            is PlainSegment.Level -> checkFrame(segment.frame)
            is PlainSegment.Thread -> checkFrame(segment.frame)
            is PlainSegment.Logger -> checkLogger(segment)
            is PlainSegment.Caller -> checkFrame(segment.frame)
            is PlainSegment.Markers -> checkFrame(segment.frame)
            is PlainSegment.Message -> checkFrame(segment.frame)
            is PlainSegment.Sequence -> checkFrame(segment.frame)
            is PlainSegment.Cause -> checkCause(segment.layout)
            is PlainSegment.Text, is PlainSegment.Fields, is PlainSegment.Context -> return
        }
    }

    private fun ValidationScope.checkLogger(segment: PlainSegment.Logger) {
        checkFrame(segment.frame)
        if (segment.abbreviation >= 0) return
        fail(
            property = PlainSegment.Logger::abbreviation,
            message = "must not be negative, but is ${segment.abbreviation}",
            code = LoggingViolationCodes.INVALID_ABBREVIATION,
        )
    }

    private fun ValidationScope.checkFrame(frame: SegmentFrame) {
        val isNegative = frame.minWidth < 0 || frame.maxWidth < 0
        if (isNegative) {
            fail(SegmentFrame::minWidth, "widths must not be negative", LoggingViolationCodes.INVALID_WIDTH)
            return
        }
        val isBounded = frame.maxWidth > 0
        val isNarrower = frame.maxWidth < frame.minWidth
        if (!isBounded || !isNarrower) return
        fail(
            property = SegmentFrame::maxWidth,
            message = "must not be below the minimum width of ${frame.minWidth}, but is ${frame.maxWidth}",
            code = LoggingViolationCodes.INVALID_WIDTH,
        )
    }

    private fun ValidationScope.checkCause(layout: CauseLayout) {
        if (layout.maxFrames < 0) {
            fail(CauseLayout::maxFrames, "must not be negative", LoggingViolationCodes.INVALID_FRAMES)
        }
        if (layout.maxCauses >= 0) return
        fail(CauseLayout::maxCauses, "must not be negative", LoggingViolationCodes.INVALID_FRAMES)
    }
}
