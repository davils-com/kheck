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

@file:OptIn(DavilsInternal::class)

package com.davils.kheck.slf4j

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.context.ContextAccess
import com.davils.kore.core.annotation.DavilsInternal
import org.slf4j.spi.MDCAdapter
import java.util.Deque
import java.util.LinkedList

internal class BridgeMdcAdapter : MDCAdapter {
    private val deques: ThreadLocal<MutableMap<String, ArrayDeque<String>>> = ThreadLocal.withInitial { mutableMapOf() }

    override fun put(key: String, value: String?) {
        ContextAccess.replace(ContextAccess.current() + (key to valueOf(value)))
    }

    override fun get(key: String): String? = textOf(ContextAccess.current()[key])

    override fun remove(key: String) {
        ContextAccess.replace(ContextAccess.current() - key)
    }

    override fun clear() {
        ContextAccess.replace(emptyMap())
        deques.get().clear()
    }

    override fun getCopyOfContextMap(): Map<String, String?> =
        ContextAccess.current().mapValues { (_, value) -> textOf(value) }

    override fun setContextMap(contextMap: Map<String, String?>) {
        ContextAccess.replace(contextMap.mapValues { (_, value) -> valueOf(value) })
    }

    override fun pushByKey(key: String, value: String) {
        val deque = deques.get().getOrPut(key) { ArrayDeque() }
        deque.addLast(value)
    }

    override fun popByKey(key: String): String? = deques.get()[key]?.removeLastOrNull()

    override fun getCopyOfDequeByKey(key: String): Deque<String>? {
        val deque = deques.get()[key] ?: return null
        return LinkedList(deque)
    }

    override fun clearDequeByKey(key: String) {
        deques.get()[key]?.clear()
    }

    private fun valueOf(value: String?): LogValue {
        if (value == null) return LogValue.Null
        return LogValue.Text(value)
    }

    private fun textOf(value: LogValue?): String? = (value as? LogValue.Text)?.value
}
