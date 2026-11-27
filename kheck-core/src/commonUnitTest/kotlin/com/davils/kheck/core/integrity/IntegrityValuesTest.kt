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

import com.davils.kheck.core.mask.fixture.secretKey
import com.davils.krypt.core.algorithm.MacAlgorithm
import com.davils.krypt.primitive.key.SecretKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.io.bytestring.ByteString

private const val KEY: String = "value-key-0123456789"
private const val SECRET_TEXT: String = "secret\n"
private val MAC: ByteString = ByteString(byteArrayOf(0x0a, 0x7f))

class IntegrityValuesTest : FunSpec({
    context("seals") {
        test("compare by sequence and MAC and show the MAC in hexadecimal digits") {
            val seal = LogSeal(sequence = 1L, mac = MAC)

            seal shouldBe LogSeal(sequence = 1L, mac = MAC)
            seal.hashCode() shouldBe LogSeal(sequence = 1L, mac = MAC).hashCode()
            seal shouldNotBe LogSeal(sequence = 2L, mac = seal.mac)
            seal.toString() shouldBe "LogSeal(sequence=1, mac=0a7f)"
            LogSeal.origin(MacAlgorithm.HmacSha384).mac.size shouldBe MacAlgorithm.HmacSha384.tagBytes
        }

        test("show sealed texts without their text") {
            val seal = LogSeal(sequence = 1L, mac = ByteString(byteArrayOf(1)))
            val sealed = SealedText(text = SECRET_TEXT, seal = seal)

            sealed shouldBe SealedText(text = SECRET_TEXT, seal = seal)
            sealed.hashCode() shouldBe SealedText(text = SECRET_TEXT, seal = seal).hashCode()
            sealed shouldNotBe SealedText(text = "other\n", seal = seal)
            sealed.toString() shouldNotContain "secret"
        }
    }

    context("reports") {
        test("compare by value") {
            val origin = LogSeal.origin(MacAlgorithm.HmacSha256)
            val intact = IntegrityReport.Intact(records = 1L, anchor = origin, last = origin)

            intact shouldBe IntegrityReport.Intact(records = 1L, anchor = origin, last = origin)
            intact.hashCode() shouldBe IntegrityReport.Intact(records = 1L, anchor = origin, last = origin).hashCode()
            intact.equals(IntegrityReport.Tampered(1L)) shouldBe false
            IntegrityReport.Tampered(1L).hashCode() shouldBe IntegrityReport.Tampered(1L).hashCode()
            IntegrityReport.Tampered(1L).equals(IntegrityReport.Incomplete(1L)) shouldBe false
            IntegrityReport.Incomplete(1L).hashCode() shouldBe IntegrityReport.Incomplete(1L).hashCode()
            IntegrityReport.Incomplete(1L).equals(IntegrityReport.Malformed(1)) shouldBe false
            IntegrityReport.Malformed(1).hashCode() shouldBe IntegrityReport.Malformed(1).hashCode()
            IntegrityReport.Malformed(1).equals(intact) shouldBe false
            val reports = listOf(
                intact,
                IntegrityReport.Tampered(1L),
                IntegrityReport.Incomplete(1L),
                IntegrityReport.Malformed(1),
            )

            reports.map(IntegrityReport::toString) shouldBe listOf(
                "Intact(records=1, anchor=$origin, last=$origin)",
                "Tampered(sequence=1)",
                "Incomplete(records=1)",
                "Malformed(line=1)",
            )
        }
    }

    context("options") {
        test("compare by key, algorithm and anchor and never show the key") {
            val key = secretKey(KEY)
            val options = optionsOf(key)
            val same = optionsOf(key)

            options shouldBe same
            options.hashCode() shouldBe same.hashCode()
            options shouldNotBe IntegrityBuilder().options()
            options.toString() shouldBe "IntegrityOptions(algorithm=HmacSha256, isKeyed=true)"
            options.toString() shouldNotContain KEY
        }
    }

    context("foreign values") {
        test("are never equal to a value of another type") {
            val seal = LogSeal(sequence = 1L, mac = MAC)

            seal.equals(KEY) shouldBe false
            SealedText(text = SECRET_TEXT, seal = seal).equals(seal) shouldBe false
            optionsOf(secretKey(KEY)).equals(seal) shouldBe false
            IntegrityReport.Malformed(1).hashCode() shouldBe 1
        }
    }
})

private fun optionsOf(key: SecretKey): IntegrityOptions {
    val builder = IntegrityBuilder()
    builder.key = key
    return builder.options()
}
