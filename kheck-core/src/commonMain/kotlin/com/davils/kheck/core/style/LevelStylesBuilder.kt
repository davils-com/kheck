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
 * Names the style of each level inside [SegmentStyleBuilder.byLevel].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class LevelStylesBuilder internal constructor(
    private val blocks: MutableMap<LogLevel, TextStyleBuilder.() -> Unit>,
) {
    /**
     * Sets the style of [LogLevel.Trace].
     *
     * @param block sets the style, starting from the base style.
     * @since 1.0.0
     */
    public fun trace(block: TextStyleBuilder.() -> Unit) {
        blocks[LogLevel.Trace] = block
    }

    /**
     * Sets the style of [LogLevel.Debug].
     *
     * @param block sets the style, starting from the base style.
     * @since 1.0.0
     */
    public fun debug(block: TextStyleBuilder.() -> Unit) {
        blocks[LogLevel.Debug] = block
    }

    /**
     * Sets the style of [LogLevel.Info].
     *
     * @param block sets the style, starting from the base style.
     * @since 1.0.0
     */
    public fun info(block: TextStyleBuilder.() -> Unit) {
        blocks[LogLevel.Info] = block
    }

    /**
     * Sets the style of [LogLevel.Warn].
     *
     * @param block sets the style, starting from the base style.
     * @since 1.0.0
     */
    public fun warn(block: TextStyleBuilder.() -> Unit) {
        blocks[LogLevel.Warn] = block
    }

    /**
     * Sets the style of [LogLevel.Error].
     *
     * @param block sets the style, starting from the base style.
     * @since 1.0.0
     */
    public fun error(block: TextStyleBuilder.() -> Unit) {
        blocks[LogLevel.Error] = block
    }
}
