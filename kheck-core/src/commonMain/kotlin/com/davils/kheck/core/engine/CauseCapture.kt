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

package com.davils.kheck.core.engine

import com.davils.kheck.core.Limits
import com.davils.kheck.core.internal.platform.framesOf
import com.davils.kheck.core.internal.platform.typeNameOf
import com.davils.kheck.core.record.LogCause

internal class CauseCapture private constructor() {
    private val seen = mutableListOf<Throwable>()

    private fun cause(throwable: Throwable, depth: Int): LogCause {
        seen += throwable
        val stack = framesOf(throwable, Limits.MAX_CAPTURED_FRAMES)
        val next = depth + 1
        return LogCause(
            type = typeNameOf(throwable),
            message = throwable.message,
            frames = stack.frames,
            omittedFrames = stack.omitted,
            cause = throwable.cause?.let { cause -> nested(cause, next) },
            suppressed = suppressed(throwable, next),
        )
    }

    private fun suppressed(throwable: Throwable, depth: Int): List<LogCause> {
        val candidates = throwable.suppressedExceptions.take(Limits.MAX_SUPPRESSED)
        return candidates.mapNotNull { candidate -> nested(candidate, depth) }
    }

    private fun nested(throwable: Throwable, depth: Int): LogCause? {
        val isSeen = seen.any { known -> known === throwable }
        if (isSeen || depth >= Limits.MAX_CAUSE_DEPTH) return null
        return cause(throwable, depth)
    }

    companion object {
        fun capture(throwable: Throwable): LogCause = CauseCapture().cause(throwable, depth = 0)
    }
}
