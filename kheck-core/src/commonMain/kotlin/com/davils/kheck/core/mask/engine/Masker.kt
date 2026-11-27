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

package com.davils.kheck.core.mask.engine

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.format.text.rendered
import com.davils.kheck.core.mask.Mask
import com.davils.kheck.core.mask.MaskingConfiguration
import com.davils.kheck.core.mask.MaskingRule
import com.davils.kheck.core.record.LogRecord
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.krypt.primitive.Crypto

@OptIn(DavilsInternal::class)
internal class Masker(
    private val configuration: MaskingConfiguration,
    crypto: Crypto,
    private val counted: (String) -> Unit,
) {
    private val keys: Set<String> = configuration.keys.map(::normalized).toSet()
    private val hasher: Hasher? = Hasher.of(configuration, crypto)
    private val decisions = HashMap<String, Boolean>()

    suspend fun record(record: LogRecord): LogRecord = record.copied(
        message = text(record.message),
        fields = entries(record.fields, depth = 0),
        context = entries(record.context, depth = 0),
        cause = record.cause?.withMessages(::text),
    )

    suspend fun text(text: String): String =
        configuration.rules.fold(bounded(text)) { current, rule -> applied(rule, current) }

    private suspend fun applied(rule: MaskingRule, text: String): String {
        val isHinted = rule.hints.isEmpty() || rule.hints.any { hint -> text.contains(hint, ignoreCase = true) }
        if (!isHinted) return text
        val replaced = StringBuilder(text.length)
        var copied = 0
        for (match in rule.pattern.findAll(text)) {
            replaced.append(text, copied, match.range.first)
            replaced.append(replacement(rule, match))
            copied = match.range.last + 1
        }
        replaced.append(text, copied, text.length)
        return replaced.toString()
    }

    private suspend fun replacement(rule: MaskingRule, match: MatchResult): String {
        if (!isConfirmed(rule, match.value)) return match.value
        counted(rule.name)
        val mask = rule.mask ?: configuration.default
        return maskedMatch(match, mask, rule.hasSecretGroup)
    }

    private fun isConfirmed(rule: MaskingRule, candidate: String): Boolean {
        val validator = rule.validator ?: return true
        return validator.isMatch(candidate)
    }

    private suspend fun maskedMatch(match: MatchResult, mask: Mask, hasSecretGroup: Boolean): String {
        val secret = secretGroup(match, hasSecretGroup) ?: return masked(match.value, mask)
        val start = match.value.lastIndexOf(secret)
        val end = start + secret.length
        val prefix = match.value.substring(startIndex = 0, endIndex = start)
        return prefix + masked(secret, mask) + match.value.substring(end)
    }

    private fun secretGroup(match: MatchResult, hasSecretGroup: Boolean): String? {
        if (!hasSecretGroup) return null
        val value = match.groups[MaskingRule.SECRET_GROUP_NAME]?.value
        return value?.takeIf { text -> text.isNotEmpty() }
    }

    private suspend fun entries(values: Map<String, LogValue>, depth: Int): Map<String, LogValue> {
        if (values.isEmpty()) return values
        val result = LinkedHashMap<String, LogValue>(values.size)
        values.forEach { (key, value) ->
            val masked = entry(key, value, depth) ?: return@forEach
            result[key] = masked
        }
        return result
    }

    private suspend fun entry(key: String, value: LogValue, depth: Int): LogValue? {
        if (!isSecretKey(key)) return value(value, depth)
        counted(KEY_RULE)
        return keyed(value)
    }

    private suspend fun keyed(value: LogValue): LogValue? {
        if (configuration.default == Mask.Remove) return null
        return LogValue.Text(masked(value.rendered(), configuration.default))
    }

    private suspend fun value(value: LogValue, depth: Int): LogValue = when (value) {
        is LogValue.Text -> LogValue.Text(text(value.value))

        LogValue.Secret -> secret()

        is LogValue.Items -> items(value, depth)

        is LogValue.Entries -> nested(value, depth)

        is LogValue.Integer, is LogValue.Decimal, is LogValue.Bool, is LogValue.Timestamp,
        is LogValue.Elapsed, LogValue.Null,
        -> value
    }

    private fun secret(): LogValue {
        counted(SECRET_RULE)
        return LogValue.Secret
    }

    private suspend fun items(value: LogValue.Items, depth: Int): LogValue {
        if (depth >= configuration.maxDepth) return tooDeep()
        return LogValue.Items(value.values.map { item -> value(item, depth + 1) })
    }

    private suspend fun nested(value: LogValue.Entries, depth: Int): LogValue {
        if (depth >= configuration.maxDepth) return tooDeep()
        return LogValue.Entries(entries(value.values, depth + 1))
    }

    private fun tooDeep(): LogValue {
        counted(DEPTH_RULE)
        return LogValue.Text(SafeText.REDACTED)
    }

    private suspend fun masked(value: String, mask: Mask): String = when (mask) {
        Mask.Full -> SafeText.REDACTED
        is Mask.Partial -> partial(value, mask)
        Mask.Hash -> hasher?.hash(value) ?: SafeText.REDACTED
        is Mask.Fixed -> mask.replacement
        Mask.Remove -> ""
    }

    private fun partial(value: String, mask: Mask.Partial): String {
        val isTooShort = value.length < mask.keepStart + mask.keepEnd + MIN_HIDDEN
        if (isTooShort) return SafeText.REDACTED
        return value.take(mask.keepStart) + SafeText.REDACTED + value.takeLast(mask.keepEnd)
    }

    private fun isSecretKey(key: String): Boolean {
        val isKnownSecret = decisions[key]
        if (isKnownSecret != null) return isKnownSecret
        if (decisions.size >= MAX_DECISIONS) decisions.clear()
        val normalized = normalized(key)
        val isPatterned = configuration.keyPatterns.any { pattern -> pattern.containsMatchIn(normalized) }
        val isSecret = normalized in keys || isPatterned
        decisions[key] = isSecret
        return isSecret
    }

    private fun bounded(text: String): String {
        val excess = text.length - configuration.maxValueLength
        if (excess <= 0) return text
        return text.take(configuration.maxValueLength) + "…(+$excess chars)"
    }

    private fun normalized(key: String): String = key.lowercase().filter(Char::isLetterOrDigit)

    internal companion object {
        const val KEY_RULE: String = "key"
        const val SECRET_RULE: String = "secret"
        const val DEPTH_RULE: String = "depth"
        private const val MIN_HIDDEN: Int = 4
        private const val MAX_DECISIONS: Int = 4_096
    }
}
