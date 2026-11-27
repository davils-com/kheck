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

import com.davils.kheck.api.Logger
import com.davils.kheck.api.info
import com.davils.kheck.core.mask.Mask
import com.davils.kheck.core.sink.custom.sink
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlinx.coroutines.runBlocking

/**
 * Measures a batch of records through the whole pipeline into a sink that discards them: plain, with the hash mask of
 * krypt on every field and message, or sealed in an integrity chain.
 *
 * @since 1.0.0
 */
@State(Scope.Benchmark)
class PipelineBenchmark {
    /**
     * What the pipeline does besides writing: `plain`, `hashed` or `sealed`.
     *
     * @since 1.0.0
     */
    @Param("plain", "hashed", "sealed")
    var mode: String = PLAIN

    private var bench: Bench? = null
    private var logger: Logger? = null

    /**
     * Starts a logging for the mode.
     *
     * @since 1.0.0
     */
    @Setup
    fun prepare() {
        val created = Bench {
            sink("discard", KeepingSink(isKeeping = false)) {
                if (mode == SEALED) integrity { key = benchmarkKey() }
            }
            if (mode == HASHED) {
                mask {
                    default = Mask.Hash
                    hashKey = benchmarkKey()
                }
            }
        }
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
     * Logs a batch of records and waits until the sink wrote them.
     *
     * @since 1.0.0
     */
    @Benchmark
    fun batch() {
        val target = logger ?: return
        val running = bench ?: return
        repeat(BATCH) { index ->
            target.info {
                field(name = "password", value = "hunter$index")
                "request $index served for alice@example.com"
            }
        }
        runBlocking { running.logging.flush() }
    }

    private companion object {
        private const val PLAIN: String = "plain"
        private const val HASHED: String = "hashed"
        private const val SEALED: String = "sealed"
        private const val BATCH: Int = 100
    }
}
