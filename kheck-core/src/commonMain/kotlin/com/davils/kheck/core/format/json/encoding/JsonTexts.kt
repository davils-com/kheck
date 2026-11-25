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

import com.davils.kheck.core.format.json.JsonFormat
import com.davils.kheck.core.format.plain.CauseLayout
import com.davils.kheck.core.format.plain.CauseStyles
import com.davils.kheck.core.format.plain.encoding.CauseWriter
import com.davils.kheck.core.format.plain.encoding.StyledWriter
import com.davils.kheck.core.record.LogCause
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.style.ColorDepth
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal class JsonTexts(private val format: JsonFormat) {
    private val causes = CauseWriter(
        layout = CauseLayout(
            maxFrames = format.maxFrames,
            maxCauses = format.maxCauses,
            isSuppressedIncluded = true,
            excludedPackages = format.excludedPackages,
            indent = INDENT,
            styles = CauseStyles(),
        ),
        writer = StyledWriter(ColorDepth.None),
    )
    private val divisor: Int = DIVISORS.getValue(format.precision.digits)

    fun timestamp(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.UTC)
        val fraction = local.nanosecond / divisor
        return buildString {
            append(padded(value = local.year, digits = YEAR_DIGITS))
            append('-')
            append(padded(value = local.month.ordinal + 1, digits = TWO_DIGITS))
            append('-')
            append(padded(value = local.day, digits = TWO_DIGITS))
            append('T')
            append(padded(value = local.hour, digits = TWO_DIGITS))
            append(':')
            append(padded(value = local.minute, digits = TWO_DIGITS))
            append(':')
            append(padded(value = local.second, digits = TWO_DIGITS))
            append('.')
            append(padded(value = fraction, digits = format.precision.digits))
            append('Z')
        }
    }

    fun nanoseconds(instant: Instant): String {
        val nanos = instant.epochSeconds * NANOS_PER_SECOND + instant.nanosecondsOfSecond
        return nanos.toString()
    }

    fun stack(cause: LogCause): String {
        val out = StringBuilder()
        causes.write(cause, out)
        return out.trimEnd().toString()
    }

    fun sequence(record: LogRecord): Long? {
        if (!format.isSequenceIncluded) return null
        return record.sequence
    }

    private fun padded(value: Int, digits: Int): String = value.toString().padStart(digits, '0')

    private companion object {
        private const val INDENT: String = "\t"
        private const val YEAR_DIGITS: Int = 4
        private const val TWO_DIGITS: Int = 2
        private const val NANOS_PER_SECOND: Long = 1_000_000_000L
        private const val MILLI_DIGITS: Int = 3
        private const val MICRO_DIGITS: Int = 6
        private const val NANO_DIGITS: Int = 9
        private val DIVISORS: Map<Int, Int> = mapOf(MILLI_DIGITS to 1_000_000, MICRO_DIGITS to 1_000, NANO_DIGITS to 1)
    }
}
