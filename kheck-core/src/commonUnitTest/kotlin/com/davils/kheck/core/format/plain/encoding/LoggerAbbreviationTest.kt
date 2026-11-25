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

package com.davils.kheck.core.format.plain.encoding

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val TARGET: Int = 10
private const val NAMES: Int = 4_100

class LoggerAbbreviationTest : FunSpec({
    context("abbreviations") {
        test("shorten the leading packages and keep the class") {
            LoggerAbbreviation(TARGET).abbreviated("com.example.inventory.Service") shouldBe "c.e.i.Service"
            LoggerAbbreviation(0).abbreviated("com.example.Service") shouldBe "com.example.Service"
        }

        test("stay correct when the cache starts over") {
            val abbreviation = LoggerAbbreviation(TARGET)

            repeat(NAMES) { index -> abbreviation.abbreviated("com.example.Service$index") }

            abbreviation.abbreviated("com.example.Service0") shouldBe "c.e.Service0"
        }
    }
})
