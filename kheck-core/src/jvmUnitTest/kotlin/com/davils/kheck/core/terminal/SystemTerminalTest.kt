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

package com.davils.kheck.core.terminal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SystemTerminalTest : FunSpec({
    context("the console of the process") {
        test("writes each target to its stream") {
            val output = ByteArrayOutputStream()
            val error = ByteArrayOutputStream()
            val originalOutput = System.out
            val originalError = System.err
            PrintStream(output, true, Charsets.UTF_8).use { capturedOutput ->
                PrintStream(error, true, Charsets.UTF_8).use { capturedError ->
                    System.setOut(capturedOutput)
                    System.setErr(capturedError)
                    try {
                        Terminal.SYSTEM.write(ConsoleTarget.StandardOutput, "out\n")
                        Terminal.SYSTEM.write(ConsoleTarget.StandardError, "err\n")
                    } finally {
                        System.setOut(originalOutput)
                        System.setErr(originalError)
                    }
                }
            }

            output.toString(Charsets.UTF_8) shouldBe "out\n"
            error.toString(Charsets.UTF_8) shouldBe "err\n"
            Terminal.SYSTEM.isInteractive(ConsoleTarget.StandardOutput) shouldBe false
        }
    }
})
