package com.azxnism.waron.scanner.modules

import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.model.VulnCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import java.net.URL

class SslTlsModule : BaseScanModule() {

    override val name: String = "SSL/TLS"
    override val description: String = "Checks SSL/TLS certificate validity, expiration, chain trust, and protocol versions"

    override suspend fun scan(targetUrl: String, client: OkHttpClient): List<Finding> {
        val findings = mutableListOf<Finding>()

        if (!targetUrl.startsWith("https://")) {
            findings.add(
                buildFinding(
                    category = VulnCategory.SSL_TLS,
                    severity = Severity.HIGH,
                    title = "No HTTPS Detected",
                    description = "The target URL uses HTTP instead of HTTPS. All communications are transmitted in plaintext and can be intercepted.",
                    evidence = "URL scheme: ${targetUrl.substringBefore("://")}",
                    remediation = "Enable HTTPS with a valid TLS certificate. Redirect all HTTP traffic to HTTPS.",
                    affectedUrl = targetUrl
                )
            )
            return findings
        }

        withContext(Dispatchers.IO) {
            checkCertificate(targetUrl, findings)
            checkProtocolVersions(targetUrl, findings)
        }

        return findings
    }

    private fun checkCertificate(targetUrl: String, findings: MutableList<Finding>) {
        try {
            val url = URL(targetUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000

            try {
                connection.connect()
                val certs = connection.serverCertificates

                if (certs.isNotEmpty() && certs[0] is X509Certificate) {
                    val cert = certs[0] as X509Certificate
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
                    val now = Date()

                    // Check expiration
                    val daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(
                        cert.notAfter.time - now.time
                    )

                    if (now.after(cert.notAfter)) {
                        findings.add(
                            buildFinding(
                                category = VulnCategory.SSL_TLS,
                                severity = Severity.CRITICAL,
                                title = "SSL Certificate Expired",
                                description = "The SSL certificate has expired. Users will see security warnings and connections are not properly secured.",
                                evidence = "Expired on: ${dateFormat.format(cert.notAfter)}\nSubject: ${cert.subjectDN}",
                                remediation = "Renew the SSL certificate immediately.",
                                affectedUrl = targetUrl
                            )
                        )
                    } else if (daysUntilExpiry <= 30) {
                        findings.add(
                            buildFinding(
                                category = VulnCategory.SSL_TLS,
                                severity = Severity.MEDIUM,
                                title = "SSL Certificate Expiring Soon",
                                description = "The SSL certificate will expire in $daysUntilExpiry days.",
                                evidence = "Expires on: ${dateFormat.format(cert.notAfter)}\nSubject: ${cert.subjectDN}",
                                remediation = "Renew the SSL certificate before it expires.",
                                affectedUrl = targetUrl
                            )
                        )
                    }

                    // Check if not yet valid
                    if (now.before(cert.notBefore)) {
                        findings.add(
                            buildFinding(
                                category = VulnCategory.SSL_TLS,
                                severity = Severity.HIGH,
                                title = "SSL Certificate Not Yet Valid",
                                description = "The SSL certificate is not valid yet.",
                                evidence = "Valid from: ${dateFormat.format(cert.notBefore)}\nSubject: ${cert.subjectDN}",
                                remediation = "Check the certificate dates and ensure the server clock is correct.",
                                affectedUrl = targetUrl
                            )
                        )
                    }

                    // Check self-signed
                    try {
                        cert.verify(cert.publicKey)
                        if (cert.subjectDN == cert.issuerDN) {
                            findings.add(
                                buildFinding(
                                    category = VulnCategory.SSL_TLS,
                                    severity = Severity.HIGH,
                                    title = "Self-Signed Certificate Detected",
                                    description = "The certificate appears to be self-signed. Browsers will show security warnings and the certificate chain cannot be verified.",
                                    evidence = "Subject: ${cert.subjectDN}\nIssuer: ${cert.issuerDN}",
                                    remediation = "Use a certificate from a trusted Certificate Authority (e.g., Let's Encrypt).",
                                    affectedUrl = targetUrl
                                )
                            )
                        }
                    } catch (_: Exception) {
                        // Not self-signed — good
                    }

                    // Certificate chain length
                    if (certs.size < 2) {
                        findings.add(
                            buildFinding(
                                category = VulnCategory.SSL_TLS,
                                severity = Severity.LOW,
                                title = "Incomplete Certificate Chain",
                                description = "The server does not appear to send intermediate certificates. Some clients may fail to validate the chain.",
                                evidence = "Only ${certs.size} certificate(s) in the chain.",
                                remediation = "Configure the server to send the full certificate chain including intermediate certificates.",
                                affectedUrl = targetUrl
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: javax.net.ssl.SSLHandshakeException) {
            findings.add(
                buildFinding(
                    category = VulnCategory.SSL_TLS,
                    severity = Severity.HIGH,
                    title = "SSL Handshake Failed",
                    description = "The SSL/TLS handshake failed, indicating a certificate trust issue.",
                    evidence = "Error: ${e.message}",
                    remediation = "Ensure the server uses a valid certificate from a trusted CA and serves the full chain.",
                    affectedUrl = targetUrl
                )
            )
        } catch (e: Exception) {
            findings.add(
                buildFinding(
                    category = VulnCategory.SSL_TLS,
                    severity = Severity.INFO,
                    title = "SSL Certificate Check Error",
                    description = "Could not fully analyze the SSL certificate: ${e.message}",
                    affectedUrl = targetUrl
                )
            )
        }
    }

    private fun checkProtocolVersions(targetUrl: String, findings: MutableList<Finding>) {
        val host = try {
            URL(targetUrl).host
        } catch (_: Exception) {
            return
        }
        val port = try {
            val p = URL(targetUrl).port
            if (p == -1) 443 else p
        } catch (_: Exception) {
            443
        }

        val deprecatedProtocols = listOf("TLSv1", "TLSv1.1", "SSLv3")

        for (protocol in deprecatedProtocols) {
            try {
                val sslContext = SSLContext.getInstance(protocol)
                sslContext.init(null, null, null)
                val factory: SSLSocketFactory = sslContext.socketFactory
                val socket = factory.createSocket(host, port) as SSLSocket
                socket.soTimeout = 10_000
                socket.enabledProtocols = arrayOf(protocol)

                try {
                    socket.startHandshake()
                    // If handshake succeeds, the deprecated protocol is supported
                    findings.add(
                        buildFinding(
                            category = VulnCategory.SSL_TLS,
                            severity = if (protocol == "SSLv3") Severity.CRITICAL else Severity.HIGH,
                            title = "Deprecated Protocol Supported: $protocol",
                            description = "The server supports $protocol, which is deprecated and has known vulnerabilities (e.g., POODLE, BEAST).",
                            evidence = "Successfully completed handshake using $protocol",
                            remediation = "Disable $protocol on the server. Only allow TLS 1.2 and TLS 1.3.",
                            affectedUrl = targetUrl
                        )
                    )
                } catch (_: Exception) {
                    // Protocol not supported — good
                } finally {
                    socket.close()
                }
            } catch (_: Exception) {
                // Protocol not available in this runtime — skip
            }
        }
    }
}
