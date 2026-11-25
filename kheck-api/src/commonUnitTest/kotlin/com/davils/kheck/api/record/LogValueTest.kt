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

package com.davils.kheck.api.record

import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val HALF: Double = 0.5
private val INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")

class LogValueTest : FunSpec({
    context("equality") {
        test("compares every type by its content") {
            val pairs: List<Pair<LogValue, LogValue>> = listOf(
                LogValue.Text("a") to LogValue.Text("a"),
                LogValue.Integer(1L) to LogValue.Integer(1L),
                LogValue.Decimal(Double.NaN) to LogValue.Decimal(Double.NaN),
                LogValue.Bool(true) to LogValue.Bool(true),
                LogValue.Timestamp(INSTANT) to LogValue.Timestamp(INSTANT),
                LogValue.Elapsed(1.seconds) to LogValue.Elapsed(1.seconds),
                LogValue.Items(listOf(LogValue.Null)) to LogValue.Items(listOf(LogValue.Null)),
                LogValue.Entries(mapOf("a" to LogValue.Secret)) to LogValue.Entries(mapOf("a" to LogValue.Secret)),
            )
            pairs.forEach { (left, right) ->
                left shouldBe right
                left.hashCode() shouldBe right.hashCode()
                left shouldNotBe LogValue.Null
            }
        }

        test("tells values of the same type with a different content apart") {
            LogValue.Text("a") shouldNotBe LogValue.Text("b")
            LogValue.Integer(1L) shouldNotBe LogValue.Integer(2L)
            LogValue.Decimal(HALF) shouldNotBe LogValue.Decimal(1.0)
            LogValue.Bool(true) shouldNotBe LogValue.Bool(false)
            LogValue.Timestamp(INSTANT) shouldNotBe LogValue.Timestamp(Instant.DISTANT_PAST)
            LogValue.Elapsed(1.seconds) shouldNotBe LogValue.Elapsed(2.seconds)
            LogValue.Items(emptyList()) shouldNotBe LogValue.Items(listOf(LogValue.Null))
            LogValue.Entries(emptyMap()) shouldNotBe LogValue.Entries(mapOf("a" to LogValue.Null))
        }
    }

    context("showing a value") {
        test("shows every type by its content") {
            LogValue.Text("a").toString() shouldBe "\"a\""
            LogValue.Integer(1L).toString() shouldBe "1"
            LogValue.Decimal(HALF).toString() shouldBe "0.5"
            LogValue.Bool(false).toString() shouldBe "false"
            LogValue.Timestamp(INSTANT).toString() shouldBe "2026-01-01T00:00:00Z"
            LogValue.Elapsed(1.seconds).toString() shouldBe "1s"
            LogValue.Items(listOf(LogValue.Null)).toString() shouldBe "[null]"
            LogValue.Entries(mapOf("a" to LogValue.Secret)).toString() shouldBe "{a=****}"
        }

        test("escapes hostile text and hostile names") {
            val hostile = "x${HostileFragments.CLEAR_SCREEN}\n${HostileFragments.RIGHT_TO_LEFT_OVERRIDE}"
            isSafeToShow(LogValue.Text(hostile).toString()) shouldBe true
            isSafeToShow(LogValue.Entries(mapOf(hostile to LogValue.Null)).toString()) shouldBe true
        }
    }

    context("collections") {
        test("are copies that a change of the source does not reach") {
            val source = mutableListOf<LogValue>(LogValue.Null)
            val items = LogValue.Items(source)
            source += LogValue.Secret
            items.values shouldBe listOf(LogValue.Null)
        }
    }
})
