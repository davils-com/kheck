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
import com.davils.kheck.api.Logger
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogEvent

/**
 * A logger that keeps what it is given, for testing the logging of a library without a logging engine.
 *
 * Its children record into the same journal with their own names and the threshold of their parent. Safe to use from
 * any thread.
 *
 * @since 1.0.0
 */
public class RecordingLogger private constructor(
    override val name: LoggerName,
    private val threshold: LogLevel,
    private val journal: LogJournal,
) : Logger {
    /**
     * Every recorded event, in the order it was emitted.
     *
     * @since 1.0.0
     */
    public val entries: List<LoggedEvent> get() = journal.entries

    /**
     * The recorded events.
     *
     * @since 1.0.0
     */
    public val events: List<LogEvent> get() = entries.map { entry -> entry.event }

    /**
     * The levels of the recorded events.
     *
     * @since 1.0.0
     */
    public val levels: List<LogLevel> get() = entries.map { entry -> entry.level }

    /**
     * The messages of the recorded events.
     *
     * @since 1.0.0
     */
    public val messages: List<String> get() = entries.map { entry -> entry.event.message }

    /**
     * Creates a logger that records every event at or above [threshold].
     *
     * @param name the name of the logger; the root by default.
     * @param threshold the lowest level recorded; [LogLevel.Trace] by default.
     * @since 1.0.0
     */
    public constructor(
        name: LoggerName = LoggerName.ROOT,
        threshold: LogLevel = LogLevel.Trace,
    ) : this(name, threshold, LogJournal())

    /**
     * Returns whether [level] is at or above the threshold.
     *
     * @param level the level.
     * @return `true` if an event of [level] is recorded.
     * @since 1.0.0
     */
    override fun isEnabled(level: LogLevel): Boolean = level.isAtLeast(threshold)

    /**
     * Records [event] if [level] is enabled.
     *
     * @param level the level.
     * @param event the event.
     * @since 1.0.0
     */
    override fun emit(level: LogLevel, event: LogEvent) {
        if (!isEnabled(level)) return
        journal.record(LoggedEvent(level = level, event = event, logger = name))
    }

    /**
     * Returns a child that records into the same journal.
     *
     * @param name the name of the child.
     * @return the child.
     * @since 1.0.0
     */
    override fun child(name: String): Logger = RecordingLogger(this.name.child(name), threshold, journal)

    /**
     * Forgets every recorded event, of this logger and of its children.
     *
     * @since 1.0.0
     */
    public fun clear() {
        journal.clear()
    }

    /**
     * Shows the name of the logger.
     *
     * @return the logger as text.
     * @since 1.0.0
     */
    override fun toString(): String = "RecordingLogger($name)"
}
