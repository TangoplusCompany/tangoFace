package com.tangoplus.facebeauty.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.tangoplus.facebeauty.ui.AnimationUtility.animateExpand
import com.tangoplus.facebeauty.ui.adapter.MeasureRVAdapter
import com.tangoplus.facebeauty.ui.view.GridSpacingItemDecoration
import com.tangoplus.facebeauty.ui.listener.OnMeasureClickListener
import com.tangoplus.facebeauty.util.FileUtility.getImageUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.getJsonUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.readJsonFromUri
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.vm.InputViewModel
import com.tangoplus.facebeauty.vm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.view.isVisible


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
        setSideBar()
        showListResult()
        setComparisonClick()
        setObserveComparisonState()

    }

    private fun setAdapter(spanCount : Int) {
        bd.rvM1.apply {
            // 페이드 아웃 애니메이션
            animate()
                .alpha(0f)
                .setDuration(150)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 기존 decoration 제거
                        while (itemDecorationCount > 0) {
                            removeItemDecorationAt(0)
                        }

                        val spacingPx = (5 * resources.displayMetrics.density).toInt() // 아이템 간 간격
                        addItemDecoration(GridSpacingItemDecoration(spanCount, spacingPx, true))
                        val measureAdapter = MeasureRVAdapter(requireContext(), mvm.currentFaceResults, mvm)

                        measureAdapter.measureClickListener = this@MainFragment
                        layoutManager = GridLayoutManager(requireContext(), spanCount)
                        adapter = measureAdapter
                        Log.v("어댑터데이터", "${mvm.currentFaceResults}")

                        // 페이드 인 애니메이션
                        animate()
                            .alpha(1f)
                            .setDuration(150)
                            .setListener(null)
                            .start()
                    }
                })
                .start()
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

