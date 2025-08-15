package com.aom.radioapp.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aom.radioapp.R
import com.aom.radioapp.databinding.ItemRadioBinding
import com.aom.radioapp.model.RadioResponseModel
import com.aom.radioapp.service.RadioService
import com.bumptech.glide.Glide

class RadioAdapter( private val context: Context):RecyclerView.Adapter<MainViewHolder>(){
    //สร้างตัวแปรเก็บรายชื่อสถานีวิทยุ
    private  var stations = mutableListOf<RadioResponseModel>()
    fun setStationList(stations: List<RadioResponseModel>){
        // โหลดรายการโปรดจาก SharedPreferences
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        //เช็คเปลี่ยนเทียบรายการโปรด
        try {
            val updatedList = stations.mapIndexed  {index, station ->
                val isFavorite = prefs.getBoolean(station.name, false)
                station.favorite = isFavorite
                station
            }
            this.stations = updatedList.toMutableList()
            notifyDataSetChanged()
        }
        catch (e:Exception){
            println(e)
        }

    }

    fun setPlayingStation(stations: List<RadioResponseModel>) {
        // โหลดรายการโปรดจาก SharedPreferences
        val prefs = context.getSharedPreferences("lastPlayed", Context.MODE_PRIVATE)
        val lastPlayedName = prefs.all.keys.firstOrNull()
        val updatedList = stations.map { station ->
            station.isPlay = (station.name == lastPlayedName)
            station
        }
        this.stations = updatedList.toMutableList()
        notifyDataSetChanged()
    }

    fun getCurrentStationList(): List<RadioResponseModel> {
        return this.stations // หรือชื่อที่คุณใช้เก็บรายการใน Adapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRadioBinding.inflate(inflater,parent,false) //แก้โค้ด 27/03/68
        return  MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return stations.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val station = stations[position]
        val binding = holder.binding
        holder.binding.textViewRadioName .text = station.name  //แก้โค้ด 27/03/68

        // โหลดรูป favicon ด้วย Glide
        val imageUrl = if (station.favicon.isEmpty()) {
            R.drawable.icon_music
        } else {
            station.favicon
        }

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.binding.imageViewRadio)


        // คลิกเพื่อเปิด URL
        holder.itemView.setOnClickListener {
            saveLastPlayed(context, station)
            //การส่ง URL ไปที่ RadioService
            val intent = Intent(context, RadioService::class.java).apply {
                putExtra("STATION_URL", station.url)
                putExtra("STATION_NAME", station.name)
                putExtra("STATION_LOGO", station.favicon)
                putExtra("STATION_FAVORITE", station.favorite)
                putExtra("STATION_ISPLAY", station.isPlay)
                action = "PLAY"
            }
            ContextCompat.startForegroundService(context, intent)

        }

        binding.linearLayoutBorderListRadio.setBackgroundResource(if (station.isPlay) R.drawable.bg_box_rounded8_border else 0)
       // notifyDataSetChanged()

        //เพิ่มรายการโปรด
        binding.imageViewFavorite.setImageResource(
            if (station.favorite) R.drawable.icon_star_solid else R.drawable.icon_star_outline
        )


        binding.imageViewFavorite.setOnClickListener {
            // เปลี่ยนสถานะรายการโปรด
            station.favorite = !station.favorite
            notifyItemChanged(position)
            // ถ้ามีระบบบันทึก ให้บันทึกไว้ เช่น SQLite, Room, SharedPreferences หรือ ViewModel
            saveFavoriteStatus(binding.root.context, station)
        }

    }

    fun saveFavoriteStatus(context: Context, station:RadioResponseModel) {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(station.name, station.favorite) // สมมติว่า station.id เป็น String หรือ unique identifier
        editor.apply()
    }

    fun saveLastPlayed(context: Context,station: RadioResponseModel){
        val prefs = context.getSharedPreferences("lastPlayed", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        // เคลียร์สถานีที่เคยเล่นก่อนหน้านี้ทั้งหมด
        editor.clear()

        // บันทึกสถานีใหม่ที่กำลังเล่นอยู่
        editor.putBoolean(station.name, true) // ใช้ stationuuid ซึ่ง unique
        editor.apply()
        setPlayingStation(stations)
    }

}



class MainViewHolder(val binding:ItemRadioBinding):RecyclerView.ViewHolder(binding.root) //แก้โค้ด 27/03/68