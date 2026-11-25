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

package com.davils.kheck.core.sink

import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.ColorMode

/**
 * Where encoded records go, such as the console, a socket or a collector of the application.
 *
 * Each sink runs on a coroutine of its own: its calls never overlap, and a slow sink delays only its own queue. A call
 * may suspend and may throw; a failure is contained, counted, reported to the diagnostics and announced as an event,
 * the batch is retried as the [retry][LogSinkOptions.retry] of the sink allows and then handed to its
 * [fallback][LogSinkOptions.fallback], if any. A sink that logs through kheck while it writes has that record dropped
 * on the JVM, so it cannot feed itself.
 *
 * @since 1.0.0
 */
public interface LogSink {
    /**
     * Returns the colors this sink shows for [mode]; [ColorDepth.None] unless overridden.
     *
     * @param mode the color mode of the format.
     * @return the colors of the sink.
     * @since 1.0.0
     */
    public fun colorDepth(mode: ColorMode): ColorDepth = ColorDepth.None

    /**
     * Opens the sink, once, before the first batch.
     *
     * @since 1.0.0
     */
    public suspend fun open(): Unit = Unit

    /**
     * Writes a batch of encoded records.
     *
     * @param lines the records and their text, in order.
     * @since 1.0.0
     */
    public suspend fun write(lines: List<LogLine>)

    /**
     * Writes what the sink buffers.
     *
     * @since 1.0.0
     */
    public suspend fun flush(): Unit = Unit

    /**
     * Closes the sink, once, after the last batch.
     *
     * @since 1.0.0
     */
    public suspend fun close(): Unit = Unit
}
