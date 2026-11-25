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

package com.davils.kheck.api.internal

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val FIRST: String = "z"
private const val SECOND: String = "a"

@OptIn(DavilsInternal::class)
class OrderedCopiesTest : FunSpec({
    context("maps") {
        test("keep the order of their entries and compare like maps") {
            val source = linkedMapOf(FIRST to 1, SECOND to 2)
            val copy = source.orderedCopy()
            source[FIRST] = 0

            copy.keys.toList() shouldBe listOf(FIRST, SECOND)
            copy[FIRST] shouldBe 1
            copy.containsKey(SECOND) shouldBe true
            copy shouldBe mapOf(SECOND to 2, FIRST to 1)
            copy.entries.first().toString() shouldBe "z=1"
            copy.entries.first() shouldBe copy.entries.first()
            copy.entries.first() shouldNotBe copy.entries.last()
            val plain = mapOf(FIRST to 1).entries.first()
            copy.entries.first().hashCode() shouldBe plain.hashCode()
            copy.entries.first().equals(FIRST) shouldBe false
            val entry = copy.entries.first()
            entry.equals(plain) shouldBe true
            entry.equals(mapOf(FIRST to 2).entries.first()) shouldBe false
            entry.equals(mapOf(SECOND to 1).entries.first()) shouldBe false
            (copy is MutableMap<*, *>) shouldBe false
            emptyMap<String, Int>().orderedCopy() shouldBe emptyMap()
        }
    }

    context("sets") {
        test("keep the order of their elements and end their iteration") {
            val copy = linkedSetOf(FIRST, SECOND).orderedCopy()
            val iterator = copy.iterator()

            copy.toList() shouldBe listOf(FIRST, SECOND)
            (SECOND in copy) shouldBe true
            copy.size shouldBe 2
            iterator.next()
            iterator.next()
            shouldThrow<NoSuchElementException> { iterator.next() }
            (iterator is MutableIterator<*>) shouldBe false
            emptySet<String>().orderedCopy() shouldBe emptySet()
        }
    }
})
