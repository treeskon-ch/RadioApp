package com.aom.radioapp.network

import com.aom.radioapp.model.RadioResponseModel
import com.aom.radioapp.repository.HostFallbackInterceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitService {
    @GET("json/stations/bycountry/thailand")
    fun getAllRadioStation(): Call<List<RadioResponseModel>> // ฟังก์ชันนี้จะเรียก API และรับข้อมูล
    //สร้าง object ของ Retrofit
    companion object {
        //aom25/7/68
        private val fallbackHosts = listOf(
            "https://de2.api.radio-browser.info",
            "https://de1.api.radio-browser.info",
            "https://de3.api.radio-browser.info"
        )

//        private val BASE_URL = "https://de2.api.radio-browser.info"

        private var retrofitService:RetrofitService?=null

        //ถ้า APT dev1 ใช้ไม่ได้ก็ให้ไปเรียนใช้ เรียน  dev2
        fun getInstance(): RetrofitService {
            if (retrofitService == null) {
                val client = OkHttpClient.Builder()
                    .addInterceptor(HostFallbackInterceptor(fallbackHosts))
                    .build()

                val retrofitBuilder = Retrofit.Builder()
                    .baseUrl(fallbackHosts[0]) // ใช้ host แรกเป็น default
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                retrofitService = retrofitBuilder.create(RetrofitService::class.java)
            }
            return retrofitService!!
        }

    }

}
