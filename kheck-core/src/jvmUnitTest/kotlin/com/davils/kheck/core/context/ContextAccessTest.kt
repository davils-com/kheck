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

@file:OptIn(DavilsInternal::class)

package com.davils.kheck.core.context

import com.davils.kheck.api.record.LogValue
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@OptIn(DavilsInternal::class)
class ContextAccessTest : FunSpec({
    context("the context of the thread") {
        test("is read and replaced") {
            val previous = ContextAccess.replace(mapOf("mdc" to LogValue.Text("value")))

            ContextAccess.current() shouldBe mapOf("mdc" to LogValue.Text("value"))
            ContextAccess.replace(previous)
            ContextAccess.current() shouldBe previous
        }
    }
})
