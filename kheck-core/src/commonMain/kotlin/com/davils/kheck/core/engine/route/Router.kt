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

import com.davils.kheck.api.LoggerName
import com.davils.kheck.core.LoggingConfiguration
import com.davils.kheck.core.route.LoggerConfiguration

internal class Router(configuration: LoggingConfiguration) {
    private val configured: Map<LoggerName, LoggerConfiguration> =
        configuration.loggers.mapKeys { (name, _) -> LoggerName(name) }

    private val defaults: List<String> =
        configuration.sinks.values
            .filterNot { definition -> definition.options.isExclusive }
            .map { definition -> definition.name }

    private val cache = HashMap<LoggerName, List<String>>()

    fun sinks(name: LoggerName): List<String> {
        val cached = cache[name]
        if (cached != null) return cached
        if (cache.size >= MAX_CACHED) cache.clear()
        val resolved = resolved(name)
        cache[name] = resolved
        return resolved
    }

    private fun resolved(name: LoggerName): List<String> {
        val result = LinkedHashSet<String>()
        val chain = generateSequence(name) { current -> current.parent }
        for (configuration in chain.mapNotNull { current -> configured[current] }) {
            result += configuration.sinks
            if (!configuration.isAdditive) return result.toList()
        }
        result += defaults
        return result.toList()
    }

    private companion object {
        private const val MAX_CACHED: Int = 8_192
    }
}
