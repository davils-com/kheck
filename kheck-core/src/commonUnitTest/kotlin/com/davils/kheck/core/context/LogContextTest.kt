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

package com.davils.kheck.core.context

import com.davils.kheck.api.Limits
import com.davils.kheck.api.record.LogValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val TENANT: String = "tenant"

class LogContextTest : FunSpec({
    context("limits") {
        test("keeps the first fields up to the limit") {
            val fields = (0..Limits.MAX_FIELDS).associate { index -> "field$index" to LogValue.Integer(index.toLong()) }

            val element = logContext(fields)[LogContext].shouldNotBeNull()

            element.fields.size shouldBe Limits.MAX_FIELDS
            element.fields.keys.first() shouldBe "field0"
        }

        test("leaves out blank and too long names") {
            val fields = mapOf(
                "" to LogValue.Text("blank"),
                "x".repeat(Limits.MAX_NAME_LENGTH + 1) to LogValue.Text("long"),
                TENANT to LogValue.Text("acme"),
            )

            val element = logContext(fields)[LogContext].shouldNotBeNull()

            element.fields.keys shouldBe setOf(TENANT)
        }
    }

    context("builder") {
        test("collects the fields of the builder") {
            val element = logContext { field(name = TENANT, value = "acme") }[LogContext].shouldNotBeNull()

            element.fields shouldBe mapOf(TENANT to LogValue.Text("acme"))
        }
    }
})
