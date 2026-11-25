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

package com.davils.kheck.jul

import com.davils.kheck.api.LogLevel
import java.util.logging.Level

private val BELOW_SEVERE: List<Pair<Level, LogLevel>> = listOf(
    Level.WARNING to LogLevel.Warn,
    Level.INFO to LogLevel.Info,
    Level.FINE to LogLevel.Debug,
    Level.ALL to LogLevel.Trace,
)

internal fun LogLevel.toJulLevel(): Level = when (this) {
    LogLevel.Trace -> Level.FINEST
    LogLevel.Debug -> Level.FINE
    LogLevel.Info -> Level.INFO
    LogLevel.Warn -> Level.WARNING
    LogLevel.Error -> Level.SEVERE
    LogLevel.Off -> Level.OFF
}

internal fun Level.toLogLevel(): LogLevel {
    val value = intValue()
    if (value >= Level.SEVERE.intValue()) return LogLevel.Error
    return BELOW_SEVERE.first { (threshold, _) -> value >= threshold.intValue() }.second
}
