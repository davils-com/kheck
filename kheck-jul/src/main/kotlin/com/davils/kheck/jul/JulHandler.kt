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

import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogEvent
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.Logging
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.contained
import java.util.logging.ErrorManager
import java.util.logging.Formatter
import java.util.logging.Handler
import java.util.logging.LogRecord

@OptIn(DavilsInternal::class)
internal class JulHandler(private val logging: Logging) : Handler() {
    private val formatter = object : Formatter() {
        override fun format(record: LogRecord): String = formatMessage(record)
    }

    override fun publish(record: LogRecord) {
        val result = contained { forwarded(record) }
        val failure = result.exceptionOrNull() ?: return
        reportError(FAILED, IllegalStateException(FAILED, failure), ErrorManager.WRITE_FAILURE)
    }

    override fun flush() = Unit

    override fun close() = Unit

    private fun forwarded(record: LogRecord) {
        val level = record.level.toLogLevel()
        val logger = logging.logger(LoggerName.bounded(record.loggerName.orEmpty()).value)
        if (!logger.isEnabled(level)) return
        logger.emit(level, event(record))
    }

    private fun event(record: LogRecord): LogEvent = LogEvent(
        message = formatter.format(record),
        fields = source(record),
        cause = record.thrown,
    )

    private fun source(record: LogRecord): Map<String, LogValue> {
        val origin = listOfNotNull(
            record.sourceClassName?.let { name -> SOURCE_CLASS to LogValue.Text(name) },
            record.sourceMethodName?.let { name -> SOURCE_METHOD to LogValue.Text(name) },
        )
        return origin.toMap()
    }

    private companion object {
        private const val SOURCE_CLASS: String = "jul.class"
        private const val SOURCE_METHOD: String = "jul.method"
        private const val FAILED: String = "a record of java.util.logging could not be handed to kheck"
    }
}
