package com.google.antigravity.data.ipc

import com.google.antigravity.domain.model.QuotaBucket
import com.google.antigravity.domain.model.QuotaGroup
import com.google.antigravity.domain.model.UserQuotaSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object QuotaClient {
    private const val TAG = "QuotaClient"
    private const val QUOTA_ENDPOINT = "https://daily-cloudcode-pa.googleapis.com/v1internal:retrieveUserQuotaSummary"

    suspend fun fetchQuotaSummary(accessToken: String): UserQuotaSummary = withContext(Dispatchers.IO) {
        val cleanToken = accessToken.trim()
        if (cleanToken.isBlank()) {
            return@withContext UserQuotaSummary(syncError = "No active OAuth access token")
        }

        val url = URL(QUOTA_ENDPOINT)
        val conn = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $cleanToken")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "Antigravity-CLI")
            doOutput = true
        }

        try {
            conn.outputStream.use { os ->
                os.write("{}".toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code != 200) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                AppLogger.log(TAG, "Quota request failed ($code): $errorBody")
                return@withContext UserQuotaSummary(
                    syncError = "HTTP $code: ${errorBody.take(60)}"
                )
            }

            val body = conn.inputStream.bufferedReader().readText()
            AppLogger.log(TAG, "Quota response received: ${body.take(100)}...")
            val root = JSONObject(body)
            val groupsList = mutableListOf<QuotaGroup>()
            val groupsJson = root.optJSONArray("groups")
            if (groupsJson != null) {
                for (i in 0 until groupsJson.length()) {
                    val gObj = groupsJson.getJSONObject(i)
                    val gName = gObj.optString("displayName")
                    val gDesc = gObj.optString("description")
                    val bucketsList = mutableListOf<QuotaBucket>()
                    val bJson = gObj.optJSONArray("buckets")
                    if (bJson != null) {
                        for (j in 0 until bJson.length()) {
                            val bObj = bJson.getJSONObject(j)
                            bucketsList.add(
                                QuotaBucket(
                                    bucketId = bObj.optString("bucketId"),
                                    displayName = bObj.optString("displayName"),
                                    window = bObj.optString("window"),
                                    resetTime = bObj.optString("resetTime"),
                                    description = bObj.optString("description"),
                                    remainingFraction = bObj.optDouble("remainingFraction", 1.0)
                                )
                            )
                        }
                    }
                    groupsList.add(QuotaGroup(displayName = gName, description = gDesc, buckets = bucketsList))
                }
            }
            UserQuotaSummary(
                groups = groupsList,
                description = root.optString("description"),
                lastSyncTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            AppLogger.log(TAG, "Failed fetching quota: ${e.message}")
            UserQuotaSummary(syncError = e.localizedMessage)
        } finally {
            conn.disconnect()
        }
    }
}
