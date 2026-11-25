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

package com.davils.kheck.core.sink.console

import com.davils.kheck.core.internal.platform.isWindowsHost
import com.davils.kheck.core.sink.LogLine
import com.davils.kheck.core.sink.LogSink
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.ColorMode
import com.davils.kheck.core.terminal.ColorDepthDetection

internal class ConsoleSink(private val configuration: ConsoleSinkConfiguration) : LogSink {
    private val detection = ColorDepthDetection(configuration.environment, configuration.terminal, isWindowsHost())

    override fun colorDepth(mode: ColorMode): ColorDepth = detection.resolve(mode, configuration.target)

    override suspend fun write(lines: List<LogLine>) {
        val text = buildString { lines.forEach { line -> append(line.text) } }
        configuration.terminal.write(configuration.target, text)
    }
}
