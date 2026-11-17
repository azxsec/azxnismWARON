package com.azxnism.waron.scanner

import android.util.Log
import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.scanner.modules.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanEngine @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "ScanEngine"
    }

    private val availableModules: Map<String, BaseScanModule> = mapOf(
        "header_security" to HeaderSecurityModule(),
        "ssl_tls" to SslTlsModule(),
        "cookie_security" to CookieSecurityModule(),
        "cors" to CorsModule(),
        "info_disclosure" to InfoDisclosureModule(),
        "redirect" to RedirectModule()
    )

    fun getAvailableModuleNames(): List<String> = availableModules.keys.toList()

    fun getModuleDescriptions(): Map<String, String> =
        availableModules.mapValues { (_, module) -> module.description }

    suspend fun startScan(
        targetUrl: String,
        modules: List<String>,
        onProgress: (completed: Int, total: Int) -> Unit,
        onFinding: (Finding) -> Unit
    ): List<Finding> = withContext(Dispatchers.IO) {
        val normalizedUrl = validateAndNormalizeUrl(targetUrl)

        val enabledModules = if (modules.isEmpty()) {
            availableModules.values.toList()
        } else {
            modules.mapNotNull { availableModules[it] }
        }

        if (enabledModules.isEmpty()) {
            throw IllegalArgumentException("No valid scan modules selected")
        }

        val totalModules = enabledModules.size
        val allFindings = mutableListOf<Finding>()
        var completedCount = 0

        onProgress(0, totalModules)

        for (module in enabledModules) {
            try {
                Log.d(TAG, "Running module: ${module.name}")
                val findings = module.scan(normalizedUrl, client)
                findings.forEach { finding ->
                    allFindings.add(finding)
                    onFinding(finding)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Module ${module.name} failed: ${e.message}", e)
                val errorFinding = Finding(
                    scanId = 0,
                    category = com.azxnism.waron.data.model.VulnCategory.INFO_DISCLOSURE,
                    severity = com.azxnism.waron.data.model.Severity.INFO,
                    title = "Module Error: ${module.name}",
                    description = "The ${module.name} module encountered an error during scanning: ${e.message}",
                    affectedUrl = normalizedUrl
                )
                allFindings.add(errorFinding)
                onFinding(errorFinding)
            } finally {
                completedCount++
                onProgress(completedCount, totalModules)
            }
        }

        Log.d(TAG, "Scan complete. Total findings: ${allFindings.size}")
        allFindings
    }

    private fun validateAndNormalizeUrl(url: String): String {
        val trimmed = url.trim()

        if (trimmed.isBlank()) {
            throw IllegalArgumentException("Target URL cannot be empty")
        }

        val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        return try {
            val parsed = URL(withScheme)
            if (parsed.host.isNullOrBlank()) {
                throw IllegalArgumentException("Invalid URL: missing host")
            }
            if (parsed.host.contains(" ")) {
                throw IllegalArgumentException("Invalid URL: host contains spaces")
            }
            withScheme.trimEnd('/')
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL format: ${e.message}")
        }
    }
}
