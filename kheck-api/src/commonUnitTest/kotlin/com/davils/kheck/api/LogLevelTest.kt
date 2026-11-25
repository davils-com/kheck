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

class LogLevelTest : FunSpec({
    context("isAtLeast") {
        test("compares severities") {
            LogLevel.Error.isAtLeast(LogLevel.Warn) shouldBe true
            LogLevel.Info.isAtLeast(LogLevel.Info) shouldBe true
            LogLevel.Debug.isAtLeast(LogLevel.Info) shouldBe false
        }

        test("never passes off") {
            LogLevel.Off.isAtLeast(LogLevel.Trace) shouldBe false
            LogLevel.Off.isAtLeast(LogLevel.Off) shouldBe false
        }
    }
})
