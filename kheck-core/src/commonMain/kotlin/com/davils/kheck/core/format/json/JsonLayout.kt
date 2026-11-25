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

package com.davils.kheck.core.format.json

/**
 * The shape of the JSON object a JSON format writes per record.
 *
 * @since 1.0.0
 */
public enum class JsonLayout {
    /**
     * The own layout of kheck: `timestamp`, `sequence`, `level`, `logger`, `thread`, `message`, `markers`, `caller`,
     * `fields`, `context`, `error` and `omitted`.
     *
     * @since 1.0.0
     */
    Standard,

    /**
     * The Elastic Common Schema 8.11: `@timestamp`, `log.level`, `log.logger`, `process.thread.name`, `message`,
     * `tags`, `log.origin.*`, `error.*`, with fields and context below `kheck`.
     *
     * @since 1.0.0
     */
    Ecs,

    /**
     * The JSON event of the Logstash encoder: `@timestamp`, `@version`, `logger_name`, `thread_name`, `level`,
     * `level_value`, `tags`, `caller_*`, `stack_trace`, `fields` and `context`.
     *
     * @since 1.0.0
     */
    Logstash,

    /**
     * The log data model of OpenTelemetry: `time_unix_nano`, `severity_number`, `severity_text`, `body`,
     * `attributes` and `instrumentation_scope`.
     *
     * @since 1.0.0
     */
    OpenTelemetry,
}
