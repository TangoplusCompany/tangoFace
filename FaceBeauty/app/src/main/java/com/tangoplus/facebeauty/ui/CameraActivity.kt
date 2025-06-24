package com.tangoplus.facebeauty.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.data.db.FaceDao
import com.tangoplus.facebeauty.data.db.FaceDatabase
import com.tangoplus.facebeauty.databinding.ActivityCameraBinding
import com.tangoplus.facebeauty.util.FileUtility.getRequiredPermissions
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.util.MathHelpers.calculateScaleFromPart
import com.tangoplus.facebeauty.util.MathHelpers.calculateSlope
import com.tangoplus.facebeauty.util.MathHelpers.getRealDistanceX
import com.tangoplus.facebeauty.util.MathHelpers.setScaleX
import com.tangoplus.facebeauty.util.SoundManager.playSound
import com.tangoplus.facebeauty.vision.FaceBlendshapesResultAdapter
import com.tangoplus.facebeauty.vision.FaceLandmarkerHelper
import com.tangoplus.facebeauty.vm.InputViewModel
import com.tangoplus.facebeauty.vm.MainViewModel
import com.tangoplus.facebeauty.vm.MeasureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.tangoplus.facebeauty.util.FileUtility.getImageUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.toFaceStatic
import com.tangoplus.facebeauty.util.FileUtility.toJSONObject
import com.tangoplus.facebeauty.util.MathHelpers.calculateAngle
import com.tangoplus.facebeauty.util.PreferenceUtility
import com.tangoplus.facebeauty.vm.GalleryViewModel
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2

class CameraActivity : AppCompatActivity(), FaceLandmarkerHelper.LandmarkerListener {

