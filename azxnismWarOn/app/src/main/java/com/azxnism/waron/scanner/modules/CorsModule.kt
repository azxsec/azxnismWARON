package com.azxnism.waron.scanner.modules

import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.model.VulnCategory
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class CorsModule : BaseScanModule() {

    override val name: String = "CORS"
    override val description: String = "Tests for overly permissive Cross-Origin Resource Sharing configurations"

    override suspend fun scan(targetUrl: String, client: OkHttpClient): List<Finding> {
        val findings = mutableListOf<Finding>()

        // Test 1: Send a request with a crafted evil Origin
        val evilOrigin = "https://evil-attacker.com"
        checkCorsWithOrigin(client, targetUrl, evilOrigin, findings)

        // Test 2: Check for null origin acceptance
        checkCorsWithOrigin(client, targetUrl, "null", findings)

        // Test 3: Check for subdomain-like bypass
        try {
            val host = URL(targetUrl).host
            val subdomainBypass = "https://$host.evil-attacker.com"
            checkCorsSubdomainBypass(client, targetUrl, subdomainBypass, host, findings)
        } catch (_: Exception) {
            // Skip this test if URL parsing fails
        }

        return findings
    }

    private suspend fun checkCorsWithOrigin(
        client: OkHttpClient,
        targetUrl: String,
        origin: String,
        findings: MutableList<Finding>
    ) {
        val response = makeRequest(client, targetUrl, mapOf("Origin" to origin)) ?: return

        response.use { resp ->
            val acao = checkHeader(resp, "Access-Control-Allow-Origin")
            val acac = checkHeader(resp, "Access-Control-Allow-Credentials")

            if (acao == "*") {
                findings.add(
                    buildFinding(
                        category = VulnCategory.CORS,
                        severity = Severity.MEDIUM,
                        title = "Wildcard CORS Policy",
                        description = "The server responds with 'Access-Control-Allow-Origin: *', allowing any website to read responses. If the application uses authentication, this can lead to data theft.",
                        evidence = "Access-Control-Allow-Origin: *\nTested Origin: $origin",
                        remediation = "Restrict CORS to specific trusted origins instead of using a wildcard. Never combine '*' with Access-Control-Allow-Credentials: true.",
                        affectedUrl = targetUrl
                    )
                )
            } else if (acao != null && acao.equals(origin, ignoreCase = true)) {
                val severity = if (acac?.equals("true", ignoreCase = true) == true) {
                    Severity.HIGH
                } else {
                    Severity.MEDIUM
                }

                val credentialNote = if (acac?.equals("true", ignoreCase = true) == true) {
                    " Additionally, Access-Control-Allow-Credentials is true, allowing the attacker to make authenticated requests and read responses."
                } else {
                    ""
                }

                findings.add(
                    buildFinding(
                        category = VulnCategory.CORS,
                        severity = severity,
                        title = "Origin Reflected in CORS Policy",
                        description = "The server reflects the Origin header value in Access-Control-Allow-Origin, allowing arbitrary origins to access responses.$credentialNote",
                        evidence = "Origin: $origin\nAccess-Control-Allow-Origin: $acao\nAccess-Control-Allow-Credentials: ${acac ?: "not set"}",
                        remediation = "Validate the Origin header against a whitelist of trusted domains. Do not blindly reflect the Origin.",
                        affectedUrl = targetUrl
                    )
                )
            }
        }
    }

    private suspend fun checkCorsSubdomainBypass(
        client: OkHttpClient,
        targetUrl: String,
        bypassOrigin: String,
        originalHost: String,
        findings: MutableList<Finding>
    ) {
        val response = makeRequest(client, targetUrl, mapOf("Origin" to bypassOrigin)) ?: return

        response.use { resp ->
            val acao = checkHeader(resp, "Access-Control-Allow-Origin")

            if (acao != null && acao.equals(bypassOrigin, ignoreCase = true)) {
                findings.add(
                    buildFinding(
                        category = VulnCategory.CORS,
                        severity = Severity.MEDIUM,
                        title = "CORS Origin Validation Bypass via Subdomain",
                        description = "The server accepted '$bypassOrigin' as a valid origin, suggesting it uses prefix/suffix matching instead of exact domain comparison. An attacker can register a domain like '$originalHost.evil-attacker.com' to bypass the CORS policy.",
                        evidence = "Origin: $bypassOrigin\nAccess-Control-Allow-Origin: $acao",
                        remediation = "Use exact string matching for allowed origins, not prefix or suffix checks.",
                        affectedUrl = targetUrl
                    )
                )
            }
        }
    }
}
