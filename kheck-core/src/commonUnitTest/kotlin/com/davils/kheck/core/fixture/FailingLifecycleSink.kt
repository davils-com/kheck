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

package com.davils.kheck.core.fixture

import com.davils.kheck.core.sink.LogLine
import com.davils.kheck.core.sink.LogSink

internal class FailingLifecycleSink : LogSink {
    val written = mutableListOf<String>()

    override suspend fun open() {
        error("cannot open")
    }

    override suspend fun write(lines: List<LogLine>) {
        written += lines.map(LogLine::text)
    }

    override suspend fun flush() {
        error("cannot flush")
    }

    override suspend fun close() {
        error("cannot close")
    }
}
