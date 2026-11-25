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

package com.davils.kheck.testing

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogEvent

/**
 * One event a [RecordingLogger] recorded.
 *
 * @property level the level of the event.
 * @property event the event.
 * @property logger the name of the logger that recorded it.
 * @since 1.0.0
 */
public class LoggedEvent(public val level: LogLevel, public val event: LogEvent, public val logger: LoggerName) {
    /**
     * Returns whether [other] has the same level, event and logger.
     *
     * @param other the object to compare with.
     * @return `true` if both are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is LoggedEvent && other.level == level && other.event == event && other.logger == logger

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = (level.hashCode() * HASH_FACTOR + event.hashCode()) * HASH_FACTOR + logger.hashCode()

    /**
     * Shows the level, the logger and the event.
     *
     * @return the event as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LoggedEvent(level=$level, logger=$logger, event=$event)"

    private companion object {
        private const val HASH_FACTOR: Int = 31
    }
}
