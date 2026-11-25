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

package com.davils.kheck.core.terminal

/**
 * The console a console sink writes to, replaceable for tests.
 *
 * An implementation is called from the coroutine of its sink, one call at a time.
 *
 * @since 1.0.0
 */
public interface Terminal {
    /**
     * Returns whether [target] is shown to a person, so that colors are worth writing.
     *
     * @param target the stream.
     * @return `true` if [target] is an interactive terminal.
     * @since 1.0.0
     */
    public fun isInteractive(target: ConsoleTarget): Boolean

    /**
     * Writes [text] to [target] and flushes it.
     *
     * @param target the stream.
     * @param text the text, already escaped and ending with a line break.
     * @since 1.0.0
     */
    public fun write(target: ConsoleTarget, text: String)

    /**
     * The console of the process.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The standard output and error of the process on the JVM, and `console.log` and `console.error` on
         * WebAssembly, which never count as interactive there.
         *
         * @since 1.0.0
         */
        public val SYSTEM: Terminal = SystemTerminal
    }
}
