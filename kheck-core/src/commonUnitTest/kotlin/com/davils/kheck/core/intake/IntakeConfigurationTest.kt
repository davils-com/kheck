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

package com.davils.kheck.core.intake

import com.davils.kheck.core.sink.LogOverflow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class IntakeConfigurationTest : FunSpec({
    context("intakes") {
        test("compare capacity and overflow and show them") {
            val intake = IntakeConfiguration(capacity = 1, overflow = LogOverflow.DropNewest)
            val same = IntakeConfiguration(capacity = 1, overflow = LogOverflow.DropNewest)

            intake shouldNotBe IntakeConfiguration(capacity = 2, overflow = LogOverflow.DropNewest)
            intake shouldNotBe IntakeConfiguration(capacity = 1, overflow = LogOverflow.DropOldest)
            intake.equals(LogOverflow.DropNewest) shouldBe false
            intake.hashCode() shouldBe same.hashCode()
            intake.toString() shouldBe "IntakeConfiguration(capacity=1, overflow=DropNewest)"
        }
    }
})
