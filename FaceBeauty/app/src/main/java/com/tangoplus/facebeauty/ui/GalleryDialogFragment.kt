package com.tangoplus.facebeauty.ui

import android.graphics.Color
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.data.db.FaceDao
import com.tangoplus.facebeauty.data.db.FaceDatabase
import com.tangoplus.facebeauty.data.db.FaceStatic
import com.tangoplus.facebeauty.databinding.FragmentGalleryDialogBinding
import com.tangoplus.facebeauty.ui.view.GridSpacingItemDecoration
import com.tangoplus.facebeauty.ui.view.OnMeasureClickListener
import com.tangoplus.facebeauty.util.FileUtility.readJsonFromUri
import com.tangoplus.facebeauty.vm.GalleryViewModel
import com.tangoplus.facebeauty.vm.InputViewModel
import com.tangoplus.facebeauty.vm.MeasureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.DrawLine
import com.tangoplus.facebeauty.data.DrawRatioLine
import com.tangoplus.facebeauty.data.FaceComparisonItem
import com.tangoplus.facebeauty.data.FaceLandmarkResult.Companion.fromCoordinates
import com.tangoplus.facebeauty.data.RVItemType
import com.tangoplus.facebeauty.ui.view.OnAdapterMoreClickListener
import com.tangoplus.facebeauty.ui.view.OnFaceStaticCheckListener
import com.tangoplus.facebeauty.util.FileUtility.extractImageCoordinates
import com.tangoplus.facebeauty.util.FileUtility.getImageUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.getJsonUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.scrollToView
import com.tangoplus.facebeauty.util.FileUtility.setImage
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.util.FileUtility.toFaceStatic
import com.tangoplus.facebeauty.util.MathHelpers.calculateRatios

class GalleryDialogFragment : DialogFragment(), OnMeasureClickListener, OnFaceStaticCheckListener, OnAdapterMoreClickListener {
    private lateinit var binding : FragmentGalleryDialogBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private val ivm : InputViewModel by activityViewModels()
    private val gvm : GalleryViewModel by activityViewModels()
    private lateinit var fDao : FaceDao
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기 셋업
        binding.clGDImage.visibility = View.GONE
        binding.clGDList.visibility = View.VISIBLE
        initShimmer()
        gvm.isShowResult.observe(viewLifecycleOwner) { isFinish ->
            when (isFinish) {
                true -> {
                    setChangeUI()
                    showDetailResult()
                }
                false -> {
                    showListResult()
                }
            }
        }