    private lateinit var binding : ActivityCameraBinding
    companion object {
        private const val TAG = "Face Landmarker"
        private const val REQUEST_CODE_PERMISSIONS = 1001

        fun hasPermissions(context: Context): Boolean {
            Log.d("PermissionCheck", "Context type: ${context::class.java.name}")
            return getRequiredPermissions().all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private val viewModel: MainViewModel by viewModels()
    private val faceBlendshapesResultAdapter by lazy {
        FaceBlendshapesResultAdapter()
    }

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    private var isCountDown = false
    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

    private var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

    var latestResult: FaceLandmarkerHelper.ResultBundle? = null
    private val mvm : MeasureViewModel by viewModels()
    private val ivm : InputViewModel by viewModels()
    private val gvm : GalleryViewModel by viewModels()
    private var seqStep = MutableLiveData(0)
    private val maxSeq = 1

    private var scaleFactorX : Float? = null
    private var scaleFactorY : Float? = null

    private lateinit var fDao : FaceDao
    private lateinit var prefsUtil : PreferenceUtility

    private var bounceAnimator: AnimatorSet? = null

    private  val mCountDown : CountDownTimer by lazy {
        object : CountDownTimer(3000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread{
                    binding.clCountDown.visibility = View.VISIBLE
                    binding.clCountDown.alpha = 1f
                    binding.tvCountDown.text = "${(millisUntilFinished / 1000.0f).roundToInt()}"
                    Log.v("count", "${binding.tvCountDown.text}")
                }
            }

            override fun onFinish() {
                if (latestResult?.result?.faceLandmarks()?.isNotEmpty() == true) {
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    latestResult?.let { resultBundleToJson(it, seqStep.value ?: -1) }
                    hideViews()
                    if (seqStep.value != null) {
                        playSound(R.raw.camera_shutter)
                        captureImage(seqStep.value ?: -1) {

                            updateUI()
                        }
                    }
                }
            }
        }
    } //  ------! 카운트 다운 끝 !-------


    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.

        // Start the FaceLandmarkerHelper again when users come back
        // to the foreground.
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_CODE_PERMISSIONS)
        } else {
            backgroundExecutor.execute {
                if (faceLandmarkerHelper.isClose()) {
                    faceLandmarkerHelper.setupFaceLandmarker()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if(this::faceLandmarkerHelper.isInitialized) {
            viewModel.setMaxFaces(faceLandmarkerHelper.maxNumFaces)
            viewModel.setMinFaceDetectionConfidence(faceLandmarkerHelper.minFaceDetectionConfidence)
            viewModel.setMinFaceTrackingConfidence(faceLandmarkerHelper.minFaceTrackingConfidence)
            viewModel.setMinFacePresenceConfidence(faceLandmarkerHelper.minFacePresenceConfidence)
            viewModel.setDelegate(faceLandmarkerHelper.currentDelegate)

            // Close the FaceLandmarkerHelper and release resources
            backgroundExecutor.execute { faceLandmarkerHelper.clearFaceLandmarker() }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 상단, 하단 상태표시줄 넣기
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller  .apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        mvm.initMeasure.observe(this) {
            if (it) {
                initSettings()
                mvm.initMeasure.value = false
            }
        }
        prefsUtil = PreferenceUtility(this@CameraActivity)

        backgroundExecutor = Executors.newSingleThreadExecutor()
        binding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        backgroundExecutor.execute {
            faceLandmarkerHelper = FaceLandmarkerHelper(
                context = this@CameraActivity,
                runningMode = RunningMode.LIVE_STREAM,
                minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                maxNumFaces = viewModel.currentMaxFaces,
                currentDelegate = viewModel.currentDelegate,
                faceLandmarkerHelperListener = this
            )
        }
        binding.btnShooting.setOnSingleClickListener {
            if (binding.btnShooting.text == "결과보기") {
                // ---------------# 데이터 갈무리해서 gallery DialogFragment로 넘기기 #-----------------
                // 각각의 static에 사용자 정보 넣기


                mvm.staticJson0.apply {
                    put("temp_server_sn", prefsUtil.getLastTempServerSn())
                    put("mediaFileName", mvm.static0FileName.toString())
                    put("jsonFileName", mvm.static0FileName.toString().replace(".jpg", ".json"))
                    put("seq", 0)
                    put("user_uuid", mvm.currentUUID)
                    put("user_name", ivm.nameValue.value.toString())
                    put("user_mobile", ivm.mobileValue.value.toString())
                }
                mvm.staticJson1.apply {
                    put("temp_server_sn", prefsUtil.getLastTempServerSn())
                    put("mediaFileName", mvm.static1FileName.toString())
                    put("jsonFileName", mvm.static1FileName.toString().replace(".jpg", ".json"))
                    put("seq", 1)
                    put("user_uuid", mvm.currentUUID)
                    put("user_name", ivm.nameValue.value.toString())
                    put("user_mobile", ivm.mobileValue.value.toString())
                }
                val transJOStatic0 = mvm.staticJson0.toFaceStatic()
                val transJOStatic1 = mvm.staticJson1.toFaceStatic()
                mvm.mergedJson0.apply {
                    put("data", JSONObject(transJOStatic0.toJSONObject()))
                    put("face_landmark", mvm.coordinates0)
                }
                mvm.mergedJson1.apply {
                    put("data", JSONObject(transJOStatic1.toJSONObject()))
                    put("face_landmark", mvm.coordinates1)
                }
//                Log.v("mvm.mergedJson", "${mvm.mergedJson0}, ${mvm.mergedJson1}")

                val jsonPath0 = saveJsonToStorage(mvm.mergedJson0, mvm.static0FileName.toString().replace(".jpg", ""))
                val jsonPath1 = saveJsonToStorage(mvm.mergedJson1, mvm.static1FileName.toString().replace(".jpg", ""))
                Log.v("mvm.mergedJson", "${jsonPath0}, ${jsonPath1}")
//                mvm.staticJson0.put("jsonFileUri", jsonPath0.toString())
//                mvm.staticJson1.put("jsonFileUri", jsonPath1.toString())
//                mvm.mergedJson0.put("data", mvm.staticJson0)
//                mvm.mergedJson1.put("data", mvm.staticJson1)

                val results = JSONArray().apply {
                    put(mvm.mergedJson0)
                    put(mvm.mergedJson1)
                }
                Log.v("mvm.mergedJson", "$results")
                // faceResult 객체 1개의 날짜 정함
                val finishedResult = FaceResult(
                    tempServerSn = prefsUtil.getNextTempServerSn(),
                    userName = ivm.nameValue.value,
                    userMobile = ivm.mobileValue.value,
                    imageUris = listOf(
                        getImageUriFromFileName(this@CameraActivity, mvm.static0FileName ?: ""),
                        getImageUriFromFileName(this@CameraActivity, mvm.static1FileName ?: "")
                    ),
                    results = results
                )
                Log.v("mvm.mergedJson", "${finishedResult.results.getJSONObject(0).getJSONObject("data")}")
                gvm.currentResult.value = finishedResult
                lifecycleScope.launch(Dispatchers.IO) {
                    val fd = FaceDatabase.getDatabase(this@CameraActivity)
                    fDao = fd.faceDao()
                    // static 2개를 만들어서 DB 저장

                    val static0 = mvm.staticJson0.toFaceStatic()
                    Log.v("static0", "변환완료: $static0")
                    fDao.insertStatic(static0)
                    val static1 = mvm.staticJson1.toFaceStatic()
                    Log.v("static1", "변환완료: $static1")
                    fDao.insertStatic(static1)

                    finishedResult.regDate = static0.reg_date

                    withContext(Dispatchers.Main) {
                        gvm.isShowResult.value = true
                        val galleryDialog = GalleryDialogFragment()
                        galleryDialog.show(supportFragmentManager, "")
                    }
                }
            } else {
                if (!isCountDown) {
                    startTimer()
                }
            }

        }
        binding.tvGoGallery.setOnSingleClickListener {
            val galleryDialog = GalleryDialogFragment()
            galleryDialog.show(supportFragmentManager, "")
        }


        binding.tvRetry.setOnSingleClickListener {
            MaterialAlertDialogBuilder(this@CameraActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setTitle("알림")
                setMessage("다시 시작하시겠습니까?")
                setPositiveButton("예") { _, _ ->
                    initSettings()
                }
                setNegativeButton("아니오") { _, _ -> }
                show()
            }
        }
        startBouncingAnimation(binding.tvSeqGuide)
//        binding.clSeqGuide.setOnSingleClickListener {
////            binding.clSeqGuide.clearAnimation()
////            binding.clSeqGuide.animate().cancel()
////            setAnimation(binding.tvSeqGuide, 500L, 0L, false) {
////                binding.btnShooting.isEnabled = true
////            }
//
//        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this@CameraActivity)
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this@CameraActivity)
        )
    }

    private fun bindCameraUseCases() {
        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetResolution(Size(1280, 720))
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetResolution(Size(1280, 720))
                .setTargetRotation(binding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectFace(image)
                    }
                }
        imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(1280, 720))
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()
        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer, imageCapture,
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }
    private fun detectFace(imageProxy: ImageProxy) {
        faceLandmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            binding.viewFinder.display.rotation
    }


    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this@CameraActivity, error, Toast.LENGTH_SHORT).show()
            faceBlendshapesResultAdapter.updateResults(null)
            faceBlendshapesResultAdapter.notifyDataSetChanged()

