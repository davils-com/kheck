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

import com.davils.kheck.api.LogLevel
import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.core.sink.LogLine
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.ColorMode
import com.davils.kheck.core.terminal.ConsoleTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest

private const val TEXT: String = "line\n"

class CapturingSinkTest : FunSpec({
    context("the capturing sink") {
        test("keeps lines and calls and fails on demand") {
            runTest {
                val sink = CapturingSink(ColorDepth.Ansi256)
                val line = LogLine(logRecord { message = "m" }, TEXT)
                sink.open()
                sink.write(listOf(line))
                sink.fail(IllegalStateException("down"))
                shouldThrow<IllegalStateException> { sink.write(listOf(line)) }
                sink.recover()
                sink.flush()
                sink.close()

                sink.colorDepth(ColorMode.Auto) shouldBe ColorDepth.Ansi256
                sink.texts shouldBe listOf(TEXT)
                sink.messages shouldBe listOf("m")
                sink.history shouldBe listOf(
                    SinkCall.Open,
                    SinkCall.Write(1),
                    SinkCall.Write(1),
                    SinkCall.Flush,
                    SinkCall.Close,
                )
                sink.clear()
                sink.lines.shouldBeEmpty()
            }
        }

        test("names its calls") {
            listOf(SinkCall.Open, SinkCall.Write(2), SinkCall.Flush, SinkCall.Close).map(SinkCall::toString) shouldBe
                listOf("Open", "Write(2)", "Flush", "Close")
            SinkCall.Write(2).hashCode() shouldBe 2
            SinkCall.Write(2) shouldNotBe SinkCall.Flush
            SinkCall.Write(2) shouldNotBe SinkCall.Write(1)
            SinkCall.Write(2).equals(SinkCall.Close) shouldBe false
        }
    }

    context("the fake terminal") {
        test("keeps what is written per target") {
            val terminal = FakeTerminal(setOf(ConsoleTarget.StandardError))
            terminal.write(ConsoleTarget.StandardOutput, "a\n")
            terminal.write(ConsoleTarget.StandardOutput, "b\n")

            terminal.lines() shouldBe listOf("a", "b")
            terminal.lines(ConsoleTarget.StandardError).shouldBeEmpty()
            terminal.isInteractive(ConsoleTarget.StandardError) shouldBe true
            terminal.isInteractive(ConsoleTarget.StandardOutput) shouldBe false
        }
    }

    context("recorded diagnostics and records") {
        test("keep reports and build records from fixed values") {
            val diagnostics = RecordingDiagnostics()
            diagnostics.report("broken")
            val record = logRecord {
                level = LogLevel.Warn
                thread = "worker"
                logger = "a.B"
                sequence = 2L
                omitted = 1
                markers = emptySet()
                caller = LogFrame(className = "a.B", methodName = "run")
                cause = IllegalStateException("x")
                fields { field(name = "f", value = 1) }
                context { field(name = "c", value = 2) }
            }

            diagnostics.messages shouldBe listOf("broken")
            record.timestamp shouldBe LogRecordBuilder.TIMESTAMP
            record.cause?.message shouldBe "x"
            record.fields.keys shouldBe setOf("f")
            record.context.keys shouldBe setOf("c")
            logRecord().message shouldBe LogRecordBuilder.MESSAGE
        }
    }
})
