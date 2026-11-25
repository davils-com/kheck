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

import com.davils.kheck.core.format.plain.Alignment
import com.davils.kheck.core.format.plain.SegmentFrame
import com.davils.kheck.core.style.SegmentStyle
import com.davils.kheck.core.style.SegmentStyleBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Sets the width and the style of a segment; the style replaces the style of the theme when it is set.
 *
 * @since 1.0.0
 */
@DavilsDsl
public open class SegmentBuilder internal constructor(private val initial: SegmentStyle) {
    /**
     * The least width; shorter text is padded with spaces; 0 for none.
     *
     * @since 1.0.0
     */
    public var minWidth: Int = 0

    /**
     * The greatest width; longer text is cut; 0 for no limit.
     *
     * @since 1.0.0
     */
    public var maxWidth: Int = 0

    /**
     * Where the text sits in its width.
     *
     * @since 1.0.0
     */
    public var alignment: Alignment = Alignment.Start

    private var styleBlock: (SegmentStyleBuilder.() -> Unit)? = null

    /**
     * Replaces the style the theme gives the segment.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun style(block: SegmentStyleBuilder.() -> Unit) {
        styleBlock = block
    }

    internal fun frame(): SegmentFrame = SegmentFrame(minWidth = minWidth, maxWidth = maxWidth, alignment = alignment)

    internal fun segmentStyle(): SegmentStyle = styled(initial, styleBlock)
}
