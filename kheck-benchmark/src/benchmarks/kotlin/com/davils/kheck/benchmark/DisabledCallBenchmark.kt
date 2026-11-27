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

package com.davils.kheck.benchmark

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.Logger
import com.davils.kheck.api.debug
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown

/**
 * Measures the call of a logger whose level is disabled, which must not build the message.
 *
 * @since 1.0.0
 */
@State(Scope.Benchmark)
class DisabledCallBenchmark {
    private var bench: Bench? = null
    private var logger: Logger? = null

    /**
     * Starts a logging at the level info.
     *
     * @since 1.0.0
     */
    @Setup
    fun prepare() {
        val created = Bench { level = LogLevel.Info }
        bench = created
        logger = created.logging.logger("com.example.Benchmark")
    }

    /**
     * Closes the logging.
     *
     * @since 1.0.0
     */
    @TearDown
    fun release() {
        bench?.closed()
    }

    /**
     * Calls the logger at the level debug.
     *
     * @since 1.0.0
     */
    @Benchmark
    fun disabled() {
        logger?.debug { "never built" }
    }
}
