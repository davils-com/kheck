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

package com.davils.kheck.core.format.plain.encoding

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.format.LogEncoder
import com.davils.kheck.core.format.plain.EntriesLayout
import com.davils.kheck.core.format.plain.PlainFormat
import com.davils.kheck.core.format.plain.PlainSegment
import com.davils.kheck.core.format.plain.SegmentAffix
import com.davils.kheck.core.format.plain.SegmentFrame
import com.davils.kheck.core.format.text.TimestampWriter
import com.davils.kheck.core.format.text.appendSanitized
import com.davils.kheck.core.format.text.appendValue
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.SegmentStyle

internal class PlainEncoder(format: PlainFormat, depth: ColorDepth) : LogEncoder {
    private val segments = format.segments

    private val writer = StyledWriter(depth)

    private val timestamps = TimestampWriter(format.zone)

    private val scratch = StringBuilder()

    private val abbreviations = HashMap<Int, LoggerAbbreviation>()

    private val causes = segments.filterIsInstance<PlainSegment.Cause>().map { segment ->
        CauseWriter(segment.layout, writer)
    }

    override fun encode(record: LogRecord, out: StringBuilder) {
        val line = Line(record, out)
        segments.forEach(line::segment)
        out.append('\n')
        val cause = record.cause ?: return
        causes.forEach { writer -> writer.write(cause, out) }
    }

    private fun abbreviated(record: LogRecord, abbreviation: Int): String {
        val abbreviator = abbreviations.getOrPut(abbreviation) { LoggerAbbreviation(abbreviation) }
        return abbreviator.abbreviated(record.source.logger.value)
    }

    private inner class Line(private val record: LogRecord, private val out: StringBuilder) {
        fun segment(segment: PlainSegment) {
            when (segment) {
                is PlainSegment.Text -> styled(segment.value, segment.style)

                is PlainSegment.Timestamp -> framed(segment.frame, segment.style) { text ->
                    timestamps.write(record.timestamp, segment.format, text)
                }

                is PlainSegment.Level -> framed(segment.frame, segment.style) { text ->
                    text.append(record.level.name.uppercase())
                }

                is PlainSegment.Thread -> framed(segment.frame, segment.style) { text ->
                    text.appendSanitized(record.source.thread)
                }

                is PlainSegment.Logger -> framed(segment.frame, segment.style) { text ->
                    text.appendSanitized(abbreviated(record, segment.abbreviation))
                }

                is PlainSegment.Caller -> caller(segment)

                is PlainSegment.Markers -> markers(segment)

                is PlainSegment.Message -> framed(segment.frame, segment.style) { text ->
                    text.appendSanitized(record.message)
                }

                is PlainSegment.Sequence -> framed(segment.frame, segment.style) { text ->
                    text.append(record.sequence)
                }

                is PlainSegment.Fields -> entries(fieldsOf(record), segment.layout)

                is PlainSegment.Context -> entries(record.context, segment.layout)

                is PlainSegment.Cause -> return
            }
        }

        private fun caller(segment: PlainSegment.Caller) {
            val caller = record.source.caller ?: return
            affixed(segment.affix) {
                framed(segment.frame, segment.style) { text ->
                    text.appendSanitized("${caller.className}.${caller.methodName}")
                    text.append(':')
                    text.append(caller.lineNumber)
                }
            }
        }

        private fun markers(segment: PlainSegment.Markers) {
            if (record.markers.isEmpty()) return
            affixed(segment.affix) {
                framed(segment.frame, segment.style) { text ->
                    val names = record.markers.joinToString(separator = ",") { marker -> marker.name }
                    text.append('[')
                    text.appendSanitized(names)
                    text.append(']')
                }
            }
        }

        private fun entries(values: Map<String, LogValue>, layout: EntriesLayout) {
            if (values.isEmpty()) return
            affixed(layout.affix) {
                var index = 0
                values.forEach { (key, value) ->
                    if (index > 0) out.append(layout.separator)
                    styledText(layout.styles.key) { text -> text.appendSanitized(key) }
                    styled(EQUALS, layout.styles.punctuation)
                    styledText(layout.styles.value) { text -> text.appendValue(value, isQuoted = true) }
                    index++
                }
            }
        }

        private inline fun affixed(affix: SegmentAffix, body: () -> Unit) {
            out.append(affix.prefix)
            body()
            out.append(affix.suffix)
        }

        private inline fun framed(frame: SegmentFrame, style: SegmentStyle, content: (StringBuilder) -> Unit) {
            scratch.setLength(0)
            content(scratch)
            writer.framed(
                text = scratch.toString(),
                frame = frame,
                style = style.levels[record.level] ?: style.base,
                out = out,
            )
        }

        private inline fun styledText(style: SegmentStyle, content: (StringBuilder) -> Unit) {
            scratch.setLength(0)
            content(scratch)
            styled(scratch, style)
        }

        private fun styled(text: CharSequence, style: SegmentStyle) {
            writer.styled(text = text, style = style, level = record.level, out = out)
        }
    }

    private fun fieldsOf(record: LogRecord): Map<String, LogValue> {
        if (record.omitted == 0) return record.fields
        return record.fields + (OMITTED to LogValue.Integer(record.omitted.toLong()))
    }

    private companion object {
        private const val OMITTED: String = "omitted"

        private const val EQUALS: String = "="
    }
}
