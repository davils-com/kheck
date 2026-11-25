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

package com.davils.kheck.core.style

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.internal.orderedCopy
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The style of a segment of a plain layout: a base style and, optionally, a style for each level that replaces it.
 *
 * @property base the style for every level without a style of its own.
 * @param levels the styles of single levels, copied.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class SegmentStyle(
    public val base: TextStyle = TextStyle.NONE,
    levels: Map<LogLevel, TextStyle> = emptyMap(),
) {
    /**
     * The styles of single levels; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val levels: Map<LogLevel, TextStyle> = levels.orderedCopy()

    /**
     * Returns the style for a record of [level].
     *
     * @param level the level of the record.
     * @return the style of [level], or [base].
     * @since 1.0.0
     */
    public fun styleFor(level: LogLevel): TextStyle = levels[level] ?: base

    /**
     * Returns whether [other] has the same base and level styles.
     *
     * @param other the object to compare with.
     * @return `true` if both styles are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is SegmentStyle && other.base == base && other.levels == levels

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * base.hashCode() + levels.hashCode()

    /**
     * Shows the base style and the levels with a style of their own.
     *
     * @return the style as text.
     * @since 1.0.0
     */
    override fun toString(): String = "SegmentStyle(base=$base, levels=${levels.keys})"

    /**
     * The style that draws nothing special.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val HASH_FACTOR: Int = 31

        /**
         * No style for any level.
         *
         * @since 1.0.0
         */
        public val NONE: SegmentStyle = SegmentStyle()
    }
}
