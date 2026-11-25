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

import com.davils.kheck.core.mask.check.Iban
import com.davils.kheck.core.mask.check.Luhn

/**
 * The masking rules kheck ships: [SECRETS], active by default, and [PERSONAL_DATA], which an application adds when
 * its records may carry such data.
 *
 * @since 1.0.0
 */
public object MaskingRules {
    private const val EMAIL_KEPT: Int = 2

    /**
     * The names of fields whose value is always masked, compared in lower case without anything but letters and
     * digits, so `X-Api-Key` matches `xapikey`.
     *
     * @since 1.0.0
     */
    public val SECRET_KEYS: Set<String> = setOf(
        "password", "passwd", "pwd", "passphrase", "secret", "token", "accesstoken", "refreshtoken", "idtoken",
        "apikey", "xapikey", "apisecret", "clientsecret", "privatekey", "authorization", "proxyauthorization",
        "cookie", "setcookie", "sessionid", "credentials", "credential",
    )

    /**
     * The pattern of names of fields whose value is masked, run on the normalized name.
     *
     * @since 1.0.0
     */
    public val SECRET_KEY_PATTERN: Regex =
        Regex("(password|passwd|passphrase|secret|token|apikey|privatekey|credential)")

    /**
     * JSON Web Tokens.
     *
     * @since 1.0.0
     */
    public val JWT: MaskingRule = MaskingRule(
        name = "jwt",
        pattern = Regex("""eyJ[A-Za-z0-9_-]{5,}\.eyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{10,}"""),
        hints = listOf("eyJ"),
    )

    /**
     * The token of a `Bearer` authorization.
     *
     * @since 1.0.0
     */
    public val BEARER_TOKEN: MaskingRule = MaskingRule(
        name = "bearer-token",
        pattern = Regex("""\bbearer\s{1,8}(?<secret>[A-Za-z0-9._~+/=-]{8,})""", RegexOption.IGNORE_CASE),
        hints = listOf("bearer"),
    )

    /**
     * The credentials of a `Basic` authorization.
     *
     * @since 1.0.0
     */
    public val BASIC_AUTHENTICATION: MaskingRule = MaskingRule(
        name = "basic-authentication",
        pattern = Regex("""\bbasic\s{1,8}(?<secret>[A-Za-z0-9+/]{8,}={0,2})""", RegexOption.IGNORE_CASE),
        hints = listOf("basic"),
    )

    /**
     * The password in a URL such as `https://user:password@host`.
     *
     * @since 1.0.0
     */
    public val URL_CREDENTIALS: MaskingRule = MaskingRule(
        name = "url-credentials",
        pattern = Regex("""(?<=://)[^\s:/@]{1,256}:(?<secret>[^\s@/]{1,256})(?=@)"""),
        hints = listOf("://"),
    )

    /**
     * A private key in PEM.
     *
     * @since 1.0.0
     */
    public val PRIVATE_KEY: MaskingRule = MaskingRule(
        name = "private-key",
        pattern = Regex(
            """-----BEGIN [A-Z ]{0,32}PRIVATE KEY-----[\s\S]{1,16384}?-----END [A-Z ]{0,32}PRIVATE KEY-----""",
        ),
        hints = listOf("PRIVATE KEY"),
    )

    /**
     * An access key id of AWS.
     *
     * @since 1.0.0
     */
    public val AWS_ACCESS_KEY: MaskingRule = MaskingRule(
        name = "aws-access-key",
        pattern = Regex("""\b(?:AKIA|ASIA)[A-Z0-9]{16}\b"""),
        hints = listOf("AKIA", "ASIA"),
    )

    /**
     * A personal access token of GitLab.
     *
     * @since 1.0.0
     */
    public val GITLAB_TOKEN: MaskingRule = MaskingRule(
        name = "gitlab-token",
        pattern = Regex("""\bglpat-[A-Za-z0-9_-]{20,}"""),
        hints = listOf("glpat-"),
    )

    /**
     * A token of GitHub.
     *
     * @since 1.0.0
     */
    public val GITHUB_TOKEN: MaskingRule = MaskingRule(
        name = "github-token",
        pattern = Regex("""\b(?:(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{22,})"""),
        hints = listOf("ghp_", "gho_", "ghu_", "ghs_", "ghr_", "github_pat_"),
    )

    /**
     * The value of an assignment such as `password=…` or `"apiKey": "…"`.
     *
     * @since 1.0.0
     */
    public val SECRET_ASSIGNMENT: MaskingRule = MaskingRule(
        name = "secret-assignment",
        pattern = Regex(
            """\b(?:password|passwd|pwd|passphrase|secret|token|api[_-]?key|access[_-]?key|client[_-]?secret|""" +
                """private[_-]?key)["']?\s{0,3}[:=]\s{0,3}["']?(?<secret>[^\s"',;&}]{1,512})""",
            RegexOption.IGNORE_CASE,
        ),
        hints = listOf("pass", "pwd", "secret", "token", "key"),
    )

    /**
     * A payment card number that passes the Luhn check.
     *
     * @since 1.0.0
     */
    public val CARD_NUMBER: MaskingRule = MaskingRule(
        name = "card-number",
        pattern = Regex("""\b(?:\d[ -]?){12,18}\d\b"""),
        validator = Luhn,
    )

    /**
     * An IBAN that passes the check of ISO 13616.
     *
     * @since 1.0.0
     */
    public val IBAN: MaskingRule = MaskingRule(
        name = "iban",
        pattern = Regex("""\b[A-Z]{2}\d{2}(?: ?[A-Z0-9]){11,30}\b"""),
        validator = Iban,
    )

    /**
     * An email address, of which the first two characters stay visible.
     *
     * @since 1.0.0
     */
    public val EMAIL: MaskingRule = MaskingRule(
        name = "email",
        pattern = Regex("""\b[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\.[A-Za-z]{2,24}\b"""),
        mask = Mask.Partial(keepStart = EMAIL_KEPT, keepEnd = 0),
        hints = listOf("@"),
    )

    /**
     * The rules of secrets, active by default.
     *
     * @since 1.0.0
     */
    public val SECRETS: List<MaskingRule> = listOf(
        PRIVATE_KEY,
        JWT,
        BEARER_TOKEN,
        BASIC_AUTHENTICATION,
        URL_CREDENTIALS,
        AWS_ACCESS_KEY,
        GITLAB_TOKEN,
        GITHUB_TOKEN,
        SECRET_ASSIGNMENT,
    )

    /**
     * The rules of personal data: card numbers, IBANs and email addresses.
     *
     * @since 1.0.0
     */
    public val PERSONAL_DATA: List<MaskingRule> = listOf(CARD_NUMBER, IBAN, EMAIL)
}
