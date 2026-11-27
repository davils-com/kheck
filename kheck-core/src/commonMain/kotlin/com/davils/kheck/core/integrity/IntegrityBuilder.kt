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

package com.davils.kheck.core.integrity

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.krypt.core.algorithm.MacAlgorithm
import com.davils.krypt.primitive.key.SecretKey

/**
 * Sets the integrity chain of a sink: every line the sink receives carries a [LogSeal] that chains it to the line
 * before, so a reader with the key can prove that no line was changed, removed, added or moved.
 *
 * The chain shows tampering; it prevents neither tampering nor loss. Lines a sink drops still leave a notice, which
 * the chain seals as well. Not safe for use from several threads; `integrity { }` uses it once.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class IntegrityBuilder internal constructor() {
    /**
     * The key of the chain, a secret key of krypt, which has at least 16 bytes; required. It stays the application's:
     * a destroyed key makes every later batch fail and go to the fallback of the sink.
     *
     * @since 1.0.0
     */
    public var key: SecretKey? = null

    /**
     * The algorithm of the chain; [MacAlgorithm.HmacSha256] by default, as kide wrote its chains.
     *
     * @since 1.0.0
     */
    public var algorithm: MacAlgorithm = MacAlgorithm.HmacSha256

    private var anchors: (suspend () -> LogSeal?)? = null

    /**
     * Sets where the chain goes on from when the sink opens, such as the last seal the sink stored before a restart;
     * without it, or when [block] returns `null`, the chain starts at [LogSeal.origin].
     *
     * @param block returns the seal to go on from; a failure is reported and the chain starts at the origin.
     * @since 1.0.0
     */
    public fun anchor(block: suspend () -> LogSeal?) {
        anchors = block
    }

    internal fun options(): IntegrityOptions = IntegrityOptions(key = key, algorithm = algorithm, anchor = anchors)
}
