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

package com.davils.kheck.core.record

import com.davils.kheck.api.LoggerName
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * Where a record comes from: its logger, its thread and, when a logger asks for it, the frame that logged it.
 *
 * @property logger the name of the logger.
 * @property thread the name of the thread that logged, as the platform names it.
 * @property caller the frame that logged, or `null` when it was not located.
 * @since 1.0.0
 */
public class LogSource(
    public val logger: LoggerName,
    public val thread: String,
    public val caller: LogFrame? = null,
) {
    /**
     * Returns whether [other] is a source with the same logger, thread and caller.
     *
     * @param other the object to compare with.
     * @return `true` if both sources are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is LogSource && logger == other.logger && thread == other.thread && caller == other.caller

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int {
        var result = logger.hashCode()
        result = HASH_FACTOR * result + thread.hashCode()
        return HASH_FACTOR * result + (caller?.hashCode() ?: 0)
    }

    /**
     * Shows the source with its names escaped.
     *
     * @return the source as text.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String {
        val shownThread = SafeText.text(thread)
        return "LogSource(logger=$logger, thread=$shownThread, caller=${caller ?: NONE})"
    }

    private companion object {
        private const val HASH_FACTOR: Int = 31
        private const val NONE: String = "none"
    }
}
