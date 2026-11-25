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

package com.davils.kheck.api

import com.davils.kheck.api.record.LogEvent

/**
 * Where a library hands its records; the only type a library that logs needs to know.
 *
 * A library logs through the extensions [trace], [debug], [info], [warn] and [error], which build the record only when
 * [isEnabled] says that its level passes, so a disabled call costs one check. An application creates its loggers
 * from its logging engine, such as `com.davils.kheck.core.Logging`; a library takes a [Logger] as a parameter and
 * falls back to [None].
 *
 * An implementation is safe to call from any thread and from any coroutine, never blocks for long and never throws:
 * a failure of the logging must not become a failure of the code that logs.
 *
 * @since 1.0.0
 */
public interface Logger {
    /**
     * The name of this logger in the hierarchy.
     *
     * @since 1.0.0
     */
    public val name: LoggerName

    /**
     * Returns whether a record of [level] would be written.
     *
     * @param level the level of the record.
     * @return `true` if the record passes the threshold of this logger.
     * @since 1.0.0
     */
    public fun isEnabled(level: LogLevel): Boolean

    /**
     * Hands a record of [level] to the logging.
     *
     * A record of a disabled level is dropped. The call returns before the record is written.
     *
     * @param level the level of the record; [LogLevel.Off] is dropped.
     * @param event the content of the record.
     * @since 1.0.0
     */
    public fun emit(level: LogLevel, event: LogEvent)

    /**
     * Returns the logger of a child of this logger, such as `billing` below `com.example`.
     *
     * @param name the name of the child, appended after a dot.
     * @return the logger of the child.
     * @throws IllegalArgumentException if the name of the child is longer than [Limits.MAX_NAME_LENGTH].
     * @since 1.0.0
     */
    public fun child(name: String): Logger

    /**
     * The logger that writes nothing, for a library whose caller passes no logger.
     *
     * @since 1.0.0
     */
    public companion object None : Logger {
        /**
         * The [LoggerName.ROOT].
         *
         * @since 1.0.0
         */
        override val name: LoggerName get() = LoggerName.ROOT

        /**
         * Returns `false`, for every level.
         *
         * @param level the level of the record.
         * @return `false`.
         * @since 1.0.0
         */
        override fun isEnabled(level: LogLevel): Boolean = false

        /**
         * Drops the record.
         *
         * @param level the level of the record.
         * @param event the content of the record.
         * @since 1.0.0
         */
        override fun emit(level: LogLevel, event: LogEvent): Unit = Unit

        /**
         * Returns this logger, which writes nothing.
         *
         * @param name the name of the child.
         * @return this logger.
         * @since 1.0.0
         */
        override fun child(name: String): Logger = this
    }
}
