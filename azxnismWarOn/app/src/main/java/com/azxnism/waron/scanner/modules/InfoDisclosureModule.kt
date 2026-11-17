package com.azxnism.waron.scanner.modules

import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.model.VulnCategory
import okhttp3.OkHttpClient

class InfoDisclosureModule : BaseScanModule() {

    override val name: String = "Information Disclosure"
    override val description: String = "Detects information leakage through server headers, version banners, and exposed sensitive paths"

    private val sensitivePaths = listOf(
        PathProbe("/robots.txt", "Robots.txt found", "The robots.txt file is publicly accessible and may reveal hidden or sensitive paths.", Severity.INFO),
        PathProbe("/.git/HEAD", "Git Repository Exposed", "A .git directory is accessible, potentially allowing an attacker to download the entire source code history.", Severity.CRITICAL),
        PathProbe("/.env", "Environment File Exposed", "An .env file is accessible, potentially exposing database credentials, API keys, and other secrets.", Severity.CRITICAL),
        PathProbe("/.git/config", "Git Config Exposed", "The .git/config file is accessible, potentially revealing repository URLs and credentials.", Severity.HIGH),
        PathProbe("/wp-admin/", "WordPress Admin Panel Detected", "The WordPress admin login page is accessible.", Severity.INFO),
        PathProbe("/wp-login.php", "WordPress Login Detected", "The WordPress login page is accessible.", Severity.INFO),
        PathProbe("/.htaccess", "Htaccess File Exposed", "An .htaccess file is accessible, potentially revealing server configuration and rewrite rules.", Severity.MEDIUM),
        PathProbe("/phpinfo.php", "PHP Info Page Exposed", "A phpinfo() page is accessible, revealing detailed server configuration, PHP version, and extensions.", Severity.HIGH),
        PathProbe("/server-status", "Apache Server Status Exposed", "The Apache server-status page is accessible, revealing active connections, request details, and server information.", Severity.MEDIUM),
        PathProbe("/elmah.axd", "ELMAH Error Log Exposed", "The ELMAH error log is accessible, potentially revealing stack traces, source code, and sensitive application data.", Severity.HIGH),
        PathProbe("/api/swagger", "Swagger API Docs Exposed", "Swagger API documentation is publicly accessible, revealing API endpoints and data models.", Severity.LOW),
        PathProbe("/.DS_Store", "DS_Store File Exposed", "A macOS .DS_Store file is accessible, potentially revealing directory structure.", Severity.LOW)
    )

    private data class PathProbe(
        val path: String,
        val title: String,
        val description: String,
        val severity: Severity
    )

    override suspend fun scan(targetUrl: String, client: OkHttpClient): List<Finding> {
        val findings = mutableListOf<Finding>()

        // Check server response headers for info leakage
        val mainResponse = makeRequest(client, targetUrl)
        mainResponse?.use { resp ->
            checkServerHeader(resp.header("Server"), targetUrl, findings)
            checkPoweredByHeader(resp.header("X-Powered-By"), targetUrl, findings)
            checkAspNetVersion(resp.header("X-AspNet-Version"), targetUrl, findings)
            checkAspNetMvcVersion(resp.header("X-AspNetMvc-Version"), targetUrl, findings)
        }

        // Probe sensitive paths
        for (probe in sensitivePaths) {
            val probeUrl = "${targetUrl.trimEnd('/')}${probe.path}"
            val response = makeRequest(client, probeUrl)

            response?.use { resp ->
                val code = resp.code
                if (code == 200) {
                    val bodyPreview = try {
                        val body = resp.body?.string()
                        body?.take(500) ?: ""
                    } catch (_: Exception) {
                        ""
                    }

                    // Filter out generic 404 pages returned as 200
                    val isLikelyReal = when {
                        probe.path == "/robots.txt" && bodyPreview.contains("User-agent", ignoreCase = true) -> true
                        probe.path.contains(".git") && (bodyPreview.contains("ref:") || bodyPreview.contains("[core]")) -> true
                        probe.path == "/.env" && (bodyPreview.contains("=") && bodyPreview.lines().size > 1) -> true
                        probe.path == "/phpinfo.php" && bodyPreview.contains("phpinfo", ignoreCase = true) -> true
                        probe.path.contains("wp-") && bodyPreview.contains("wordpress", ignoreCase = true) -> true
                        probe.path == "/server-status" && bodyPreview.contains("Apache", ignoreCase = true) -> true
                        probe.path == "/elmah.axd" && bodyPreview.contains("error", ignoreCase = true) -> true
                        probe.path.contains("swagger") && bodyPreview.contains("swagger", ignoreCase = true) -> true
                        probe.path == "/.DS_Store" && bodyPreview.length > 4 -> true
                        probe.path == "/.htaccess" && bodyPreview.contains("Rewrite", ignoreCase = true) -> true
                        else -> false
                    }

                    if (isLikelyReal) {
                        findings.add(
                            buildFinding(
                                category = VulnCategory.INFO_DISCLOSURE,
                                severity = probe.severity,
                                title = probe.title,
                                description = probe.description,
                                evidence = "HTTP $code at $probeUrl\nBody preview: ${bodyPreview.take(200)}",
                                remediation = "Remove or restrict access to '${probe.path}'. Configure the web server to deny access to sensitive files and directories.",
                                affectedUrl = probeUrl
                            )
                        )
                    }
                }
            }
        }

        return findings
    }

