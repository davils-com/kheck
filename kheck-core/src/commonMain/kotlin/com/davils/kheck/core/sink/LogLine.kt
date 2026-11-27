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

package com.davils.kheck.core.sink

import com.davils.kheck.core.integrity.LogSeal
import com.davils.kheck.core.record.LogRecord

/**
 * A record and its text as the format of a sink encoded it.
 *
 * @property record the record.
 * @property text the encoded text, escaped and ending with a line break.
 * @property seal the seal of the line in the integrity chain of the sink, or `null` when the sink keeps none.
 * @since 1.0.0
 */
public class LogLine(public val record: LogRecord, public val text: String, public val seal: LogSeal? = null) {
    private val values: List<Any?> get() = listOf(record, text, seal)

    /**
     * Compares by value: two [LogLine] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [LogLine].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is LogLine && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the record, not the text.
     *
     * @return the line as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LogLine(record=$record, length=${text.length}, seal=${seal?.sequence ?: NONE})"

    private companion object {
        private const val NONE: String = "none"
    }
}
