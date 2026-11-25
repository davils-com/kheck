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

package com.davils.kheck.api.record

import com.davils.kheck.api.Limits
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Collects the typed fields of a record, in the order they are added; a later field replaces an earlier one of the
 * same name.
 *
 * The builder enforces the [Limits] of kheck: a field beyond [Limits.MAX_FIELDS], a field whose name is longer than
 * [Limits.MAX_NAME_LENGTH], a value nested deeper than [Limits.MAX_DEPTH] and an item beyond [Limits.MAX_ITEMS] are
 * left out and counted in [omitted], so that the record says that it is incomplete.
 *
 * A builder is used by one thread, inside one call of the logging, and never escapes it.
 *
 * @since 1.0.0
 */
@DavilsDsl
@OptIn(DavilsInternal::class)
public open class LogFieldsBuilder @DavilsInternal constructor() {
    private val values = LinkedHashMap<String, LogValue>()
    private var depth: Int = 0
    private var dropped: Int = 0

    /**
     * How many fields, items and nested values this builder left out so far, nested groups included.
     *
     * @since 1.0.0
     */
    @DavilsInternal
    public val omitted: Int get() = dropped

    /**
     * Adds a field with a value of any type.
     *
     * [LogValue.Items] and [LogValue.Entries] are checked against the [Limits] as a whole: what exceeds them is left
     * out of the stored copy and counted.
     *
     * @param name the name of the field.
     * @param value the value of the field.
     * @since 1.0.0
     */
    public fun field(name: String, value: LogValue) {
        val isTooLong = name.length > Limits.MAX_NAME_LENGTH
        val isFull = values.size >= Limits.MAX_FIELDS && name !in values
        if (isTooLong || isFull) {
            dropped += 1
            return
        }
        values[name] = bounded(value, depth + 1)
    }

    /**
     * Adds a field with text, or [LogValue.Null] for `null`.
     *
     * @param name the name of the field.
     * @param value the text, or `null`.
     * @since 1.0.0
     */
    public fun field(name: String, value: String?) {
        if (value == null) {
            field(name, LogValue.Null)
            return
        }
        field(name, LogValue.Text(value))
    }

    /**
     * Adds a field with a whole number.
     *
     * @param name the name of the field.
     * @param value the number.
     * @since 1.0.0
     */
    public fun field(name: String, value: Int) {
        field(name, LogValue.Integer(value.toLong()))
    }

    /**
     * Adds a field with a whole number.
     *
     * @param name the name of the field.
     * @param value the number.
     * @since 1.0.0
     */
    public fun field(name: String, value: Long) {
        field(name, LogValue.Integer(value))
    }

    /**
     * Adds a field with a floating point number.
     *
     * @param name the name of the field.
     * @param value the number.
     * @since 1.0.0
     */
    public fun field(name: String, value: Double) {
        field(name, LogValue.Decimal(value))
    }

    /**
     * Adds a field with a truth value.
     *
     * @param name the name of the field.
     * @param value the truth value.
     * @since 1.0.0
     */
    public fun field(name: String, value: Boolean) {
        field(name, LogValue.Bool(value))
    }

    /**
     * Adds a field with a point in time.
     *
     * @param name the name of the field.
     * @param value the point in time.
     * @since 1.0.0
     */
    public fun timestamp(name: String, value: Instant) {
        field(name, LogValue.Timestamp(value))
    }

    /**
     * Adds a field with a span of time.
     *
     * @param name the name of the field.
     * @param value the span of time.
     * @since 1.0.0
     */
    public fun duration(name: String, value: Duration) {
        field(name, LogValue.Elapsed(value))
    }

    /**
     * Adds a field that says a secret was there without revealing it; layouts write the mask instead.
     *
     * The value is dropped at once and never stored, so no part of the logging can reveal it.
     *
     * @param name the name of the field.
     * @since 1.0.0
     */
    public fun secret(name: String) {
        field(name, LogValue.Secret)
    }

    /**
     * Adds a field that groups the fields [block] adds, such as `http { field("method", "GET") }`.
     *
     * @param name the name of the group.
     * @param block adds the fields of the group.
     * @since 1.0.0
     */
    public fun group(name: String, block: LogFieldsBuilder.() -> Unit) {
        if (depth + 1 >= Limits.MAX_DEPTH) {
            dropped += 1
            return
        }
        val nested = LogFieldsBuilder()
        nested.depth = depth + 1
        nested.block()
        dropped += nested.dropped
        field(name, LogValue.Entries(nested.values))
    }

    /**
     * Returns a copy of the fields added so far.
     *
     * @return the fields, in the order they were added.
     * @since 1.0.0
     */
    @DavilsInternal
    public fun collected(): Map<String, LogValue> {
        if (values.isEmpty()) return emptyMap()
        return LinkedHashMap(values)
    }

    private fun bounded(value: LogValue, level: Int): LogValue = when (value) {
        is LogValue.Items -> boundedItems(value, level)
        is LogValue.Entries -> boundedEntries(value, level)
        is LogValue.Text, is LogValue.Integer, is LogValue.Decimal, is LogValue.Bool -> value
        is LogValue.Timestamp, is LogValue.Elapsed, LogValue.Secret, LogValue.Null -> value
    }

    private fun boundedItems(value: LogValue.Items, level: Int): LogValue {
        if (level >= Limits.MAX_DEPTH) return omittedValue()
        val kept = value.values.take(Limits.MAX_ITEMS)
        dropped += value.values.size - kept.size
        return LogValue.Items(kept.map { item -> bounded(item, level + 1) })
    }

    private fun boundedEntries(value: LogValue.Entries, level: Int): LogValue {
        if (level >= Limits.MAX_DEPTH) return omittedValue()
        val kept = LinkedHashMap<String, LogValue>()
        value.values.forEach { (name, nested) -> keep(target = kept, name = name, value = nested, level = level) }
        return LogValue.Entries(kept)
    }

    private fun keep(target: MutableMap<String, LogValue>, name: String, value: LogValue, level: Int) {
        val isAdmitted = target.size < Limits.MAX_FIELDS && name.length <= Limits.MAX_NAME_LENGTH
        if (!isAdmitted) {
            dropped += 1
            return
        }
        target[name] = bounded(value, level + 1)
    }

    private fun omittedValue(): LogValue {
        dropped += 1
        return LogValue.Null
    }
}