    private fun checkServerHeader(value: String?, targetUrl: String, findings: MutableList<Finding>) {
        if (value.isNullOrBlank()) return

        findings.add(
            buildFinding(
                category = VulnCategory.INFO_DISCLOSURE,
                severity = Severity.LOW,
                title = "Server Version Disclosed",
                description = "The Server header reveals the web server software and potentially its version, aiding attackers in targeting known vulnerabilities.",
                evidence = "Server: $value",
                remediation = "Remove or genericize the Server header. For Apache, use 'ServerTokens Prod'. For Nginx, use 'server_tokens off'.",
                affectedUrl = targetUrl
            )
        )
    }

    private fun checkPoweredByHeader(value: String?, targetUrl: String, findings: MutableList<Finding>) {
        if (value.isNullOrBlank()) return

        findings.add(
            buildFinding(
                category = VulnCategory.INFO_DISCLOSURE,
                severity = Severity.LOW,
                title = "X-Powered-By Header Disclosed",
                description = "The X-Powered-By header reveals the application framework or technology stack, aiding attackers in fingerprinting the server.",
                evidence = "X-Powered-By: $value",
                remediation = "Remove the X-Powered-By header from server responses.",
                affectedUrl = targetUrl
            )
        )
    }

    private fun checkAspNetVersion(value: String?, targetUrl: String, findings: MutableList<Finding>) {
        if (value.isNullOrBlank()) return

        findings.add(
            buildFinding(
                category = VulnCategory.INFO_DISCLOSURE,
                severity = Severity.LOW,
                title = "ASP.NET Version Disclosed",
                description = "The X-AspNet-Version header reveals the ASP.NET framework version.",
                evidence = "X-AspNet-Version: $value",
                remediation = "Disable the X-AspNet-Version header in web.config: <httpRuntime enableVersionHeader=\"false\" />.",
                affectedUrl = targetUrl
            )
        )
    }

    private fun checkAspNetMvcVersion(value: String?, targetUrl: String, findings: MutableList<Finding>) {
        if (value.isNullOrBlank()) return

        findings.add(
            buildFinding(
                category = VulnCategory.INFO_DISCLOSURE,
                severity = Severity.LOW,
                title = "ASP.NET MVC Version Disclosed",
                description = "The X-AspNetMvc-Version header reveals the ASP.NET MVC framework version.",
                evidence = "X-AspNetMvc-Version: $value",
                remediation = "Remove the X-AspNetMvc-Version header by setting MvcHandler.DisableMvcResponseHeader = true in Application_Start.",
                affectedUrl = targetUrl
            )
        )
    }
}
