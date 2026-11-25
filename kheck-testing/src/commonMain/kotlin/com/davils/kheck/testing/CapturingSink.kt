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

package com.davils.kheck.testing

import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.sink.LogLine
import com.davils.kheck.core.sink.LogSink
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.ColorMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A sink that keeps every line it is given and every call made on it, and fails on demand.
 *
 * @param depth the colors the sink claims to show; none by default.
 * @since 1.0.0
 */
public class CapturingSink(private val depth: ColorDepth = ColorDepth.None) : LogSink {
    private val captured = MutableStateFlow<List<LogLine>>(emptyList())
    private val failure = MutableStateFlow<Throwable?>(null)
    private val calls = MutableStateFlow<List<SinkCall>>(emptyList())

    /**
     * Every line written so far.
     *
     * @since 1.0.0
     */
    public val lines: List<LogLine> get() = captured.value

    /**
     * The records of the lines.
     *
     * @since 1.0.0
     */
    public val records: List<LogRecord> get() = lines.map(LogLine::record)

    /**
     * The encoded texts of the lines.
     *
     * @since 1.0.0
     */
    public val texts: List<String> get() = lines.map(LogLine::text)

    /**
     * The messages of the records.
     *
     * @since 1.0.0
     */
    public val messages: List<String> get() = records.map(LogRecord::message)

    /**
     * Every call made on the sink, in order.
     *
     * @since 1.0.0
     */
    public val history: List<SinkCall> get() = calls.value

    /**
     * Makes every following write throw [cause].
     *
     * @param cause what a write throws.
     * @since 1.0.0
     */
    public fun fail(cause: Throwable) {
        failure.value = cause
    }

    /**
     * Makes the following writes succeed again.
     *
     * @since 1.0.0
     */
    public fun recover() {
        failure.value = null
    }

    /**
     * Forgets the captured lines.
     *
     * @since 1.0.0
     */
    public fun clear() {
        captured.value = emptyList()
    }

    /**
     * Returns the colors given at creation.
     *
     * @param mode ignored.
     * @return the colors.
     * @since 1.0.0
     */
    override fun colorDepth(mode: ColorMode): ColorDepth = depth

    /**
     * Records the call.
     *
     * @since 1.0.0
     */
    override suspend fun open() {
        called(SinkCall.Open)
    }

    /**
     * Records the call and keeps [lines], or throws the cause set by [fail].
     *
     * @param lines the lines.
     * @since 1.0.0
     */
    override suspend fun write(lines: List<LogLine>) {
        called(SinkCall.Write(lines.size))
        val cause = failure.value
        if (cause != null) throw cause
        captured.update { current -> current + lines }
    }

    /**
     * Records the call.
     *
     * @since 1.0.0
     */
    override suspend fun flush() {
        called(SinkCall.Flush)
    }

    /**
     * Records the call.
     *
     * @since 1.0.0
     */
    override suspend fun close() {
        called(SinkCall.Close)
    }

    private fun called(call: SinkCall) {
        calls.update { current -> current + call }
    }
}
