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

package com.davils.kheck.core.sample

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

private const val TENANT: String = "tenant"

class RateLimitKeyTest : FunSpec({
    context("keys") {
        test("compare fields by name and show themselves") {
            RateLimitKey.PerField(TENANT) shouldBe RateLimitKey.PerField(TENANT)
            RateLimitKey.PerField(TENANT).hashCode() shouldBe TENANT.hashCode()
            RateLimitKey.PerField(TENANT) shouldNotBe RateLimitKey.Global
            listOf(RateLimitKey.Global, RateLimitKey.PerLogger, RateLimitKey.PerLevel, RateLimitKey.PerField("t"))
                .map(RateLimitKey::toString) shouldBe listOf("Global", "PerLogger", "PerLevel", "PerField(t)")
            Deduplication(1.seconds, 1).toString() shouldBe "Deduplication(window=1s, maxEntries=1)"
        }
    }
})
