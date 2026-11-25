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

package com.davils.kheck.core.sink.custom

import com.davils.kheck.core.LoggingBuilder
import com.davils.kheck.core.sink.LogSink
import com.davils.kore.core.annotation.DavilsInternal

/**
 * Declares a sink of the application, such as a shipper to a collector.
 *
 * The same instance serves every configuration the logging applies, so a reconfiguration closes it and opens it
 * again; an implementation that cannot be reopened belongs in a sink of its own kind instead.
 *
 * @param name the name of the sink.
 * @param sink the sink.
 * @param block configures the options of the sink.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public fun LoggingBuilder.sink(name: String, sink: LogSink, block: CustomSinkBuilder.() -> Unit = {}) {
    register(name = name, builder = CustomSinkBuilder(sink), block = block) { configuration -> configuration.sink }
}
