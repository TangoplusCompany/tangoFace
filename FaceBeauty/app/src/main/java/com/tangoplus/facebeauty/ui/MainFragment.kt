package com.tangoplus.facebeauty.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.data.db.FaceDao
import com.tangoplus.facebeauty.data.db.FaceDatabase
import com.tangoplus.facebeauty.data.db.FaceStatic
import com.tangoplus.facebeauty.databinding.FragmentMainBinding
import com.tangoplus.facebeauty.ui.adapter.MeasureRVAdapter
import com.tangoplus.facebeauty.ui.view.GridSpacingItemDecoration
import com.tangoplus.facebeauty.ui.listener.OnMeasureClickListener
import com.tangoplus.facebeauty.util.FileUtility.getImageUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.getJsonUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.readJsonFromUri
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.vm.InputViewModel
import com.tangoplus.facebeauty.vm.MainViewModel
import com.tangoplus.facebeauty.vm.MeasureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject


class MainFragment : Fragment(), OnMeasureClickListener {
    private lateinit var bd : FragmentMainBinding

//    private val mvm : MeasureViewModel by activityViewModels()
    private val ivm : InputViewModel by activityViewModels()
    private val mvm : MainViewModel by activityViewModels()
    private lateinit var fDao : FaceDao
//    override fun onResume() {
//        super.onResume()
//        // full Screen code
//        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
//        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bd = FragmentMainBinding.inflate(layoutInflater)
        return bd.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기 셋업

        bd.clMList.visibility = View.VISIBLE
        initShimmer()
        showListResult()
        setComparisonClick()
    }

    private fun setAdapter() {
        bd.rvM1.apply {
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }

            val spacingPx = (5 * resources.displayMetrics.density).toInt() // 아이템 간 간격
            addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))
            val measureAdapter = MeasureRVAdapter(requireContext(), mvm.currentFaceResults, mvm)
            measureAdapter.measureClickListener = this@MainFragment
            // 40dp를 픽셀로 변환
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = measureAdapter
            Log.v("어댑터데이터", "${mvm.currentFaceResults}")
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
        val existingTempServerSns = mvm.currentFaceResults.map { it.tempServerSn }.toSet()

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
                mvm.currentFaceResults.add(it)
            }
            mvm.currentFaceResults.sortByDescending { it.tempServerSn }

            mvm.currentResult.value = mvm.currentFaceResults.firstOrNull()
            if (mvm.isMeasureFinish) {
                val infoDialog = InformationDialogFragment()
                infoDialog.show(requireActivity().supportFragmentManager, "")
            }
            // TODO 잘 들어오는지 확인하기 
//            Log.v("mediaJson파일이름", "sn: ${gvm.currentFaceResults.map { it.tempServerSn }} media: ${gvm.currentFaceResults.map { it.imageUris }}")
            withContext(Dispatchers.Main) {
                cancelShimmer()
                val countText = "총 기록 건수: ${mvm.currentFaceResults.size}건"
                bd.tvMMeasureCount.text = countText
                setAdapter()
            }


        }
    }

    private fun setComparisonClick() {
        if (mvm.currentFaceResults.size < 2) {
            bd.btnMComparision.isEnabled = false
            val balloon = Balloon.Builder(requireContext())
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText("측정을 더 시도해 비교해주세요")
                .setTextColorResource(R.color.black)
                .setTextSize(20f)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowSize(0)
                .setMargin(6)
                .setPadding(12)
                .setCornerRadius(8f)
                .setBackgroundColorResource(R.color.white)
                .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                .setLifecycleOwner(viewLifecycleOwner)
                .setOnBalloonDismissListener {  }
                .build()
            bd.btnMComparision.showAlignBottom(balloon)
            balloon.dismissWithDelay(3000L)
            balloon.setOnBalloonClickListener { balloon.dismiss() }

        } else {
            bd.btnMComparision.isEnabled = true
        }
        bd.btnMComparision.setOnSingleClickListener {

            if (mvm.tempComparisonDoubleItem.value?.size == 2 && bd.btnMComparision.text == "결과 확인") {
                val sortedTempItems = mvm.tempComparisonDoubleItem.value?.sortedByDescending { it.regDate }
                mvm.comparisonDoubleItem = Pair(sortedTempItems?.get(1) , sortedTempItems?.get(0))

                val infoDialog = InformationDialogFragment()
                infoDialog.show(requireActivity().supportFragmentManager, "")
                return@setOnSingleClickListener
            }

            if (mvm.getComparisonState()) {
                bd.btnMComparision.text = "비교 하기"
//                bd.btnMComparision.isEnabled = true
                mvm.setComparisonState(false)

            } else {
                bd.btnMComparision.text = "결과 확인"
//                bd.btnMComparision.isEnabled = false
                mvm.setComparisonState(true)
            }

            // 변경 시작 할 때 observe 설정
            setObserveComparisonItems()
        }
    }

    private fun setObserveComparisonItems() {
        mvm.tempComparisonDoubleItem.observe(viewLifecycleOwner) {

            if (it.size < 2) {
                bd.btnMComparision.isEnabled = false
            } else {
                bd.btnMComparision.isEnabled = true
            }
        }
    }


    override fun onMeasureClick(tempServerSn: Int) {
        mvm.currentResult.value = mvm.currentFaceResults.find { it.tempServerSn == tempServerSn }
//        scrollToView(bd.ibtnMBack, bd.nsvM)

        val infoDialog = InformationDialogFragment()
        infoDialog.show(requireActivity().supportFragmentManager, null)
    }

    private fun initShimmer() {
        bd.sflM.apply {
            visibility = View.VISIBLE
            startShimmer()
        }
    }

    private fun cancelShimmer() {
        bd.sflM.apply {
            visibility = View.GONE
            stopShimmer()
        }
    }

}