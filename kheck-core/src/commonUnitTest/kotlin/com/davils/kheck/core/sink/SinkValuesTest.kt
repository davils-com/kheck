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

package com.davils.kheck.core.sink

import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.seconds

class SinkValuesTest : FunSpec({
    context("overflows") {
        test("compare by content and show their names") {
            LogOverflow.Block(1.seconds) shouldBe LogOverflow.Block(1.seconds)
            LogOverflow.Block(1.seconds).hashCode() shouldBe 1.seconds.hashCode()
            LogOverflow.Block(1.seconds) shouldNotBe LogOverflow.DropNewest
            LogOverflow.Block(1.seconds).toString() shouldBe "Block(maxWait=1s)"
            LogOverflow.DropNewest.toString() shouldBe "DropNewest"
            LogOverflow.DropOldest.toString() shouldBe "DropOldest"
        }
    }

    context("lines and retries") {
        test("show the record and the length, and the retry") {
            LogLine(logRecord(), "text\n").toString() shouldContain "length=5"
            SinkRetry.NONE.toString() shouldBe "SinkRetry(attempts=1, backoff=0s, maxBackoff=0s)"
        }
    }
})
