package com.tangoplus.facebeauty.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
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
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignTop
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

class GalleryDialogFragment : DialogFragment(), OnMeasureClickListener {
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

        binding.clGDList.visibility = View.VISIBLE
        initShimmer()
        showListResult()

//        gvm.isShowResult.observe(viewLifecycleOwner) { isFinish ->
//            when (isFinish) {
//                true -> {
//                    setChangeUI()
//                    showDetailResult()
//                }
//                false -> {
//                    showListResult()
//                }
//            }
//        }

        binding.ibtnGDBack.setOnSingleClickListener {
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

//            val isListVisible = binding.clGDList.isVisible
//            if (isListVisible) {
//                // 결과보기 였을 경우
//                if (gvm.isShowResult.value == true) {
//
//                } else {
//                    dismiss()
//                }
//            } else {
//                setChangeUI()
//            }
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
    override fun onMeasureClick(tempServerSn: Int) {
        gvm.currentResult.value = gvm.currentFaceResults.find { it.tempServerSn == tempServerSn }
        scrollToView(binding.ibtnGDBack, binding.nsvGD)

        val infoDialog = InformationDialogFragment()
        infoDialog.show(requireActivity().supportFragmentManager, null)
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

}