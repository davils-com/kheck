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

package com.davils.kheck.slf4j

import com.davils.kheck.api.LogLevel
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.event.LoggingEvent
import org.slf4j.helpers.AbstractLogger
import org.slf4j.helpers.MessageFormatter
import org.slf4j.spi.LoggingEventAware

internal class BridgeLogger(name: String) :
    AbstractLogger(),
    LoggingEventAware {
    init {
        this.name = name
    }

    override fun isTraceEnabled(): Boolean = isEnabled(LogLevel.Trace)

    override fun isTraceEnabled(marker: Marker?): Boolean = isTraceEnabled

    override fun isDebugEnabled(): Boolean = isEnabled(LogLevel.Debug)

    override fun isDebugEnabled(marker: Marker?): Boolean = isDebugEnabled

    override fun isInfoEnabled(): Boolean = isEnabled(LogLevel.Info)

    override fun isInfoEnabled(marker: Marker?): Boolean = isInfoEnabled

    override fun isWarnEnabled(): Boolean = isEnabled(LogLevel.Warn)

    override fun isWarnEnabled(marker: Marker?): Boolean = isWarnEnabled

    override fun isErrorEnabled(): Boolean = isEnabled(LogLevel.Error)

    override fun isErrorEnabled(marker: Marker?): Boolean = isErrorEnabled

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: Level,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?,
    ) {
        val message = formatted(messagePattern, arguments)
        val markers = listOfNotNull(marker)
        val event = eventOf(message = message, keyValues = emptyList(), cause = throwable, markers = markers)
        Slf4jBridge.emit(name, level.toLogLevel(), event)
    }

    override fun log(event: LoggingEvent) {
        val level = event.level.toLogLevel()
        if (!isEnabled(level)) return
        val message = formatted(event.message, event.argumentArray)
        val pairs = event.keyValuePairs.orEmpty().map { pair -> pair.key to pair.value }
        val markers = event.markers.orEmpty()
        val logged = eventOf(message = message, keyValues = pairs, cause = event.throwable, markers = markers)
        Slf4jBridge.emit(name, level, logged)
    }

    private fun isEnabled(level: LogLevel): Boolean = Slf4jBridge.isEnabled(name, level)

    private fun formatted(pattern: String?, arguments: Array<out Any?>?): String {
        val tuple = MessageFormatter.arrayFormat(pattern, arguments)
        return tuple.message.orEmpty()
    }
}