//            if (errorCode == FaceLandmarkerHelper.GPU_ERROR) {
//                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
//                    FaceLandmarkerHelper.DELEGATE_CPU, false
//                )
//            }
        }
    }

    override fun onResults(
        resultBundle: FaceLandmarkerHelper.ResultBundle
    ) {
        runOnUiThread {

            // Pass necessary information to OverlayView for drawing on the canvas
            latestResult = resultBundle
            var isFaceCenter = false
            // 263 -362 구간의 좌표값을 판단해서 scale을 가변설정해줘야함
            val landmarks = latestResult?.result?.faceLandmarks()
            landmarks?.forEach { faceLandmarks ->

                isFaceCenter = isFaceInCenter(faceLandmarks[6].x(), faceLandmarks[6].y())
                val point1 = faceLandmarks?.get(468)
                val point2 = faceLandmarks?.get(473)

                if (point1 != null && point2 != null) {
                    val dx = point1.x() - point2.x()
                    val dy = point1.y() - point2.y()
                    val distance = sqrt(dx * dx + dy * dy)
                    // 이 distance는 0~1 사이의 상대 거리입니다. 예: 0.05 = 화면 너비의 5%
                    // 여기서 원하는 scale값을 거리 기반으로 정하면 됩니다.
//                    val scale = yourScalingFunction(distance)
//                    Log.v("거리", "$distance")
                    // 0.085 ~ 0.09 일 때 32f
                    // 0.0
                    // 3.7
                    val scale = calculateScaleFromPart(distance)
                    setScaleX(scale)
                }

                val leftEye = faceLandmarks[33]
                val rightEye = faceLandmarks[263]
                val middleEye = faceLandmarks[6]

                val noseTip = faceLandmarks[1]     // 코끝
                val leftEarPoint = faceLandmarks[234] // 왼쪽 귓바퀴 근처
                val rightEarPoint = faceLandmarks[454] // 오른쪽 귓바퀴 근처

                val leftEyeDistance = getRealDistanceX(Pair(leftEye.x(), leftEye.y()), Pair(middleEye.x(), middleEye.y()))
                val rightEyeDistance = getRealDistanceX(Pair(rightEye.x(), rightEye.y()), Pair(middleEye.x(), middleEye.y()))
                val eyeDistanceGap = abs(leftEyeDistance - rightEyeDistance)

                val horizontalLineVector = calculateAngle(leftEarPoint.x(), leftEarPoint.y(), noseTip.x(), noseTip.y(),rightEarPoint.x(), rightEarPoint.y())
                val vertiBoolean = if (eyeDistanceGap < 0.35f) true else false
                binding.overlay.setVerti(vertiBoolean)

                val horizonBoolean = horizontalLineVector in 125f..140f
                binding.overlay.setHorizon(horizonBoolean)
//                Log.v("얼굴 중앙", "$isFaceCenter")
//                Log.v("라인벡터", "코: ${faceLandmarks[0].x()}, ${faceLandmarks[0].y()}, 왼쪽눈: ${faceLandmarks[33].x()}, ${faceLandmarks[33].x()}, 오른쪽 눈: ${faceLandmarks[263].x()}, ${faceLandmarks[263].x()}")
            }

            binding.overlay.setResults(
                resultBundle.result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
//            Log.v("인풋이미지", "${resultBundle.inputImageHeight}, ${resultBundle.inputImageWidth}")
//            Log.v("results", "${resultBundle.result.faceLandmarks().map { it.size }}")
            // Force a redraw
            binding.overlay.invalidate()

            // overlay 줌인과

            if (ivm.isFinishInput && isFaceCenter && resultBundle.result.faceLandmarks().isNotEmpty()) {
                bounceAnimator?.cancel()
                bounceAnimator = null
                binding.tvSeqGuide.animate().cancel()
                binding.tvSeqGuide.clearAnimation()
                binding.tvSeqGuide.translationY = 0f
                binding.tvSeqGuide.text = "턱관절 교합상태를 진단합니다\n편한 상태로 입을 다물고 정면을 응시해주세요"

                Handler(Looper.getMainLooper()).postDelayed({
                    animateTextViewToTopLeft(binding.clSeqGuide, binding.tvSeqGuide)
                }, 2000)

                binding.fdgv.triggerIntroAnimationIfNeeded()
                if (binding.overlay.getVerti() && binding.overlay.getHorizon()) {
                    binding.fdgv.startSuccessAnimation {
                        if (!isCountDown) {
                            startTimer()
                        }
                    }
                } else {
                    binding.fdgv.resetSuccessMode()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_CODE_PERMISSIONS) return  // 잘못된 요청 코드 방지

        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            setUpCamera()
            Log.v("스켈레톤 Init", "모든 권한 승인 완료")
        } else {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] == PackageManager.PERMISSION_DENIED
            }
            Log.v("스켈레톤 Init", "거부된 권한: ${deniedPermissions.joinToString()}")

            // "다시 묻지 않음"을 체크한 경우 -> 앱 종료
            if (deniedPermissions.all { !shouldShowRequestPermissionRationale(it) }) {
                finish()
                Toast.makeText(this, "권한을 모두 허용한 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            } else {
                // 한 번 거부한 경우 -> 설명 다이얼로그 표시
                showPermissionExplanationDialog()
            }
        }
    }
    // -------------------------# 타이머, UI업데이트, 이미지캡처, 결과처리 #-----------------------------
    private fun hideViews() {
        binding.clCountDown.visibility = View.INVISIBLE
        binding.btnShooting.visibility = View.VISIBLE
        startCameraShutterAnimation()
    }

    private fun startCameraShutterAnimation() {
        // 첫 번째 애니메이션: VISIBLE로 만들고 alpha를 0에서 1로
        binding.flCameraShutter.visibility = View.VISIBLE
        binding.flCameraShutter.alpha = 0f
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeIn = ObjectAnimator.ofFloat(binding.flCameraShutter, "alpha", 0f, 1f).apply {
                duration = 100 // 0.1초
                interpolator = AccelerateDecelerateInterpolator()
            }
            // 두 번째 애니메이션: alpha를 1에서 0으로 만들고, 끝난 후 INVISIBLE로 설정
            val fadeOut = ObjectAnimator.ofFloat(binding.flCameraShutter, "alpha", 1f, 0f).apply {
                duration = 100 // 0.1초
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.flCameraShutter.visibility = View.INVISIBLE
                    }
                })
            }

            fadeIn.start()
            fadeIn.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fadeOut.start()
                }
            })
        }, 0)
    }


