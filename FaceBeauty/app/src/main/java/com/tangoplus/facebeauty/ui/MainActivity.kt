package com.tangoplus.facebeauty.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.db.FaceDatabase
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

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, MainFragment())
            commit()
        }
        bd.btnMainStart.setOnSingleClickListener {
            val intent = Intent(this@MainActivity, CameraActivity::class.java)
            startActivity(intent)
        }

        cacheDir.deleteRecursively()
    }

    private var backPressedOnce = false
    private val backPressHandler = Handler(Looper.getMainLooper())
    private val backPressRunnable = Runnable { backPressedOnce = false }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val fragmentManager = supportFragmentManager

            if (fragmentManager.fragments.lastOrNull() is MainFragment) {
                if (backPressedOnce) {
                    FaceDatabase.closeDatabase()
                    finishAffinity() // 앱 종료
                } else {
                    backPressedOnce = true
                    Toast.makeText(this@MainActivity, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
                    backPressHandler.postDelayed(backPressRunnable, 1000) // 1초 내에 다시 누르면 종료
                }
            }
        }
    }
}