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

import com.davils.kheck.api.info
import com.davils.kheck.core.fixture.ChattySink
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.sink.custom.sink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class SinkReentranceTest : FunSpec({
    context("reentrance") {
        test("drops records a sink logs while it writes") {
            runTest {
                val chatty = ChattySink()
                val logging = testLogging { sink("chatty", chatty) }
                chatty.logger = logging.logger(SERVICE)

                logging.logger(SERVICE).info { "outer" }
                logging.flush()

                chatty.written shouldBe listOf("outer")
                logging.statistics().reentrant shouldBe 1L
            }
        }
    }
})
