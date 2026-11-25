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

import com.davils.kore.core.annotation.DavilsDsl

/**
 * Sets the colors and attributes of a [TextStyle], starting from another style.
 *
 * @param initial the style the builder starts from.
 * @since 1.0.0
 */
@DavilsDsl
public open class TextStyleBuilder internal constructor(initial: TextStyle = TextStyle.NONE) {
    /**
     * The color of the text, or `null` for the color of the terminal.
     *
     * @since 1.0.0
     */
    public var foreground: Color? = initial.foreground

    /**
     * The color behind the text, or `null` for the color of the terminal.
     *
     * @since 1.0.0
     */
    public var background: Color? = initial.background

    /**
     * Whether the text is bold.
     *
     * @since 1.0.0
     */
    public var isBold: Boolean = initial.isBold

    /**
     * Whether the text is dimmed.
     *
     * @since 1.0.0
     */
    public var isDim: Boolean = initial.isDim

    /**
     * Whether the text is italic.
     *
     * @since 1.0.0
     */
    public var isItalic: Boolean = initial.isItalic

    /**
     * Whether the text is underlined.
     *
     * @since 1.0.0
     */
    public var isUnderline: Boolean = initial.isUnderline

    /**
     * Whether the text is struck through.
     *
     * @since 1.0.0
     */
    public var isStrikethrough: Boolean = initial.isStrikethrough

    internal fun textStyle(): TextStyle = TextStyle(
        foreground = foreground,
        background = background,
        isBold = isBold,
        isDim = isDim,
        isItalic = isItalic,
        isUnderline = isUnderline,
        isStrikethrough = isStrikethrough,
    )
}
