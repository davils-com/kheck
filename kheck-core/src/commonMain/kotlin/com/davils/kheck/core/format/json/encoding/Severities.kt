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

package com.davils.kheck.core.format.json.encoding

import com.davils.kheck.api.LogLevel

private const val OTEL_TRACE: Int = 1
private const val OTEL_DEBUG: Int = 5
private const val OTEL_INFO: Int = 9
private const val OTEL_WARN: Int = 13
private const val OTEL_ERROR: Int = 17
private const val LOGSTASH_TRACE: Int = 5_000
private const val LOGSTASH_DEBUG: Int = 10_000
private const val LOGSTASH_INFO: Int = 20_000
private const val LOGSTASH_WARN: Int = 30_000
private const val LOGSTASH_ERROR: Int = 40_000

internal fun LogLevel.openTelemetrySeverity(): Int = when (this) {
    LogLevel.Trace -> OTEL_TRACE
    LogLevel.Debug -> OTEL_DEBUG
    LogLevel.Info -> OTEL_INFO
    LogLevel.Warn -> OTEL_WARN
    LogLevel.Error, LogLevel.Off -> OTEL_ERROR
}

internal fun LogLevel.logstashSeverity(): Int = when (this) {
    LogLevel.Trace -> LOGSTASH_TRACE
    LogLevel.Debug -> LOGSTASH_DEBUG
    LogLevel.Info -> LOGSTASH_INFO
    LogLevel.Warn -> LOGSTASH_WARN
    LogLevel.Error, LogLevel.Off -> LOGSTASH_ERROR
}

internal fun LogLevel.severityText(): String = name.uppercase()