        binding.ibtnGDBack.setOnSingleClickListener {
            val isListVisible = binding.clGDList.isVisible
            if (isListVisible) {
                // 결과보기 였을 경우
                if (gvm.isShowResult.value == true) {
                    MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                        setTitle("알림")
                        setMessage("측정을 다시 시작하시겠습니까?")
                        setPositiveButton("예") { _,_ ->
                            mvm.initMeasure.value = true
                            dismiss()
                        }
                        setNegativeButton("아니오") { _, _ -> }
                        show()
                    }
                } else {
                    dismiss()
                }
            } else {
                setChangeUI()
            }
        }


    }

    private fun setAdapter() {
        binding.rvGD1.apply {
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }

            val spacingPx = (5 * resources.displayMetrics.density).toInt() // 아이템 간 간격
            addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))
            val measureAdapter = MeasureRVAdapter(requireContext(), gvm.currentFaceResults)
            measureAdapter.measureClickListener = this@GalleryDialogFragment
            // 40dp를 픽셀로 변환
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = measureAdapter
            Log.v("어댑터데이터", "${gvm.currentFaceResults}")
        }
    }

    private fun convertToFaceResult(faceStatics: List<FaceStatic>): FaceResult {
        val imageUris = mutableListOf<Uri?>()
        val jsonArray = JSONArray()

        faceStatics.forEach { faceStatic ->
            // 1. mediaFileUri를 Uri로 변환해서 추가
            try {
                val imageUri = getImageUriFromFileName(requireContext(), faceStatic.mediaFileName)
//                Log.v("imageUri있나요?", "$imageUri, ${faceStatic.mediaFileName} ${faceStatic.user_name}/${faceStatic.user_mobile}")
                imageUris.add(imageUri)
            } catch (e: Exception) {
                imageUris.add(null)
                Log.e("FaceResultConverter", "이미지 URI 파싱 실패: ${faceStatic.mediaFileName}", e)
            }

            // 2. jsonFileUri에서 JSON 파일 읽어서 JSONObject로 변환
            try {
                val jsonUri = getJsonUriFromFileName(requireContext(), faceStatic.jsonFileName)
                val jsonObject = jsonUri?.let { readJsonFromUri(requireContext(), it) }
//                Log.v("jsonObject있나요?", "${faceStatic.jsonFileName} $jsonUri ${faceStatic.user_name}/${faceStatic.user_mobile}, $jsonObject")
                jsonArray.put(jsonObject)
            } catch (e: Exception) {
                Log.e("FaceResultConverter", "JSON 파일 읽기 실패: ${faceStatic.jsonFileName}", e)
                // 빈 JSONObject라도 추가해서 배열 순서 맞추기
                jsonArray.put(JSONObject())
            }
        }

        return FaceResult(
            tempServerSn = faceStatics[0].temp_server_sn,
            userName = faceStatics[0].user_name,
            userMobile = faceStatics[0].user_mobile,
            imageUris = imageUris,
            results = jsonArray,
            regDate = faceStatics[0].reg_date
        )
    }


    private fun convertToFaceResults(faceStaticList: List<FaceStatic>): List<FaceResult> {
        // 현재 ViewModel에 있는 tempServerSn 목록 가져오기
        val existingTempServerSns = gvm.currentFaceResults.map { it.tempServerSn }.toSet()

        // temp_server_sn으로 그룹화하되, 이미 존재하는 것은 제외
        val groupedData = faceStaticList
            .filter { it.temp_server_sn >= 0 && !existingTempServerSns.contains(it.temp_server_sn) }
            .groupBy { it.temp_server_sn }

        Log.v("그룹된데이터", "$groupedData")
        Log.v("기존데이터", "기존 tempServerSns: $existingTempServerSns")

        return groupedData.map { (_, faceStatics) ->
            convertToFaceResult(faceStatics)
        }
    }

    private fun setChangeUI() {
        val isListVisible = binding.clGDList.isVisible

        when (isListVisible) {
            true -> {
                // 선택 후 이미지 보이기
                binding.clGDList.visibility = View.GONE
                binding.clGDImage.visibility = View.VISIBLE
                showDetailResult()
            }
            false -> {
                binding.clGDList.visibility = View.VISIBLE
                binding.clGDImage.visibility = View.GONE
                showListResult()

                // 선택한 체크박스 초기화
                gvm.currentCheckedLines.clear()
                binding.msGD.isChecked = false
            }
        }

    }
    private fun showListResult() {
        lifecycleScope.launch(Dispatchers.IO) {
            val fd = FaceDatabase.getDatabase(requireContext())
            fDao = fd.faceDao()

            val allFaceStatics = fDao.getAllData()
            Log.v("전부", "${allFaceStatics.map { it.temp_server_sn }}")
            val aa = convertToFaceResults(allFaceStatics)
            aa.forEach {
                gvm.currentFaceResults.add(it)
            }
            gvm.currentFaceResults.sortByDescending { it.tempServerSn }
//            Log.v("mediaJson파일이름", "sn: ${gvm.currentFaceResults.map { it.tempServerSn }} media: ${gvm.currentFaceResults.map { it.imageUris }}")
            withContext(Dispatchers.Main) {
                cancelShimmer()
                val countText = "총 기록 건수: ${gvm.currentFaceResults.size}건"
                binding.tvGDMeasureCount.text = countText
                setAdapter()
            }
        }
    }

    private fun showDetailResult() {
        binding.ssiv1.recycle()
        binding.ssiv2.recycle()

        val faceStaticJson0 = gvm.currentResult.value?.results?.getJSONObject(0)?.getJSONObject("data")
        val faceStatic0 = faceStaticJson0.toFaceStatic()
        Log.v("스태틱가져오기", "0: $faceStaticJson0")
        val faceStaticJson1 = gvm.currentResult.value?.results?.getJSONObject(1)?.getJSONObject("data")
        val faceStatic1 = faceStaticJson1.toFaceStatic()
        Log.v("스태틱가져오기", "1: $faceStaticJson1")

        gvm.currentFaceComparision = buildFaceComparisonList(faceStatic0, faceStatic1).toMutableList()
        gvm.currentFaceComparision.apply {
//            add(0, FaceComparisonItem("", 0f, 0f, type = RVItemType.TITLE))
//            add(0, FaceComparisonItem("", 0f, 0f, type = RVItemType.TITLE))
        }
        Log.v("staticDatas", "${gvm.currentFaceComparision}")

        val faceStaticAdapter = FaceStaticRVAdapter(gvm.currentFaceComparision)

        binding.msGD.setOnCheckedChangeListener { _, isChecked ->
            // 리스너 동작
            setLinesInImage(isChecked)
            faceStaticAdapter.setAllChecked(isChecked)
        }

        binding.rvGD2.apply {
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
            val spacingPx = (10 * resources.displayMetrics.density).toInt()
            val specialSpacingPx = (30 * resources.displayMetrics.density).toInt()

            val specialRows = setOf(3) // row index 기준: 0부터 시작
            addItemDecoration(GridSpacingItemDecoration(
                spanCount = 3,
                spacingPx = spacingPx,
                includeEdge = true,
                specialRowSpacingPx  = specialSpacingPx,
                specialRows  = specialRows
            ))

            faceStaticAdapter.faceStaticCheckListener = this@GalleryDialogFragment
            faceStaticAdapter.adapterMoreClickedListener = this@GalleryDialogFragment
            val layoutManagerr = GridLayoutManager(requireContext(), 3)
            layoutManager = layoutManagerr
            layoutManagerr.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (faceStaticAdapter.getItemViewType(position)) {
                        FaceStaticRVAdapter.TYPE_MORE -> 3 // 더보기 버튼은 전체 차지
                        else -> 1 // 일반 아이템은 1칸만 차지
                    }
                }
            }

