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

private const val EXHAUSTED: String = "The set has no further element"

internal class FrozenSet<E : Any>(source: Iterable<E>) : AbstractSet<E>() {
    private val backing: LinkedHashSet<E> = LinkedHashSet<E>().apply { addAll(source) }

    override val size: Int get() = backing.size

    override fun contains(element: E): Boolean = element in backing

    override fun iterator(): Iterator<E> {
        val source = backing.iterator()
        return object : Iterator<E> {
            override fun hasNext(): Boolean = source.hasNext()

            override fun next(): E {
                if (!source.hasNext()) throw NoSuchElementException(EXHAUSTED)
                return source.next()
            }
        }
    }
}
