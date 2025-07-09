package com.tangoplus.facebeauty.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.db.FaceDao
import com.tangoplus.facebeauty.databinding.FragmentMainBinding
import com.tangoplus.facebeauty.ui.AnimationUtility.animateExpand
import com.tangoplus.facebeauty.ui.adapter.MeasureRVAdapter
import com.tangoplus.facebeauty.ui.listener.OnMeasureClickListener
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.vm.InputViewModel
import com.tangoplus.facebeauty.vm.MainViewModel
import androidx.core.view.isVisible
import com.tangoplus.facebeauty.data.FaceDisplay


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

        mvm.dataLoadComplete.observe(viewLifecycleOwner) { isLoaded ->

            if (isLoaded == true) {
//                Log.v("displayList", "${mvm.displayList.value}")

                if (mvm.isMeasureFinish) {
                    mvm.currentResult.value = mvm.currentFaceResults.firstOrNull()
                    val infoDialog = InformationDialogFragment()
                    infoDialog.show(requireActivity().supportFragmentManager, "")
                }
//                Log.v("displayList", "${mvm.displayList.value}")

                cancelShimmer()
                val countText = "총 기록 건수: ${mvm.currentFaceResults.size}건"
                bd.tvMMeasureCount.text = countText
                setAdapter(mvm.displayList.value ?: listOf(), 2)

                mvm.dataLoadComplete.value = false

                if (mvm.currentFaceResults.size == 0) {
                    bd.ivMEmpty.visibility = View.VISIBLE
                } else {
                    bd.ivMEmpty.visibility = View.GONE
                }
            }
        }

        // Room DB 로딩 시작
        mvm.loadDataFromDB(requireContext())


        bd.clMList.visibility = View.VISIBLE
        initShimmer()
        setSideBar()
        setComparisonClick()
        setObserveComparisonState()
        setSearchInput()
    }

    private fun setAdapter(data: List<FaceDisplay>, spanCount : Int) {
        bd.rvM1.apply {
//            while (itemDecorationCount > 0 ) removeItemDecorationAt(0)
            val measureAdapter = MeasureRVAdapter(requireContext(), data, mvm)
            measureAdapter.measureClickListener = this@MainFragment

            // 페이드 아웃 애니메이션
            animate()
                .alpha(0f)
                .setDuration(250)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 기존 decoration 제거
//                        val spacingPx = (5 * resources.displayMetrics.density).toInt() // 아이템 간 간격
//                        addItemDecoration(GridSpacingItemDecoration(spanCount, spacingPx, true))

                        layoutManager = GridLayoutManager(requireContext(), spanCount)
                        adapter = measureAdapter
//                        Log.v("어댑터데이터", "${mvm.currentFaceResults}")
                        // 페이드 인 애니메이션
                        animate()
                            .alpha(1f)
                            .setDuration(250)
                            .setListener(null)
                            .start()


                    }
                })
                .start()
//            Log.v("어댑터데이터", "${mvm.currentFaceResults}")
        }

    }

