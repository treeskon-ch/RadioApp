package com.aom.radioapp.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aom.radioapp.repository.MainRepository
import com.aom.radioapp.viewmodel.MainViewModel

//เอาไว้เช็ค MainViewModel ว่าทำงานปกติหรือไหม เช่นตัว MainViewModel มีปัญหาในการเรียก response.enqueue แสเงว่า API ไม่ได้มีปัญหา แต่มีปัญหาตรงเรียกข้อมูลมาใช้
class MainViewModelFactory constructor(private val repository: MainRepository):ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        //ถ้าสามารถเรียกใช้งาน MainViewModel ได้ ให้ return MainViewModel
        return if(modelClass.isAssignableFrom(MainViewModel::class.java)){
            MainViewModel(this.repository) as T
        }else{
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}