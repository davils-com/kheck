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

/**
 * How many colors a terminal shows, from none to 24-bit color.
 *
 * Colors a style names are reduced to the depth of the terminal: an RGB color becomes the nearest entry of the
 * palette, an entry of the 256-color palette the nearest of the 16 basic colors.
 *
 * @since 1.0.0
 */
public enum class ColorDepth {
    /**
     * No colors and no styles at all.
     *
     * @since 1.0.0
     */
    None,

    /**
     * The 16 basic colors.
     *
     * @since 1.0.0
     */
    Ansi16,

    /**
     * The 256-color palette.
     *
     * @since 1.0.0
     */
    Ansi256,

    /**
     * 24-bit RGB color.
     *
     * @since 1.0.0
     */
    TrueColor,
}
