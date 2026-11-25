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

@file:OptIn(DavilsInternal::class)

package com.davils.kheck.slf4j

import com.davils.kheck.api.Limits
import com.davils.kheck.api.record.LogEvent
import com.davils.kheck.api.record.LogFieldsBuilder
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.api.record.LogValue
import com.davils.kore.core.annotation.DavilsInternal
import org.slf4j.Marker

internal fun eventOf(
    message: String,
    keyValues: List<Pair<String, Any?>>,
    cause: Throwable?,
    markers: List<Marker>,
): LogEvent {
    val fields = LogFieldsBuilder()
    keyValues.forEach { (key, value) -> fields.field(name = key, value = valueOf(value)) }
    val accepted = markers.mapNotNull { marker -> markerOf(marker.name) }
    val kept = accepted.take(Limits.MAX_MARKERS).toSet()
    val omitted = fields.omitted + markers.size - kept.size
    return LogEvent(message = message, fields = fields.collected(), cause = cause, markers = kept, omitted = omitted)
}

private fun markerOf(name: String?): LogMarker? {
    val candidate = name?.takeIf { text -> text.isNotBlank() && text.length <= Limits.MAX_NAME_LENGTH }
    return candidate?.let(::LogMarker)
}

private fun valueOf(value: Any?): LogValue {
    if (value == null) return LogValue.Null
    val known = numberOf(value) ?: (value as? Boolean)?.let(LogValue::Bool)
    return known ?: value as? LogValue ?: LogValue.Text("$value")
}

private fun numberOf(value: Any): LogValue? {
    if (value is Int) return LogValue.Integer(value.toLong())
    return (value as? Long)?.let(LogValue::Integer) ?: (value as? Double)?.let(LogValue::Decimal)
}
