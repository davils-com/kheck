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

internal class FrozenMap<K : Any, V : Any>(source: Map<K, V>) : AbstractMap<K, V>() {
    private val backing: LinkedHashMap<K, V> = LinkedHashMap(source)

    override val entries: Set<Map.Entry<K, V>> =
        FrozenSet(backing.entries.map { entry -> FrozenEntry(entry.key, entry.value) })

    override fun get(key: K): V? = backing[key]

    override fun containsKey(key: K): Boolean = backing.containsKey(key)
}
