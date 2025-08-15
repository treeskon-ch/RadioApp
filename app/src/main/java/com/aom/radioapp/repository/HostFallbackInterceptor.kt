package com.aom.radioapp.repository

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class HostFallbackInterceptor(private val fallbackUrls: List<String>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var lastException: Exception? = null
        var lastResponse: Response? = null

        for (baseUrl in fallbackUrls) {
            val newUrl = originalRequest.url.newBuilder()
                .host(baseUrl.toHttpUrlOrNull()!!.host)
                .scheme(baseUrl.toHttpUrlOrNull()!!.scheme)
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()

            try {
                val response = chain.proceed(newRequest)

                // ถ้าสำเร็จ → ปิด response ก่อนหน้าแล้ว return response นี้
                if (response.isSuccessful) {
                    lastResponse?.close() // ปิด response ก่อนหน้า (ถ้ามี)
                    return response
                } else {
                    // ปิด response ที่ไม่สำเร็จ
                    response.close()
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        lastResponse?.close() // ปิด response สุดท้ายก่อน throw
        throw lastException ?: IOException("All fallback URLs failed")
    }
}
