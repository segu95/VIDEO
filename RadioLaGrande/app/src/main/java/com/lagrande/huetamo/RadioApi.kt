package com.lagrande.huetamo

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object RadioApi {
    fun fetchConfig(endpoint: String): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
        return try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            JSONObject(body)
        } finally { connection.disconnect() }
    }
}
