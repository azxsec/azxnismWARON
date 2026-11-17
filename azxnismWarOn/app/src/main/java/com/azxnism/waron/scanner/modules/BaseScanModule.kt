package com.azxnism.waron.scanner.modules

import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.model.VulnCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

abstract class BaseScanModule {

    abstract val name: String
    abstract val description: String

    abstract suspend fun scan(targetUrl: String, client: OkHttpClient): List<Finding>

    protected suspend fun makeRequest(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Response? {
        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(url).get()
                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }
                client.newCall(requestBuilder.build()).execute()
            } catch (e: Exception) {
                null
            }
        }
    }

    protected fun checkHeader(response: Response, headerName: String): String? {
        return response.header(headerName)
    }

    protected fun buildFinding(
        category: VulnCategory,
        severity: Severity,
        title: String,
        description: String,
        evidence: String = "",
        remediation: String = "",
        affectedUrl: String = "",
        request: String = "",
        responseSnippet: String = ""
    ): Finding {
        return Finding(
            scanId = 0,
            category = category,
            severity = severity,
            title = title,
            description = description,
            evidence = evidence,
            remediation = remediation,
            affectedUrl = affectedUrl,
            request = request,
            response = responseSnippet
        )
    }
}
