package com.aom.radioapp.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aom.radioapp.R
import com.aom.radioapp.adapters.RadioAdapter
import com.aom.radioapp.databinding.ActivityMainBinding
import com.aom.radioapp.model.RadioResponseModel
import com.aom.radioapp.network.RetrofitService
import com.aom.radioapp.repository.MainRepository
import com.aom.radioapp.service.NetworkLiveData
import com.aom.radioapp.service.RadioActionService
import com.aom.radioapp.service.RadioService
import com.aom.radioapp.viewmodel.ActionRadioViewmodel
import com.aom.radioapp.viewmodel.MainViewModel
import com.aom.radioapp.viewmodelfactory.MainViewModelFactory

class MainActivity : AppCompatActivity() {
    //สร้างตัวแปรสำหรับ binding
    private lateinit var binding: ActivityMainBinding

    //สร้างตัวแปรสำหรับตรวจสอบ net
    private lateinit var networkLiveData: NetworkLiveData
    //สร้างตัวแปรสำหรับเรียนใช้ ViewModel
    private lateinit var viewModel: MainViewModel
    //สร้างตัวแปรสำหรับเรียกใช้ RetrofitService
    private lateinit var retrofitService: RetrofitService

    //สร้างตัวแปรเรียกใช้ Adapter
    private lateinit var  adapter : RadioAdapter
    //สร้างตัวแปลสำหรับใช้เปลี่ยนสีพื้นหลังของปุ่มเมนูรายการทั้งหมดหรือรายการโปรด
    private var activeCard = false

    //สร้างตัวแปรสำหรับใช้ ActionRadioViewmodel
    private lateinit var actionRadioViewmodel: ActionRadioViewmodel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // สร้าง instance ของ binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        // ตั้งค่า view หลักของ Activity
        setContentView(binding.root)

