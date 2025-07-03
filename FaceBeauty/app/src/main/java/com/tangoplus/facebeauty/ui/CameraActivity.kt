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
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.data.db.FaceDao
import com.tangoplus.facebeauty.data.db.FaceDatabase
import com.tangoplus.facebeauty.databinding.ActivityCameraBinding
import com.tangoplus.facebeauty.ui.AnimationUtility.animateTextViewToTopLeft
import com.tangoplus.facebeauty.util.FileUtility.getRequiredPermissions
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.util.MathHelpers.calculateScaleFromPart
import com.tangoplus.facebeauty.util.MathHelpers.calculateSlope
import com.tangoplus.facebeauty.util.MathHelpers.getRealDistanceX
import com.tangoplus.facebeauty.util.MathHelpers.setScaleX
import com.tangoplus.facebeauty.util.SoundManager.playSound
import com.tangoplus.facebeauty.vision.face.FaceBlendshapesResultAdapter
import com.tangoplus.facebeauty.vision.face.FaceLandmarkerHelper
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
import com.tangoplus.facebeauty.util.MathHelpers.calculatePolygonArea
import com.tangoplus.facebeauty.util.MathHelpers.correctingValue
import com.tangoplus.facebeauty.util.MathHelpers.getRealDistanceY
import com.tangoplus.facebeauty.util.PreferenceUtility
import com.tangoplus.facebeauty.vision.pose.PoseLandmarkerHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs

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
    // 카메라 플래그
    private var isCameraActive = false
    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

    private var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

    var latestResult: FaceLandmarkerHelper.ResultBundle? = null
    private val mvm : MeasureViewModel by viewModels()
    private val ivm : InputViewModel by viewModels()

    private var seqStep = MutableLiveData(0)
    private val maxSeq = 6

    private var scaleFactorX : Float? = null
    private var scaleFactorY : Float? = null

    private lateinit var fDao : FaceDao
    private lateinit var prefsUtil : PreferenceUtility

    private var bounceAnimator: AnimatorSet? = null

    // pose
    private lateinit var poseLandmarker: PoseLandmarkerHelper

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
                    hideViews()
                    if (seqStep.value != null) {
                        playSound(R.raw.camera_shutter)
                        lifecycleScope.launch {
                            captureImage(seqStep.value ?: -1)
                            latestResult?.let { resultBundleToJson(it, seqStep.value ?: -1) }
                            viewModel.setSeqFinishedFlag(true)
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
        if (!isCameraActive) {
            if (!hasPermissions(this)) {
                ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_CODE_PERMISSIONS)
            } else {
                backgroundExecutor.execute {
                    if (faceLandmarkerHelper.isClose()) {
                        faceLandmarkerHelper.setupFaceLandmarker()
                    }
                }
            }
            isCameraActive = true
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
        }
    }

    override fun onStop() {
        super.onStop()
        isCameraActive = false

        // onStop에서만 실제 리소스 해제
        if(this::faceLandmarkerHelper.isInitialized) {
            try {
                faceLandmarkerHelper.clearFaceLandmarker()
            } catch (e: Exception) {
                Log.e("CameraActivity", "Error clearing face landmarker: ${e.message}")
            }
        }

        if(this::poseLandmarker.isInitialized) {
            try {
                poseLandmarker.clearPoseLandmarker()
            } catch (e: Exception) {
                Log.e("CameraActivity", "Error clearing pose landmarker: ${e.message}")
            }
        }
        cameraProvider?.unbindAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        // onDestroy에서 executor 종료
        if (!backgroundExecutor.isShutdown) {
            backgroundExecutor.shutdownNow()
        }
    }
    override fun onStart() {
        super.onStart()

        // 1. 백그라운드 executor 다시 생성
        if (backgroundExecutor.isShutdown) {
            backgroundExecutor = Executors.newSingleThreadExecutor()
        }

        // 2. 얼굴 랜드마커 다시 생성
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

        // 3. 카메라 다시 설정
        binding.viewFinder.post {
            setUpCamera()
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
        initializePoseLandmarker()

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

                val staticJSON0 = setJo(mvm.staticJA.getJSONObject(0),0)
                val staticJSON1 = setJo(mvm.staticJA.getJSONObject(1),1)
                val staticJSON2 = setJo(mvm.staticJA.getJSONObject(2),2)
                val staticJSON3 = setJo(mvm.staticJA.getJSONObject(3),3)
                val staticJSON4 = setJo(mvm.staticJA.getJSONObject(4),4)
                val staticJSON5 = setJo(mvm.staticJA.getJSONObject(5),5)

                val staticJos = listOf(staticJSON0, staticJSON1, staticJSON2, staticJSON3, staticJSON4, staticJSON5)
                val transStatics = staticJos.map { it.toFaceStatic() } // static으로 변하기 -> json으로 저장 전 파일 이름과 landmark 담아야함

                val mergedJOBeforeFileName = mutableListOf<JSONObject>()
                for (i in 0 until 6) {
                    val mergedJSON = JSONObject().apply {
                        put("data", JSONObject(transStatics[i].toJSONObject()))
                        put("face_landmark", mvm.coordinatesJA.getJSONArray(i))
                        put("pose_landmark", mvm.plrJA.getJSONArray(i))
                    }
                    mergedJOBeforeFileName.add(mergedJSON)
                    val jsonPath = saveJsonToStorage(mergedJSON, mvm.staticFileNames[i].replace(".jpg", ""))
                    mergedJOBeforeFileName[i].put("jsonFileName", jsonPath)
                    mvm.mergedJA.put(mergedJOBeforeFileName[i])
                }

                Log.v("mvm.mergedJson", "${mvm.mergedJA}")
                // faceResult 객체 1개의 날짜 정함
                val finishedResult = FaceResult(
                    tempServerSn = prefsUtil.getNextTempServerSn(),
                    userName = ivm.nameValue.value,
                    userMobile = ivm.mobileValue.value,
                    imageUris = listOf(
                        getImageUriFromFileName(this@CameraActivity, mvm.staticFileNames[0]),
                        getImageUriFromFileName(this@CameraActivity, mvm.staticFileNames[1]),
                        getImageUriFromFileName(this@CameraActivity, mvm.staticFileNames[2]),
                        getImageUriFromFileName(this@CameraActivity, mvm.staticFileNames[3]),
                        getImageUriFromFileName(this@CameraActivity, mvm.staticFileNames[4]),
                        getImageUriFromFileName(this@CameraActivity, mvm.staticFileNames[5]),
                    ),
                    results = mvm.mergedJA
                )
                Log.v("mvm.mergedJson", "${finishedResult.results.getJSONObject(0).getJSONObject("data")}")
                viewModel.currentResult.value = finishedResult
                lifecycleScope.launch(Dispatchers.IO) {
                    val fd = FaceDatabase.getDatabase(this@CameraActivity)
                    fDao = fd.faceDao()
                    // static 2개를 만들어서 DB 저장
                    val static0 = mvm.mergedJA.getJSONObject(0).getJSONObject("data").toFaceStatic()
                    val static1 = mvm.mergedJA.getJSONObject(1).getJSONObject("data").toFaceStatic()
                    val static2 = mvm.mergedJA.getJSONObject(2).getJSONObject("data").toFaceStatic()
                    val static3 = mvm.mergedJA.getJSONObject(3).getJSONObject("data").toFaceStatic()
                    val static4 = mvm.mergedJA.getJSONObject(4).getJSONObject("data").toFaceStatic()
                    val static5 = mvm.mergedJA.getJSONObject(5).getJSONObject("data").toFaceStatic()
                    Log.v("static0", "변환완료: $static0")
                    Log.v("static1", "변환완료: $static1")
                    Log.v("static2", "변환완료: $static2")
                    Log.v("static3", "변환완료: $static3")
                    fDao.insertStatic(static0)
                    fDao.insertStatic(static1)
                    fDao.insertStatic(static2)
                    fDao.insertStatic(static3)
                    fDao.insertStatic(static4)
                    fDao.insertStatic(static5)
                    finishedResult.regDate = static0.reg_date

                    withContext(Dispatchers.Main) {
                        viewModel.comparisonDoubleItem = null
                        val intent = Intent(this@CameraActivity, MainActivity::class.java)
                        intent.putExtra("isMeasureFinish", true)
                        startActivity(intent)
                        finishAffinity()

                    }
                }
            } else {
                if (!isCountDown) {
                    startTimer()
                }
            }

        }
        binding.tvGoGallery.setOnSingleClickListener {
            val intent = Intent(this@CameraActivity, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
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
//        bounceAnimator = AnimatorSet()
//        startBouncingAnimation(bounceAnimator, binding.tvSeqGuide)
        binding.lavCamera.setAnimation("focusing_screen.json")
        binding.lavCamera.repeatCount = LottieDrawable.INFINITE
        binding.lavCamera.playAnimation()


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

            if (errorCode == FaceLandmarkerHelper.GPU_ERROR) {
               Log.e("GPU_ERROR", "$errorCode, FaceLandmarkerHelper GPU_ERROR")
            }
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
                val vertiBoolean = if (eyeDistanceGap < 0.275f) true else false
                val horizonBoolean = if (seqStep.value == 5) {
                    horizontalLineVector in 45f..85f
                } else {
                    horizontalLineVector in 110f..150f
                }
                binding.overlay.setHorizon(horizonBoolean)
//                Log.v("얼굴 중앙", "$isFaceCenter")
//                Log.v("라인벡터", "코: ${faceLandmarks[0].x()}, ${faceLandmarks[0].y()}, 왼쪽눈: ${faceLandmarks[33].x()}, ${faceLandmarks[33].x()}, 오른쪽 눈: ${faceLandmarks[263].x()}, ${faceLandmarks[263].x()}")

                val eyeSlope = calculateSlope(leftEye.x(), leftEye.y(), rightEye.x(), rightEye.y())
                val eyeParallel = (eyeSlope in 176.5f..181f) || (eyeSlope in -181f .. -176.5f)

                val vertiMediator = vertiBoolean && eyeParallel

//                Log.v("eye평행", "$eyeSlope ${eyeParallel}, ${vertiBoolean}, ")
                binding.overlay.setVerti(vertiMediator)
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

            if (ivm.isFinishInput && isFaceCenter && resultBundle.result.faceLandmarks().isNotEmpty() && !viewModel.getSeqFinishedFlag()) {
                // 애니메이션 제거 flag
                if (!viewModel.getGuideTextFlag()) {
                    binding.lavCamera.clearAnimation()
                    binding.lavCamera.visibility = View.INVISIBLE
                    bounceAnimator?.cancel()
                    bounceAnimator = null
                    binding.tvSeqGuide.animate().cancel()
                    binding.tvSeqGuide.clearAnimation()
                    binding.tvSeqGuide.translationY = 0f
                    binding.tvSeqGuide.text = "턱관절 교합상태를 진단합니다\n편한 상태로 입을 다물고 정면을 응시해주세요\n게이지가 차면 자동으로 촬영을 시작합니다 !"
                    viewModel.setGuideTextFlag(true)
                }

                // guideText 움직이기 flag
                if (!viewModel.getTextAnimationFlag()) {
                    Handler(Looper.getMainLooper()).postDelayed({

                        animateTextViewToTopLeft(binding.clSeqGuide, binding.tvSeqGuide, 0.1f, 0.05f)
                    }, 2000)
                    viewModel.setTextAnimationFlag(true)
                }
                binding.fdgv.triggerIntroAnimationIfNeeded()
            }

            if (
                ivm.isFinishInput &&
                isFaceCenter &&
                !viewModel.getSeqFinishedFlag() &&
                !viewModel.getCountDownFlag() &&
                binding.overlay.getVerti() &&
                binding.overlay.getHorizon()
                ) {
                binding.fdgv.startSuccessAnimation {
                    if (!isCountDown && !viewModel.getCountDownFlag()) {
                        startTimer()
                    }
                }
            } else {
                binding.fdgv.resetSuccessMode()
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
            clearAnimation()
            text = when (seq) {
                0 -> "턱관절 교합상태를 진단합니다\n편한 상태로 입을 다물고 정면을 응시해주세요\n게이지가 차면 자동으로 촬영을 시작합니다 !"
                1 -> "두번째, 교합 상태입니다\n이를 맞물리게 물고 입술을 벌려보세요"
                2 -> "다음은 턱 가측 이동 상태입니다\n정면에서 아래턱을 최대한 왼쪽으로 위치해주세요"
                3 -> "잘하셨습니다 !\n이번엔 아래턱을 최대한 오른쪽으로 위치해주세요"
                4 -> "다섯번째는 개구상태입니다\n입을 벌려 정면을 응시해주세요"
                5 -> "마지막 단계는 목을 편 상태입니다\n턱을 위로 들어 기다려주세요\n자동으로 촬영을 시작합니다"
                6 -> "수고하셨습니다\n버튼을 눌러 결과를 확인해보세요"
                else -> ""
            }
            visibility = View.VISIBLE
            animation = animationSet
        }
    }


    private fun isFaceInCenter(glabellaX: Float, glabellaY: Float): Boolean {
        val targetRect = RectF(0.35f, 0.175f, 0.65f, 0.525f)
        return targetRect.contains(glabellaX, glabellaY)
    }

    // ------! 타이머 control 시작 !------
    private fun startTimer() {
        // 시작 버튼 후 시작
        binding.btnShooting.isEnabled = false
        binding.tvGoGallery.isEnabled = false
        Log.v("seqStep", "${seqStep.value} / 6")
        mCountDown.start()
        viewModel.setCountDownFlag(true)
        // ------! 타이머 control 끝 !------
    }

    private fun updateUI() {
        seqStep.value = seqStep.value?.plus(1)
        when (seqStep.value) {
            maxSeq -> {
                setGuideAnimation(maxSeq)
                binding.clCountDown.visibility = View.GONE
                binding.btnShooting.apply {
                    visibility = View.VISIBLE
                    text = "결과보기"
                    isEnabled = true
                }
                mCountDown.cancel()
                Log.v("몇단계?", "Max repeats reached, stopping the loop")

            }
            else -> {
                seqStep.value?.let { setGuideAnimation(it) }
                binding.tvSeqCount.text = "${seqStep.value?.plus(1)} / 6"
                viewModel.setCountDownFlag(false)
                binding.fdgv.resetSuccessMode()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            viewModel.setSeqFinishedFlag(false)
        }, 1500)
        isCountDown = false
        binding.tvGoGallery.isEnabled = true
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

    private suspend fun captureImage(step: Int): Boolean {
        val imageCapture = imageCapture
        val name = getFileName(step)

        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val tempFile = File.createTempFile("temp_capture", ".jpg", cacheDir)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(this@CameraActivity),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val tempUri = Uri.fromFile(tempFile)
                            saveMediaToStorage(this@CameraActivity, tempUri, name)
                            continuation.resume(true)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                            if (tempFile.exists()) tempFile.delete()
                            continuation.resume(false)
                        }
                    }
                )
            }
        }
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

            // --------------------------# 목젖 위치를 잡기 위한 detectAsync #-------------------------
            Log.v("포즈 들가자", "${seqStep.value}")
            val resultBundle = poseLandmarker.detectImage(bitmap)
            if (resultBundle?.results?.first()?.landmarks()?.isNotEmpty() == true) {
                val plr = resultBundle.results.first().landmarks()[0]
//                    val tempLandmarks = mutableMapOf<Int, Triple<Float, Float, Float>>()
//                    // 원시데이터 pose 33개
//                    plr.forEachIndexed { index, poseLandmark ->
//                        tempLandmarks[index] = Triple(
//                            poseLandmark.x(),
//                            poseLandmark.y(),
//                            poseLandmark.z()
//                        )
//                    }
                mvm.tempPlrJA = JSONArray()
                plr.forEachIndexed { index, _ ->
                    val swapIndex = if (index >= 7 && index % 2 == 0) index - 1 // 짝수인 경우 뒤의 홀수 인덱스로 교체
                    else if (index >= 7) index + 1 // 홀수인 경우 앞의 짝수 인덱스로 교체
                    else index // 7 미만인 경우 그대로 사용
                    val targetLandmark = plr[swapIndex]

                    val scaledX = calculateScreenX(targetLandmark.x())
                    val scaledY = calculateScreenY(targetLandmark.y())

                    val jo = JSONObject().apply {
                        put("index", index)
                        put("isActive", true)
                        put("sx", scaledX)
                        put("sy", scaledY)
                        put("wx", targetLandmark.x())
                        put("wy", targetLandmark.y())
                        put("wz", targetLandmark.z())
                    }

//                        if (index in listOf(7, 8, 11, 12)) {
//                            Log.v("포즈 들가자", "${index} : (${calculateScreenX(targetLandmark.x()).roundToInt()}, ${calculateScreenY(targetLandmark.y()).roundToInt()}), 원본: (${tempLandmarks[index]?.first}, ${tempLandmarks[index]?.second})")
//                        }
                    mvm.tempPlrJA.put(jo)
                    mvm.currentPlrCoordinate.add(Pair(scaledX, scaledY)) // 33 개가 다 담김 // TODO 그냥 일단 다 담고 잘 되면 4개만 넣는 걸로
                }
                // 마지막 자세만 값 저장
                mvm.plrJA.put(mvm.tempPlrJA)
            }
            // -------------------------# 목젖 위치를 잡기 위한 detectAsync 끝 #-----------------------
            imageUri?.let { imageURI ->
                context.contentResolver.openOutputStream(imageURI)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()
                }
                Log.d("SaveMedia", "Image saved to Pictures/TangoBeauty: $fileName$extension")
            } ?: run {
                Log.e("SaveMedia", "Failed to create image URI")
            }
            mvm.staticFileNames.add("$fileName$extension")

            Log.d("SaveMedia", "seqStep: ${seqStep.value} vmFileName: ${mvm.staticFileNames}")

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
            0 -> "1-$timestamp"
            1 -> "2-$timestamp"
            2 -> "3-$timestamp"
            3 -> "4-$timestamp"
            4 -> "5-$timestamp"
            5 -> "6-$timestamp"
            else -> ""
        }
    }
    // --------------------------------- # pose landmark # -----------------------------------------
    private val poseLandmarkListener = object : PoseLandmarkerHelper.LandmarkerListener {
        override fun onError(error: String, errorCode: Int) {
            runOnUiThread {
                Log.e("PoseLandmark", "Pose detection error: $error $errorCode")
                Toast.makeText(this@CameraActivity, "Pose detection error: $error", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {}
    }
    private fun initializePoseLandmarker() {
        try {
            poseLandmarker = PoseLandmarkerHelper(
                context = this, // 이제 this가 완전히 초기화된 상태
                runningMode = RunningMode.IMAGE,
                minPoseDetectionConfidence = PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE,
                minPoseTrackingConfidence = PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE,
                minPosePresenceConfidence = PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE,
                currentDelegate = PoseLandmarkerHelper.DELEGATE_CPU,
                poseLandmarkerHelperListener = poseLandmarkListener
            )
            Log.d("PoseLandmarker", "Initialization successful!")
        } catch (e: Exception) {
            Log.e("PoseLandmarker", "Failed to initialize: ${e.message}", e)
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
            val flr = resultBundle.result.faceLandmarks()?.get(0)

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
            mvm.tempCoordinateJA = JSONArray()
            flr?.forEachIndexed { index, faceLandmark ->
                val scaledX = calculateScreenX(faceLandmark.x())
                val scaledY = calculateScreenY(faceLandmark.y())

                val jo = JSONObject().apply {
                    put("index", index)
                    put("isActive", true)
                    put("sx", scaledX)
                    put("sy", scaledY)
                    put("wx", faceLandmark.x())
                    put("wy", faceLandmark.y())
                    put("wz", faceLandmark.z())
                }
                mvm.tempCoordinateJA.put(jo)
                mvm.currentCoordinate.add(Pair(scaledX, scaledY))
                mvm.relativeCoordinate.add(Pair(faceLandmark.x(), faceLandmark.y()))
            }
            // seq 1개의 좌표를 담고 tempCoordinates는 초기화
            mvm.coordinatesJA.put(mvm.tempCoordinateJA)
            val vmFlr = mvm.currentCoordinate
            when (step) {
                0 -> {
                    val eyeBrowAngle = calculateSlope(vmFlr[107].first , vmFlr[107].second, vmFlr[336].first, vmFlr[336].second)

                    // 468: 실제 오른쪽 눈  473: 실제 왼쪽 눈
                    val eyeAngle = calculateSlope(vmFlr[468].first , vmFlr[468].second, vmFlr[473].first, vmFlr[473].second)
                    val correctionValue = 180f - eyeAngle
                    // 입술끝 왼: 291 오: 61
                    val tipOfLipsAngle = calculateSlope(vmFlr[61].first, vmFlr[61].second, vmFlr[291].first, vmFlr[291].second)
                    val correctTipOfLipsAngle = correctingValue(tipOfLipsAngle, correctionValue)
                    // 턱
                    val tipOfChinsAngle = calculateSlope(vmFlr[148].first, vmFlr[148].second, vmFlr[377].first, vmFlr[377].second)
                    val correctTipOfChinsAngle = correctingValue(tipOfChinsAngle, correctionValue)
                    val canthusOralAngle = Pair(
                        calculateSlope(vmFlr[33].first, vmFlr[33].second, vmFlr[61].first, vmFlr[61].second),
                        calculateSlope(vmFlr[291].first, vmFlr[291].second, vmFlr[263].first, vmFlr[263].second)
                    )
                    val nasalWingLipsAngle = Pair(
                        calculateSlope(vmFlr[64].first, vmFlr[64].second, vmFlr[61].first, vmFlr[61].second),
                        calculateSlope(vmFlr[291].first, vmFlr[291].second, vmFlr[294].first, vmFlr[294].second)
                    )
                    val leftCheeks = listOf(197, 127, 234, 93, 132, 58, 172, 136, 150, 149, 176, 148, 152)
                    val rightCheeks = listOf(197, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152)
                    val leftCheeksPoint = leftCheeks.map { vmFlr[it] }
                    val rightCheeksPoint = rightCheeks.map { vmFlr[it] }
                    val cheeksExtents = Pair(
                        calculatePolygonArea(leftCheeksPoint),
                        calculatePolygonArea(rightCheeksPoint)
                    )
                    // 양쪾을 벌렸을 때 7.2 안벌렸을 때 4.6
                    val tempStatic = JSONObject().apply {
                        put("resting_eye_horizontal_angle", eyeAngle)
                        put("resting_eyebrow_horizontal_angle", eyeBrowAngle)
                        put("resting_tip_of_lips_horizontal_angle", correctTipOfLipsAngle)
                        put("resting_tip_of_chin_horizontal_angle", correctTipOfChinsAngle)
                        put("resting_canthus_oral_left_vertical_angle", canthusOralAngle.second)
                        put("resting_canthus_oral_right_vertical_angle", canthusOralAngle.first)
                        put("resting_nasal_wing_tip_of_lips_left_vertical_angle", nasalWingLipsAngle.second)
                        put("resting_nasal_wing_tip_of_lips_right_vertical_angle", nasalWingLipsAngle.first)
                        put("resting_left_cheeks_extent", cheeksExtents.second)
                        put("resting_right_cheeks_extent", cheeksExtents.first)

                    }
                    mvm.staticJA.put(tempStatic)
                    Log.v("정면 각도들", "eyeAngle: $eyeAngle eyebrowAngle: $eyeBrowAngle tipOfLipsAngle: $tipOfLipsAngle chinAngle: $correctTipOfChinsAngle canthusOralAngle: $canthusOralAngle nasalWingLipsAngle:$nasalWingLipsAngle, cheeksExtent: $cheeksExtents")
//                    Log.v("제이슨", "${}")
                }
                1 -> {
                    val eyeBrowAngle = calculateSlope(vmFlr[107].first , vmFlr[107].second, vmFlr[336].first, vmFlr[336].second)

                    // 468: 실제 오른쪽 눈  473: 실제 왼쪽 눈
                    val eyeAngle = calculateSlope(vmFlr[468].first , vmFlr[468].second, vmFlr[473].first, vmFlr[473].second)
                    val correctionValue = 180f - eyeAngle
                    // 입술끝 왼: 291 오: 61
                    val tipOfLipsAngle = calculateSlope(vmFlr[61].first, vmFlr[61].second, vmFlr[291].first, vmFlr[291].second)
                    val correctTipOfLipsAngle = correctingValue(tipOfLipsAngle, correctionValue)
                    // 턱
                    val tipOfChinsAngle = calculateSlope(vmFlr[148].first, vmFlr[148].second, vmFlr[377].first, vmFlr[377].second)
                    val correctTipOfChinsAngle = correctingValue(tipOfChinsAngle, correctionValue)
                    val canthusOralAngle = Pair(
                        calculateSlope(vmFlr[33].first, vmFlr[33].second, vmFlr[61].first, vmFlr[61].second),
                        calculateSlope(vmFlr[291].first, vmFlr[291].second, vmFlr[263].first, vmFlr[263].second)
                    )
                    val nasalWingLipsAngle = Pair(
                        calculateSlope(vmFlr[64].first, vmFlr[64].second, vmFlr[61].first, vmFlr[61].second),
                        calculateSlope(vmFlr[291].first, vmFlr[291].second, vmFlr[294].first, vmFlr[294].second)
                    )
                    val leftCheeks = listOf(197, 127, 234, 93, 132, 58, 172, 136, 150, 149, 176, 148, 152)
                    val rightCheeks = listOf(197, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152)
                    val leftCheeksPoint = leftCheeks.map { vmFlr[it] }
                    val rightCheeksPoint = rightCheeks.map { vmFlr[it] }
                    val cheeksExtents = Pair(
                        calculatePolygonArea(leftCheeksPoint),
                        calculatePolygonArea(rightCheeksPoint)
                    )
                    // 양쪾을 벌렸을 때 7.2 안벌렸을 때 4.6
                    val tempStatic = JSONObject().apply {
                        put("occlusal_eye_horizontal_angle", eyeAngle)
                        put("occlusal_eyebrow_horizontal_angle", eyeBrowAngle)
                        put("occlusal_tip_of_lips_horizontal_angle", correctTipOfLipsAngle)
                        put("occlusal_tip_of_chin_horizontal_angle", correctTipOfChinsAngle)
                        put("occlusal_canthus_oral_left_vertical_angle", canthusOralAngle.second)
                        put("occlusal_canthus_oral_right_vertical_angle", canthusOralAngle.first)
                        put("occlusal_nasal_wing_tip_of_lips_left_vertical_angle", nasalWingLipsAngle.second)
                        put("occlusal_nasal_wing_tip_of_lips_right_vertical_angle", nasalWingLipsAngle.first)
                        put("occlusal_left_cheeks_extent", cheeksExtents.second)
                        put("occlusal_right_cheeks_extent", cheeksExtents.first)

                    }
                    mvm.staticJA.put(tempStatic)
                    Log.v("교합 각도들", "eyeAngle: $eyeAngle eyebrowAngle: $eyeBrowAngle tipOfLipsAngle: $tipOfLipsAngle chinAngle: $correctTipOfChinsAngle canthusOralAngle: $canthusOralAngle nasalWingLipsAngle:$nasalWingLipsAngle, cheeksExtent: $cheeksExtents")
//                    Log.v("제이슨", "${}")
                }
                2 -> {
                    val tiltNoseChinAngle = calculateSlope(vmFlr[1].first , vmFlr[1].second, vmFlr[152].first, vmFlr[152].second)
                    val tiltTipOfLipsAngle = calculateSlope(vmFlr[61].first , vmFlr[1].second, vmFlr[291].first, vmFlr[291].second)
                    val mandibularDistance = Pair(
                        getRealDistanceX(Pair(vmFlr[13].first, vmFlr[13].second,), Pair(vmFlr[58].first, vmFlr[58].second)),
                        getRealDistanceX(Pair(vmFlr[13].first, vmFlr[13].second,), Pair(vmFlr[288].first, vmFlr[288].second))
                    )

                    // 양쪾을 벌렸을 때 7.2 안벌렸을 때 4.6
                    val tempStatic = JSONObject().apply {
                        put("jaw_left_tilt_nose_chin_vertical_angle", tiltNoseChinAngle)
                        put("jaw_left_tilt_tip_of_lips_horizontal_anngle", tiltTipOfLipsAngle)
                        put("jaw_left_tilt_left_mandibular_distance", mandibularDistance.second)
                        put("jaw_left_tilt_right_mandibular_distance", mandibularDistance.first)

                    }
                    mvm.staticJA.put(tempStatic)
                    Log.v("왼쪽 턱 쏠림 각도들", "noseChinAngle: $tiltNoseChinAngle tipOfLipsAngle: $tiltTipOfLipsAngle  mandibularDistance: $mandibularDistance")
                }
                3 -> {
                    val tiltNoseChinAngle = calculateSlope(vmFlr[1].first , vmFlr[1].second, vmFlr[152].first, vmFlr[152].second)
                    val tiltTipOfLipsAngle = calculateSlope(vmFlr[61].first , vmFlr[1].second, vmFlr[291].first, vmFlr[291].second)
                    val mandibularDistance = Pair(
                        getRealDistanceX(Pair(vmFlr[13].first, vmFlr[13].second,), Pair(vmFlr[58].first, vmFlr[58].second)),
                        getRealDistanceX(Pair(vmFlr[13].first, vmFlr[13].second,), Pair(vmFlr[288].first, vmFlr[288].second))
                    )

                    // 양쪾을 벌렸을 때 7.2 안벌렸을 때 4.6
                    val tempStatic = JSONObject().apply {
                        put("jaw_right_tilt_nose_chin_vertical_angle", tiltNoseChinAngle)
                        put("jaw_right_tilt_tip_of_lips_horizontal_anngle", tiltTipOfLipsAngle)
                        put("jaw_right_tilt_left_mandibular_distance", mandibularDistance.second)
                        put("jaw_right_tilt_right_mandibular_distance", mandibularDistance.first)

                    }
                    mvm.staticJA.put(tempStatic)
                    Log.v("오른쪽 턱 쏠림 각도들", "noseChinAngle: $tiltNoseChinAngle tipOfLipsAngle: $tiltTipOfLipsAngle  mandibularDistance: $mandibularDistance")

                }
                4 -> {
                    val openingLipsDisance = getRealDistanceY(Pair(vmFlr[13].first, vmFlr[13].second,), Pair(vmFlr[14].first, vmFlr[14].second))
                    val openingLipsAngle = calculateSlope(vmFlr[13].first , vmFlr[13].second, vmFlr[14].first, vmFlr[14].second)

                    // 양쪾을 벌렸을 때 7.2 안벌렸을 때 4.6
                    val tempStatic = JSONObject().apply {
                        put("jaw_opening_lips_distance", openingLipsDisance)
                        put("jaw_opening_lips_vertical_angle", openingLipsAngle)
                    }
                    mvm.staticJA.put(tempStatic)
                    Log.v("입 벌림 각도들", "openingLipsDisance: $openingLipsDisance openingLipsAngle: $openingLipsAngle ")

                }
                5 -> {
                    // TODO 이 값들은 전부 pose_landmark를 사용해야 함
                    val plr = mvm.currentPlrCoordinate

                    val shoulderHorizontalAngle = calculateSlope(plr[11].first , plr[11].second, plr[12].first, plr[12].second)
                    val earHorizontalAngle = calculateSlope(plr[7].first , plr[7].second, plr[8].first, plr[8].second)

                    val midShoulder = Pair((plr[11].first + plr[12].first)/ 2, (plr[11].second + plr[12].second) / 2)
                    val neckVerticalAngle = calculateSlope(midShoulder.first , midShoulder.second, vmFlr[1].first, vmFlr[1].second)

                    // 양쪾을 벌렸을 때 7.2 안벌렸을 때 4.6
                    val tempStatic = JSONObject().apply {
                        put("neck_extention_shoulder_horizontal_angle", shoulderHorizontalAngle)
                        put("neck_extention_ear_horizontal_angle", earHorizontalAngle)
                        put("neck_extention_neck_vertical_angle", neckVerticalAngle)
                    }
                    mvm.staticJA.put(tempStatic)
                    Log.v("고개 젖힘", "shoulderHorizontalAngle: $shoulderHorizontalAngle earHorizontalAngle: $earHorizontalAngle  neckVerticalAngle: $neckVerticalAngle")

                }
            }
            mvm.currentFaceLandmarks = JSONArray()
        }
    }

    private fun initSettings() {
        binding.btnShooting.apply {
            text = "촬영"
            isEnabled = true
            visibility = View.INVISIBLE
        }
        binding.tvSeqCount.text = "1 / 4"
        mvm.staticFileNames.clear()
//        mvm.static1FileName = null
//        mvm.mergedJson0 = JSONObject()
//        mvm.mergedJson1 = JSONObject()
//        mvm.staticJson0 = JSONObject()
//        mvm.staticJson1 = JSONObject()
//        mvm.coordinates0 = JSONArray()
//        mvm.coordinates1 = JSONArray()
        mvm.currentCoordinate = mutableListOf()
        mvm.currentFaceLandmarks = JSONArray()
        ivm.nameValue.value = ""
        ivm.mobileValue.value = ""
        ivm.isShownBtn = false
        viewModel.comparisonDoubleItem = null
        val inputDialog = TextInputDialogFragment()
        inputDialog.show(supportFragmentManager, "")
        seqStep.value = 0

        // flat 초기화
        viewModel.setSeqFinishedFlag(false)
        viewModel.setGuideTextFlag(false)
        viewModel.setCountDownFlag(false)
        viewModel.setTextAnimationFlag(false)
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
    private fun setJo(originJo : JSONObject, seq: Int) : JSONObject {
        return originJo.apply {
            put("temp_server_sn", prefsUtil.getLastTempServerSn())
            put("mediaFileName", mvm.staticFileNames[seq])
            put("jsonFileName", mvm.staticFileNames[seq].replace(".jpg", ".json"))
            put("seq", seq)
            put("user_uuid", mvm.currentUUID)
            put("user_name", ivm.nameValue.value.toString())
            put("user_mobile", ivm.mobileValue.value.toString())
        }
    }
}