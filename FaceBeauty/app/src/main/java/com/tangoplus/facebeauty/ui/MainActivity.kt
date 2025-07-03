package com.tangoplus.facebeauty.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.databinding.ActivityMainBinding
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.vm.MainViewModel

class MainActivity : AppCompatActivity() {
    private val mvm : MainViewModel by viewModels()
    private lateinit var bd : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bd = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bd.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mvm.isMeasureFinish = intent.getBooleanExtra("isMeasureFinish", false)


        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, MainFragment())
            commit()
        }
        bd.btnMainStart.setOnSingleClickListener {
            val intent = Intent(this@MainActivity, CameraActivity::class.java)
            startActivity(intent)
        }
    }
}