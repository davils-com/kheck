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

import com.davils.kheck.core.fixture.loggingCodes
import com.davils.kheck.core.mask.Mask
import com.davils.kheck.core.sink.LogOverflow
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val TOO_SHORT: Int = 10
private const val TOO_DEEP: Int = 65

class ValidationsTest : FunSpec({
    context("sampling") {
        test("refuses a window, a period and counts that cannot work") {
            loggingCodes { sample { dedupe(Duration.INFINITE, maxEntries = 0) } } shouldContain
                LoggingViolationCodes.INVALID_WINDOW
            loggingCodes { sample { dedupe(1.seconds, maxEntries = 0) } } shouldContain
                LoggingViolationCodes.INVALID_LIMIT
            loggingCodes { sample { limit(records = 0, per = Duration.ZERO) } } shouldContain
                LoggingViolationCodes.INVALID_LIMIT
            loggingCodes { sample { rate = 2.0 } } shouldContain LoggingViolationCodes.INVALID_RATE
        }
    }

    context("masking") {
        test("refuses a short scan, a deep scan and negative partial masks") {
            loggingCodes { mask { maxValueLength = TOO_SHORT } } shouldContain LoggingViolationCodes.INVALID_SCAN_LENGTH
            loggingCodes { mask { maxDepth = TOO_DEEP } } shouldContain LoggingViolationCodes.INVALID_DEPTH
            loggingCodes { mask { default = Mask.Partial(keepStart = -1, keepEnd = 0) } } shouldContain
                LoggingViolationCodes.INVALID_PARTIAL
        }
    }

    context("sinks") {
        test("refuses an overflow that never ends its wait") {
            loggingCodes {
                sink("blocked", CapturingSink()) { overflow = LogOverflow.Block(Duration.ZERO) }
            } shouldContain LoggingViolationCodes.INVALID_WAIT
        }
    }
})
