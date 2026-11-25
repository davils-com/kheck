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

import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.core.record.LogRecord

internal fun LogRecord.markerNames(): List<String> = markers.map { marker -> marker.name }

internal fun LogFrame.function(): String = "$className.$methodName"

internal fun LogFrame.knownLine(): Long? {
    if (lineNumber < 0) return null
    return lineNumber.toLong()
}

internal fun LogRecord.omittedCount(): Long? {
    if (omitted == 0) return null
    return omitted.toLong()
}
