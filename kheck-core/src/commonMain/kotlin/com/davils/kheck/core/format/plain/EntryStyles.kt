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

import com.davils.kheck.core.style.SegmentStyle

/**
 * The styles of the fields or the context of a record in a plain layout.
 *
 * @property key the style of a name.
 * @property value the style of a value.
 * @property punctuation the style of the `=` between them.
 * @since 1.0.0
 */
public class EntryStyles(
    public val key: SegmentStyle = SegmentStyle.NONE,
    public val value: SegmentStyle = SegmentStyle.NONE,
    public val punctuation: SegmentStyle = SegmentStyle.NONE,
) {
    private val values: List<Any?> get() = listOf(key, value, punctuation)

    /**
     * Compares by value: two [EntryStyles] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [EntryStyles].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is EntryStyles && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the three styles.
     *
     * @return the styles as text.
     * @since 1.0.0
     */
    override fun toString(): String = "EntryStyles(key=$key, value=$value, punctuation=$punctuation)"
}
