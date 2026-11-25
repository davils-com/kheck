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

package com.davils.kheck.api

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

/**
 * The dotted name of a logger, such as `com.example.billing.Invoices`, which places it in a hierarchy.
 *
 * The empty name is the [ROOT] of the hierarchy. A name is [within][isWithin] each of its ancestors, so the settings of
 * `com.example` apply to `com.example.billing.Invoices` unless a closer logger overrides them.
 *
 * A name is plain text and may come from anywhere; every layout of kheck escapes it where it writes it.
 *
 * @property value the dotted name, empty for the root.
 * @throws IllegalArgumentException if [value] is longer than [Limits.MAX_NAME_LENGTH].
 * @since 1.0.0
 */
@JvmInline
public value class LoggerName(public val value: String) {
    init {
        require(value.length <= Limits.MAX_NAME_LENGTH) { TOO_LONG }
    }

    /**
     * Whether this is the root of the hierarchy.
     *
     * @since 1.0.0
     */
    public val isRoot: Boolean get() = value.isEmpty()

    /**
     * The name one level up, the [ROOT] for a name without a dot, and `null` for the root itself.
     *
     * @since 1.0.0
     */
    public val parent: LoggerName?
        get() {
            if (isRoot) return null
            val end = value.lastIndexOf(SEPARATOR)
            if (end < 0) return ROOT
            return LoggerName(value.substring(startIndex = 0, endIndex = end))
        }

    /**
     * Returns whether this name is [ancestor] itself or lies below it.
     *
     * `a.bc` is not within `a.b`: only whole segments count.
     *
     * @param ancestor the name that may contain this one.
     * @return `true` if [ancestor] is the root, this name, or a prefix of this name that ends at a dot.
     * @since 1.0.0
     */
    public fun isWithin(ancestor: LoggerName): Boolean {
        val isSame = ancestor.isRoot || value == ancestor.value
        if (isSame) return true
        return value.startsWith(ancestor.value + SEPARATOR)
    }

    /**
     * Returns the name of a child of this name.
     *
     * @param segment the name of the child, appended after a dot; it may contain dots itself.
     * @return the name of the child.
     * @throws IllegalArgumentException if the name of the child is longer than [Limits.MAX_NAME_LENGTH].
     * @since 1.0.0
     */
    public fun child(segment: String): LoggerName {
        if (isRoot) return LoggerName(segment)
        return LoggerName(value + SEPARATOR + segment)
    }

    /**
     * Shows the name, escaped, so that it cannot forge a line of a log.
     *
     * @return the escaped name.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = SafeText.text(value)

    /**
     * The root and the creation of names from types.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val SEPARATOR: Char = '.'
        private const val COMPANION: String = ".Companion"
        private const val TOO_LONG: String = "The name of a logger must not be longer than ${Limits.MAX_NAME_LENGTH}"

        /**
         * The empty name at the top of the hierarchy.
         *
         * @since 1.0.0
         */
        public val ROOT: LoggerName = LoggerName("")

        /**
         * Returns the name of a logger for [type]: its qualified name, without the `.Companion` of a companion object.
         *
         * A type without a qualified name, such as a local or anonymous class, falls back to its simple name and, when
         * it has none either, to the [ROOT].
         *
         * @param type the class that logs.
         * @return the name for [type].
         * @since 1.0.0
         */
        public fun of(type: KClass<*>): LoggerName {
            val qualified = type.qualifiedName ?: type.simpleName ?: return ROOT
            return LoggerName(qualified.removeSuffix(COMPANION))
        }
    }
}