//            Log.v("mediaJson파일이름", "sn: ${gvm.currentFaceResults.map { it.tempServerSn }} media: ${gvm.currentFaceResults.map { it.imageUris }}")
            withContext(Dispatchers.Main) {
                if (mvm.isMeasureFinish) {
                    mvm.currentResult.value = mvm.currentFaceResults.firstOrNull()
                    val infoDialog = InformationDialogFragment()
                    infoDialog.show(requireActivity().supportFragmentManager, "")
                }
                cancelShimmer()
                val countText = "총 기록 건수: ${mvm.currentFaceResults.size}건"
                bd.tvMMeasureCount.text = countText
                setAdapter(2)
            }
        }
    }

    private fun setComparisonClick() {
        bd.btnMComparision.setOnSingleClickListener {
            if (bd.btnMComparision.text == "취소") {
                mvm.clearItems()
                bd.btnMComparision.text = "비교 하기"
                mvm.setComparisonState(true)
            }
            if (mvm.currentFaceResults.size < 2) {
                val balloon = Balloon.Builder(requireContext())
                    .setWidth(BalloonSizeSpec.WRAP)
                    .setHeight(BalloonSizeSpec.WRAP)
                    .setText("측정 기록이 최소 2개 이상 있어야 합니다 !\n하단의 버튼을 눌러 측정 해보세요")
                    .setTextColorResource(R.color.black)
                    .setTextSize(16f)
                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                    .setArrowSize(0)
                    .setMargin(16)
                    .setPadding(12)
                    .setCornerRadius(12f)
                    .setBackgroundColorResource(R.color.white)
                    .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                    .setLifecycleOwner(viewLifecycleOwner)
                    .setOnBalloonDismissListener {  }
                    .build()
                bd.btnMComparision.showAlignBottom(balloon)
                balloon.dismissWithDelay(3000L)
                balloon.setOnBalloonClickListener { balloon.dismiss() }
                return@setOnSingleClickListener
            }

            if (mvm.tempComparisonItems.value?.size == 2 && bd.btnMComparision.text == "결과 보기") {
                val sortedTempItems = mvm.tempComparisonItems.value?.sortedByDescending { it.regDate }
                mvm.comparisonDoubleItem = Pair(sortedTempItems?.get(0)!! , sortedTempItems[1])
                val infoDialog = InformationDialogFragment()
                infoDialog.show(requireActivity().supportFragmentManager, "")

                // 비교해제하면서 전부 끄기
                Handler(Looper.getMainLooper()).postDelayed({ mvm.setComparisonState(false) }, 250)
                return@setOnSingleClickListener
            }

            if (mvm.getComparisonState()) {
                bd.btnMComparision.text = "비교 하기"
//                bd.btnMComparision.isEnabled = true
                mvm.setComparisonState(false)

            } else {
                bd.btnMComparision.text = "취소"
//                bd.btnMComparision.isEnabled = false
                mvm.setComparisonState(true)

                // 변경 시작 할 때 observe 설정
                if (!mvm.tempComparisonItems.hasObservers()) {
                    setObserveComparisonItems()
                }
            }
        }
    }
    private fun setObserveComparisonState() {
        mvm.comparisonState.observe(viewLifecycleOwner) { isComparison ->
            if (!isComparison) {
                // 비교 모드가 false가 되면 초기화
                mvm.clearItems()
                bd.btnMComparision.text = "비교 하기"
                resetAllRecyclerViewItems()
            }
        }

    }

    private fun resetAllRecyclerViewItems() {
        val adapter = bd.rvM1.adapter as? MeasureRVAdapter
        adapter?.resetAllItemsUI()
    }


    private fun setComparisonAdapter() {
        if (mvm.tempComparisonItems.value != null) {
            val adapter = MeasureRVAdapter(requireContext(), mvm.tempComparisonItems.value!!, mvm, 1)
            val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            bd.rvM2.layoutManager = layoutManager
            bd.rvM2.adapter = adapter
        }

    }

    private fun setObserveComparisonItems() {
        mvm.tempComparisonItems.observe(viewLifecycleOwner) {
            setComparisonAdapter()

            if (it.size == 2) {
                bd.btnMComparision.text = "결과 보기"
                Log.v("버튼풀림0", "${it[0]}")
                Log.v("버튼풀림1", "${it[1]}")
            } else {
                bd.btnMComparision.text = "취소"
            }
        }
    }

    private fun setSideBar() {
        bd.ibtnMNavi.setOnSingleClickListener {
            if (bd.clMNavi.isVisible) {
                // 숨기기
                animateExpand(requireContext(), bd.clMNavi, false) {
                    bd.clMNavi.visibility = View.GONE
                    setAdapter(3)
                }
            } else {
                // 보이기
                bd.clMNavi.visibility = View.VISIBLE
                animateExpand(requireContext(), bd.clMNavi, true) {
                    setAdapter(2)
                }
            }
        }
    }
    fun analyzeString(input: String)  {

    }
    private fun setSearchInput() {
        bd.etMSearch.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val afterText = s.toString()


                var filteredResult = listOf<FaceResult>()
                if (afterText.isNotEmpty()) {
                    when {
                        afterText.all { it.isDigit() } -> {
                            val number = afterText.toIntOrNull()
                            if (number != null) {
                                filteredResult = mvm.currentFaceResults.filter { it.userMobile?.contains(afterText) == true }
                            }
                        }
                        afterText.any { it.isLetter() } -> {
                            val hasEnglish = afterText.any { it.isLetter() && it.code in 65..122 }
                            val hasKorean = afterText.any { it.code in 0xAC00..0xD7A3 }
                            if (hasEnglish || hasKorean) {
                                filteredResult = mvm.currentFaceResults.filter { it.userName?.contains(afterText) == true }
                            }
                        }
                    }
                }
                // TODO 어댑터에 들어갈 데이터 변경 후 새롭게 갱신되게끔 하기 + TODO 어댑터위에 텍스트 데코 레이션 넣기
//                setAdapter(filteredResult, 2)

                
            }
        })
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