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

/**
 * Returns a read-only copy of this map that keeps the order of its entries and cannot be changed through a cast.
 *
 * @param K the type of the keys.
 * @param V the type of the values.
 * @return the copy.
 * @since 1.0.0
 */
@DavilsInternal
public fun <K : Any, V : Any> Map<K, V>.orderedCopy(): Map<K, V> {
    if (isEmpty()) return emptyMap()
    return FrozenMap(this)
}

/**
 * Returns a read-only copy of this set that keeps the order of its elements and cannot be changed through a cast.
 *
 * @param E the type of the elements.
 * @return the copy.
 * @since 1.0.0
 */
@DavilsInternal
public fun <E : Any> Set<E>.orderedCopy(): Set<E> {
    if (isEmpty()) return emptySet()
    return FrozenSet(this)
}
