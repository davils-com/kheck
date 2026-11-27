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

import com.davils.kheck.core.style.TextStyle

/**
 * The styles of a stack trace in a plain layout.
 *
 * @property type the style of the type of a throwable.
 * @property message the style of its message.
 * @property label the style of `Caused by:` and `Suppressed:`.
 * @property frame the style of a frame and of the notes of omitted frames.
 * @property location the style of the file and line of a frame.
 * @since 1.0.0
 */
public class CauseStyles(
    public val type: TextStyle = TextStyle.NONE,
    public val message: TextStyle = TextStyle.NONE,
    public val label: TextStyle = TextStyle.NONE,
    public val frame: TextStyle = TextStyle.NONE,
    public val location: TextStyle = TextStyle.NONE,
) {
    private val values: List<Any?> get() = listOf(type, message, label, frame, location)

    /**
     * Compares by value: two [CauseStyles] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [CauseStyles].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is CauseStyles && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the five styles.
     *
     * @return the styles as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "CauseStyles(type=$type, message=$message, label=$label, frame=$frame, location=$location)"
}
