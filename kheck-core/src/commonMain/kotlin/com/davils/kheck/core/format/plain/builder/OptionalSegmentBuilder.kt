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

import com.davils.kheck.core.format.plain.SegmentAffix
import com.davils.kheck.core.style.SegmentStyle
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Sets a segment that may be empty, with a prefix and a suffix written only when it is not.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OptionalSegmentBuilder internal constructor(
    initial: SegmentStyle,
    initialAffix: SegmentAffix,
) : SegmentBuilder(initial) {
    /**
     * The text before the segment.
     *
     * @since 1.0.0
     */
    public var prefix: String = initialAffix.prefix

    /**
     * The text after the segment.
     *
     * @since 1.0.0
     */
    public var suffix: String = initialAffix.suffix

    internal fun affix(): SegmentAffix = SegmentAffix(prefix = prefix, suffix = suffix)
}
