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

internal object EcsLayout {
    private const val VERSION: String = "8.11.0"

    fun write(record: LogRecord, texts: JsonTexts, writer: JsonWriter) {
        val caller = record.source.caller
        val cause = record.cause
        writer.beginObject()
        writer.field(name = "@timestamp", value = texts.timestamp(record.timestamp))
        writer.field(name = "log.level", value = record.level.severityText().lowercase())
        writer.field(name = "log.logger", value = record.source.logger.value)
        writer.field(name = "process.thread.name", value = record.source.thread)
        writer.field(name = "message", value = record.message)
        writer.field(name = "event.sequence", value = texts.sequence(record))
        writer.strings("tags", record.markerNames())
        writer.field(name = "log.origin.function", value = caller?.function())
        writer.field(name = "log.origin.file.name", value = caller?.fileName)
        writer.field(name = "log.origin.file.line", value = caller?.knownLine())
        writer.field(name = "error.type", value = cause?.type)
        writer.field(name = "error.message", value = cause?.message)
        writer.field(name = "error.stack_trace", value = cause?.let(texts::stack))
        custom(record, writer)
        writer.field(name = "ecs.version", value = VERSION)
        writer.endObject()
    }

    private fun custom(record: LogRecord, writer: JsonWriter) {
        val isEmpty = record.fields.isEmpty() && record.context.isEmpty() && record.omitted == 0
        if (isEmpty) return
        writer.name("kheck")
        writer.beginObject()
        writer.fields("fields", record.fields)
        writer.fields("context", record.context)
        writer.field(name = "omitted", value = record.omittedCount())
        writer.endObject()
    }
}
