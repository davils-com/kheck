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

package com.davils.kheck.core.format

import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.ColorMode

/**
 * How a sink writes records: the factory of its encoder.
 *
 * kheck offers [plain][com.davils.kheck.core.format.plain.plain] and [json][com.davils.kheck.core.format.json.json];
 * an application may implement its own.
 *
 * @since 1.0.0
 */
public interface LogFormat {
    /**
     * Whether the format writes colors.
     *
     * @since 1.0.0
     */
    public val colorMode: ColorMode

    /**
     * Creates the encoder of one sink.
     *
     * @param depth the colors the sink shows, as it resolved them from [colorMode].
     * @return a new encoder.
     * @since 1.0.0
     */
    public fun encoder(depth: ColorDepth): LogEncoder
}
