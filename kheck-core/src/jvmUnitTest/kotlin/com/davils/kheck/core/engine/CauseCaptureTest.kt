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

package com.davils.kheck.core.engine

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val DEPTH: Int = 20
private const val MAX_DEPTH: Int = 16

class CauseCaptureTest : FunSpec({
    context("chains") {
        test("stops a long chain of causes at the depth limit") {
            val chain = (1..DEPTH).fold(
                RuntimeException("root"),
            ) { cause, index -> RuntimeException("level $index", cause) }

            val captured = CauseCapture.capture(chain)

            generateSequence(captured) { cause -> cause.cause }.count() shouldBe MAX_DEPTH
        }

        test("captures a failure that suppresses itself only once") {
            val failure = IllegalStateException("loop")
            val wrapper = RuntimeException("wrapper", failure)
            failure.addSuppressed(wrapper)

            val captured = CauseCapture.capture(wrapper)

            captured.cause?.suppressed shouldBe emptyList()
        }
    }
})
