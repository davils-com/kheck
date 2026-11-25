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

import com.davils.kheck.core.record.LogRecord

internal object StandardLayout {
    fun write(record: LogRecord, texts: JsonTexts, writer: JsonWriter) {
        writer.beginObject()
        writer.field(name = "timestamp", value = texts.timestamp(record.timestamp))
        writer.field(name = "sequence", value = texts.sequence(record))
        writer.field(name = "level", value = record.level.severityText())
        writer.field(name = "logger", value = record.source.logger.value)
        writer.field(name = "thread", value = record.source.thread)
        writer.field(name = "message", value = record.message)
        writer.strings("markers", record.markerNames())
        caller(record, writer)
        writer.fields("fields", record.fields)
        writer.fields("context", record.context)
        error(record, texts, writer)
        writer.field(name = "omitted", value = record.omittedCount())
        writer.endObject()
    }

    private fun caller(record: LogRecord, writer: JsonWriter) {
        val caller = record.source.caller ?: return
        writer.name("caller")
        writer.beginObject()
        writer.field(name = "className", value = caller.className)
        writer.field(name = "method", value = caller.methodName)
        writer.field(name = "file", value = caller.fileName)
        writer.field(name = "line", value = caller.knownLine())
        writer.endObject()
    }

    private fun error(record: LogRecord, texts: JsonTexts, writer: JsonWriter) {
        val cause = record.cause ?: return
        writer.name("error")
        writer.beginObject()
        writer.field(name = "type", value = cause.type)
        writer.field(name = "message", value = cause.message)
        writer.field(name = "stack", value = texts.stack(cause))
        writer.endObject()
    }
}
