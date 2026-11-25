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

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.record.LogCause
import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.core.record.LogRecord

internal object OpenTelemetryLayout {
    private const val THREAD: String = "thread.name"
    private const val SEQUENCE: String = "log.record.sequence"
    private const val OMITTED: String = "log.record.omitted"
    private const val MARKERS: String = "log.markers"
    private const val NAMESPACE: String = "code.namespace"
    private const val FUNCTION: String = "code.function"
    private const val LINE: String = "code.lineno"
    private const val EXCEPTION_TYPE: String = "exception.type"
    private const val EXCEPTION_MESSAGE: String = "exception.message"
    private const val EXCEPTION_STACKTRACE: String = "exception.stacktrace"

    fun write(record: LogRecord, texts: JsonTexts, writer: JsonWriter) {
        writer.beginObject()
        writer.field(name = "time_unix_nano", value = texts.nanoseconds(record.timestamp))
        writer.field(name = "severity_number", value = record.level.openTelemetrySeverity().toLong())
        writer.field(name = "severity_text", value = record.level.severityText())
        writer.field(name = "body", value = record.message)
        writer.name("attributes")
        writer.entries(attributes(record, texts))
        writer.name("instrumentation_scope")
        writer.beginObject()
        writer.field(name = "name", value = record.source.logger.value)
        writer.endObject()
        writer.endObject()
    }

    private fun attributes(record: LogRecord, texts: JsonTexts): Map<String, LogValue> {
        val attributes = LinkedHashMap<String, LogValue>()
        attributes += record.context
        attributes += record.fields
        attributes[THREAD] = LogValue.Text(record.source.thread)
        texts.sequence(record)?.let { sequence -> attributes[SEQUENCE] = LogValue.Integer(sequence) }
        record.omittedCount()?.let { omitted -> attributes[OMITTED] = LogValue.Integer(omitted) }
        val markers = record.markerNames()
        if (markers.isNotEmpty()) attributes[MARKERS] = LogValue.Items(markers.map(LogValue::Text))
        record.source.caller?.let { caller -> attributes += callerOf(caller) }
        record.cause?.let { cause -> attributes += exception(cause, texts) }
        return attributes
    }

    private fun callerOf(caller: LogFrame): Map<String, LogValue> {
        val attributes = linkedMapOf<String, LogValue>(
            NAMESPACE to LogValue.Text(caller.className),
            FUNCTION to LogValue.Text(caller.methodName),
        )
        val line = caller.knownLine() ?: return attributes
        attributes[LINE] = LogValue.Integer(line)
        return attributes
    }

    private fun exception(cause: LogCause, texts: JsonTexts): Map<String, LogValue> = mapOf(
        EXCEPTION_TYPE to LogValue.Text(cause.type),
        EXCEPTION_MESSAGE to (cause.message?.let(LogValue::Text) ?: LogValue.Null),
        EXCEPTION_STACKTRACE to LogValue.Text(texts.stack(cause)),
    )
}
