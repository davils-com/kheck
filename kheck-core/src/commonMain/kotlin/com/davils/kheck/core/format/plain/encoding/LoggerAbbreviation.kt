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

package com.davils.kheck.core.format.plain.encoding

internal class LoggerAbbreviation(private val target: Int) {
    private val cache = HashMap<String, String>()

    fun abbreviated(name: String): String {
        val isShort = target == 0 || name.length <= target
        if (isShort) return name
        return cache[name] ?: remembered(name)
    }

    private fun remembered(name: String): String {
        if (cache.size >= MAX_CACHED) cache.clear()
        val result = shortened(name)
        cache[name] = result
        return result
    }

    private fun shortened(name: String): String {
        val parts = name.split(SEPARATOR)
        val result = parts.toMutableList()
        var length = name.length
        var index = 0
        while (length > target && index < parts.lastIndex) {
            val part = parts[index]
            if (part.length > 1) {
                length -= part.length - 1
                result[index] = part.take(1)
            }
            index++
        }
        return result.joinToString(SEPARATOR.toString())
    }

    private companion object {
        private const val SEPARATOR: Char = '.'
        private const val MAX_CACHED: Int = 4_096
    }
}
