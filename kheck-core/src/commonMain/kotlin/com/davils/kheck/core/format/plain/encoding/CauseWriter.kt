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

import com.davils.kheck.core.format.plain.CauseLayout
import com.davils.kheck.core.format.text.appendSanitized
import com.davils.kheck.core.record.LogCause
import com.davils.kheck.core.record.LogFrame

internal class CauseWriter(private val layout: CauseLayout, private val writer: StyledWriter) {
    private val scratch = StringBuilder()

    fun write(cause: LogCause, out: StringBuilder) {
        write(Entry(cause = cause, label = null, enclosing = emptyList(), margin = "", depth = 0), out)
    }

    private fun write(entry: Entry, out: StringBuilder) {
        header(entry, out)
        val common = commonTail(frames = entry.cause.frames, enclosing = entry.enclosing)
        frames(entry, entry.cause.frames.size - common, out)
        if (common > 0) line(margin = entry.margin, text = "... $common more", out = out)
        suppressed(entry, out)
        chained(entry, out)
    }

    private fun header(entry: Entry, out: StringBuilder) {
        out.append(entry.margin)
        val label = entry.label
        if (label != null) {
            writer.styled(label, layout.styles.label, out)
            out.append(' ')
        }
        writer.styled(sanitized(entry.cause.type), layout.styles.type, out)
        val message = entry.cause.message
        if (message != null) {
            out.append(": ")
            writer.styled(sanitized(message), layout.styles.message, out)
        }
        out.append('\n')
    }

    private fun frames(entry: Entry, unique: Int, out: StringBuilder) {
        var printed = 0
        var omitted = entry.cause.omittedFrames
        entry.cause.frames.take(unique).forEach { frame ->
            val isHidden = printed >= layout.maxFrames || isExcluded(frame)
            if (isHidden) {
                omitted++
                return@forEach
            }
            frame(entry.margin, frame, out)
            printed++
        }
        if (omitted > 0) line(margin = entry.margin, text = "... $omitted frames omitted", out = out)
    }

    private fun frame(margin: String, frame: LogFrame, out: StringBuilder) {
        out.append(margin)
        out.append(layout.indent)
        out.append("at ")
        writer.styled(sanitized("${frame.className}.${frame.methodName}"), layout.styles.frame, out)
        out.append('(')
        writer.styled(sanitized(location(frame)), layout.styles.location, out)
        out.append(")\n")
    }

    private fun suppressed(entry: Entry, out: StringBuilder) {
        if (!layout.isSuppressedIncluded) return
        entry.cause.suppressed.forEach { suppressed ->
            write(
                Entry(
                    cause = suppressed,
                    label = SUPPRESSED,
                    enclosing = entry.cause.frames,
                    margin = entry.margin + layout.indent,
                    depth = entry.depth + 1,
                ),
                out,
            )
        }
    }

    private fun chained(entry: Entry, out: StringBuilder) {
        val next = entry.cause.cause ?: return
        if (entry.depth + 1 >= layout.maxCauses) {
            line(margin = entry.margin, text = "... further causes omitted", out = out)
            return
        }
        write(
            Entry(
                cause = next,
                label = CAUSED_BY,
                enclosing = entry.cause.frames,
                margin = entry.margin,
                depth = entry.depth + 1,
            ),
            out,
        )
    }

    private fun line(margin: String, text: String, out: StringBuilder) {
        out.append(margin)
        out.append(layout.indent)
        writer.styled(text, layout.styles.frame, out)
        out.append('\n')
    }

    private fun isExcluded(frame: LogFrame): Boolean =
        layout.excludedPackages.any { prefix -> frame.className.startsWith(prefix) }

    private fun location(frame: LogFrame): String {
        val file = frame.fileName ?: return UNKNOWN_SOURCE
        val line = frame.lineNumber.takeIf { number -> number >= 0 }
        return listOfNotNull(file, line).joinToString(":")
    }

    private fun sanitized(text: String): String {
        scratch.setLength(0)
        return scratch.appendSanitized(text).toString()
    }

    private fun commonTail(frames: List<LogFrame>, enclosing: List<LogFrame>): Int {
        var count = 0
        while (count < frames.size && count < enclosing.size) {
            val isSame = frames[frames.size - 1 - count] == enclosing[enclosing.size - 1 - count]
            if (!isSame) return count
            count++
        }
        return count
    }

    private data class Entry(
        val cause: LogCause,
        val label: String?,
        val enclosing: List<LogFrame>,
        val margin: String,
        val depth: Int,
    )

    private companion object {
        private const val CAUSED_BY: String = "Caused by:"
        private const val SUPPRESSED: String = "Suppressed:"
        private const val UNKNOWN_SOURCE: String = "Unknown Source"
    }
}
