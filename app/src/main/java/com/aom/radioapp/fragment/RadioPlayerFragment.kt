package com.aom.radioapp.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.aom.radioapp.R
import com.aom.radioapp.databinding.FragmentRadioPlayerBinding
import com.aom.radioapp.model.RadioItem
import com.aom.radioapp.viewmodel.ActionRadioViewmodel
import com.bumptech.glide.Glide

class RadioPlayerFragment : Fragment() {
    //สร้างตัวแปรสำหรับ binding
    private var _binding: FragmentRadioPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var actionRadioViewmodel: ActionRadioViewmodel
    private var playStat = false //เช็คสถานะการเล่น
    private var fgPlayStat =false //เช็คว่ามีการกดปุ่ม play
    private lateinit var receiver: BroadcastReceiver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRadioPlayerBinding.inflate(inflater, container, false)
        return binding.root
        // Inflate the layout for this fragment
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAction ()
        observeViewModelRadioPlaying()

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val name = it.getStringExtra("station_name")?:""
                    val logo = it.getStringExtra("station_logo")?:""
                    val isFav = it.getBooleanExtra("station_fav", false)
                    val isPlaying = it.getBooleanExtra("is_playing", false)

                    val radioItemPlaying = RadioItem(
                        name = name,
                        logo = logo,
                        isFav = isFav,
                        isPlay = isPlaying
                    )

                    updateUI(name, logo, isFav, isPlaying) // อัปเดต UI
                    setStationPlaying(radioItemPlaying) // อัปเดต UI ในหน้า home
                }
            }
        }
        requireContext().registerReceiver(receiver, IntentFilter("RADIO_UPDATE"))
    }

    private fun observeViewModelRadioPlaying(){
        // Observe การเปลี่ยนแปลงของสถานีที่กำลังเล่น
        actionRadioViewmodel = ViewModelProvider(requireActivity())[ActionRadioViewmodel::class.java]
        actionRadioViewmodel.stationPlaying.observe(viewLifecycleOwner){ radioItem ->
            radioItem?.let {
                updateUI(radioItem.name, radioItem.logo, radioItem.isFav, radioItem.isPlay)
            }
        }
    }

    private fun updateUI(name: String?, logo: String?, isFav: Boolean, isPlaying: Boolean) {
        binding.textViewRadioPlayerName.text = name
        Glide.with(this)
            .load(logo)
            .error(R.drawable.icon_music) // รูปที่ใช้กรณีโหลดไม่ได้
            .into(binding.circleImageViewRadioPlayer) // ImageView ที่จะแสดงโลโก้

        binding.imageViewIconStarNoneRadioPlayer.setImageResource(
            if (isFav) R.drawable.icon_star_radio_player else R.drawable.icon_star_none_radio_player
        )

        binding.imageViewIconPlayRadioPlayer.setImageResource(
            if(isPlaying) {
                R.drawable.icon_pause_radio_player
            } else {
                R.drawable.icon_play_radio_player
            }
        )
        playStat = isPlaying
        fgPlayStat = false
    }

    private fun  setAction (){
        actionRadioViewmodel = ViewModelProvider(requireActivity())[ActionRadioViewmodel::class.java]
        // เรียกเมื่อผู้ใช้กดปุ่มกวบคุมให้ส่ง action
        binding.imageViewIconPlayRadioPlayer.setOnClickListener {
            if(playStat!=fgPlayStat){
                actionRadioViewmodel.setAction("STOP")
                binding.imageViewIconPlayRadioPlayer.setImageResource(R.drawable.icon_play_radio_player)
                fgPlayStat=playStat
            }else{
                actionRadioViewmodel.setAction("PLAY")
                binding.imageViewIconPlayRadioPlayer.setImageResource(R.drawable.icon_pause_radio_player)
            }

        }
        binding.imageViewIconPrevRadioPlayer.setOnClickListener {
            actionRadioViewmodel.setAction("PREV")
        }

        binding.imageViewIconNextRadioPlayer.setOnClickListener {
            actionRadioViewmodel.setAction("NEXT")
        }
    }

    private fun setStationPlaying(radioItem: RadioItem){
        actionRadioViewmodel = ViewModelProvider(requireActivity())[ActionRadioViewmodel::class.java]
        actionRadioViewmodel.setStationPlaying(radioItem)
    }

}