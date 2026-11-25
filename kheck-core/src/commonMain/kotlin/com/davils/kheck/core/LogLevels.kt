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

package com.davils.kheck.core

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Changes the thresholds of loggers while the logging runs, such as from an endpoint of administration, without a
 * new configuration.
 *
 * An override wins over the configuration for the logger it names and the loggers below it, until it is cleared.
 * Every logger handed out so far follows a change at once. Safe to use from any thread.
 *
 * @since 1.0.0
 */
public class LogLevels internal constructor(private val changed: () -> Unit) {
    private val mutableOverrides = MutableStateFlow<Map<LoggerName, LogLevel>>(emptyMap())

    /**
     * The overrides in effect, per logger.
     *
     * @since 1.0.0
     */
    public val overrides: StateFlow<Map<LoggerName, LogLevel>> = mutableOverrides.asStateFlow()

    /**
     * Sets the threshold of [logger] and the loggers below it.
     *
     * @param logger the dotted name of the logger; empty for the root.
     * @param level the threshold.
     * @throws IllegalArgumentException if [logger] is longer than the limit of a name.
     * @since 1.0.0
     */
    public fun set(logger: String, level: LogLevel) {
        val name = LoggerName(logger)
        mutableOverrides.update { overrides -> overrides + (name to level) }
        changed()
    }

    /**
     * Removes the override of [logger], so the configuration applies again.
     *
     * @param logger the dotted name of the logger.
     * @throws IllegalArgumentException if [logger] is longer than the limit of a name.
     * @since 1.0.0
     */
    public fun clear(logger: String) {
        val name = LoggerName(logger)
        mutableOverrides.update { overrides -> overrides - name }
        changed()
    }

    /**
     * Removes every override.
     *
     * @since 1.0.0
     */
    public fun reset() {
        mutableOverrides.value = emptyMap()
        changed()
    }
}
