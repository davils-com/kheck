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

import com.davils.kheck.core.LoggingBuilder
import com.davils.kore.core.annotation.DavilsInternal

private const val DEFAULT_NAME: String = "console"

/**
 * Declares a sink that writes to the console, such as `console { target = ConsoleTarget.StandardError }`.
 *
 * Colors follow the [color mode][com.davils.kheck.core.format.LogFormat.colorMode] of the format and what the
 * terminal shows, as [ColorMode.Auto][com.davils.kheck.core.style.ColorMode.Auto] describes.
 *
 * @param name the name of the sink; `console` by default.
 * @param block configures the sink.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public fun LoggingBuilder.console(name: String = DEFAULT_NAME, block: ConsoleSinkBuilder.() -> Unit = {}) {
    register(name = name, builder = ConsoleSinkBuilder(environment), block = block, create = ::ConsoleSink)
}
