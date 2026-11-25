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

package com.davils.kheck.testing

import com.davils.kheck.core.record.LogRecord

/**
 * Builds a record for a test, such as `logRecord { level = LogLevel.Error; cause = failure }`.
 *
 * @param block changes the fixed values of [LogRecordBuilder].
 * @return the record.
 * @since 1.0.0
 */
public fun logRecord(block: LogRecordBuilder.() -> Unit = {}): LogRecord {
    val builder = LogRecordBuilder()
    builder.block()
    return builder.build()
}
