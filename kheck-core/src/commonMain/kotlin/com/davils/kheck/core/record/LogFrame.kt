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

package com.davils.kheck.core.record

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * One frame of a stack: where a record was logged or where a throwable passed.
 *
 * The names come from the platform and are written escaped by every layout. On WebAssembly the platform reports no
 * frames, so records there carry none.
 *
 * @property className the qualified name of the class.
 * @property methodName the name of the method.
 * @property fileName the name of the source file, or `null` when the platform does not know it.
 * @property lineNumber the line in the source file, or [UNKNOWN_LINE].
 * @since 1.0.0
 */
public class LogFrame(
    public val className: String,
    public val methodName: String,
    public val fileName: String? = null,
    public val lineNumber: Int = UNKNOWN_LINE,
) {
    /**
     * Returns whether [other] is a frame of the same class, method, file and line.
     *
     * @param other the object to compare with.
     * @return `true` if both frames are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is LogFrame) return false
        val isSameMethod = className == other.className && methodName == other.methodName
        return isSameMethod && fileName == other.fileName && lineNumber == other.lineNumber
    }

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int {
        var result = className.hashCode()
        result = HASH_FACTOR * result + methodName.hashCode()
        result = HASH_FACTOR * result + (fileName?.hashCode() ?: 0)
        return HASH_FACTOR * result + lineNumber
    }

    /**
     * Shows the frame as a stack trace does, such as `com.example.Service.run(Service.kt:12)`, escaped.
     *
     * @return the frame as text.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = SafeText.text("$className.$methodName(${fileName ?: UNKNOWN}:$lineNumber)")

    /**
     * The constant for an unknown line.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val HASH_FACTOR: Int = 31
        private const val UNKNOWN: String = "unknown"

        /**
         * The line number of a frame whose line is not known.
         *
         * @since 1.0.0
         */
        public const val UNKNOWN_LINE: Int = -1
    }
}
