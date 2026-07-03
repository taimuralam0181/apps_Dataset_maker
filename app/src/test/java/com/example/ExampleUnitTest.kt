package com.example

import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    val client = OkHttpClient()
    
    // Try fetching openapi.json
    try {
      val request = Request.Builder()
        .url("https://prescription-api-a4d1.onrender.com/openapi.json")
        .build()
      client.newCall(request).execute().use { response ->
        println("--- OPENAPI.JSON RESPONSE ---")
        println("Code: ${response.code}")
        println(response.body?.string() ?: "Empty body")
      }
    } catch (e: Exception) {
      println("Failed to fetch openapi.json: ${e.message}")
    }

    // Try fetching general docs or root
    try {
      val request = Request.Builder()
        .url("https://prescription-api-a4d1.onrender.com/")
        .build()
      client.newCall(request).execute().use { response ->
        println("--- ROOT RESPONSE ---")
        println("Code: ${response.code}")
        println(response.body?.string()?.take(500) ?: "Empty body")
      }
    } catch (e: Exception) {
      println("Failed to fetch root: ${e.message}")
    }
  }
}
