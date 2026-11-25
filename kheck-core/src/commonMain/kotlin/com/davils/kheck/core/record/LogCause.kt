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

package com.davils.kheck.core.record

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * A throwable as a record carries it: captured once when the record is made, so that the record holds no reference to
 * the throwable and every sink sees the same values.
 *
 * The capture is bounded by the [Limits][com.davils.kheck.core.Limits] of kheck: frames beyond the limit are counted
 * in [omittedFrames], and the chain of causes and suppressed throwables ends at the limit, and at a throwable that was
 * already captured, so a cycle cannot loop.
 *
 * @property type the qualified name of the class of the throwable.
 * @property message the message of the throwable, foreign text that every layout escapes, or `null`.
 * @param frames the frames of the stack, copied.
 * @property omittedFrames how many frames of the stack were left out.
 * @property cause the cause of the throwable, if any.
 * @param suppressed the throwables suppressed by this one, copied.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LogCause(
    public val type: String,
    public val message: String?,
    frames: List<LogFrame> = emptyList(),
    public val omittedFrames: Int = 0,
    public val cause: LogCause? = null,
    suppressed: List<LogCause> = emptyList(),
) {
    /**
     * The frames of the stack, innermost first; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val frames: List<LogFrame> = frames.immutableList()

    /**
     * The throwables suppressed by this one; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val suppressed: List<LogCause> = suppressed.immutableList()

    internal fun withMessages(transform: (String) -> String): LogCause = LogCause(
        type = type,
        message = message?.let(transform),
        frames = frames,
        omittedFrames = omittedFrames,
        cause = cause?.withMessages(transform),
        suppressed = suppressed.map { nested -> nested.withMessages(transform) },
    )

    /**
     * Shows the type of the throwable and the count of its frames, but not its message, which is foreign text.
     *
     * @return the cause as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LogCause(type=$type, frames=${frames.size}, omittedFrames=$omittedFrames)"
}
