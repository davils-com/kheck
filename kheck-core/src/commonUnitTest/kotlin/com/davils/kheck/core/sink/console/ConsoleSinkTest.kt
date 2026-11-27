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

import com.davils.kheck.api.info
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.format.plain.plain
import com.davils.kheck.core.style.Color
import com.davils.kheck.core.terminal.ConsoleTarget
import com.davils.kheck.testing.FakeTerminal
import com.davils.kheck.testing.LogSinkContract
import com.davils.knot.environment.Environment
import com.davils.knot.environment.environment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest

class ConsoleSinkTest : FunSpec({
    context("terminal") {
        test("writes plain lines to a terminal that is not interactive") {
            runTest {
                val terminal = FakeTerminal()
                val logging = testLogging {
                    console {
                        this.terminal = terminal
                        format = plain { layout { message() } }
                    }
                }

                logging.logger(SERVICE).info { "first" }
                logging.flush()
                logging.logger(SERVICE).info { "second" }
                logging.flush()

                terminal.lines() shouldBe listOf("first", "second")
                terminal.lines(ConsoleTarget.StandardError) shouldBe emptyList()
            }
        }

        test("colors lines for an interactive terminal") {
            runTest {
                val terminal = FakeTerminal(setOf(ConsoleTarget.StandardError))
                val logging = testLogging {
                    console("errors") {
                        this.terminal = terminal
                        target = ConsoleTarget.StandardError
                        environment = environment { variables(mapOf("COLORTERM" to "truecolor")) }
                        format = plain { layout { message { style { foreground = Color.hex("#00FF00") } } } }
                    }
                }

                logging.logger(SERVICE).info { "green" }
                logging.flush()

                terminal.text(ConsoleTarget.StandardError) shouldContain "\u001B[38;2;0;255;0mgreen"
            }
        }

        test("keeps the contract of a sink") {
            val configuration = ConsoleSinkBuilder(Environment.EMPTY).build()

            LogSinkContract.verify { ConsoleSink(configuration) } shouldBe emptyList()
        }

        test("describes its target") {
            val configuration = ConsoleSinkBuilder(Environment.EMPTY).build()

            configuration.toString() shouldContain "target=StandardOutput"
        }
    }
})
