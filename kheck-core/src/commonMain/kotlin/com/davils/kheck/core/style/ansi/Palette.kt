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

package com.davils.kheck.core.style.ansi

import com.davils.kheck.core.style.Color

internal object Palette {
    private const val CUBE_START: Int = 16
    private const val CUBE_SIDE: Int = 6
    private const val CUBE_PLANE: Int = 36
    private const val GRAY_START: Int = 232
    private const val GRAY_STEPS: Int = 24
    private const val GRAY_BASE: Int = 8
    private const val GRAY_STEP: Int = 10
    private const val CUBE_FIRST_LEVEL: Int = 55
    private const val CUBE_LEVEL_STEP: Int = 40

    private val basic: List<Color.Rgb> = listOf(
        Color.Rgb(red = 0, green = 0, blue = 0),
        Color.Rgb(red = 205, green = 0, blue = 0),
        Color.Rgb(red = 0, green = 205, blue = 0),
        Color.Rgb(red = 205, green = 205, blue = 0),
        Color.Rgb(red = 0, green = 0, blue = 238),
        Color.Rgb(red = 205, green = 0, blue = 205),
        Color.Rgb(red = 0, green = 205, blue = 205),
        Color.Rgb(red = 229, green = 229, blue = 229),
        Color.Rgb(red = 127, green = 127, blue = 127),
        Color.Rgb(red = 255, green = 0, blue = 0),
        Color.Rgb(red = 0, green = 255, blue = 0),
        Color.Rgb(red = 255, green = 255, blue = 0),
        Color.Rgb(red = 92, green = 92, blue = 255),
        Color.Rgb(red = 255, green = 0, blue = 255),
        Color.Rgb(red = 0, green = 255, blue = 255),
        Color.Rgb(red = 255, green = 255, blue = 255),
    )

    fun extendedRgb(index: Int): Color.Rgb {
        if (index >= GRAY_START) return gray(index)
        val offset = index - CUBE_START
        return Color.Rgb(
            red = cubeLevel(offset / CUBE_PLANE),
            green = cubeLevel(offset / CUBE_SIDE % CUBE_SIDE),
            blue = cubeLevel(offset % CUBE_SIDE),
        )
    }

    fun nearestBasic(color: Color.Rgb): Int = basic.indices.minBy { index ->
        distance(first = basic[index], second = color)
    }

    fun nearestIndexed(color: Color.Rgb): Int {
        val cube = CUBE_START +
            CUBE_PLANE *
            cubeIndex(color.red) +
            CUBE_SIDE *
            cubeIndex(color.green) +
            cubeIndex(color.blue)
        val gray = nearestGray(color)
        val grayDistance = distance(first = extendedRgb(gray), second = color)
        val cubeDistance = distance(first = extendedRgb(cube), second = color)
        if (grayDistance < cubeDistance) return gray
        return cube
    }

    private fun gray(index: Int): Color.Rgb {
        val level = GRAY_BASE + (index - GRAY_START) * GRAY_STEP
        return Color.Rgb(red = level, green = level, blue = level)
    }

    private fun nearestGray(color: Color.Rgb): Int {
        val average = (color.red + color.green + color.blue) / 3
        val step = ((average - GRAY_BASE) / GRAY_STEP).coerceIn(minimumValue = 0, maximumValue = GRAY_STEPS - 1)
        return GRAY_START + step
    }

    private fun cubeLevel(index: Int): Int {
        if (index == 0) return 0
        return CUBE_FIRST_LEVEL + index * CUBE_LEVEL_STEP
    }

    private fun cubeIndex(component: Int): Int = (0 until CUBE_SIDE).minBy { index ->
        val level = cubeLevel(index)
        (level - component) * (level - component)
    }

    private fun distance(first: Color.Rgb, second: Color.Rgb): Int {
        val red = first.red - second.red
        val green = first.green - second.green
        val blue = first.blue - second.blue
        return red * red + green * green + blue * blue
    }
}
