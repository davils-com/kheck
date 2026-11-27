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

package com.davils.kheck.core.sink.console

import com.davils.kheck.core.sink.LogSinkBuilder
import com.davils.kheck.core.terminal.ConsoleTarget
import com.davils.kheck.core.terminal.Terminal
import com.davils.knot.environment.Environment
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Configures a console sink inside `console { }`.
 *
 * @param environment the environment the colors are detected from, the one of the logging by default.
 * @since 1.0.0
 */
@DavilsDsl
public class ConsoleSinkBuilder internal constructor(environment: Environment) :
    LogSinkBuilder<ConsoleSinkConfiguration>() {
    /**
     * Names the sink in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "console sink"

    /**
     * The stream the sink writes to; [ConsoleTarget.StandardOutput] by default.
     *
     * @since 1.0.0
     */
    public var target: ConsoleTarget = ConsoleTarget.StandardOutput

    /**
     * The console; [Terminal.SYSTEM] by default.
     *
     * @since 1.0.0
     */
    public var terminal: Terminal = Terminal.SYSTEM

    /**
     * The environment the colors are detected from, an environment of knot; the one of the logging by default.
     *
     * @since 1.0.0
     */
    public var environment: Environment = environment

    /**
     * Creates the configuration.
     *
     * @return the configuration, validated afterwards.
     * @since 1.0.0
     */
    override fun assemble(): ConsoleSinkConfiguration = ConsoleSinkConfiguration(
        options = options(),
        target = target,
        terminal = terminal,
        environment = environment,
    )
}
