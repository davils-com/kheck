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

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.internal.platform.currentThreadName
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.record.LogSource
import kotlin.time.Clock

internal class Notices(private val clock: () -> Clock, private val sequence: () -> Long) {
    fun dropped(sink: String, count: Long): LogRecord = LogRecord(
        sequence = sequence(),
        timestamp = clock().now(),
        level = LogLevel.Warn,
        source = LogSource(logger = LOGGER, thread = currentThreadName()),
        message = "dropped $count records because the queue of sink '$sink' was full",
        fields = mapOf(DROPPED to LogValue.Integer(count)),
    )

    internal companion object {
        val LOGGER: LoggerName = LoggerName("kheck.logging")
        const val DROPPED: String = "dropped"
    }
}
