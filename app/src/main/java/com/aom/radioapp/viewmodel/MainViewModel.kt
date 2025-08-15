package com.aom.radioapp.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aom.radioapp.model.RadioResponseModel
import com.aom.radioapp.repository.MainRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel constructor(private val repository: MainRepository):ViewModel(){
    val stationList = MutableLiveData<List<RadioResponseModel>>()
    val errorMessage = MutableLiveData<String>()
    //สร้างฟังชั่นดึงข้อมูลRadio
    fun getAllStation(){
        val response = repository.getAllRadioStation()
        response.enqueue(object :  Callback<List<RadioResponseModel>> {
            override fun onResponse(
                call: Call<List<RadioResponseModel>>,
                response: Response<List<RadioResponseModel>>
            ) {
                stationList.postValue(response.body())
            }
            override fun onFailure(call: Call<List<RadioResponseModel>>, t: Throwable) {
                errorMessage.postValue(t.message)
            }
        })
    }
}