//    private fun getUserListByDB() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            val fd = FaceDatabase.getDatabase(requireContext())
//            fDao = fd.faceDao()
//
//            val allFaceStatics = fDao.getAllData()
//            Log.v("전부", "${allFaceStatics.map { it.temp_server_sn }}")
//            val aa = convertToFaceResults(allFaceStatics)
//            aa.forEach {
//                mvm.currentFaceResults.add(it)
//            }
//            mvm.currentFaceResults.sortByDescending { it.tempServerSn }
//
////            Log.v("mediaJson파일이름", "sn: ${gvm.currentFaceResults.map { it.tempServerSn }} media: ${gvm.currentFaceResults.map { it.imageUris }}")
//            withContext(Dispatchers.Main) {
//                if (mvm.isMeasureFinish) {
//                    mvm.currentResult.value = mvm.currentFaceResults.firstOrNull()
//                    val infoDialog = InformationDialogFragment()
//                    infoDialog.show(requireActivity().supportFragmentManager, "")
//                }
//                cancelShimmer()
//                val countText = "총 기록 건수: ${mvm.currentFaceResults.size}건"
//                bd.tvMMeasureCount.text = countText
//                setAdapter(mvm.currentFaceResults,2)
//            }
//        }
//    }

    private fun setComparisonClick() {
        bd.btnMComparision.setOnSingleClickListener {
            if (bd.btnMComparision.text == "취소") {
                mvm.clearItems()
                bd.btnMComparision.text = "아이템 선택"
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
                val sortedTempItems = mvm.tempComparisonItems.value?.sortedBy { it.regDate }

                // 왼쪽 오른쪽 찾아서 DOUBLE 만들기
                val leftResult = mvm.currentFaceResults.find { it.tempServerSn == sortedTempItems?.get(0)?.tempServerSn }
                val rightResult = mvm.currentFaceResults.find { it.tempServerSn == sortedTempItems?.get(1)?.tempServerSn }


                mvm.comparisonDoubleItem = Pair(leftResult!! , rightResult!!)
                val infoDialog = InformationDialogFragment()
                infoDialog.show(requireActivity().supportFragmentManager, "")

                // 비교해제하면서 전부 끄기
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        mvm.setComparisonState(false)
                        bd.etMSearch.setText("")
                    }, 250)
                return@setOnSingleClickListener
            }

            if (mvm.getComparisonState()) {
                bd.btnMComparision.text = "아이템 선택"
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
            } else {
                bd.btnMComparision.text = "취소"
            }
        }
    }

    private fun setSideBar() {
        bd.ibtnMNavi.setOnSingleClickListener {
            if (bd.clMNavi.isVisible) {
                // 숨기기

                animateExpand(requireContext(), bd.clMNavi,   false) {
                    bd.clMNavi.visibility = View.GONE
                    bd.etMSearch.setText("")
                    mvm.displayList.value?.let {
                        it1 -> setAdapter(it1,4)
                    }
                }
            } else {
                // 보이기
                bd.clMNavi.visibility = View.VISIBLE
                animateExpand(requireContext(), bd.clMNavi, true) {
                    mvm.displayList.value?.let { it1 -> setAdapter(it1,2) }
                }
            }
        }
    }

    private fun setSearchInput() {
        val hangulSyllableRegex = Regex("^[가-힣]+$")

        bd.etMSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val afterText = s.toString()
                val filteredResult: List<FaceDisplay>

                if (afterText.isNotEmpty()) {
                    when {
                        // 숫자만 입력된 경우
                        afterText.all { it.isDigit() } -> {
                            val number = afterText.toIntOrNull()
                            if (number != null) {
                                filteredResult = mvm.displayList.value?.filter {
                                    it.userMobile?.contains(afterText) == true
                                }!!
                                setAdapter(filteredResult, 2)
                            }
                        }
                        hangulSyllableRegex.matches(afterText) -> {
                            filteredResult = mvm.displayList.value?.filter {
                                it.userName?.contains(afterText) == true
                            }!!
                            setAdapter(filteredResult, 2)
                        }
                        else -> {}
                    }
                } else {
                    mvm.displayList.value?.let { it1 -> setAdapter(it1,2) }
                    return
                }
            }
        })
    }
//    private fun setSwitchRvState() {
//        bd.msMFilter.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
//                bd.etMSearch.setText("")
//                setGrouppingByUser(mvm.currentFaceResults)
//            } else {
//                setAdapter(mvm.currentFaceResults, 2, true)
//            }
//        }
//    }
//    private fun setGrouppingByUser(faceResults: List<FaceResult>) {
//        // 1. 이름별로 그룹화하고 정렬
//        val groupedByName = faceResults
//            .groupBy { it.userName ?: "이름 없음" }
//            .toSortedMap() // 이름 기준 오름차순 정렬
//
//        // 2. 각 그룹 내에서 tempServerSn 기준으로 정렬하고 평면화
//        val sortedList = groupedByName
//            .flatMap { (_, group) ->
//                group.sortedByDescending { it.tempServerSn }
//            }
//        val isHeaderFunction: (Int) -> Boolean = { position ->
//            if (position == 0) {
//                true
//            } else {
//                val current = sortedList[position]
//                val previous = sortedList[position - 1]
//                (current.userName ?: "이름 없음") != (previous.userName ?: "이름 없음")
//            }
//        }
//
//        // 3. 어댑터 설정
////        setAdapter(sortedList, 2)
//        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
//        gridLayoutManager.spanSizeLookup = object  : GridLayoutManager.SpanSizeLookup() {
//            override fun getSpanSize(position: Int): Int {
//                Log.v("isHeaderFunction", "${isHeaderFunction(position)}")
//                return  if (isHeaderFunction(position)) {
//                    Log.v("헤더", "2")
//                    2
//                } else {
//                    Log.v("헤더", "1")
//                    1
//                }
////                return 1
//            }
//        }
//        bd.rvM1.layoutManager = gridLayoutManager
//        while (bd.rvM1.itemDecorationCount > 0) {
//            bd.rvM1.removeItemDecorationAt(0)
//        }
//
//        // 4. 스티키 헤더 데코레이션 추가
//        val decoration = StickyHeaderItemDecoration(
//            isHeader = { position ->
//                if (position == 0) {
//                    true // 첫 번째 아이템은 항상 헤더
//                } else {
//                    val current = sortedList[position]
//                    val previous = sortedList[position - 1]
//                    current.userName != previous.userName // 이름이 바뀌면 새 헤더
//                }
//            },
//            getHeaderTitle = { position ->
//                sortedList[position].userName ?: "이름 없음"
//            }
//        )
//
//        // 기존 데코레이션 제거 후 새로 추가
//        bd.rvM1.addItemDecoration(decoration)
//
//        Log.v("이름 묶음", "그룹 수: ${groupedByName.size}")
//        Log.v("이름 묶음", "전체 아이템: ${sortedList.size}")
//        groupedByName.forEach { (name, items) ->
//            Log.v("이름 묶음", "$name: ${items.size}개")
//        }
//    }

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