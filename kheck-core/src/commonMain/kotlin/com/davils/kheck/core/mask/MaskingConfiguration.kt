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

import com.davils.kheck.api.internal.orderedCopy
import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty

/**
 * The masking of a logging: which fields and which patterns of text are replaced, and how.
 *
 * The masking runs on every record before any sink sees it: on the message, on every text field, on the fields with
 * a sensitive name, on the context and on the messages of a captured throwable. Created by `mask { }`.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class MaskingConfiguration internal constructor(builder: MaskingBuilder) : Validatable {
    /**
     * The mask of sensitive fields and of rules without a mask of their own.
     *
     * @since 1.0.0
     */
    public val default: Mask = builder.default

    /**
     * The key of [Mask.Hash], if any.
     *
     * @since 1.0.0
     */
    public val hashKey: MaskingKey? = builder.hashKey

    /**
     * The longest text the rules scan.
     *
     * @since 1.0.0
     */
    public val maxValueLength: Int = builder.maxValueLength

    /**
     * The deepest nesting the masking descends into.
     *
     * @since 1.0.0
     */
    public val maxDepth: Int = builder.maxDepth

    /**
     * The names of fields whose value is always masked; the set cannot be changed.
     *
     * @since 1.0.0
     */
    public val keys: Set<String> =
        (defaults(builder.isDefaultsIncluded, MaskingRules.SECRET_KEYS) + builder.keys).toSet().orderedCopy()

    /**
     * The patterns of names of fields whose value is masked; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val keyPatterns: List<Regex> =
        (defaults(builder.isDefaultsIncluded, listOf(MaskingRules.SECRET_KEY_PATTERN)) + builder.keyPatterns)
            .immutableList()

    /**
     * The rules, in the order they run; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val rules: List<MaskingRule> =
        (defaults(builder.isDefaultsIncluded, MaskingRules.SECRETS) + builder.rules).immutableList()

    /**
     * Checks the limits, the partial masks and that every hash mask has a key.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (maxValueLength !in MIN_VALUE_LENGTH..MAX_VALUE_LENGTH) {
            fail(
                property = ::maxValueLength,
                message = "must lie between $MIN_VALUE_LENGTH and $MAX_VALUE_LENGTH, but is $maxValueLength",
                code = LoggingViolationCodes.INVALID_SCAN_LENGTH,
            )
        }
        if (maxDepth !in 1..MAX_DEPTH) {
            fail(
                property = ::maxDepth,
                message = "must lie between 1 and $MAX_DEPTH, but is $maxDepth",
                code = LoggingViolationCodes.INVALID_DEPTH,
            )
        }
        checkMask(::default, default)
        each(::rules, rules) { rule -> rule.mask?.let { mask -> checkMask(MaskingRule::mask, mask) } }
    }

    /**
     * Shows the default mask, the counts of names and patterns, and the names of the rules; never the key.
     *
     * @return the configuration as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "MaskingConfiguration(default=$default, keys=${keys.size}, keyPatterns=${keyPatterns.size}, " +
            "rules=${rules.map { rule -> rule.name }}, maxValueLength=$maxValueLength, maxDepth=$maxDepth)"

    private fun ValidationScope.checkMask(property: KProperty<*>, mask: Mask) {
        if (mask is Mask.Partial) checkPartial(property, mask)
        val isHashUnkeyed = mask == Mask.Hash && hashKey == null
        if (!isHashUnkeyed) return
        fail(property, "hashes values only with a hash key", LoggingViolationCodes.MISSING_HASH_KEY)
    }

    private fun ValidationScope.checkPartial(property: KProperty<*>, mask: Mask.Partial) {
        val isNegative = mask.keepStart < 0 || mask.keepEnd < 0
        if (!isNegative) return
        fail(property, "must keep a count of characters that is not negative", LoggingViolationCodes.INVALID_PARTIAL)
    }

    private fun <T> defaults(isIncluded: Boolean, values: Collection<T>): List<T> {
        if (!isIncluded) return emptyList()
        return values.toList()
    }

    private companion object {
        private const val MIN_VALUE_LENGTH: Int = 256
        private const val MAX_VALUE_LENGTH: Int = 16_777_216
        private const val MAX_DEPTH: Int = 64
    }
}
