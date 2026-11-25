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

package com.davils.kheck.core.engine.route

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.core.LoggingConfiguration
import com.davils.kheck.core.route.LoggerConfiguration

internal class LevelResolver(
    private val configuration: LoggingConfiguration,
    private val overrides: Map<LoggerName, LogLevel>,
) {
    private val configured: Map<LoggerName, LoggerConfiguration> =
        configuration.loggers.mapKeys { (name, _) -> LoggerName(name) }

    fun threshold(name: LoggerName): LogLevel = chain(
        name,
    ).firstNotNullOfOrNull { current -> overrides[current] ?: configured[current]?.level } ?: configuration.level

    fun isCallerIncluded(name: LoggerName): Boolean =
        chain(name).firstNotNullOfOrNull { current -> configured[current]?.isCallerIncluded } == true

    private fun chain(name: LoggerName): Sequence<LoggerName> = generateSequence(
        name,
    ) { current -> current.parent }
}
