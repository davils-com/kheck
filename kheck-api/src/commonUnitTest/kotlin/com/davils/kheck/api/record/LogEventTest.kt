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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class LogEventTest : FunSpec({
    context("equality") {
        test("compares message, fields, cause, markers and omitted parts") {
            val cause = IllegalStateException("x")
            val event = LogEvent(
                message = "m",
                fields = mapOf("a" to LogValue.Null),
                cause = cause,
                markers = setOf(LogMarker.AUDIT),
                omitted = 1,
            )
            val same = LogEvent(
                message = "m",
                fields = mapOf("a" to LogValue.Null),
                cause = cause,
                markers = setOf(LogMarker.AUDIT),
                omitted = 1,
            )
            event shouldBe same
            event shouldBe event
            event.hashCode() shouldBe same.hashCode()
            event shouldNotBe LogEvent("m")
            event shouldNotBe LogEvent(
                message = "m",
                fields = mapOf("a" to LogValue.Null),
                cause = cause,
                markers = setOf(LogMarker.AUDIT),
                omitted = 2,
            )
            event shouldNotBe LogEvent(
                message = "m",
                fields = mapOf("a" to LogValue.Null),
                cause = cause,
                markers = emptySet(),
                omitted = 1,
            )
            event.equals("m") shouldBe false
            LogEvent("m") shouldNotBe LogEvent("n")
            LogEvent("m") shouldNotBe LogEvent("m", fields = mapOf("a" to LogValue.Null))
            LogEvent("m") shouldNotBe LogEvent("m", cause = cause)
            LogEvent("m").hashCode() shouldBe LogEvent("m").hashCode()
        }

        test("refuses a negative count of omitted parts") {
            shouldThrow<IllegalArgumentException> { LogEvent("m", omitted = -1) }
        }
    }

    context("showing an event") {
        test("escapes its message and shows the type of its cause only") {
            val secret = "password=hunter2"
            val event = LogEvent("${HostileFragments.CLEAR_SCREEN}\nforged", cause = IllegalStateException(secret))
            val shown = event.toString()
            isSafeToShow(shown) shouldBe true
            shown shouldNotContain secret
            shown shouldContain "IllegalStateException"
            LogEvent("m").toString() shouldContain "cause=none"
            LogEvent("m", cause = object : IllegalStateException() {}).toString() shouldContain "cause=anonymous"
        }
    }
})
