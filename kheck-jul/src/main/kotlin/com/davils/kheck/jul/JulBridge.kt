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

package com.davils.kheck.jul

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.core.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

/**
 * Routes `java.util.logging` into a kheck logging while it is open, created by [bridgeJul].
 *
 * It replaces the handlers of the root logger of JUL with one that hands every record to kheck, with its formatted
 * message, its cause and its source class and method as the fields `jul.class` and `jul.method`. It mirrors the
 * levels of kheck, configured and overridden, into the loggers of JUL, so JUL does not even build a record that kheck
 * would drop. [close] restores the handlers and levels JUL had before.
 *
 * JUL is configured once per process, so only one bridge should be open at a time.
 *
 * @since 1.0.0
 */
public class JulBridge internal constructor(
    private val logging: Logging,
    scope: CoroutineScope,
) : AutoCloseable {
    private val root: Logger = LogManager.getLogManager().getLogger("")
    private val previousHandlers: List<Handler> = root.handlers.toList()
    private val previousLevel: Level? = root.level
    private val handler = JulHandler(logging)
    private val mirrored = mutableMapOf<String, Logger>()
    private val job: Job

    init {
        previousHandlers.forEach(root::removeHandler)
        root.addHandler(handler)
        job = scope.launch { logging.levels.overrides.collect { overrides -> mirrored(overrides) } }
    }

    /**
     * Restores the handlers and levels of JUL and stops mirroring levels.
     *
     * @since 1.0.0
     */
    override fun close() {
        job.cancel()
        root.removeHandler(handler)
        previousHandlers.forEach(root::addHandler)
        root.level = previousLevel
        mirrored.values.forEach { logger -> logger.level = null }
        mirrored.clear()
    }

    private fun mirrored(overrides: Map<LoggerName, LogLevel>) {
        val configured = logging.configuration.loggers.mapNotNull { (name, logger) ->
            logger.level?.let { level -> LoggerName(name) to level }
        }
        val levels = configured.toMap() + overrides
        root.level = (levels[LoggerName.ROOT] ?: logging.configuration.level).toJulLevel()
        levels.filterKeys { name -> !name.isRoot }.forEach { (name, level) -> mirror(name.value, level) }
    }

    private fun mirror(name: String, level: LogLevel) {
        val logger = mirrored.getOrPut(name) { Logger.getLogger(name) }
        logger.level = level.toJulLevel()
    }
}
