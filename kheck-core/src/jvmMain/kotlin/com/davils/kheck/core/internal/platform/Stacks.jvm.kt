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

package com.davils.kheck.core.internal.platform

import com.davils.kheck.core.record.LogFrame

private val walker: StackWalker = StackWalker.getInstance()

internal actual fun locateCaller(skipped: List<String>): LogFrame? = walker.walk { frames ->
    val candidates = frames.filter { candidate -> skipped.none { prefix -> candidate.className.startsWith(prefix) } }
    val first = candidates.findFirst()
    val located = first.map { frame ->
        LogFrame(
            className = frame.className,
            methodName = frame.methodName,
            fileName = frame.fileName,
            lineNumber = frame.lineNumber,
        )
    }
    located.orElse(null)
}

internal actual fun framesOf(throwable: Throwable, limit: Int): StackFrames {
    val trace = throwable.stackTrace
    val kept = trace.take(limit).map { element ->
        LogFrame(
            className = element.className,
            methodName = element.methodName,
            fileName = element.fileName,
            lineNumber = element.lineNumber,
        )
    }
    return StackFrames(frames = kept, omitted = trace.size - kept.size)
}

internal actual fun typeNameOf(throwable: Throwable): String = throwable.javaClass.name