        networkLiveData = NetworkLiveData(this@MainActivity) //สิบทอดออปเจ็ค
        retrofitService = RetrofitService.getInstance()
        adapter = RadioAdapter(this)
        fnInitCheckNetWork()

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(MainRepository(retrofitService))
        ).get(MainViewModel::class.java)

        fnEventListener()
        actionObserve() //สั่งเกตุการเปลี่ยน action

    }

    override fun onStart() {
        super.onStart()
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.recyclerViewRadio.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewRadio.adapter = adapter
        //สังเกตการเปลี่ยนแปลงของ stationList
        try {
            viewModel.stationList.observe(this, Observer { it ->
                if (::adapter.isInitialized && it != null){
                    updateCardSelection(activeCard)
                    adapter.setStationList(it) // อัปเดตข้อมูลใน Adapter
                    adapter.setPlayingStation(it)

                    // ตรวจสอบว่า station ไหนกำลังเล่น → สั่งเล่น (ถ้ายังไม่ได้เล่นจริง)
                    val prefs = this.getSharedPreferences("lastPlayed", MODE_PRIVATE)
                    val playingName = prefs.all.keys.firstOrNull()
                    val currentStation = it.firstOrNull { it.name == playingName }
                    if (currentStation != null) {
                        val serviceIntent = Intent(this, RadioService::class.java).apply {
                            putExtra("STATION_URL", currentStation.url)
                            putExtra("STATION_NAME", currentStation.name)
                            putExtra("STATION_LOGO", currentStation.favicon)
                            putExtra("STATION_FAVORITE", currentStation.favorite)
                            putExtra("STATION_ISPLAY", currentStation.isPlay)
                            action = "PLAY"
                        }
                        ContextCompat.startForegroundService(this, serviceIntent)
                    }
                }
            })
        }catch (e:Exception){
            println("Error:$e")
        }


        //Error
        viewModel.errorMessage.observe(this,Observer{it->
            Log.d("Message","Error Call View Model")
        })

        viewModel.getAllStation()
    }

    private fun fnInitCheckNetWork() {
        networkLiveData.observe(this) { isConnected ->
            if (isConnected) {
                this.stopService(Intent(this, RadioActionService::class.java))
                // fnInitRadioList()
                binding.recyclerViewRadioTextHasWarning.visibility = View.GONE
                binding.linearLayoutMainBoxRadio.visibility = View.VISIBLE

            } else {
                binding.recyclerViewRadioTextHasWarning.visibility = View.VISIBLE
                binding.linearLayoutMainBoxRadio.visibility = View.GONE
                this.stopService(Intent(this, RadioActionService::class.java))
            }
        }
    }

    private fun fnEventListener() {
        binding.linearLayoutLoadingBoxRadio.visibility = View.GONE

        binding.cardViewAllList.setOnClickListener {
            activeCard = false
            updateCardSelection(activeCard)
            viewModel.stationList.value?.let {
                adapter.setStationList(it)
            }
        }

        binding.cardViewFavList.setOnClickListener {
            activeCard = true
            updateCardSelection(activeCard)
            viewModel.stationList.value?.let {
                val prefs = this.getSharedPreferences("favorites", Context.MODE_PRIVATE)
                val favoriteStations = it.filter { station ->
                    prefs.getBoolean(station.name, false)
                }
                adapter.setStationList(favoriteStations)
            }
        }
    }

    // True เลือกรายการทั้งหมด False รายการโปรด
    fun updateCardSelection(isAllSelected: Boolean) {
        val selectedColor = ContextCompat.getColor(this, R.color.transparent)
        val defaultColor = ContextCompat.getColor(this, R.color.toggleListFav)

        binding.cardViewAllList.setCardBackgroundColor(
            if (isAllSelected) selectedColor else defaultColor
        )
        binding.cardViewFavList.setCardBackgroundColor(
            if (!isAllSelected) selectedColor else defaultColor
        )
    }

    fun actionObserve(){
        actionRadioViewmodel = ViewModelProvider(this)[ActionRadioViewmodel::class.java]
        actionRadioViewmodel.action.observe(this) { action ->
            val currentStationList = adapter.getCurrentStationList() // ดึงรายการสถานีที่กำลังแสดง
            Log.d("RadioNewFragment", "Received action: $action")
            handleRadioAction(action, currentStationList)
            // เมื่อ action เปลี่ยน ทำสิ่งที่คุณต้องการ เช่น:
        }
    }

    //ฟังชั่นสำหรับประมวลผลรายการสถานีวิทยุตาม Action
    fun handleRadioAction(mAction:String, stationList: List<RadioResponseModel>) {
        val prefs = this.getSharedPreferences("lastPlayed", Context.MODE_PRIVATE)
        val playingName = prefs.all.keys.firstOrNull()
        val currentIndex = stationList.indexOfFirst { it.name == playingName }

        var targetStation: RadioResponseModel? = null
        Log.d("RadioNewFragment", "Received action2: $mAction")

        when (mAction) {
            "PLAY" -> {
                targetStation = if (currentIndex != -1) {
                    stationList[currentIndex]
                } else {
                    stationList.firstOrNull()
                }
            }

            "STOP" -> {
                Log.d("RadioNewFragment", "Received action3: $mAction")
                actionRadioViewmodel.setIsPlaying(false) //aom24/7/68
                stopRadio()
                return
            }

            "NEXT" -> {
                if (currentIndex != -1 && currentIndex < stationList.size - 1) {
                    targetStation = stationList[currentIndex + 1]
                } else {
                    targetStation = stationList.firstOrNull() // วนลูปไปที่แรก
                }
            }

            "PREV" -> {
                if (currentIndex > 0) {
                    targetStation = stationList[currentIndex - 1]
                } else {
                    targetStation = stationList.lastOrNull() // วนลูปไปที่สุดท้าย
                }
            }
        }

        targetStation?.let { station ->
            // บันทึกว่าเล่นสถานีนี้ → อัปเดต SharedPreferences และ UI
            adapter.saveLastPlayed(this, station) //เรียนเพื่อให้มีการอัพเดทขอบของรายการวิทยุ
            val editor = prefs.edit()
            editor.clear()
            editor.putString(station.name, station.url)
            editor.apply()

            val intent = Intent(this, RadioService::class.java).apply {
                putExtra("STATION_URL", station.url)
                putExtra("STATION_NAME", station.name)
                putExtra("STATION_LOGO", station.favicon)
                putExtra("STATION_FAVORITE", station.favorite)
                putExtra("STATION_ISPLAY", station.isPlay)
                action = "PLAY"
            }
            ContextCompat.startForegroundService(this, intent)
        }
    }

    private fun stopRadio() {
        val intent = Intent(this, RadioService::class.java).apply {
            action = "STOP"
        }
        this.startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}