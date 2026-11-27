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

import com.davils.kheck.api.info
import com.davils.kheck.core.integrity.IntegrityChain
import com.davils.kheck.core.integrity.IntegrityReport
import com.davils.kheck.core.integrity.LogSeal
import com.davils.kheck.core.integrity.SealedText
import com.davils.kheck.core.sink.custom.sink
import com.davils.krypt.core.algorithm.MacAlgorithm
import com.davils.krypt.primitive.Crypto
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking

/**
 * Measures the verification of an integrity chain of a thousand lines.
 *
 * @since 1.0.0
 */
@State(Scope.Benchmark)
class ChainBenchmark {
    private val crypto = Crypto()
    private val key = benchmarkKey()
    private var entries: List<SealedText> = emptyList()

    /**
     * Seals a thousand lines through a logging.
     *
     * @since 1.0.0
     */
    @Setup
    fun prepare() {
        val sink = KeepingSink(isKeeping = true)
        val bench = Bench { sink("keep", sink) { integrity { key = this@ChainBenchmark.key } } }
        val logger = bench.logging.logger("com.example.Benchmark")
        repeat(LINES) { index -> logger.info { "line $index" } }
        runBlocking { bench.logging.flush() }
        bench.closed()
        entries = sink.lines.mapNotNull { line -> line.seal?.let { seal -> SealedText(line.text, seal) } }
    }

    /**
     * Verifies the chain.
     *
     * @return the report, intact.
     * @since 1.0.0
     */
    @Benchmark
    fun verify(): IntegrityReport = runBlocking {
        IntegrityChain.verify(
            crypto = crypto,
            key = key,
            algorithm = MacAlgorithm.HmacSha256,
            anchor = LogSeal.origin(MacAlgorithm.HmacSha256),
            entries = entries.asFlow(),
        )
    }

    private companion object {
        private const val LINES: Int = 1_000
    }
}
