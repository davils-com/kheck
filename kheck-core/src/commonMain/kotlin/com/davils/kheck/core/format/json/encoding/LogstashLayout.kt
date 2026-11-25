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

internal object LogstashLayout {
    private const val VERSION: String = "1"

    fun write(record: LogRecord, texts: JsonTexts, writer: JsonWriter) {
        val caller = record.source.caller
        writer.beginObject()
        writer.field(name = "@timestamp", value = texts.timestamp(record.timestamp))
        writer.field(name = "@version", value = VERSION)
        writer.field(name = "message", value = record.message)
        writer.field(name = "logger_name", value = record.source.logger.value)
        writer.field(name = "thread_name", value = record.source.thread)
        writer.field(name = "level", value = record.level.severityText())
        writer.field(name = "level_value", value = record.level.logstashSeverity().toLong())
        writer.field(name = "sequence", value = texts.sequence(record))
        writer.strings("tags", record.markerNames())
        writer.field(name = "caller_class_name", value = caller?.className)
        writer.field(name = "caller_method_name", value = caller?.methodName)
        writer.field(name = "caller_file_name", value = caller?.fileName)
        writer.field(name = "caller_line_number", value = caller?.knownLine())
        writer.field(name = "stack_trace", value = record.cause?.let(texts::stack))
        writer.fields("fields", record.fields)
        writer.fields("context", record.context)
        writer.field(name = "omitted", value = record.omittedCount())
        writer.endObject()
    }
}
