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

package com.davils.kheck.core.mask.engine

import com.davils.kheck.core.mask.MaskingKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val BYTE_MASK: Int = 0xFF
private const val HEX_RADIX: Int = 16
private const val LONG_KEY: Int = 131
private const val KEY_BYTE: Int = 0x0B
private const val LONG_KEY_BYTE: Int = 0xAA
private const val KEY_LENGTH: Int = 20

private fun ByteArray.hex(): String = joinToString(separator = "") { byte ->
    (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
}

class HmacSha256Test : FunSpec({
    context("SHA-256 of FIPS 180-4") {
        test("digests the empty message, abc and a message of two blocks") {
            Sha256.digest(ByteArray(0)).hex() shouldBe
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            Sha256.digest("abc".encodeToByteArray()).hex() shouldBe
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
            Sha256.digest("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()).hex() shouldBe
                "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1"
        }
    }

    context("HMAC-SHA-256 of RFC 4231") {
        test("signs with a short key") {
            val key = ByteArray(KEY_LENGTH) { KEY_BYTE.toByte() }

            HmacSha256.mac(key = key, message = "Hi There".encodeToByteArray()).hex() shouldBe
                "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7"
        }

        test("hashes a key longer than a block first") {
            val key = ByteArray(LONG_KEY) { LONG_KEY_BYTE.toByte() }
            val message = "Test Using Larger Than Block-Size Key - Hash Key First".encodeToByteArray()

            HmacSha256.mac(key = key, message = message).hex() shouldBe
                "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54"
        }
    }

    context("the hasher") {
        test("masks nothing once its key is destroyed") {
            val key = MaskingKey.of("a-key-of-16-bytes")
            val hasher = Hasher(key)

            hasher.hash("value")?.startsWith("hash:") shouldBe true
            key.destroy()
            hasher.hash("value") shouldBe null
        }
    }
})
