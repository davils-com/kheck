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
import com.davils.kheck.api.internal.orderedCopy
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The configuration of one logger: its threshold, its sinks and whether it locates callers.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LoggerConfiguration internal constructor(builder: LoggerBuilder) : Validatable {
    /**
     * The threshold, or `null` to inherit it.
     *
     * @since 1.0.0
     */
    public val level: LogLevel? = builder.level

    /**
     * Whether the records also go to the sinks of the ancestors.
     *
     * @since 1.0.0
     */
    public val isAdditive: Boolean = builder.isAdditive

    /**
     * Whether the frame that logged is located, or `null` to inherit it.
     *
     * @since 1.0.0
     */
    public val isCallerIncluded: Boolean? = builder.isCallerIncluded

    /**
     * The names of the sinks of the logger; the set cannot be changed.
     *
     * @since 1.0.0
     */
    public val sinks: Set<String> = builder.sinks.orderedCopy()

    /**
     * Has nothing to check; the names of the sinks are checked by the logging.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate(): Unit = Unit

    /**
     * Shows the configuration.
     *
     * @return the configuration as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LoggerConfiguration(level=${level ?: INHERITED}, isAdditive=$isAdditive, " +
        "isCallerIncluded=${isCallerIncluded ?: INHERITED}, sinks=$sinks)"

    private companion object {
        private const val INHERITED: String = "inherited"
    }
}
