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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val SERVICE: String = "com.example.Service"

class LoggerNameTest : FunSpec({
    context("the hierarchy") {
        test("knows its parents up to the root") {
            val chain = generateSequence(LoggerName(SERVICE)) { current -> current.parent }
            chain.map(LoggerName::value).toList() shouldBe listOf(SERVICE, "com.example", "com", "")
            LoggerName.ROOT.parent shouldBe null
            LoggerName.ROOT.isRoot shouldBe true
        }

        test("lies within itself, its ancestors and the root but not within a sibling prefix") {
            val name = LoggerName(SERVICE)
            name.isWithin(LoggerName.ROOT) shouldBe true
            name.isWithin(name) shouldBe true
            name.isWithin(LoggerName("com.example")) shouldBe true
            name.isWithin(LoggerName("com.exam")) shouldBe false
        }

        test("names a child below itself and directly below the root") {
            LoggerName("kheck.redis").child("pool") shouldBe LoggerName("kheck.redis.pool")
            LoggerName.ROOT.child("kheck") shouldBe LoggerName("kheck")
        }
    }

    context("names of types") {
        test("are the qualified name and, for a companion, the name of its owner") {
            val owner = "com.davils.kheck.api.LoggerName"
            LoggerName.of(LoggerName::class).value shouldBe owner
            LoggerName.of(LoggerName.Companion::class).value shouldBe owner
        }

        test("fall back to the simple name of a local type") {
            class Local
            val name = LoggerName.of(Local::class)
            name.value.endsWith("Local") shouldBe true
        }

        test("fall back to the root for a type without any name") {
            val anonymous = object {}
            val name = LoggerName.of(anonymous::class)
            name.value.isEmpty() || name.value.contains("LoggerNameTest") shouldBe true
        }
    }

    context("limits") {
        test("refuse a name longer than the limit") {
            shouldThrow<IllegalArgumentException> { LoggerName("a".repeat(Limits.MAX_NAME_LENGTH + 1)) }
            LoggerName("a".repeat(Limits.MAX_NAME_LENGTH)).value.length shouldBe Limits.MAX_NAME_LENGTH
        }

        @OptIn(DavilsInternal::class)
        test("shorten a bounded name instead of refusing it") {
            val long = "b".repeat(Limits.MAX_NAME_LENGTH + 7)

            LoggerName.bounded(long).value shouldBe "b".repeat(Limits.MAX_NAME_LENGTH)
            LoggerName.bounded(SERVICE).value shouldBe SERVICE
        }
    }

    context("showing a name") {
        test("escapes what could forge a line") {
            val shown = LoggerName("a${HostileFragments.CLEAR_SCREEN}\nb").toString()
            isSafeToShow(shown) shouldBe true
            shown shouldContain "a"
        }
    }
})
