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

package com.davils.kheck.core.sample.engine

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.sample.Deduplication
import kotlin.time.TimeSource

internal class Deduplicator(
    private val deduplication: Deduplication,
    private val timeSource: TimeSource.WithComparableMarks,
) {
    private val entries = LinkedHashMap<DuplicateKey, DuplicateEntry>()

    private val summaries = mutableListOf<LogRecord>()

    fun isDuplicate(record: LogRecord): Boolean {
        val key = DuplicateKey(record.source.logger, record.level, record.message)
        val entry = entries[key]
        if (entry != null && entry.started.elapsedNow() < deduplication.window) {
            entries[key] = entry.copy(suppressed = entry.suppressed + 1)
            return true
        }
        if (entry != null) retire(key, entry)
        if (entries.size >= deduplication.maxEntries) evictOldest()
        entries[key] = DuplicateEntry(record, timeSource.markNow())
        return false
    }

    fun expired(): List<LogRecord> {
        val stale = entries.filterValues { entry -> entry.started.elapsedNow() >= deduplication.window }
        stale.forEach { (key, entry) -> retire(key, entry) }
        if (summaries.isEmpty()) return emptyList()
        val result = summaries.toList()
        summaries.clear()
        return result
    }

    private fun evictOldest() {
        val oldest = entries.entries.first()
        retire(oldest.key, oldest.value)
    }

    private fun retire(key: DuplicateKey, entry: DuplicateEntry) {
        entries.remove(key)
        if (entry.suppressed == 0L) return
        summaries += entry.first.copied(
            fields = entry.first.fields + (REPEATED to LogValue.Integer(entry.suppressed)),
        )
    }

    internal companion object {
        const val REPEATED: String = "repeated"
    }
}
