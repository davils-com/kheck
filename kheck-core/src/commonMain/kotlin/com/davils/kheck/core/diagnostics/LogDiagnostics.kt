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

package com.davils.kheck.core.diagnostics

/**
 * Where the logging reports its own trouble, such as a sink that failed or a filter that threw, because it cannot log
 * about itself through the sinks that fail.
 *
 * A report is plain text that may contain foreign text, such as the message of an exception; the implementations of
 * kheck escape it. An implementation is called from the coroutines of the logging, must be safe to call from any
 * thread and must not throw.
 *
 * @since 1.0.0
 */
public fun interface LogDiagnostics {
    /**
     * Reports a problem of the logging.
     *
     * @param message what happened.
     * @since 1.0.0
     */
    public fun report(message: String)

    /**
     * The diagnostics kheck offers.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Writes each report as one escaped line to the standard error, prefixed with `[kheck] `; on WebAssembly to
         * `console.error`.
         *
         * @since 1.0.0
         */
        public val StandardError: LogDiagnostics = StandardErrorDiagnostics

        /**
         * Drops every report.
         *
         * @since 1.0.0
         */
        public val Silent: LogDiagnostics = LogDiagnostics { }
    }
}
