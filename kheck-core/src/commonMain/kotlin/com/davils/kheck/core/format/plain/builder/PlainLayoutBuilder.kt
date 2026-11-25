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

package com.davils.kheck.core.format.plain.builder

import com.davils.kheck.core.format.TimestampFormat
import com.davils.kheck.core.format.plain.PlainSegment
import com.davils.kheck.core.format.plain.PlainTheme
import com.davils.kheck.core.format.plain.SegmentAffix
import com.davils.kheck.core.style.SegmentStyle
import com.davils.kheck.core.style.SegmentStyleBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Declares the segments of a plain line in order, each styled by the theme unless it sets a style of its own.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class PlainLayoutBuilder internal constructor(private val theme: PlainTheme) {
    private val segments = mutableListOf<PlainSegment>()

    /**
     * Adds fixed text.
     *
     * @param value the text, written as it is.
     * @param block sets the style of the text.
     * @since 1.0.0
     */
    public fun text(value: String, block: SegmentStyleBuilder.() -> Unit = {}) {
        segments += PlainSegment.Text(value, styled(SegmentStyle.NONE, block))
    }

    /**
     * Adds the time of the record.
     *
     * @param format how the time is written; [TimestampFormat.DateTime] by default.
     * @param block sets the width and the style.
     * @since 1.0.0
     */
    public fun timestamp(format: TimestampFormat = TimestampFormat.DateTime, block: SegmentBuilder.() -> Unit = {}) {
        val builder = segment(theme.timestamp, block)
        segments += PlainSegment.Timestamp(format, builder.frame(), builder.segmentStyle())
    }

    /**
     * Adds the level of the record, padded to five characters unless the block changes it.
     *
     * @param block sets the width and the style.
     * @since 1.0.0
     */
    public fun level(block: SegmentBuilder.() -> Unit = {}) {
        val builder = SegmentBuilder(theme.level)
        builder.minWidth = LEVEL_WIDTH
        builder.block()
        segments += PlainSegment.Level(builder.frame(), builder.segmentStyle())
    }

    /**
     * Adds the name of the thread.
     *
     * @param block sets the width and the style.
     * @since 1.0.0
     */
    public fun thread(block: SegmentBuilder.() -> Unit = {}) {
        val builder = segment(theme.thread, block)
        segments += PlainSegment.Thread(builder.frame(), builder.segmentStyle())
    }

    /**
     * Adds the name of the logger.
     *
     * @param abbreviation the length the name is shortened to, or 0 to keep it whole.
     * @param block sets the width and the style.
     * @since 1.0.0
     */
    public fun logger(abbreviation: Int = 0, block: SegmentBuilder.() -> Unit = {}) {
        val builder = segment(theme.logger, block)
        segments += PlainSegment.Logger(abbreviation, builder.frame(), builder.segmentStyle())
    }

    /**
     * Adds the frame that logged, after a space unless the block changes it.
     *
     * @param block sets the affix, the width and the style.
     * @since 1.0.0
     */
    public fun caller(block: OptionalSegmentBuilder.() -> Unit = {}) {
        val builder = optional(theme.caller, SegmentAffix(prefix = " "), block)
        segments += PlainSegment.Caller(builder.affix(), builder.frame(), builder.segmentStyle())
    }

    /**
     * Adds the markers in brackets, followed by a space unless the block changes it.
     *
     * @param block sets the affix, the width and the style.
     * @since 1.0.0
     */
    public fun markers(block: OptionalSegmentBuilder.() -> Unit = {}) {
        val builder = optional(theme.markers, SegmentAffix(suffix = " "), block)
        segments += PlainSegment.Markers(builder.affix(), builder.frame(), builder.segmentStyle())
    }

    /**
     * Adds the message.
     *
     * @param block sets the width and the style.
     * @since 1.0.0
     */
    public fun message(block: SegmentBuilder.() -> Unit = {}) {
        val builder = segment(theme.message, block)
        segments += PlainSegment.Message(builder.frame(), builder.segmentStyle())
    }

    /**
     * Adds the sequence number.
     *
     * @param block sets the width and the style.
     * @since 1.0.0
     */
    public fun sequence(block: SegmentBuilder.() -> Unit = {}) {
        val builder = segment(SegmentStyle.NONE, block)
        segments += PlainSegment.Sequence(builder.frame(), builder.segmentStyle())
    }

    /**
     * Adds the fields of the event.
     *
     * @param block sets how the fields are written.
     * @since 1.0.0
     */
    public fun fields(block: EntriesSegmentBuilder.() -> Unit = {}) {
        val builder = EntriesSegmentBuilder(theme.entries)
        builder.block()
        segments += PlainSegment.Fields(builder.layout())
    }

    /**
     * Adds the fields of the log context and of the enrichment.
     *
     * @param block sets how the fields are written.
     * @since 1.0.0
     */
    public fun context(block: EntriesSegmentBuilder.() -> Unit = {}) {
        val builder = EntriesSegmentBuilder(theme.entries)
        builder.block()
        segments += PlainSegment.Context(builder.layout())
    }

    /**
     * Adds the stack trace, on the lines below the line of the record.
     *
     * @param block sets how the stack trace is written.
     * @since 1.0.0
     */
    public fun cause(block: CauseSegmentBuilder.() -> Unit = {}) {
        val builder = CauseSegmentBuilder(theme.cause)
        builder.block()
        segments += PlainSegment.Cause(builder.layout())
    }

    internal fun segments(): List<PlainSegment> = segments.toList()

    private fun segment(initial: SegmentStyle, block: SegmentBuilder.() -> Unit): SegmentBuilder {
        val builder = SegmentBuilder(initial)
        builder.block()
        return builder
    }

    private fun optional(
        initial: SegmentStyle,
        affix: SegmentAffix,
        block: OptionalSegmentBuilder.() -> Unit,
    ): OptionalSegmentBuilder {
        val builder = OptionalSegmentBuilder(initial, affix)
        builder.block()
        return builder
    }

    private companion object {
        private const val LEVEL_WIDTH: Int = 5
    }
}
