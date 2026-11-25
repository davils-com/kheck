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
 * Whether a format writes colors.
 *
 * @since 1.0.0
 */
public enum class ColorMode {
    /**
     * Colors as deep as the terminal shows them, and none when it is not interactive, when `NO_COLOR` is set or when
     * `TERM` is `dumb`; `FORCE_COLOR` forces a depth.
     *
     * @since 1.0.0
     */
    Auto,

    /**
     * Colors always, at least the 16 basic colors, even where [Auto] would write none.
     *
     * @since 1.0.0
     */
    Always,

    /**
     * Never any color.
     *
     * @since 1.0.0
     */
    Never,
}
