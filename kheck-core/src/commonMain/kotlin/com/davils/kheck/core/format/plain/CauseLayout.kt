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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * How a plain layout writes the stack trace of a record, below its line.
 *
 * @property maxFrames the most frames written per throwable; the rest is counted in a note.
 * @property maxCauses the longest chain of causes written; the rest is replaced by a note.
 * @property isSuppressedIncluded whether suppressed throwables are written.
 * @param excludedPackages the prefixes of class names whose frames are left out and counted, copied.
 * @property indent the text before a frame.
 * @property styles the styles of the stack trace.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class CauseLayout(
    public val maxFrames: Int,
    public val maxCauses: Int,
    public val isSuppressedIncluded: Boolean,
    excludedPackages: List<String>,
    public val indent: String,
    public val styles: CauseStyles,
) {
    /**
     * The prefixes of class names whose frames are left out; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val excludedPackages: List<String> = excludedPackages.immutableList()

    private val values: List<Any?>
        get() = listOf(maxFrames, maxCauses, isSuppressedIncluded, indent, styles, excludedPackages)

    /**
     * Compares by value: two [CauseLayout] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [CauseLayout].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is CauseLayout && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the limits and the excluded packages.
     *
     * @return the layout as text.
     * @since 1.0.0
     */
    override fun toString(): String = "CauseLayout(maxFrames=$maxFrames, maxCauses=$maxCauses, " +
        "isSuppressedIncluded=$isSuppressedIncluded, excludedPackages=$excludedPackages)"
}
