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

/**
 * The meters a logging declared in a kollect registry through [Logging.bindTo]; closing removes them again.
 *
 * Counters and gauges read the statistics of the logging when the registry is read; the timer of the writes of each
 * sink records every write as it happens.
 *
 * @since 1.0.0
 */
public class LoggingMeters internal constructor(private val unbind: () -> Unit) : AutoCloseable {
    /**
     * Stops the timer and removes the meters of the logging from the registry; the registry stays open.
     *
     * @since 1.0.0
     */
    override fun close() {
        unbind()
    }

    /**
     * Names the meters.
     *
     * @return the meters as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LoggingMeters"
}
