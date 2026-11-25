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
 * The width of a segment: padded up to [minWidth] and cut down to [maxWidth].
 *
 * @property minWidth the least width; shorter text is padded with spaces; 0 for none.
 * @property maxWidth the greatest width; longer text is cut; 0 for no limit.
 * @property alignment where the text sits and which end is kept.
 * @since 1.0.0
 */
public class SegmentFrame(
    public val minWidth: Int = 0,
    public val maxWidth: Int = 0,
    public val alignment: Alignment = Alignment.Start,
) {
    /**
     * Returns whether [other] has the same widths and alignment.
     *
     * @param other the object to compare with.
     * @return `true` if both frames are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is SegmentFrame &&
        other.minWidth == minWidth &&
        other.maxWidth == maxWidth &&
        other.alignment == alignment

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = (HASH_FACTOR * minWidth + maxWidth) * HASH_FACTOR + alignment.hashCode()

    /**
     * Shows the widths and the alignment.
     *
     * @return the frame as text.
     * @since 1.0.0
     */
    override fun toString(): String = "SegmentFrame(minWidth=$minWidth, maxWidth=$maxWidth, alignment=$alignment)"

    /**
     * The frame that changes nothing.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val HASH_FACTOR: Int = 31

        /**
         * No padding and no limit.
         *
         * @since 1.0.0
         */
        public val NONE: SegmentFrame = SegmentFrame()
    }
}
