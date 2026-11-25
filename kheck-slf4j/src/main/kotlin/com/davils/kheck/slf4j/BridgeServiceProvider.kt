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

package com.davils.kheck.slf4j

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

/**
 * The provider SLF4J 2 finds through the service loader; an application never creates it, it attaches its logging
 * with [Slf4jBridge.attach].
 *
 * The MDC of SLF4J is the log context of kheck on the current thread, so `MDC.put` adds a field to every record of the
 * thread, and markers become markers of kheck.
 *
 * @since 1.0.0
 */
public class BridgeServiceProvider : SLF4JServiceProvider {
    private val loggers = BridgeLoggerFactory()
    private val markers = BasicMarkerFactory()
    private val mdc = BridgeMdcAdapter()

    /**
     * Returns the factory of the loggers that write into kheck.
     *
     * @return the factory.
     * @since 1.0.0
     */
    override fun getLoggerFactory(): ILoggerFactory = loggers

    /**
     * Returns the basic marker factory of SLF4J.
     *
     * @return the factory.
     * @since 1.0.0
     */
    override fun getMarkerFactory(): IMarkerFactory = markers

    /**
     * Returns the MDC adapter backed by the log context of kheck.
     *
     * @return the adapter.
     * @since 1.0.0
     */
    override fun getMDCAdapter(): MDCAdapter = mdc

    /**
     * Returns the version of SLF4J this provider implements.
     *
     * @return `2.0.99`.
     * @since 1.0.0
     */
    override fun getRequestedApiVersion(): String = API_VERSION

    /**
     * Does nothing: the provider is ready when it is created.
     *
     * @since 1.0.0
     */
    override fun initialize(): Unit = Unit

    private companion object {
        private const val API_VERSION: String = "2.0.99"
    }
}
