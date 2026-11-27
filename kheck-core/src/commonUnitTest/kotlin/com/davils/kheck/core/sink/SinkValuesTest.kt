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

import com.davils.kheck.core.integrity.LogSeal
import com.davils.kheck.core.mask.fixture.secretKey
import com.davils.kheck.core.sink.custom.CustomSinkBuilder
import com.davils.kheck.testing.CapturingSink
import com.davils.kheck.testing.logRecord
import com.davils.krypt.core.algorithm.MacAlgorithm
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.seconds

private const val TEXT: String = "text\n"

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

    context("lines") {
        test("show the record and the length") {
            LogLine(logRecord(), TEXT).toString() shouldContain "length=5"
        }
    }

    context("equality") {
        test("compares lines by value") {
            val record = logRecord()

            LogLine(record, TEXT) shouldBe LogLine(record, TEXT)
            LogLine(record, TEXT).hashCode() shouldBe LogLine(record, TEXT).hashCode()
            LogLine(record, TEXT) shouldNotBe LogLine(record, "other\n")
        }
    }

    context("foreign values and options") {
        test("compare options by value and show the stages and the chain") {
            val sink = CapturingSink()
            val first = CustomSinkBuilder(sink).build().options
            val second = CustomSinkBuilder(sink).build().options
            val sealed = LogLine(
                record = logRecord(),
                text = TEXT,
                seal = LogSeal.origin(MacAlgorithm.HmacSha256),
            )

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first.equals(sink) shouldBe false
            first.toString() shouldContain "stages=0, integrity=none"
            val sealing = CustomSinkBuilder(sink)
            sealing.integrity { key = secretKey("sink-key-0123456789") }
            sealing.build().options.toString() shouldContain "integrity=IntegrityOptions(algorithm=HmacSha256"
            sealed.toString() shouldContain "seal=0"
            sealed.equals(TEXT) shouldBe false
        }
    }
})
