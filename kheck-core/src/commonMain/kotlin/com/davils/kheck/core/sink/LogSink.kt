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
 * This is the extension point for every destination: kheck writes no file and opens no connection itself, a sink of
 * the application does. The contract every implementation can rely on, which `logSinkContract` of `kheck-testing`
 * checks:
 *
 * - **One coroutine per sink.** Calls never overlap, and a slow sink delays only its own queue.
 * - **One life per applied configuration.** [open] runs before the first batch and [close] after the last one of each
 *   configuration; a [reconfigure][com.davils.kheck.core.Logging.reconfigure] closes the sink and opens it again, so
 *   an instance given to `sink(name, instance)` must be reopenable.
 * - **Failures are contained.** A call may suspend and may throw; a failure is counted, reported to the diagnostics,
 *   announced as an event and passed through the [stages][LogSinkOptions.stages] of the sink, such as a retry, and
 *   then the batch goes to the [fallback][LogSinkOptions.fallback], if any. A refusal of a stage, such as an open
 *   circuit breaker, skips the sink.
 * - **Cancellation is cooperative.** A time limiter among the stages cancels a call that takes too long; a sink that
 *   blocks without suspending is not interrupted.
 * - **Lines are final.** Their text is escaped and ends with a line break; with an integrity chain each carries its
 *   [seal][LogLine.seal], which the sink stores with the text.
 *
 * A sink that logs through kheck while it writes has that record dropped on the JVM, so it cannot feed itself.
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
     * Opens the sink before the first batch of a configuration; after [close], a new configuration opens it again.
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
     * Closes the sink after the last batch of a configuration, after a final [flush].
     *
     * @since 1.0.0
     */
    public suspend fun close(): Unit = Unit
}
