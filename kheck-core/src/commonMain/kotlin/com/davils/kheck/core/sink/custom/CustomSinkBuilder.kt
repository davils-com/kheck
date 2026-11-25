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

import com.davils.kheck.core.sink.LogSink
import com.davils.kheck.core.sink.LogSinkBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Configures a sink of the application inside `sink("name", sink) { }`.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class CustomSinkBuilder internal constructor(private val sink: LogSink) :
    LogSinkBuilder<CustomSinkConfiguration>() {
    /**
     * Names the sink in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "sink"

    /**
     * Creates the configuration.
     *
     * @return the configuration, validated afterwards.
     * @since 1.0.0
     */
    override fun assemble(): CustomSinkConfiguration = CustomSinkConfiguration(options(), sink)
}
