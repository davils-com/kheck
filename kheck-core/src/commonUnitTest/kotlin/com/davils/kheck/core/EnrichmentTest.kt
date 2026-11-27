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

package com.davils.kheck.core

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.sink.console.ConsoleSinkBuilder
import com.davils.knot.environment.Environment
import com.davils.knot.environment.environment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val HOST: String = "host"
private const val USER: String = "user"

private const val TERM_VARIABLE: String = "TERM"
private const val XTERM: String = "xterm"

class EnrichmentTest : FunSpec({
    context("enrichment") {
        test("reads the host and the user from the variables of every platform") {
            val variables = environment { variables(mapOf("COMPUTERNAME" to "WS-01", "USERNAME" to "bob")) }
            val windows = EnrichmentBuilder(variables)
            windows.hostname()
            windows.user()

            windows.fields() shouldBe mapOf(HOST to LogValue.Text("WS-01"), USER to LogValue.Text("bob"))
        }

        test("falls back to unknown") {
            val empty = EnrichmentBuilder(Environment.EMPTY)
            empty.hostname()
            empty.user()

            empty.fields() shouldBe mapOf(HOST to LogValue.Text("unknown"), USER to LogValue.Text("unknown"))
        }
    }

    context("environments") {
        test("pass from the logging to the configuration of a console sink") {
            val terms = environment { variables(mapOf(TERM_VARIABLE to XTERM)) }
            val builder = LoggingBuilder()
            builder.environment = terms
            val console = ConsoleSinkBuilder(builder.environment)

            console.environment.variable(TERM_VARIABLE) shouldBe XTERM
            console.build().environment shouldBe terms
            LoggingBuilder().environment shouldBe Environment.SYSTEM
        }
    }
})
