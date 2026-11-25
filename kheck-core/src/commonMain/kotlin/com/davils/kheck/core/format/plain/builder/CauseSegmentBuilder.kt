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

import com.davils.kheck.core.format.plain.CauseLayout
import com.davils.kheck.core.format.plain.CauseStyles
import com.davils.kheck.core.style.TextStyleBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Sets how the stack trace of a record is written.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class CauseSegmentBuilder internal constructor(private val initial: CauseStyles) {
    /**
     * The most frames written per throwable; 64 by default.
     *
     * @since 1.0.0
     */
    public var maxFrames: Int = DEFAULT_MAX_FRAMES

    /**
     * The longest chain of causes written; 8 by default.
     *
     * @since 1.0.0
     */
    public var maxCauses: Int = DEFAULT_MAX_CAUSES

    /**
     * Whether suppressed throwables are written; `true` by default.
     *
     * @since 1.0.0
     */
    public var isSuppressedIncluded: Boolean = true

    /**
     * The text before a frame; four spaces by default.
     *
     * @since 1.0.0
     */
    public var indent: String = DEFAULT_INDENT

    private val excluded = mutableListOf<String>()
    private var typeBlock: (TextStyleBuilder.() -> Unit)? = null
    private var messageBlock: (TextStyleBuilder.() -> Unit)? = null
    private var labelBlock: (TextStyleBuilder.() -> Unit)? = null
    private var frameBlock: (TextStyleBuilder.() -> Unit)? = null
    private var locationBlock: (TextStyleBuilder.() -> Unit)? = null

    /**
     * Leaves out the frames of classes whose name starts with one of [packages], such as `kotlinx.coroutines.`.
     *
     * @param packages the prefixes of class names.
     * @since 1.0.0
     */
    public fun exclude(vararg packages: String) {
        excluded += packages
    }

    /**
     * Replaces the style of the type of a throwable.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun type(block: TextStyleBuilder.() -> Unit) {
        typeBlock = block
    }

    /**
     * Replaces the style of the message of a throwable.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun message(block: TextStyleBuilder.() -> Unit) {
        messageBlock = block
    }

    /**
     * Replaces the style of `Caused by:` and `Suppressed:`.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun label(block: TextStyleBuilder.() -> Unit) {
        labelBlock = block
    }

    /**
     * Replaces the style of a frame.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun frame(block: TextStyleBuilder.() -> Unit) {
        frameBlock = block
    }

    /**
     * Replaces the style of the file and line of a frame.
     *
     * @param block sets the style.
     * @since 1.0.0
     */
    public fun location(block: TextStyleBuilder.() -> Unit) {
        locationBlock = block
    }

    internal fun layout(): CauseLayout = CauseLayout(
        maxFrames = maxFrames,
        maxCauses = maxCauses,
        isSuppressedIncluded = isSuppressedIncluded,
        excludedPackages = excluded,
        indent = indent,
        styles = CauseStyles(
            type = styled(initial.type, typeBlock),
            message = styled(initial.message, messageBlock),
            label = styled(initial.label, labelBlock),
            frame = styled(initial.frame, frameBlock),
            location = styled(initial.location, locationBlock),
        ),
    )

    internal companion object {
        const val DEFAULT_MAX_FRAMES: Int = 64
        const val DEFAULT_MAX_CAUSES: Int = 8
        const val DEFAULT_INDENT: String = "    "
    }
}
