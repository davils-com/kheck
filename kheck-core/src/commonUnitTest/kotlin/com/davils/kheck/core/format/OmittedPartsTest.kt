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

package com.davils.kheck.core.format

import com.davils.kheck.core.format.fixture.rendered
import com.davils.kheck.core.format.json.JsonLayout
import com.davils.kheck.core.format.json.json
import com.davils.kheck.core.format.plain.plain
import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val OMITTED: Int = 3

class OmittedPartsTest : FunSpec({
    context("a record that lost parts to the limits") {
        test("says so in every layout") {
            val record = logRecord { omitted = OMITTED }

            plain { layout { fields() } }.rendered(record) shouldContain "omitted=3"
            json().rendered(record) shouldContain """"omitted":3"""
            json { layout = JsonLayout.Ecs }.rendered(record) shouldContain """"kheck":{"omitted":3}"""
            json { layout = JsonLayout.Logstash }.rendered(record) shouldContain """"omitted":3"""
            json { layout = JsonLayout.OpenTelemetry }.rendered(record) shouldContain """"log.record.omitted":3"""
        }
    }

    context("a caller without a line") {
        test("leaves the line out") {
            val record = logRecord { caller = LogFrame(className = "a.B", methodName = "run") }

            json().rendered(record) shouldNotContain """"line""""
            json { layout = JsonLayout.OpenTelemetry }.rendered(record) shouldNotContain "code.lineno"
        }
    }
})
