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

package com.davils.kheck.core.mask

import com.davils.kheck.core.mask.fixture.CountingMasker
import com.davils.kheck.core.mask.fixture.masked
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val TOKEN_CHARACTER: String = "a"
private const val GITHUB_LENGTH: Int = 36
private const val GITLAB_LENGTH: Int = 21
private const val SIGNATURE_LENGTH: Int = 43
private val JWT_PARTS: List<String> =
    listOf("eyJhbGciOiJIUzI1NiJ9", "eyJzdWIiOiIxMjM0NTY3ODkwIn0", TOKEN_CHARACTER.repeat(SIGNATURE_LENGTH))
private val JWT: String = JWT_PARTS.joinToString(separator = ".")
private val GITHUB: String = "ghp_" + TOKEN_CHARACTER.repeat(GITHUB_LENGTH)
private val GITLAB: String = "glpat-" + TOKEN_CHARACTER.repeat(GITLAB_LENGTH)

class MaskingRulesTest : FunSpec({
    context("secrets") {
        test("masks a JSON web token") {
            masked("session $JWT expired") shouldBe "session **** expired"
        }

        test("masks only the credential of an authorization header") {
            masked("Authorization: Bearer abcdefgh12345678") shouldBe "Authorization: Bearer ****"
            masked("authorization: basic dXNlcjpwYXNzd29yZA==") shouldBe "authorization: basic ****"
        }

        test("masks the password inside a connection URL") {
            masked("jdbc:postgresql://kide:hunter2@db:5432/kide") shouldBe "jdbc:postgresql://kide:****@db:5432/kide"
        }

        test("masks a private key block") {
            masked("key -----BEGIN RSA PRIVATE KEY-----\nMIIEpAIB\n-----END RSA PRIVATE KEY----- done") shouldBe
                "key **** done"
        }

        test("masks cloud and forge tokens") {
            masked("AKIAIOSFODNN7EXAMPLE $GITLAB $GITHUB") shouldBe "**** **** ****"
        }

        test("masks the value of a secret assignment") {
            masked("password=hunter2; user=alice") shouldBe "password=****; user=alice"
            masked("""{"api_key": "0123456789"}""") shouldBe """{"api_key": "****"}"""
        }

        test("leaves text without secrets alone") {
            masked("loaded 3 servers from rack r1") shouldBe "loaded 3 servers from rack r1"
        }
    }

    context("personal data") {
        val personal: MaskingBuilder.() -> Unit = { rules(MaskingRules.PERSONAL_DATA) }

        test("masks card numbers that pass the Luhn check") {
            masked("card 4111 1111 1111 1111 used", personal) shouldBe "card **** used"
            masked("order 1234 5678 9012 3456 shipped", personal) shouldBe "order 1234 5678 9012 3456 shipped"
        }

        test("masks IBANs that pass the checksum") {
            masked("pay to DE89 3704 0044 0532 0130 00", personal) shouldBe "pay to ****"
            masked("pay to DE00 3704 0044 0532 0130 00", personal) shouldBe "pay to DE00 3704 0044 0532 0130 00"
        }

        test("keeps the start of an email address") {
            masked("mail alice@example.com", personal) shouldBe "mail al****"
        }
    }

    context("custom rules") {
        test("masks the secret group of a custom rule with its own mask") {
            val rule = MaskingRule(
                name = "community",
                pattern = Regex("community (?<secret>\\S+)"),
                mask = Mask.Fixed("<hidden>"),
                hints = listOf("community"),
            )

            masked("snmp community public") { rule(rule) } shouldBe "snmp community <hidden>"
        }

        test("skips a rule whose hint is missing") {
            val counting = CountingMasker {
                rule(
                    MaskingRule(name = "never", pattern = Regex("x"), hints = listOf("zzz")),
                )
            }

            counting.text("x") shouldBe "x"
            counting.rules shouldBe emptyList()
        }

        test("keeps a match its validator refuses") {
            val rule = MaskingRule(
                name = "digits",
                pattern = Regex("\\d+"),
                validator = { candidate -> candidate.length > 2 },
            )

            masked("1 22 333") { rule(rule) } shouldBe "1 22 ****"
        }

        test("counts every masked match by rule") {
            val counting = CountingMasker { }

            counting.text("password=a token=b")
            counting.rules shouldBe listOf("secret-assignment", "secret-assignment")
        }

        test("applies no rule when the defaults are excluded") {
            masked("password=hunter2") { isDefaultsIncluded = false } shouldBe "password=hunter2"
        }
    }
})