//    fun getFileNameFromUri(uri: Uri): String? {
//        var fileName: String? = null
//
//        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
//                if (displayNameIndex != -1) {
//                    fileName = cursor.getString(displayNameIndex)
//                }
//            }
//        }
//
//        return fileName
//    }

    private fun setGuideAnimation(seq: Int) {
        binding.btnShooting.isEnabled = false
        val slide = TranslateAnimation(0f, 0f, -100f, 0f)
        slide.apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            fillAfter = true
        }
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 800
        }
        val animationSet = AnimationSet(true).apply {
            addAnimation(slide)
            addAnimation(fadeIn)
            fillAfter = true
        }
        binding.tvSeqGuide.apply {
            text = when (seq) {
                0 -> "턱관절 교합상태를 진단합니다\n편한 상태로 입을 다물고 정면을 응시해주세요"
                1 -> "다음 단계는 교합 상태입니다\n이를 맞물리게 물고 입술을 벌려보세요"
                2 -> "수고하셨습니다\n버튼을 눌러 결과를 확인해보세요"
                else -> ""
            }
            visibility = View.VISIBLE
            animation = animationSet
        }
        val endDelay = when (seq) {
            2 -> 2000L
            else -> 3500L
        }

//        setAnimation(binding.tvSeqGuide, 500L, 0L, true) { }
//        setAnimation(binding.tvSeqGuide, 800L, endDelay, false) {
//            binding.btnShooting.isEnabled = true
//        }
    }
    private fun isFaceInCenter(glabellaX: Float, glabellaY: Float): Boolean {
        val targetRect = RectF(0.4f, 0.2f, 0.5f, 0.6f)
        return targetRect.contains(glabellaX, glabellaY)
    }

    private fun animateTextViewToTopLeft(clSeqGuide: ConstraintLayout, tvTarget: TextView) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(clSeqGuide)

        // Bias 변경
        constraintSet.setHorizontalBias(tvTarget.id, 0.1f)
        constraintSet.setVerticalBias(tvTarget.id, 0.05f)

        // 애니메이션 설정
        val transition = ChangeBounds().apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
        }

        // 애니메이션 실행
        TransitionManager.beginDelayedTransition(clSeqGuide, transition)
        constraintSet.applyTo(clSeqGuide)
    }
    fun startBouncingAnimation(textView: TextView) {
        val bounceDistance = -10 * textView.resources.displayMetrics.density

        val upAnim = ObjectAnimator.ofFloat(textView, "translationY", 0f, bounceDistance).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        val downAnim = ObjectAnimator.ofFloat(textView, "translationY", bounceDistance, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        bounceAnimator = AnimatorSet().apply {
            playSequentially(upAnim, downAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 애니메이션이 취소된 경우 반복하지 않음
                    if (!isCancelled) {
                        start()
                    }
                }

                var isCancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }
            })
            start()
        }
    }

    // ------! 타이머 control 시작 !------
    private fun startTimer() {
        // 시작 버튼 후 시작
        binding.btnShooting.isEnabled = false
        binding.tvGoGallery.isEnabled = false
        Log.v("seqStep", "${seqStep.value} / 2")
        mCountDown.start()
        // ------! 타이머 control 끝 !------
    }

    private fun updateUI() {
        when (seqStep.value) {
            maxSeq -> {
                binding.clCountDown.visibility = View.GONE

                binding.btnShooting.text = "결과보기"
                mCountDown.cancel()
                Log.v("몇단계?", "Max repeats reached, stopping the loop")
                setGuideAnimation(2)
            }
            else -> {
                seqStep.value = seqStep.value?.plus(1)
                binding.tvSeqCount.text = "${seqStep.value?.plus(1)} / 2"
                setGuideAnimation(1)
            }
        }
        isCountDown = false
        binding.tvGoGallery.isEnabled = true
    }


    private fun setAnimation(tv: View, duration : Long, delay: Long, fade: Boolean, callback: () -> Unit) {

        val animator = ObjectAnimator.ofFloat(tv, "alpha", if (fade) 0f else 1f, if (fade) 1f else 0f)
        animator.duration = duration
        animator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                tv.visibility = if (fade) View.VISIBLE else View.INVISIBLE
                callback()
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            animator.start()
        }, delay)
    }
    private var permissionDialog: AlertDialog? = null
    private fun showPermissionExplanationDialog() {
        if (permissionDialog?.isShowing == true) return  // 이미 다이얼로그가 떠 있으면 return

        permissionDialog = AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("측정을 위해서는 사진 및 갤러리 권한을 모두 허용해야 합니다.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        permissionDialog?.let { dialog ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.black))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.black))
        }
    }

    private fun captureImage(step: Int, onComplete: () -> Unit) {
        val imageCapture = imageCapture
        val name = getFileName(step)

        // 임시로 캐시에 저장할 OutputFileOptions 설정
        val tempFile = File.createTempFile("temp_capture", ".jpg", cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // 임시 파일의 URI를 생성해서 전처리 함수로 전달
                    val tempUri = Uri.fromFile(tempFile)
                    saveMediaToStorage(this@CameraActivity, tempUri, name)
                    onComplete()
                    // 임시 파일 삭제는 saveMediaToStorage에서 처리
                    Log.d(TAG, "Photo captured and processing started")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    // 에러 발생시 임시 파일 정리
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                }
            }
        )
    }
    fun saveJsonToStorage(jsonObject: JSONObject, fileName: String) : String? {
        val resolver = contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.json")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents/TangoPlus")
        }
        return try {
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(jsonObject.toString().toByteArray())
                    outputStream.flush()
                }
                // 저장 성공
                Log.d("JSON_SAVE", "JSON 파일이 성공적으로 저장되었습니다: $fileName.json, uri: $uri")
                "$fileName.json"
            }
        } catch (e: Exception) {
            Log.e("JSON_SAVE", "JSON 파일 저장 실패", e)
            null
        }
    }

    fun saveMediaToStorage(context: Context, uri: Uri, fileName: String) {
        try {
            val extension = ".jpg"

            // Pictures/TangoBeauty에 저장할 ContentValues 설정
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName$extension")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TangoPlus")
            }

            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("tempImage", null, context.cacheDir)
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // EXIF 데이터 읽기
            val exif = ExifInterface(tempFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            Log.d("ExifDebug", "Exif Orientation: $orientation")

            // 비트맵 디코딩
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(tempFile.absolutePath, options)

            val sourceWidth = options.outWidth
            val sourceHeight = options.outHeight

            val targetWidth = 1280
            val targetHeight = 720

            // 이미지 스케일 계산
            val widthRatio = sourceWidth.toFloat() / targetWidth
            val heightRatio = sourceHeight.toFloat() / targetHeight
            val scale = maxOf(widthRatio, heightRatio)

            options.inJustDecodeBounds = false
            options.inSampleSize = scale.toInt()

            var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, options)
            val matrix = Matrix()

            // 회전 적용
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)   // 시계 방향 90도
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f) // 시계 방향 180도
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f) // 시계 방향 270도
            }

            // ★★★ 좌우반전 ★★★
            matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)

            // 모든 변환을 한번에 적용
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // 스케일 조정
            bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

            // Pictures/TangoBeauty에 저장
            val imageUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            imageUri?.let { imageURI ->
                context.contentResolver.openOutputStream(imageURI)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()
                }

                Log.d("SaveMedia", "Image saved to Pictures/TangoBeauty: $fileName$extension")
            } ?: run {
                Log.e("SaveMedia", "Failed to create image URI")
            }
            when (seqStep.value) {
                0 -> mvm.static0FileName = "$fileName$extension"
                1 -> mvm.static1FileName = "$fileName$extension"
            }
            Log.d("SaveMedia", "seqStep: ${seqStep.value} vm0FileName: ${mvm.static0FileName} vm1FileName: ${mvm.static1FileName}")

            // 임시 파일과 비트맵 정리
            tempFile.delete()
            bitmap.recycle()

        } catch (e: IndexOutOfBoundsException) {
            Log.e("SaveMediaIndex", "${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("SaveMediaIllegal", "${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("SaveMediaIllegal", "${e.message}")
        } catch (e: NullPointerException) {
            Log.e("SaveMediaNull", "${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("SaveMediaException", "${e.message}")
        }
    }

    private fun getFileName(step: Int) : String {
        return when (step) {
            0 -> "1-1-$timestamp"
            1 -> "2-2-$timestamp"
            else -> ""
        }
    }

    fun resultBundleToJson(resultBundle: FaceLandmarkerHelper.ResultBundle?, step: Int) {
        if (scaleFactorX == null && scaleFactorY == null) {
            val inputWidth = latestResult?.inputImageWidth
            val inputHeight = latestResult?.inputImageHeight
            if (inputWidth != null && inputWidth != 0 && inputHeight != null && inputHeight != 0) {
                scaleFactorX = (binding.overlay.width / inputWidth).toFloat()
                scaleFactorY = (binding.overlay.height / inputHeight).toFloat()
            } else {
                scaleFactorX = 1f
                scaleFactorY = 1f
            }
//            Log.v("ScreenSettings", "scaleFactor(x, y): ($scaleFactorX, $scaleFactorY), imageSize(width, height): (${latestResult?.inputImageWidth}, ${latestResult?.inputImageHeight})")
        }


        if (resultBundle?.result?.faceLandmarks()?.isNotEmpty() == true) {
            val plr = resultBundle.result.faceLandmarks()?.get(0)

            /* 1. 키오스크 -> pose좌표까지 좌우 반전 -> 8이 왼쪽 어깨
            *  2. 일치시키기 위해 모바일 pose좌표 -> 동일
            *  3. 실제 기울기를 계산하는 곳만 반대로 들어감 -> 7 이 왼쪽 어깨
            *  4. 이유는 기울기와 거리 계산하는 곳의 모든 인자를 변경해줘야 하기 때문에.
            *  5. 값 계산에서는 왼쪽의 기울기는 second 혹은 1번 index 임.
            *  6. 이를 일치 시키려면? -> mvm에 들어가는 인자들을 거울모드로 변경 -> 값들은 원상복귀
            * */
            Log.v("현재렌즈위치", "$cameraFacing == 정면${CameraSelector.LENS_FACING_FRONT}, 후면${CameraSelector.LENS_FACING_BACK}")
            // 비우기
            mvm.currentCoordinate.clear()
            mvm.relativeCoordinate.clear()

            plr?.forEachIndexed { index, faceLandmark ->
                val scaledX = calculateScreenX(faceLandmark.x())
                val scaledY = calculateScreenY(faceLandmark.y())

                val jo = JSONObject().apply {
                    put("index", index)
                    put("isActive", true)
                    put("sx", scaledX)
                    put("sy", scaledY)
                }
                when (step) {
                    0 -> mvm.coordinates0.put(jo)
                    1 -> mvm.coordinates1.put(jo)
                }
                mvm.currentCoordinate.add(Pair(scaledX, scaledY))
                mvm.relativeCoordinate.add(Pair(faceLandmark.x(), faceLandmark.y()))
            }

            val vmPlr = mvm.currentCoordinate
            val vmRePlr = mvm.relativeCoordinate
            when (step) {
                0 -> {
                    // 468: 실제 오른쪽 눈  473: 실제 왼쪽 눈
                    val eyeAngle = calculateSlope(vmPlr[468].first , vmPlr[468].second, vmPlr[473].first, vmPlr[473].second)
                    // 234: 오른쪽 귓바퀴 454: 왼쪽 귓바퀴
                    val earFlapsAngle = calculateSlope(vmPlr[234].first, vmPlr[234].second, vmPlr[454].first, vmPlr[454].second)
                    // 입술끝 왼: 291 오: 61
                    val tipOfLipsAngle = calculateSlope(vmPlr[61].first, vmPlr[61].second, vmPlr[291].first, vmPlr[291].second)
                    // 미간-코
                    val glabellaNoseAngle = calculateSlope(vmPlr[1].first, vmPlr[1].second, vmPlr[8].first, vmPlr[8].second)
                    // 코-턱
                    val noseChinAngle = calculateSlope(vmPlr[152].first, vmPlr[152].second, vmPlr[1].first, vmPlr[1].second)
                    // 콧날개 오 64 왼 294
                    val earFlapNasalWingAngles =  Pair(
                        calculateSlope(vmPlr[234].first, vmPlr[234].second, vmPlr[64].first, vmPlr[64].second),
                        calculateSlope(vmPlr[294].first, vmPlr[294].second, vmPlr[454].first, vmPlr[454].second)
                    )
                    // 귓바퀴-코 거리
                    val earFlapNoseDistance =  Pair(
                        getRealDistanceX(Pair(vmRePlr[1].first, vmRePlr[1].second), Pair(vmRePlr[234].first, vmRePlr[234].second)),
                        getRealDistanceX(Pair(vmRePlr[1].first, vmRePlr[1].second), Pair(vmRePlr[454].first, vmRePlr[454].second))
                    )
                    // 입술산 중앙 - 양 입술 거리
                    val tipOfLipsCenterLipsDistance = Pair(
                        getRealDistanceX(Pair(vmRePlr[291].first, vmRePlr[291].second), Pair(vmRePlr[0].first, vmRePlr[0].second)),
                        getRealDistanceX(Pair(vmRePlr[61].first, vmRePlr[61].second), Pair(vmRePlr[0].first, vmRePlr[0].second))
                        )
                    // 양쪾을 벌렸을 때 7.2 안벌렸을 때 4.6
                    mvm.staticJson0.apply {
                        put("resting_eye_horizontal_angle", eyeAngle)
                        put("resting_earflaps_horizontal_angle", earFlapsAngle)
                        put("resting_tip_of_lips_horizontal_angle", tipOfLipsAngle)
                        put("resting_glabella_nose_vertical_angle", glabellaNoseAngle)
                        put("resting_nose_chin_vertical_angle", noseChinAngle)
                        put("resting_left_earflaps_nasal_wing_horizontal_angle", earFlapNasalWingAngles.first)
                        put("resting_right_earflaps_nasal_wing_horizontal_angle", earFlapNasalWingAngles.second)
                        put("resting_left_earflaps_nose_distance", earFlapNoseDistance.first)
                        put("resting_right_earflaps_nose_distance", earFlapNoseDistance.second)
                        put("resting_left_tip_of_lips_center_lips_distance", tipOfLipsCenterLipsDistance.first)
                        put("resting_right_tip_of_lips_center_lips_distance", tipOfLipsCenterLipsDistance.second)
                    }
                    Log.v("정면 각도들", "eyeAngle: $eyeAngle earFlapsAngle: $earFlapsAngle tipOfLipsAngle: $tipOfLipsAngle glabellaNoseAngle: $glabellaNoseAngle noseChinAngle: $noseChinAngle earFlapNasalWingAngle:$earFlapNasalWingAngles earFlapNoseDistance: $earFlapNoseDistance tipOfLipsCenterLipsDistance: $tipOfLipsCenterLipsDistance")
                    Log.v("제이슨", "${mvm.staticJson0}")
                }
                1 -> {
// 468: 실제 오른쪽 눈  473: 실제 왼쪽 눈
                    val eyeAngle = calculateSlope(vmPlr[468].first , vmPlr[468].second, vmPlr[473].first, vmPlr[473].second)
                    // 234: 오른쪽 귓바퀴 454: 왼쪽 귓바퀴
                    val earFlapsAngle = calculateSlope(vmPlr[234].first, vmPlr[234].second, vmPlr[454].first, vmPlr[454].second)
                    // 입술끝 왼: 291 오: 61
                    val tipOfLipsAngle = calculateSlope(vmPlr[61].first, vmPlr[61].second, vmPlr[291].first, vmPlr[291].second)
                    // 미간-코
                    val glabellaNoseAngle = calculateSlope(vmPlr[1].first, vmPlr[1].second, vmPlr[8].first, vmPlr[8].second)
                    // 코-턱
                    val noseChinAngle = calculateSlope(vmPlr[152].first, vmPlr[152].second, vmPlr[1].first, vmPlr[1].second)
                    // 콧날개 오 64 왼 294
                    val earFlapNasalWingAngles =  Pair(
                        calculateSlope(vmPlr[234].first, vmPlr[234].second, vmPlr[64].first, vmPlr[64].second),
                        calculateSlope(vmPlr[294].first, vmPlr[294].second, vmPlr[454].first, vmPlr[454].second)
                    )
                    // 귓바퀴-코 거리
                    val earFlapNoseDistance =  Pair(
                        getRealDistanceX(Pair(vmRePlr[1].first, vmRePlr[1].second), Pair(vmRePlr[234].first, vmRePlr[234].second)),
                        getRealDistanceX(Pair(vmRePlr[1].first, vmRePlr[1].second), Pair(vmRePlr[454].first, vmRePlr[454].second))
                    )
                    // 입술산 중앙 - 양 입술 거리
                    val tipOfLipsCenterLipsDistance = Pair(
                        getRealDistanceX(Pair(vmRePlr[291].first, vmRePlr[291].second), Pair(vmRePlr[0].first, vmRePlr[0].second)),
                        getRealDistanceX( Pair(vmRePlr[64].first, vmRePlr[64].second), Pair(vmRePlr[0].first, vmRePlr[0].second))
                    )

                    mvm.staticJson1.apply {
                        put("occlusal_eye_horizontal_angle", eyeAngle)
                        put("occlusal_earflaps_horizontal_angle", earFlapsAngle)
                        put("occlusal_tip_of_lips_horizontal_angle", tipOfLipsAngle)
                        put("occlusal_glabella_nose_vertical_angle", glabellaNoseAngle)
                        put("occlusal_nose_chin_vertical_angle", noseChinAngle)
                        put("occlusal_left_earflaps_nasal_wing_horizontal_angle", earFlapNasalWingAngles.first)
                        put("occlusal_right_earflaps_nasal_wing_horizontal_angle", earFlapNasalWingAngles.second)
                        put("occlusal_left_earflaps_nose_distance", earFlapNoseDistance.first)
                        put("occlusal_right_earflaps_nose_distance", earFlapNoseDistance.second)
                        put("occlusal_left_tip_of_lips_center_lips_distance", tipOfLipsCenterLipsDistance.first)
                        put("occlusal_right_tip_of_lips_center_lips_distance", tipOfLipsCenterLipsDistance.second)
                    }
                    Log.v("악물었을 때", "eyeAngle: $eyeAngle earFlapsAngle: $earFlapsAngle tipOfLipsAngle: $tipOfLipsAngle glabellaNoseAngle: $glabellaNoseAngle noseChinAngle: $noseChinAngle earFlapNasalWingAngle:$earFlapNasalWingAngles earFlapNoseDistance: $earFlapNoseDistance tipOfLipsCenterLipsDistance: $tipOfLipsCenterLipsDistance")
                    Log.v("제이슨", "${mvm.staticJson1}")
                }
            }
            mvm.currentFaceLandmarks = JSONArray()
        }
    }

    private fun initSettings() {
        binding.btnShooting.apply {
            text = "촬영"
            isEnabled = true
        }
        binding.tvSeqCount.text = "1 / 2"
        mvm.static0FileName = null
        mvm.static1FileName = null
        mvm.mergedJson0 = JSONObject()
        mvm.mergedJson1 = JSONObject()
        mvm.staticJson0 = JSONObject()
        mvm.staticJson1 = JSONObject()
        mvm.coordinates0 = JSONArray()
        mvm.coordinates1 = JSONArray()
        mvm.currentCoordinate = mutableListOf()
        mvm.currentFaceLandmarks = JSONArray()
        ivm.nameValue.value = ""
        ivm.mobileValue.value = ""
        ivm.isShownBtn = false
        gvm.isShowResult.value = false
        val inputDialog = TextInputDialogFragment()
        inputDialog.show(supportFragmentManager, "")
        seqStep.value = 0
    }

    private fun calculateScreenX(xx: Float): Float {
        val scaleFactor = binding.overlay.width * 1f / 1280
        val offsetX = ((binding.overlay.width - 1280 * scaleFactor) / 2 )
        val x = xx * binding.overlay.width / scaleFactor + offsetX

        return x
    }

    private fun calculateScreenY(yy: Float): Float {
        val scaleFactor = binding.overlay.height * 1f / 720
        val offsetY = (binding.overlay.height - 720 * scaleFactor) / 2
        val y = yy * binding.overlay.height / scaleFactor + offsetY

        return y
    }

}