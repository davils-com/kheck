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

package com.davils.kheck.core.format.text

import com.davils.kheck.core.format.TimestampFormat
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Instant

internal class TimestampWriter(private val zone: TimeZone) {
    fun write(instant: Instant, format: TimestampFormat, out: StringBuilder) {
        when (format) {
            TimestampFormat.Time -> time(instant.toLocalDateTime(zone), out)
            TimestampFormat.DateTime -> dateTime(instant.toLocalDateTime(zone), separator = ' ', out = out)
            TimestampFormat.Iso -> iso(instant, out)
            TimestampFormat.IsoUtc -> isoUtc(instant, out)
            TimestampFormat.EpochMillis -> out.append(instant.toEpochMilliseconds())
        }
    }

    private fun iso(instant: Instant, out: StringBuilder) {
        dateTime(instant.toLocalDateTime(zone), separator = 'T', out = out)
        val seconds = zone.offsetAt(instant).totalSeconds
        if (seconds == 0) {
            out.append('Z')
            return
        }
        out.append(sign(seconds))
        val minutes = seconds.absoluteValue / SECONDS_PER_MINUTE
        padded(value = minutes / MINUTES_PER_HOUR, digits = TWO_DIGITS, out = out)
        out.append(':')
        padded(value = minutes % MINUTES_PER_HOUR, digits = TWO_DIGITS, out = out)
    }

    private fun isoUtc(instant: Instant, out: StringBuilder) {
        dateTime(instant.toLocalDateTime(TimeZone.UTC), separator = 'T', out = out)
        out.append('Z')
    }

    private fun dateTime(local: LocalDateTime, separator: Char, out: StringBuilder) {
        padded(value = local.year, digits = YEAR_DIGITS, out = out)
        out.append('-')
        padded(value = local.month.ordinal + 1, digits = TWO_DIGITS, out = out)
        out.append('-')
        padded(value = local.day, digits = TWO_DIGITS, out = out)
        out.append(separator)
        time(local, out)
    }

    private fun time(local: LocalDateTime, out: StringBuilder) {
        padded(value = local.hour, digits = TWO_DIGITS, out = out)
        out.append(':')
        padded(value = local.minute, digits = TWO_DIGITS, out = out)
        out.append(':')
        padded(value = local.second, digits = TWO_DIGITS, out = out)
        out.append('.')
        padded(value = local.nanosecond / NANOS_PER_MILLI, digits = MILLI_DIGITS, out = out)
    }

    private fun sign(seconds: Int): Char {
        if (seconds < 0) return '-'
        return '+'
    }

    private fun padded(value: Int, digits: Int, out: StringBuilder) {
        val text = value.toString()
        repeat(digits - text.length) { out.append('0') }
        out.append(text)
    }

    private companion object {
        private const val YEAR_DIGITS: Int = 4
        private const val TWO_DIGITS: Int = 2
        private const val MILLI_DIGITS: Int = 3
        private const val NANOS_PER_MILLI: Int = 1_000_000
        private const val SECONDS_PER_MINUTE: Int = 60
        private const val MINUTES_PER_HOUR: Int = 60
    }
}
