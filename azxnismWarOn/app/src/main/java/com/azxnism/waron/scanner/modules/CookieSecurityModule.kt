package com.azxnism.waron.scanner.modules

import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.model.VulnCategory
import okhttp3.OkHttpClient

class CookieSecurityModule : BaseScanModule() {

    override val name: String = "Cookie Security"
    override val description: String = "Analyzes Set-Cookie headers for missing Secure, HttpOnly, and SameSite flags"

    override suspend fun scan(targetUrl: String, client: OkHttpClient): List<Finding> {
        val findings = mutableListOf<Finding>()

        val response = makeRequest(client, targetUrl) ?: return listOf(
            buildFinding(
                category = VulnCategory.COOKIE_SECURITY,
                severity = Severity.INFO,
                title = "Target Unreachable",
                description = "Could not connect to $targetUrl to check cookie security.",
                affectedUrl = targetUrl
            )
        )

        response.use { resp ->
            val setCookieHeaders = resp.headers("Set-Cookie")

            if (setCookieHeaders.isEmpty()) {
                return findings
            }

            for (cookieHeader in setCookieHeaders) {
                val cookieName = cookieHeader.substringBefore("=").trim()
                val cookieLower = cookieHeader.lowercase()

                // Check Secure flag
                if (!cookieLower.contains("secure")) {
                    findings.add(
                        buildFinding(
                            category = VulnCategory.COOKIE_SECURITY,
                            severity = Severity.MEDIUM,
                            title = "Cookie Missing Secure Flag: $cookieName",
                            description = "The cookie '$cookieName' does not have the Secure flag set. It may be transmitted over unencrypted HTTP connections, allowing interception.",
                            evidence = "Set-Cookie: $cookieHeader",
                            remediation = "Add the 'Secure' flag to the cookie so it is only sent over HTTPS connections.",
                            affectedUrl = targetUrl
                        )
                    )
                }

                // Check HttpOnly flag
                if (!cookieLower.contains("httponly")) {
                    findings.add(
                        buildFinding(
                            category = VulnCategory.COOKIE_SECURITY,
                            severity = Severity.MEDIUM,
                            title = "Cookie Missing HttpOnly Flag: $cookieName",
                            description = "The cookie '$cookieName' does not have the HttpOnly flag set. It can be accessed by client-side JavaScript, increasing the impact of XSS attacks.",
                            evidence = "Set-Cookie: $cookieHeader",
                            remediation = "Add the 'HttpOnly' flag to the cookie to prevent JavaScript access.",
                            affectedUrl = targetUrl
                        )
                    )
                }

                // Check SameSite attribute
                if (!cookieLower.contains("samesite")) {
                    findings.add(
                        buildFinding(
                            category = VulnCategory.COOKIE_SECURITY,
                            severity = Severity.LOW,
                            title = "Cookie Missing SameSite Attribute: $cookieName",
                            description = "The cookie '$cookieName' does not have the SameSite attribute set. This may leave the application vulnerable to CSRF attacks.",
                            evidence = "Set-Cookie: $cookieHeader",
                            remediation = "Add the 'SameSite=Strict' or 'SameSite=Lax' attribute to the cookie.",
                            affectedUrl = targetUrl
                        )
                    )
                } else if (cookieLower.contains("samesite=none")) {
                    findings.add(
                        buildFinding(
                            category = VulnCategory.COOKIE_SECURITY,
                            severity = Severity.LOW,
                            title = "Cookie SameSite Set to None: $cookieName",
                            description = "The cookie '$cookieName' has SameSite=None, which allows the cookie to be sent in cross-site requests. This requires the Secure flag and may expose the application to CSRF.",
                            evidence = "Set-Cookie: $cookieHeader",
                            remediation = "If cross-site usage is not required, set SameSite to 'Lax' or 'Strict'.",
                            affectedUrl = targetUrl
                        )
                    )
                }
            }
        }

        return findings
    }
}
