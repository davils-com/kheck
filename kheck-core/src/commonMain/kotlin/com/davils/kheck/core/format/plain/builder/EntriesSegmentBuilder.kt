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

package com.davils.kheck.core.format.plain.builder

import com.davils.kheck.core.format.plain.EntriesLayout
import com.davils.kheck.core.format.plain.EntryStyles
import com.davils.kheck.core.format.plain.SegmentAffix
import com.davils.kheck.core.style.SegmentStyleBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Sets how the fields or the context of a record are written as `name=value` pairs.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class EntriesSegmentBuilder internal constructor(private val initial: EntryStyles) {
    /**
     * The text before the pairs; a space by default.
     *
     * @since 1.0.0
     */
    public var prefix: String = DEFAULT_PREFIX

    /**
     * The text after the pairs.
     *
     * @since 1.0.0
     */
    public var suffix: String = ""

    /**
     * The text between two pairs; a space by default.
     *
     * @since 1.0.0
     */
    public var separator: String = DEFAULT_SEPARATOR

    private var keyBlock: (SegmentStyleBuilder.() -> Unit)? = null
    private var valueBlock: (SegmentStyleBuilder.() -> Unit)? = null
    private var punctuationBlock: (SegmentStyleBuilder.() -> Unit)? = null

    /**
     * Replaces the style of the names.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun key(block: SegmentStyleBuilder.() -> Unit) {
        keyBlock = block
    }

    /**
     * Replaces the style of the values.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun value(block: SegmentStyleBuilder.() -> Unit) {
        valueBlock = block
    }

    /**
     * Replaces the style of the `=`.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun punctuation(block: SegmentStyleBuilder.() -> Unit) {
        punctuationBlock = block
    }

    internal fun layout(): EntriesLayout = EntriesLayout(
        affix = SegmentAffix(prefix = prefix, suffix = suffix),
        separator = separator,
        styles = EntryStyles(
            key = styled(initial.key, keyBlock),
            value = styled(initial.value, valueBlock),
            punctuation = styled(initial.punctuation, punctuationBlock),
        ),
    )

    private companion object {
        private const val DEFAULT_PREFIX: String = " "
        private const val DEFAULT_SEPARATOR: String = " "
    }
}
