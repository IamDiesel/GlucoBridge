package de.glucobridge.app.export

import de.glucobridge.core.model.GlucoseReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest

/** Laedt Messwerte als Nightscout `entries` hoch (POST /api/v1/entries, api-secret = SHA-1(secret)). */
class NightscoutUploader(private val client: OkHttpClient = OkHttpClient()) {

    suspend fun upload(baseUrl: String, secret: String, readings: List<GlucoseReading>): Result<Int> =
        withContext(Dispatchers.IO) {
            if (readings.isEmpty()) return@withContext Result.success(0)
            runCatching {
                val url = baseUrl.trim().trimEnd('/') + "/api/v1/entries"
                val body = GlucoseExporters.nightscoutEntries(readings)
                    .toRequestBody("application/json".toMediaType())
                val builder = Request.Builder().url(url).post(body)
                if (secret.isNotBlank()) builder.addHeader("api-secret", sha1(secret))
                client.newCall(builder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    readings.size
                }
            }
        }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
