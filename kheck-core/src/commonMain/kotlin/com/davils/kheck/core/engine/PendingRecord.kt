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
import com.davils.kheck.api.record.LogEvent
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.record.LogFrame
import kotlin.time.Instant

internal class PendingRecord(
    val sequence: Long,
    val timestamp: Instant,
    val level: LogLevel,
    val logger: LoggerName,
    val thread: String,
    val event: LogEvent,
    val context: Map<String, LogValue>,
    val caller: LogFrame?,
)