//            Log.v("어댑터데이터", "${gvm.currentFaceResults}")

            adapter  = faceStaticAdapter
        }
        setImage()
        showZoomInDialogFragment()
        setRatioCheckSwitch()
    }

    fun buildFaceComparisonList(resting: FaceStatic, occlusal: FaceStatic): List<FaceComparisonItem> {
        return listOf(
            FaceComparisonItem("눈 수평 각도", resting.resting_eye_horizontal_angle, occlusal.occlusal_eye_horizontal_angle),
            FaceComparisonItem("귓바퀴 수평 각도", resting.resting_earflaps_horizontal_angle, occlusal.occlusal_earflaps_horizontal_angle),
            FaceComparisonItem("입술 끝 수평 각도", resting.resting_tip_of_lips_horizontal_angle, occlusal.occlusal_tip_of_lips_horizontal_angle),
            FaceComparisonItem("미간-코 수직 각도", resting.resting_glabella_nose_vertical_angle, occlusal.occlusal_glabella_nose_vertical_angle),
            FaceComparisonItem("코-턱 수직 각도", resting.resting_nose_chin_vertical_angle, occlusal.occlusal_nose_chin_vertical_angle),
            FaceComparisonItem("좌측 귓바퀴-콧망울 수평 각도", resting.resting_left_earflaps_nasal_wing_horizontal_angle, occlusal.occlusal_left_earflaps_nasal_wing_horizontal_angle),
            FaceComparisonItem("우측 귓바퀴-콧망울 수평 각도", resting.resting_right_earflaps_nasal_wing_horizontal_angle, occlusal.occlusal_right_earflaps_nasal_wing_horizontal_angle),
            FaceComparisonItem("좌측 귓바퀴-코 거리", resting.resting_left_earflaps_nose_distance, occlusal.occlusal_left_earflaps_nose_distance),
            FaceComparisonItem("우측 귓바퀴-코 거리", resting.resting_right_earflaps_nose_distance, occlusal.occlusal_right_earflaps_nose_distance),
            FaceComparisonItem("좌측 입꼬리-입술 중앙 거리", resting.resting_left_tip_of_lips_center_lips_distance, occlusal.occlusal_left_tip_of_lips_center_lips_distance),
            FaceComparisonItem("우측 입꼬리-입술 중앙 거리", resting.resting_right_tip_of_lips_center_lips_distance, occlusal.occlusal_right_tip_of_lips_center_lips_distance),
        )
    }

    override fun onMeasureClick(tempServerSn: Int) {
        gvm.currentResult.value = gvm.currentFaceResults.find { it.tempServerSn == tempServerSn }
        setChangeUI()
        scrollToView(binding.ibtnGDBack, binding.nsvGD)
    }

    override fun onFaceStaticCheck(drawLineIndex: Int, isChecked: Boolean) {
        val selectedLine = when (drawLineIndex) {
            0 -> DrawLine.A_EYE
            1 -> DrawLine.A_EARFLAP
            2 -> DrawLine.A_TIP_OF_LIPS
            3 -> DrawLine.A_GLABELLA_NOSE
            4 -> DrawLine.A_NOSE_CHIN
            5 -> DrawLine.A_EARFLAP_NASAL_WING
            6 -> DrawLine.A_EARFLAP_NASAL_WING
            7 -> DrawLine.D_EARFLAP_NOSE
            8 -> DrawLine.D_EARFLAP_NOSE
            9 -> DrawLine.D_TIP_OF_LIPS_CENTER_LIPS
            10 -> DrawLine.D_TIP_OF_LIPS_CENTER_LIPS
            else -> DrawLine.A_EYE
        }
        when (isChecked) {
            true -> gvm.currentCheckedLines.add(selectedLine)
            false -> gvm.currentCheckedLines.remove(selectedLine)
        }
        Log.v("체크", "$drawLineIndex, $isChecked / $selectedLine ${gvm.currentCheckedLines}")
        setImage()
    }
    private fun setImage() {
        lifecycleScope.launch {
            Log.v("체크", "setImage: ${gvm.currentCheckedRatioLines}")
            gvm.currentResult.value?.let { setImage(this@GalleryDialogFragment, it, 0, binding.ssiv1, gvm) }
            gvm.currentResult.value?.let { setImage(this@GalleryDialogFragment, it, 1, binding.ssiv2, gvm) }
        }
    }

    private fun showZoomInDialogFragment() {
        binding.ssiv1.setOnLongClickListener {
            val zoomInDialog = ZoomInDialogFragment.newInstance(0)
            zoomInDialog.show(requireActivity().supportFragmentManager, "")
            true
        }
        binding.ssiv2.setOnLongClickListener {
            val zoomInDialog = ZoomInDialogFragment.newInstance(1)
            zoomInDialog.show(requireActivity().supportFragmentManager, "")
            true
        }
    }

    private fun setLinesInImage(switchedOn: Boolean) {
        when (switchedOn) {
            true -> {
                gvm.currentCheckedLines.add(DrawLine.A_EYE)
                gvm.currentCheckedLines.add(DrawLine.A_EARFLAP)
                gvm.currentCheckedLines.add(DrawLine.A_TIP_OF_LIPS)
//                gvm.currentCheckedLines.add(DrawLine.A_GLABELLA_NOSE)
//                gvm.currentCheckedLines.add(DrawLine.A_NOSE_CHIN)
//                gvm.currentCheckedLines.add(DrawLine.A_EARFLAP_NASAL_WING)
//                gvm.currentCheckedLines.add(DrawLine.D_EARFLAP_NOSE)
//                gvm.currentCheckedLines.add(DrawLine.D_TIP_OF_LIPS_CENTER_LIPS)
            }
            false -> {
                gvm.currentCheckedLines.clear()
            }
        }
        setImage()
    }

    private fun initShimmer() {
        binding.sflGD.apply {
            visibility = View.VISIBLE
            startShimmer()
        }
    }

    private fun cancelShimmer() {
        binding.sflGD.apply {
            visibility = View.GONE
            stopShimmer()
        }
    }

    private fun setRatioCheckSwitch() {
        val df = DecimalFormat("#.#")
        // 값 적기
        val jsonData = gvm.currentResult.value?.results?.getJSONObject(0)
        val coordinates = extractImageCoordinates(jsonData)
        if (coordinates != null) {
            val vertiIndices = listOf(234, 33, 133, 362, 263, 356)
            val vertiCoordinates = vertiIndices.map { coordinates[it] }
            Log.v("vertiCoordinates", "$vertiCoordinates")
            val horizonIndices = listOf(8, 2, 13, 152)
            val horizonCoordinates = horizonIndices.map { coordinates[it] }
            Log.v("horizonCoordinates", "$horizonCoordinates")
            val vertiText = calculateRatios(vertiCoordinates, true)
            val horizonText = calculateRatios(horizonCoordinates, false)
            binding.tvGDVerti.text = "${vertiText.map { df.format(it) }}"
                .replace("[", "")
                .replace("]", "")
                .replace(", ", " : ")
            binding.tvGDHorizon.text = "${horizonText.map { df.format(it) }}"
                .replace("[", "")
                .replace("]", "")
                .replace(", ", " : ")
        }





        binding.cbGDVerti.setOnCheckedChangeListener { _, isChecked ->
            setRatioLineInImage(true, isChecked)
        }
        binding.cbGDHorizon.setOnCheckedChangeListener { _, isChecked ->
            setRatioLineInImage(false, isChecked)
        }
    }

    private fun setRatioLineInImage(isVerti: Boolean, switchedOn: Boolean) {
        when (switchedOn) {
            true -> {
                if (isVerti) gvm.currentCheckedRatioLines.add(DrawRatioLine.A_VERTI)
                else gvm.currentCheckedRatioLines.add(DrawRatioLine.A_HORIZON)
            }
            false -> {
                if (isVerti) gvm.currentCheckedRatioLines.remove(DrawRatioLine.A_VERTI)
                else gvm.currentCheckedRatioLines.remove(DrawRatioLine.A_HORIZON)
            }
        }
        Log.v("비율이미지", "${gvm.currentCheckedRatioLines}")
        setImage()
    }

    override fun adapterMoreClicked(isExpanded: Boolean) {
        Log.v("확장", "inFragment = $isExpanded")
        if (!isExpanded) {
            binding.llGDRatioTitle.visibility = View.VISIBLE
            binding.tvGDRatioTitle.visibility = View.VISIBLE
        } else {
            binding.llGDRatioTitle.visibility = View.GONE
            binding.tvGDRatioTitle.visibility = View.GONE
        }
    }
}