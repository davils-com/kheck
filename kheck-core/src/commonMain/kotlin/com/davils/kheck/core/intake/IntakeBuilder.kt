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

package com.davils.kheck.core.intake

import com.davils.kheck.core.sink.LogOverflow
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures the intake inside `intake { }`: the queue between the loggers and the coroutine of the logging.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class IntakeBuilder internal constructor() : ValidatingBuilder<IntakeConfiguration>() {
    /**
     * Names the intake in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "intake"

    /**
     * The most records waiting in the intake; 16 384 by default.
     *
     * @since 1.0.0
     */
    public var capacity: Int = DEFAULT_CAPACITY

    /**
     * What happens to a record when the intake is full; [LogOverflow.DropOldest] by default.
     *
     * @since 1.0.0
     */
    public var overflow: LogOverflow = LogOverflow.DropOldest

    /**
     * Creates the configuration.
     *
     * @return the configuration, validated afterwards.
     * @since 1.0.0
     */
    override fun assemble(): IntakeConfiguration = IntakeConfiguration(capacity, overflow)

    /**
     * The defaults of the intake.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default of [capacity].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_CAPACITY: Int = 16_384
    }
}
