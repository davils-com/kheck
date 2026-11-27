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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kheck.core.engine

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.Logger
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogEvent
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class EngineLogger(override val name: LoggerName, private val engine: LoggingEngine) : Logger {
    private val state = AtomicLong(UNRESOLVED)

    val isCallerIncluded: Boolean get() = resolved() and CALLER_BIT != 0L

    override fun isEnabled(level: LogLevel): Boolean {
        if (level == LogLevel.Off) return false
        return level.ordinal >= (resolved() and LEVEL_MASK).toInt()
    }

    override fun emit(level: LogLevel, event: LogEvent) {
        if (!isEnabled(level)) return
        engine.submit(this, level, event)
    }

    override fun child(name: String): Logger = engine.logger(this.name.child(name))

    private fun resolved(): Long {
        val known = state.load()
        val generation = engine.generation
        if (known ushr GENERATION_SHIFT == generation) return known
        val resolver = engine.resolver()
        val caller = callerBit(resolver.isCallerIncluded(name))
        val packed = generation shl GENERATION_SHIFT or caller or resolver.threshold(name).ordinal.toLong()
        state.store(packed)
        return packed
    }

    private fun callerBit(isIncluded: Boolean): Long {
        if (isIncluded) return CALLER_BIT
        return 0L
    }

    override fun toString(): String = "Logger($name)"

    private companion object {
        private const val GENERATION_SHIFT: Int = 8
        private const val LEVEL_MASK: Long = 0x7FL
        private const val CALLER_BIT: Long = 0x80L
        private const val UNRESOLVED: Long = -1L
    }
}
