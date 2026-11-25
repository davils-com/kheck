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

package com.davils.kheck.testing

import com.davils.kheck.core.terminal.ConsoleTarget
import com.davils.kheck.core.terminal.Terminal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A console that keeps what is written to it.
 *
 * @param interactive the targets that count as interactive terminals.
 * @since 1.0.0
 */
public class FakeTerminal(private val interactive: Set<ConsoleTarget> = emptySet()) : Terminal {
    private val written = MutableStateFlow<Map<ConsoleTarget, String>>(emptyMap())

    /**
     * Returns whether [target] was given as interactive.
     *
     * @param target the stream.
     * @return `true` if [target] is interactive.
     * @since 1.0.0
     */
    override fun isInteractive(target: ConsoleTarget): Boolean = target in interactive

    /**
     * Keeps [text].
     *
     * @param target the stream.
     * @param text the text.
     * @since 1.0.0
     */
    override fun write(target: ConsoleTarget, text: String) {
        written.update { current -> current + (target to current[target].orEmpty() + text) }
    }

    /**
     * Returns everything written to [target].
     *
     * @param target the stream; the standard output by default.
     * @return the text.
     * @since 1.0.0
     */
    public fun text(target: ConsoleTarget = ConsoleTarget.StandardOutput): String = written.value[target].orEmpty()

    /**
     * Returns the lines written to [target].
     *
     * @param target the stream; the standard output by default.
     * @return the lines, without the last line break.
     * @since 1.0.0
     */
    public fun lines(target: ConsoleTarget = ConsoleTarget.StandardOutput): List<String> {
        val text = text(target)
        if (text.isEmpty()) return emptyList()
        return text.removeSuffix("\n").split('\n')
    }
}
