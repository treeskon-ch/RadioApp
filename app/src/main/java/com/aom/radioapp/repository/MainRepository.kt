package com.aom.radioapp.repository

import com.aom.radioapp.network.RetrofitService

class MainRepository constructor(private val retrofitService: RetrofitService) {
    //เก็บ Rest API Metthod จาก RetrofitService
    fun getAllRadioStation()  = retrofitService.getAllRadioStation()
}