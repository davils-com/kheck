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

package com.davils.kheck.core.format.fixture

import com.davils.kheck.core.format.LogFormat
import com.davils.kheck.core.record.LogCause
import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.style.ColorDepth

internal const val ESCAPE: String = "\u001B["

internal const val RESET: String = "\u001B[0m"

internal fun LogFormat.rendered(record: LogRecord, depth: ColorDepth = ColorDepth.None): String {
    val out = StringBuilder()
    encoder(depth).encode(record, out)
    return out.toString()
}

internal fun frame(type: String, method: String, line: Int): LogFrame = LogFrame(
    className = type,
    methodName = method,
    fileName = "${type.substringAfterLast('.')}.kt",
    lineNumber = line,
)

internal val ROOT_FRAME: LogFrame = frame(type = "com.example.Main", method = "main", line = 3)

internal val SERVICE_FRAME: LogFrame = frame(type = "com.example.Service", method = "run", line = 10)

internal val READER_FRAME: LogFrame = frame(type = "com.example.Reader", method = "read", line = 5)

internal val FAILURE: LogCause = LogCause(
    type = "java.lang.IllegalStateException",
    message = "the inventory could not be loaded",
    frames = listOf(SERVICE_FRAME, ROOT_FRAME),
    cause = LogCause(
        type = "java.io.IOException",
        message = "disk full",
        frames = listOf(READER_FRAME, ROOT_FRAME),
    ),
)
