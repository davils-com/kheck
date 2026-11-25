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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val HOST: String = "host"
private const val USER: String = "user"

class EnrichmentTest : FunSpec({
    context("enrichment") {
        test("reads the host and the user from the variables of every platform") {
            val windows = EnrichmentBuilder(mapOf("COMPUTERNAME" to "WS-01", "USERNAME" to "bob"))
            windows.hostname()
            windows.user()

            windows.fields() shouldBe mapOf(HOST to LogValue.Text("WS-01"), USER to LogValue.Text("bob"))
        }

        test("falls back to unknown") {
            val empty = EnrichmentBuilder(emptyMap())
            empty.hostname()
            empty.user()

            empty.fields() shouldBe mapOf(HOST to LogValue.Text("unknown"), USER to LogValue.Text("unknown"))
        }
    }
})
