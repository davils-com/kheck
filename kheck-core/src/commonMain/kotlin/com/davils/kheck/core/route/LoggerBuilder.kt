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

package com.davils.kheck.core.route

import com.davils.kheck.api.LogLevel
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures one logger and, through the hierarchy, the loggers below it, inside `logger("name") { }`.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class LoggerBuilder internal constructor() : ValidatingBuilder<LoggerConfiguration>() {
    /**
     * Names the logger in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "logger"

    /**
     * The threshold of the logger, or `null` to inherit it from the closest configured ancestor.
     *
     * @since 1.0.0
     */
    public var level: LogLevel? = null

    /**
     * Whether the records also go to the sinks of the ancestors; `true` by default.
     *
     * @since 1.0.0
     */
    public var isAdditive: Boolean = true

    /**
     * Whether the frame that logged is located, which costs a walk of the stack per record; `null` to inherit it,
     * and `false` when no ancestor sets it. Only the JVM locates frames.
     *
     * @since 1.0.0
     */
    public var isCallerIncluded: Boolean? = null

    internal val sinks = LinkedHashSet<String>()

    /**
     * Routes the records of the logger to the sinks named [names], in addition to those of its ancestors unless
     * [isAdditive] is `false`.
     *
     * @param names the names of declared sinks.
     * @since 1.0.0
     */
    public fun sinks(vararg names: String) {
        checkOpen()
        sinks += names
    }

    /**
     * Creates the configuration.
     *
     * @return the configuration.
     * @since 1.0.0
     */
    override fun assemble(): LoggerConfiguration = LoggerConfiguration(this)
}
