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

package com.davils.kheck.core.style

import com.davils.kheck.api.LogLevel
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Sets the base style of a segment and, through [byLevel], the styles of single levels, which start from the base.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class SegmentStyleBuilder internal constructor() : TextStyleBuilder() {
    private val levelBlocks = LinkedHashMap<LogLevel, TextStyleBuilder.() -> Unit>()

    /**
     * Sets the styles of single levels; a level set twice keeps the last block.
     *
     * @param block names the levels and their styles.
     * @since 1.0.0
     */
    public fun byLevel(block: LevelStylesBuilder.() -> Unit) {
        LevelStylesBuilder(levelBlocks).block()
    }

    internal fun segmentStyle(): SegmentStyle {
        val base = textStyle()
        val levels = levelBlocks.mapValues { (_, block) ->
            val builder = TextStyleBuilder(base)
            builder.block()
            builder.textStyle()
        }
        return SegmentStyle(base = base, levels = levels)
    }
}
