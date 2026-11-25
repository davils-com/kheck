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
 * Builds the style of a segment, such as
 * `segmentStyle { isBold = true; byLevel { error { foreground = Color.Red } } }`.
 *
 * @param block sets the base style and the styles of single levels.
 * @return the style.
 * @since 1.0.0
 */
public fun segmentStyle(block: SegmentStyleBuilder.() -> Unit): SegmentStyle {
    val builder = SegmentStyleBuilder()
    builder.block()
    return builder.segmentStyle()
}

/**
 * Builds a style of text, such as `textStyle { foreground = Color.Cyan }`.
 *
 * @param block sets the colors and attributes.
 * @return the style.
 * @since 1.0.0
 */
public fun textStyle(block: TextStyleBuilder.() -> Unit): TextStyle {
    val builder = TextStyleBuilder()
    builder.block()
    return builder.textStyle()
}
