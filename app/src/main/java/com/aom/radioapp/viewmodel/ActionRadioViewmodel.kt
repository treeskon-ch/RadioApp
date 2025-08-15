package com.aom.radioapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aom.radioapp.model.RadioItem

class ActionRadioViewmodel : ViewModel(){
    //ใช้ในการเก็บ action เล่น หยุด เล่นสถานีถักไป เล่นสถานีก่อนหน้า
    private val _action = MutableLiveData<String>()
    val action: LiveData<String> get() = _action
    fun setAction(action:String) {
        _action.postValue(action)
    }

    //ใช้ในการเก็บข้อมูลรายการที่กำลังเล่น
    private val _stationPlaying = MutableLiveData<RadioItem>()
    val stationPlaying: LiveData<RadioItem> get() = _stationPlaying
    fun setStationPlaying(radioItem:RadioItem) {
        _stationPlaying.postValue(radioItem)
    }
    //ใช้Set เฉพาะข้อมมูล isPlay ใน  RadioItem
    fun setIsPlaying(isPlaying: Boolean) {
        val currentItem = _stationPlaying.value
        if (currentItem != null) {
            val updatedItem = currentItem.copy(isPlay = isPlaying)
            _stationPlaying.postValue(updatedItem)
        }
    }

}