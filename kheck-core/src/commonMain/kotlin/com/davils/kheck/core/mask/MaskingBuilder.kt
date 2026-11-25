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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures the masking inside `mask { }` of the logging.
 *
 * By default the names of [MaskingRules.SECRET_KEYS], the [MaskingRules.SECRET_KEY_PATTERN] and the rules of
 * [MaskingRules.SECRETS] are active; set [isDefaultsIncluded] to `false` to start from nothing.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class MaskingBuilder internal constructor() : ValidatingBuilder<MaskingConfiguration>() {
    /**
     * Names the masking in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "masking"

    /**
     * Whether the default names, pattern and rules are active; `true` by default.
     *
     * @since 1.0.0
     */
    public var isDefaultsIncluded: Boolean = true

    /**
     * The mask of sensitive fields and of rules without a mask of their own; [Mask.Full] by default.
     *
     * @since 1.0.0
     */
    public var default: Mask = Mask.Full

    /**
     * The key of [Mask.Hash]; required when any mask hashes.
     *
     * @since 1.0.0
     */
    public var hashKey: MaskingKey? = null

    /**
     * The longest text the rules scan, in UTF-16 code units; longer text is cut to it and marked; 65 536 by default.
     *
     * @since 1.0.0
     */
    public var maxValueLength: Int = DEFAULT_MAX_VALUE_LENGTH

    /**
     * The deepest nesting of lists and groups the masking descends into; deeper values are masked whole; 8 by
     * default.
     *
     * @since 1.0.0
     */
    public var maxDepth: Int = DEFAULT_MAX_DEPTH

    internal val keys = LinkedHashSet<String>()
    internal val keyPatterns = mutableListOf<Regex>()
    internal val rules = mutableListOf<MaskingRule>()

    /**
     * Adds names of fields whose value is always masked.
     *
     * @param names the names, compared in lower case without anything but letters and digits.
     * @since 1.0.0
     */
    public fun keys(vararg names: String) {
        checkOpen()
        keys += names
    }

    /**
     * Adds names of fields whose value is always masked.
     *
     * @param names the names, compared in lower case without anything but letters and digits.
     * @since 1.0.0
     */
    public fun keys(names: Collection<String>) {
        checkOpen()
        keys += names
    }

    /**
     * Adds a pattern of names of fields whose value is masked, run on the normalized name.
     *
     * @param pattern the pattern.
     * @since 1.0.0
     */
    public fun keyPattern(pattern: Regex) {
        checkOpen()
        keyPatterns += pattern
    }

    /**
     * Adds a rule.
     *
     * @param rule the rule.
     * @since 1.0.0
     */
    public fun rule(rule: MaskingRule) {
        checkOpen()
        rules += rule
    }

    /**
     * Adds rules, such as [MaskingRules.PERSONAL_DATA].
     *
     * @param rules the rules.
     * @since 1.0.0
     */
    public fun rules(rules: Collection<MaskingRule>) {
        checkOpen()
        this.rules += rules
    }

    /**
     * Creates the configuration.
     *
     * @return the configuration, validated afterwards.
     * @since 1.0.0
     */
    override fun assemble(): MaskingConfiguration = MaskingConfiguration(this)

    /**
     * The defaults of the masking.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default of [maxValueLength].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_MAX_VALUE_LENGTH: Int = 65_536

        /**
         * The default of [maxDepth].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_MAX_DEPTH: Int = 8
    }
}
