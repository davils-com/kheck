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

import com.davils.kheck.api.Logger
import com.davils.kheck.core.engine.LoggingEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Starts a logging on [scope] and [dispatcher], such as:
 *
 * ```kotlin
 * val logging = logging(scope, Dispatchers.Default) {
 *     level = LogLevel.Info
 *     console { format = json { layout = JsonLayout.Ecs } }
 * }
 * ```
 *
 * kheck starts no thread of its own: the coroutines of the pipeline and of every sink run on [dispatcher] as
 * children of [scope]. Cancelling [scope] stops them without flushing; call [Logging.close] first.
 *
 * @param scope the scope the coroutines of the logging belong to.
 * @param dispatcher the dispatcher they run on; one that may block briefly, because sinks write synchronously.
 * @param block configures the logging.
 * @return the running logging.
 * @throws com.davils.kore.core.validation.report.ValidationException if the configuration is invalid.
 * @since 1.0.0
 */
public fun logging(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    block: LoggingBuilder.() -> Unit = {},
): Logging {
    val builder = LoggingBuilder()
    builder.block()
    return Logging(LoggingEngine(scope, dispatcher, builder.build()))
}

/**
 * Returns the logger named after [T].
 *
 * @param T the class that logs.
 * @return the logger.
 * @since 1.0.0
 */
public inline fun <reified T : Any> Logging.logger(): Logger = logger(T::class)
