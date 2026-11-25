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
import com.davils.kheck.core.sink.LogSinkConfiguration
import com.davils.kheck.core.sink.LogSinkOptions
import com.davils.kore.core.validation.ValidationScope

/**
 * The configuration of a sink of the application.
 *
 * @property options the options every sink shares.
 * @property sink the sink.
 * @since 1.0.0
 */
public class CustomSinkConfiguration internal constructor(
    override val options: LogSinkOptions,
    public val sink: LogSink,
) : LogSinkConfiguration {
    /**
     * Checks the options.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        with(options) { validate() }
    }

    /**
     * Shows the sink and the options.
     *
     * @return the configuration as text.
     * @since 1.0.0
     */
    override fun toString(): String = "CustomSinkConfiguration(sink=$sink, options=$options)"
}
