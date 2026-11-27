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

package com.davils.kheck.core.integrity

import com.davils.krypt.core.algorithm.MacAlgorithm
import com.davils.krypt.primitive.key.SecretKey

/**
 * The integrity chain of a sink, as `integrity { }` set it.
 *
 * @property key the key of the chain, if set.
 * @property algorithm the algorithm of the chain.
 * @property anchor returns the seal the chain goes on from, if set.
 * @since 1.0.0
 */
public class IntegrityOptions internal constructor(
    public val key: SecretKey?,
    public val algorithm: MacAlgorithm,
    public val anchor: (suspend () -> LogSeal?)?,
) {
    private val values: List<Any?> get() = listOf(key, algorithm, anchor)

    /**
     * Compares by value: the keys and algorithms are equal and the anchors are the same function.
     *
     * @param other the object to compare with.
     * @return whether [other] is equal options.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is IntegrityOptions && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the algorithm, never the key.
     *
     * @return the options as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IntegrityOptions(algorithm=$algorithm, isKeyed=${key != null})"
}
