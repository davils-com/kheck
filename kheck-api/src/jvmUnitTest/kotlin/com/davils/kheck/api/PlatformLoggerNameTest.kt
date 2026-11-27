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

package com.davils.kheck.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlatformLoggerNameTest : FunSpec({
    context("names of types on the JVM") {
        test("name an anonymous type after its binary name") {
            val anonymous = object {}

            LoggerName.of(anonymous::class).value shouldBe anonymous::class.java.name
        }

        test("name a local type after its binary name") {
            class Local

            LoggerName.of(Local::class).value shouldBe Local::class.java.name
        }
    }
})
