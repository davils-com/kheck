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

package com.davils.kheck.core.format.plain

/**
 * Text written before and after a segment that may be empty, and only when it is not.
 *
 * @property prefix the text before the segment.
 * @property suffix the text after the segment.
 * @since 1.0.0
 */
public class SegmentAffix(public val prefix: String = "", public val suffix: String = "") {
    /**
     * Returns whether [other] has the same prefix and suffix.
     *
     * @param other the object to compare with.
     * @return `true` if both affixes are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is SegmentAffix && other.prefix == prefix && other.suffix == suffix

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * prefix.hashCode() + suffix.hashCode()

    /**
     * Shows the prefix and the suffix in quotes.
     *
     * @return the affix as text.
     * @since 1.0.0
     */
    override fun toString(): String = """SegmentAffix(prefix="$prefix", suffix="$suffix")"""

    /**
     * The affix that adds nothing.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val HASH_FACTOR: Int = 31

        /**
         * No prefix and no suffix.
         *
         * @since 1.0.0
         */
        public val NONE: SegmentAffix = SegmentAffix()
    }
}
