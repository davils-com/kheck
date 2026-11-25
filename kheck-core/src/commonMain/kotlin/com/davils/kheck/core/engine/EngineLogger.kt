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
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class EngineLogger(override val name: LoggerName, private val engine: LoggingEngine) : Logger {
    private val threshold = AtomicInt(LogLevel.Info.ordinal)
    private val callerIncluded = AtomicBoolean(false)

    val isCallerIncluded: Boolean get() = callerIncluded.load()

    override fun isEnabled(level: LogLevel): Boolean {
        if (level == LogLevel.Off) return false
        return level.ordinal >= threshold.load()
    }

    override fun emit(level: LogLevel, event: LogEvent) {
        if (!isEnabled(level)) return
        engine.submit(this, level, event)
    }

    override fun child(name: String): Logger = engine.logger(this.name.child(name))

    fun update(level: LogLevel, isCallerIncluded: Boolean) {
        threshold.store(level.ordinal)
        callerIncluded.store(isCallerIncluded)
    }

    override fun toString(): String = "Logger($name)"
}
