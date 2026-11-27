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

package com.davils.kheck.core.mask

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText

/**
 * A pattern of sensitive text that the masking replaces wherever it occurs in a message, a text field or the message
 * of a throwable.
 *
 * When the pattern has a group named [SECRET_GROUP_NAME], only that group is masked, so `Bearer abc…` stays
 * `Bearer ****`; the group is located by its last occurrence in the match, so it belongs at the end of the pattern.
 * [hints] make the rule cheap: the pattern runs only on text that contains one of them, ignoring case.
 *
 * Patterns run on the regular expressions of the platform, which differ between the JVM and WebAssembly; the
 * patterns of [MaskingRules] use only what both offer, and a pattern of an application should too. A pattern must
 * not backtrack without bound: the masking limits the length of the text it scans, but not the work of a pattern.
 *
 * @property name the name of the rule, counted in the statistics.
 * @property pattern the pattern of sensitive text.
 * @property mask how a match is replaced, or `null` for the default of the masking.
 * @param hints texts that every match contains, copied; empty to run the pattern on every text.
 * @property validator confirms a match before it is masked, or `null` to mask every match.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class MaskingRule(
    public val name: String,
    public val pattern: Regex,
    public val mask: Mask? = null,
    hints: List<String> = emptyList(),
    public val validator: MaskingValidator? = null,
) {
    /**
     * Texts that every match contains; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val hints: List<String> = hints.immutableList()

    internal val hasSecretGroup: Boolean = pattern.pattern.contains(SECRET_GROUP)

    private val values: List<Any?> get() = listOf(name, pattern.pattern, pattern.options, mask, hints, validator)

    /**
     * Compares by value: two rules are equal when their names, the sources and options of their patterns, their
     * masks, their hints and their validators are equal; validators are compared as objects.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [MaskingRule].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is MaskingRule && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the name of the rule.
     *
     * @return the rule as text.
     * @since 1.0.0
     */
    override fun toString(): String = "MaskingRule(name=${SafeText.text(name)})"

    /**
     * The name of the group that holds the secret part of a match.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The name of the group that holds the secret part of a match: `(?<secret>…)`.
         *
         * @since 1.0.0
         */
        public const val SECRET_GROUP_NAME: String = "secret"

        private const val SECRET_GROUP: String = "(?<$SECRET_GROUP_NAME>"
    }
}
