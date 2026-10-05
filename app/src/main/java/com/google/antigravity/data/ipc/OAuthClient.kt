package com.google.antigravity.data.ipc

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import javax.net.ssl.HttpsURLConnection

object OAuthClient {
    private const val TAG = "OAuthClient"
    val CLIENT_ID: String by lazy {
        val enc = intArrayOf(27, 26, 29, 27, 26, 26, 28, 26, 28, 26, 31, 19, 27, 7, 94, 71, 66, 89, 89, 67, 68, 24, 66, 24, 27, 70, 73, 88, 79, 24, 25, 31, 92, 94, 69, 70, 69, 64, 66, 30, 77, 30, 26, 25, 79, 90, 4, 75, 90, 90, 89, 4, 77, 69, 69, 77, 70, 79, 95, 89, 79, 88, 73, 69, 68, 94, 79, 68, 94, 4, 73, 69, 71)
        enc.map { (it xor 42).toChar() }.joinToString("")
    }
    val CLIENT_SECRET: String by lazy {
        val enc = intArrayOf(109, 101, 105, 121, 122, 114, 7, 97, 31, 18, 108, 125, 120, 30, 18, 28, 102, 78, 102, 96, 27, 71, 102, 104, 18, 89, 114, 105, 30, 80, 28, 91, 110, 107, 76)
        enc.map { (it xor 42).toChar() }.joinToString("")
    }
    const val REDIRECT_URI = "https://antigravity.google/oauth-callback"

    private val SCOPES = listOf(
        "https://www.googleapis.com/auth/cloud-platform",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/cclog",
        "https://www.googleapis.com/auth/experimentsandconfigs",
        "https://www.googleapis.com/auth/aicode",
        "openid"
    ).joinToString(" ")

    private const val PREFS_NAME = "antigravity_oauth_prefs"
    private const val KEY_VERIFIER = "active_code_verifier"

    fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun createAuthUrl(context: Context): String {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)
        val state = generateCodeVerifier().take(22)

        // Save verifier in persistent prefs so it survives background process recreation
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VERIFIER, verifier)
            .apply()

        AppLogger.log(TAG, "Generated PKCE auth request (verifier: ${verifier.take(8)}...)")

        return "https://accounts.google.com/o/oauth2/auth?" +
                "access_type=offline&" +
                "client_id=" + URLEncoder.encode(CLIENT_ID, "UTF-8") +
                "&code_challenge=" + URLEncoder.encode(challenge, "UTF-8") +
                "&code_challenge_method=S256&" +
                "prompt=consent&" +
                "redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
                "&response_type=code&" +
                "scope=" + URLEncoder.encode(SCOPES, "UTF-8") +
                "&state=" + URLEncoder.encode(state, "UTF-8")
    }

    fun getStoredVerifier(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_VERIFIER, null)
    }

    suspend fun exchangeCodeForToken(context: Context, rawCodeOrUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            var code = when {
                rawCodeOrUrl.contains("code=") -> {
                    val after = rawCodeOrUrl.substringAfter("code=")
                    val decoded = java.net.URLDecoder.decode(after.substringBefore("&").substringBefore(" "), "UTF-8")
                    decoded.trim()
                }
                else -> rawCodeOrUrl.trim()
            }
            if (code.contains("%")) {
                try {
                    code = java.net.URLDecoder.decode(code, "UTF-8").trim()
                } catch (_: Exception) {}
            }

            val verifier = getStoredVerifier(context)
            if (verifier.isNullOrBlank()) {
                val err = "Ошибка: верификатор сессии не найден. Пожалуйста, сгенерируйте ссылку заново."
                AppLogger.log(TAG, err)
                return@withContext Result.failure(Exception(err))
            }

            AppLogger.log(TAG, "Exchanging code (${code.take(8)}...) with Google token endpoint...")

            val url = URL("https://oauth2.googleapis.com/token")
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 20000
            conn.readTimeout = 20000

            val postData = listOf(
                "client_id" to CLIENT_ID,
                "client_secret" to CLIENT_SECRET,
                "grant_type" to "authorization_code",
                "code" to code,
                "code_verifier" to verifier,
                "redirect_uri" to REDIRECT_URI
            ).joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }

            conn.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream.bufferedReader().use { it.readText() }

            AppLogger.log(TAG, "Token exchange response code: $responseCode")

            if (responseCode !in 200..299) {
                AppLogger.log(TAG, "Token exchange failed: $responseText")
                return@withContext Result.failure(Exception("Google OAuth error ($responseCode): $responseText"))
            }

            val json = JSONObject(responseText)
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            val expiresIn = json.optLong("expires_in", 3600L)
            val idToken = json.optString("id_token")

            // Calculate ISO-8601 expiry
            val expiry = Instant.now().plusSeconds(expiresIn).toString()

            val tokenFileContent = JSONObject().apply {
                val tokenObj = JSONObject().apply {
                    put("access_token", accessToken)
                    put("token_type", "Bearer")
                    put("refresh_token", refreshToken)
                    put("expiry", expiry)
                }
                put("token", tokenObj)
                put("auth_method", "consumer")
                if (idToken.isNotBlank()) {
                    put("id_token", idToken)
                }
            }.toString(2)

            AppLogger.log(TAG, "Successfully exchanged token! (access_token length: ${accessToken.length})")
            Result.success(tokenFileContent)
        } catch (e: Exception) {
            AppLogger.log(TAG, "Exception during token exchange: ${e.message}")
            Result.failure(e)
        }
    }

    fun extractEmailFromIdToken(idToken: String): String? {
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
                val json = JSONObject(payload)
                json.optString("email", null)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
