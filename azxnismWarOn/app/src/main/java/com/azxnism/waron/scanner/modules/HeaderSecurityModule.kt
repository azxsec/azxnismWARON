package com.azxnism.waron.scanner.modules

import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.model.VulnCategory
import okhttp3.OkHttpClient

class HeaderSecurityModule : BaseScanModule() {

    override val name: String = "Header Security"
    override val description: String = "Checks for missing or misconfigured HTTP security headers"

    private data class HeaderCheck(
        val headerName: String,
        val category: VulnCategory,
        val severity: Severity,
        val title: String,
        val description: String,
        val remediation: String,
        val validator: ((String) -> Boolean)? = null,
        val misconfigTitle: String? = null,
        val misconfigDescription: ((String) -> String)? = null
    )

    private val headerChecks = listOf(
        HeaderCheck(
            headerName = "X-Frame-Options",
            category = VulnCategory.CLICKJACKING,
            severity = Severity.MEDIUM,
            title = "Missing X-Frame-Options Header",
            description = "The X-Frame-Options header is not set, leaving the application vulnerable to clickjacking attacks where an attacker can embed the page in an iframe.",
            remediation = "Set the X-Frame-Options header to 'DENY' or 'SAMEORIGIN' to prevent the page from being framed.",
            validator = { value ->
                val upper = value.uppercase()
                upper == "DENY" || upper == "SAMEORIGIN" || upper.startsWith("ALLOW-FROM")
            },
            misconfigTitle = "Misconfigured X-Frame-Options Header",
            misconfigDescription = { value ->
                "The X-Frame-Options header is set to '$value', which is not a recognized valid value."
            }
        ),
        HeaderCheck(
            headerName = "X-Content-Type-Options",
            category = VulnCategory.CONTENT_TYPE,
            severity = Severity.LOW,
            title = "Missing X-Content-Type-Options Header",
            description = "The X-Content-Type-Options header is not set. Browsers may MIME-sniff the content type, potentially executing malicious content.",
            remediation = "Set the X-Content-Type-Options header to 'nosniff'.",
            validator = { it.equals("nosniff", ignoreCase = true) },
            misconfigTitle = "Misconfigured X-Content-Type-Options Header",
            misconfigDescription = { value ->
                "X-Content-Type-Options is set to '$value' instead of 'nosniff'."
            }
        ),
        HeaderCheck(
            headerName = "X-XSS-Protection",
            category = VulnCategory.XSS,
            severity = Severity.LOW,
            title = "Missing X-XSS-Protection Header",
            description = "The X-XSS-Protection header is not set. While deprecated in modern browsers, it provides defense-in-depth for older browsers.",
            remediation = "Set X-XSS-Protection to '1; mode=block' or consider relying on Content-Security-Policy instead."
        ),
        HeaderCheck(
            headerName = "Strict-Transport-Security",
            category = VulnCategory.SSL_TLS,
            severity = Severity.MEDIUM,
            title = "Missing Strict-Transport-Security (HSTS) Header",
            description = "The HSTS header is not set, leaving users vulnerable to protocol downgrade attacks and cookie hijacking.",
            remediation = "Set Strict-Transport-Security with a max-age of at least 31536000 (1 year). Include 'includeSubDomains' and consider 'preload'.",
            validator = { value ->
                val maxAgeMatch = Regex("max-age=(\\d+)").find(value.lowercase())
                val maxAge = maxAgeMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
                maxAge >= 31536000
            },
            misconfigTitle = "Weak HSTS Configuration",
            misconfigDescription = { value ->
                "HSTS is set to '$value'. The max-age should be at least 31536000 seconds (1 year)."
            }
        ),
        HeaderCheck(
            headerName = "Content-Security-Policy",
            category = VulnCategory.XSS,
            severity = Severity.MEDIUM,
            title = "Missing Content-Security-Policy Header",
            description = "No Content-Security-Policy header is set. CSP is a critical defense against XSS, data injection, and other code injection attacks.",
            remediation = "Implement a strict Content-Security-Policy. Start with 'default-src 'self'' and add specific directives as needed. Avoid 'unsafe-inline' and 'unsafe-eval'.",
            validator = { value ->
                val lower = value.lowercase()
                !lower.contains("unsafe-inline") || !lower.contains("unsafe-eval")
            },
            misconfigTitle = "Weak Content-Security-Policy",
            misconfigDescription = { value ->
                "CSP contains potentially unsafe directives: '$value'. Avoid 'unsafe-inline' and 'unsafe-eval' where possible."
            }
        ),
        HeaderCheck(
            headerName = "Referrer-Policy",
            category = VulnCategory.HEADER_SECURITY,
            severity = Severity.LOW,
            title = "Missing Referrer-Policy Header",
            description = "The Referrer-Policy header is not set. The browser may send the full URL in the Referer header, potentially leaking sensitive information.",
            remediation = "Set Referrer-Policy to 'strict-origin-when-cross-origin' or 'no-referrer'."
        ),
        HeaderCheck(
            headerName = "Permissions-Policy",
            category = VulnCategory.HEADER_SECURITY,
            severity = Severity.LOW,
            title = "Missing Permissions-Policy Header",
            description = "The Permissions-Policy (formerly Feature-Policy) header is not set. This header controls which browser features and APIs can be used.",
            remediation = "Set a Permissions-Policy header to restrict access to sensitive browser features like camera, microphone, geolocation, etc."
        )
    )

    override suspend fun scan(targetUrl: String, client: OkHttpClient): List<Finding> {
        val findings = mutableListOf<Finding>()

        val response = makeRequest(client, targetUrl) ?: return listOf(
            buildFinding(
                category = VulnCategory.HEADER_SECURITY,
                severity = Severity.INFO,
                title = "Target Unreachable",
                description = "Could not connect to $targetUrl to check security headers.",
                affectedUrl = targetUrl
            )
        )

        response.use { resp ->
            for (check in headerChecks) {
                val headerValue = checkHeader(resp, check.headerName)

                if (headerValue == null) {
                    findings.add(
                        buildFinding(
                            category = check.category,
                            severity = check.severity,
                            title = check.title,
                            description = check.description,
                            evidence = "Header '${check.headerName}' is absent from the response.",
                            remediation = check.remediation,
                            affectedUrl = targetUrl
                        )
                    )
                } else if (check.validator != null && !check.validator.invoke(headerValue)) {
                    findings.add(
                        buildFinding(
                            category = check.category,
                            severity = check.severity,
                            title = check.misconfigTitle ?: check.title,
                            description = check.misconfigDescription?.invoke(headerValue) ?: check.description,
                            evidence = "${check.headerName}: $headerValue",
                            remediation = check.remediation,
                            affectedUrl = targetUrl
                        )
                    )
                }
            }
        }

        return findings
    }
}
