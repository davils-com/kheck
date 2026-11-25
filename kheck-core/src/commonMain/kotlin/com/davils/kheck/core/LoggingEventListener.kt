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
 * Receives the [events][LoggingEvent] of a logging, such as to raise an alert when a sink fails.
 *
 * A listener runs on the coroutine of the logging and must return quickly; one that throws is counted in the
 * statistics and reported to the diagnostics, and the logging goes on.
 *
 * @since 1.0.0
 */
public fun interface LoggingEventListener {
    /**
     * Receives one event.
     *
     * @param event the event.
     * @since 1.0.0
     */
    public fun onEvent(event: LoggingEvent)
}
