package com.azxnism.waron.scanner.modules

import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.model.VulnCategory
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class RedirectModule : BaseScanModule() {

    override val name: String = "Open Redirect"
    override val description: String = "Tests for open redirect vulnerabilities via common redirect parameters"

    private val redirectParams = listOf(
        "url", "redirect", "redirect_uri", "redirect_url",
        "next", "return", "returnTo", "return_url",
        "rurl", "dest", "destination", "redir",
        "target", "view", "continue", "go",
        "out", "forward", "to", "link"
    )

    private val evilTarget = "https://evil-attacker.com/phished"

    override suspend fun scan(targetUrl: String, client: OkHttpClient): List<Finding> {
        val findings = mutableListOf<Finding>()
        val baseUrl = targetUrl.trimEnd('/')

        // Build a non-redirect-following client to inspect the 3xx
        val noRedirectClient = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        for (param in redirectParams) {
            val testUrl = "$baseUrl/?${param}=${URLEncoder.encode(evilTarget, "UTF-8")}"

            val response = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url(testUrl).get().build()
                    noRedirectClient.newCall(request).execute()
                } catch (_: Exception) {
                    null
                }
            } ?: continue

            response.use { resp ->
                val code = resp.code
                val locationHeader = resp.header("Location")

                if (code in 300..399 && locationHeader != null) {
                    if (locationHeader.contains("evil-attacker.com", ignoreCase = true)) {
                        findings.add(
                            buildFinding(
                                category = VulnCategory.OPEN_REDIRECT,
                                severity = Severity.MEDIUM,
                                title = "Open Redirect via '$param' Parameter",
                                description = "The application redirects to an attacker-controlled URL when the '$param' parameter is set to an external domain. This can be used for phishing attacks by crafting a legitimate-looking URL that redirects to a malicious site.",
                                evidence = "Request: GET $testUrl\nResponse: HTTP $code\nLocation: $locationHeader",
                                remediation = "Validate redirect URLs against a whitelist of allowed domains. Use relative URLs for redirects. Never directly redirect to user-supplied URLs without validation.",
                                affectedUrl = testUrl,
                                request = "GET $testUrl",
                                responseSnippet = "HTTP $code\nLocation: $locationHeader"
                            )
                        )
                    }
                }

                // Also check for meta refresh or JavaScript-based redirects in the body
                if (code == 200) {
                    val body = try {
                        resp.body?.string()?.take(2000) ?: ""
                    } catch (_: Exception) {
                        ""
                    }

                    if (body.contains("evil-attacker.com", ignoreCase = true)) {
                        val hasMetaRefresh = body.contains("http-equiv=\"refresh\"", ignoreCase = true) ||
                                body.contains("http-equiv='refresh'", ignoreCase = true)
                        val hasJsRedirect = body.contains("window.location", ignoreCase = true) ||
                                body.contains("document.location", ignoreCase = true) ||
                                body.contains("location.href", ignoreCase = true)

                        if (hasMetaRefresh || hasJsRedirect) {
                            val redirectType = if (hasMetaRefresh) "meta refresh" else "JavaScript"
                            findings.add(
                                buildFinding(
                                    category = VulnCategory.OPEN_REDIRECT,
                                    severity = Severity.MEDIUM,
                                    title = "Open Redirect via '$param' ($redirectType)",
                                    description = "The application uses a $redirectType redirect to the attacker-controlled URL provided in the '$param' parameter.",
                                    evidence = "Request: GET $testUrl\nResponse body contains redirect to evil-attacker.com via $redirectType",
                                    remediation = "Validate redirect URLs against a whitelist. Do not reflect user-supplied URLs in redirect mechanisms.",
                                    affectedUrl = testUrl,
                                    request = "GET $testUrl",
                                    responseSnippet = body.take(500)
                                )
                            )
                        }
                    }
                }
            }
        }

        return findings
    }
}
