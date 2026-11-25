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

import com.davils.kheck.api.LogLevel
import com.davils.kheck.core.format.plain.Alignment
import com.davils.kheck.core.format.plain.SegmentFrame
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.SegmentStyle
import com.davils.kheck.core.style.TextStyle
import com.davils.kheck.core.style.ansi.AnsiRenderer

internal class StyledWriter(depth: ColorDepth) {
    private val renderer = AnsiRenderer(depth)

    fun styled(text: CharSequence, style: TextStyle, out: StringBuilder) {
        if (text.isEmpty()) return
        out.append(renderer.open(style))
        out.append(text)
        out.append(renderer.close(style))
    }

    fun styled(text: CharSequence, style: SegmentStyle, level: LogLevel, out: StringBuilder) {
        styled(text, style.styleFor(level), out)
    }

    fun framed(text: String, frame: SegmentFrame, style: TextStyle, out: StringBuilder) {
        val content = truncated(text, frame)
        val padding = (frame.minWidth - content.length).coerceAtLeast(0)
        if (frame.alignment == Alignment.End) pad(padding, out)
        styled(content, style, out)
        if (frame.alignment == Alignment.Start) pad(padding, out)
    }

    private fun truncated(text: String, frame: SegmentFrame): String {
        val isTooLong = frame.maxWidth in 1 until text.length
        if (!isTooLong) return text
        return clipped(text, frame)
    }

    private fun clipped(text: String, frame: SegmentFrame): String = when (frame.alignment) {
        Alignment.Start -> text.take(frame.maxWidth)
        Alignment.End -> text.takeLast(frame.maxWidth)
    }

    private fun pad(count: Int, out: StringBuilder) {
        repeat(count) { out.append(' ') }
    }
}
