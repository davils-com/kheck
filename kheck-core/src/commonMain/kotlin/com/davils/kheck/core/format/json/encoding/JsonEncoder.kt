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

import com.davils.kheck.core.format.LogEncoder
import com.davils.kheck.core.format.json.JsonFormat
import com.davils.kheck.core.format.json.JsonLayout
import com.davils.kheck.core.record.LogRecord

internal class JsonEncoder(private val format: JsonFormat) : LogEncoder {
    private val texts = JsonTexts(format)

    override fun encode(record: LogRecord, out: StringBuilder) {
        val writer = JsonWriter(out)
        when (format.layout) {
            JsonLayout.Standard -> StandardLayout.write(record, texts, writer)
            JsonLayout.Ecs -> EcsLayout.write(record, texts, writer)
            JsonLayout.Logstash -> LogstashLayout.write(record, texts, writer)
            JsonLayout.OpenTelemetry -> OpenTelemetryLayout.write(record, texts, writer)
        }
        out.append('\n')
    }
}
