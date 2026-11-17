package com.azxnism.waron.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.azxnism.waron.data.local.Converters

enum class Severity(val label: String, val weight: Int) {
    CRITICAL("Critical", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1),
    INFO("Informational", 0)
}

enum class ScanStatus {
    QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
}

enum class VulnCategory(val label: String, val description: String) {
    XSS("Cross-Site Scripting", "Reflected/Stored XSS via unescaped user input"),
    SQLI("SQL Injection", "Database query manipulation through user input"),
    CSRF("CSRF", "Missing cross-site request forgery protection"),
    SSRF("SSRF", "Server-side request forgery via manipulated URLs"),
    OPEN_REDIRECT("Open Redirect", "Unvalidated redirects to external domains"),
    HEADER_SECURITY("Security Headers", "Missing or misconfigured HTTP security headers"),
    SSL_TLS("SSL/TLS", "Certificate and encryption configuration issues"),
    INFO_DISCLOSURE("Information Disclosure", "Leaked server info, stack traces, or versions"),
    CORS("CORS Misconfiguration", "Overly permissive cross-origin resource sharing"),
    COOKIE_SECURITY("Cookie Security", "Missing Secure, HttpOnly, or SameSite flags"),
    DIRECTORY_LISTING("Directory Listing", "Exposed directory indexes"),
    CLICKJACKING("Clickjacking", "Missing frame protection headers"),
    CONTENT_TYPE("Content Sniffing", "Missing X-Content-Type-Options header")
}

@Entity(tableName = "scans")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetUrl: String,
    val status: ScanStatus = ScanStatus.QUEUED,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val totalChecks: Int = 0,
    val completedChecks: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val infoCount: Int = 0,
    @TypeConverters(Converters::class)
    val enabledModules: List<String> = emptyList(),
    val notes: String = ""
)

@Entity(tableName = "findings")
data class Finding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanId: Long,
    val category: VulnCategory,
    val severity: Severity,
    val title: String,
    val description: String,
    val evidence: String = "",
    val remediation: String = "",
    val affectedUrl: String = "",
    val request: String = "",
    val response: String = "",
    val foundAt: Long = System.currentTimeMillis()
)

data class ScanWithFindings(
    val scan: ScanRecord,
    val findings: List<Finding>
)

data class DashboardStats(
    val totalScans: Int = 0,
    val totalFindings: Int = 0,
    val criticalFindings: Int = 0,
    val highFindings: Int = 0,
    val mediumFindings: Int = 0,
    val lowFindings: Int = 0
)
