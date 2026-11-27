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

import com.davils.kheck.core.integrity.LogSeal
import com.davils.kheck.core.sink.LogLine
import com.davils.kheck.core.sink.LogSink
import com.davils.kore.core.failure.attempted
import com.davils.kore.testing.HostileFragments
import kotlinx.io.bytestring.ByteString

/**
 * Checks a [LogSink] against the contract kheck relies on, independent of the test framework: every step of the
 * lifecycle of [LogSink] in order, a second life after closing, empty batches, the hostile fragments of
 * `kore-testing` and sealed lines.
 *
 * ```kotlin
 * test("keeps the contract of a sink") {
 *     LogSinkContract.verify { CollectorSink(fakeCollector()) } shouldBe emptyList()
 * }
 * ```
 *
 * The sink must be healthy for the check: it reports every step that throws as a violation. What the sink stored is
 * for the test of the sink to check.
 *
 * @since 1.0.0
 */
public object LogSinkContract {
    private const val LIVES: Int = 2
    private const val NEW_LINE: String = "\n"
    private const val SEAL_BYTES: Int = 32

    /**
     * Runs the contract on a sink [create] returns and on the same sink after it was closed once.
     *
     * @param create creates the sink to check; called once.
     * @return a description of every step that threw, empty when the sink keeps the contract.
     * @since 1.0.0
     */
    public suspend fun verify(create: () -> LogSink): List<String> {
        val sink = create()
        val violations = mutableListOf<String>()
        repeat(LIVES) { life ->
            violations += lived(sink, life + 1)
        }
        return violations
    }

    private suspend fun lived(sink: LogSink, life: Int): List<String> = listOfNotNull(
        step("open", life) { sink.open() },
        step("write an empty batch", life) { sink.write(emptyList()) },
        step("write plain lines", life) { sink.write(lines(listOf("plain"))) },
        step("write hostile lines", life) { sink.write(lines(HostileFragments.all)) },
        step("write sealed lines", life) { sink.write(sealed()) },
        step("flush", life) { sink.flush() },
        step("close", life) { sink.close() },
    )

    private suspend fun step(name: String, life: Int, block: suspend () -> Unit): String? {
        val failure = attempted { block() }.exceptionOrNull() ?: return null
        val type = listOfNotNull(failure::class.simpleName).joinToString()
        return "life $life: $name failed with $type"
    }

    private fun lines(texts: List<String>): List<LogLine> = texts.map { text ->
        LogLine(record = logRecord { message = text }, text = text + NEW_LINE)
    }

    private fun sealed(): List<LogLine> = lines(listOf("sealed")).mapIndexed { index, line ->
        val seal = LogSeal(sequence = index + 1L, mac = ByteString(ByteArray(SEAL_BYTES)))
        LogLine(record = line.record, text = line.text, seal = seal)
    }
}
