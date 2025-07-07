package com.tangoplus.facebeauty.ui

import android.graphics.Color
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.DrawLine
import com.tangoplus.facebeauty.data.DrawRatioLine
import com.tangoplus.facebeauty.data.FaceComparisonItem
import com.tangoplus.facebeauty.data.db.FaceStatic
import com.tangoplus.facebeauty.databinding.FragmentInformationDialogBinding
import com.tangoplus.facebeauty.ui.adapter.FaceStaticRVAdapter
import com.tangoplus.facebeauty.ui.view.GridSpacingItemDecoration
import com.tangoplus.facebeauty.ui.listener.OnAdapterMoreClickListener
import com.tangoplus.facebeauty.ui.listener.OnFaceStaticCheckListener
import com.tangoplus.facebeauty.util.BitmapUtility.extractFaceCoordinates
import com.tangoplus.facebeauty.util.BitmapUtility.setImage
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.util.FileUtility.toFaceStatic
import com.tangoplus.facebeauty.util.MathHelpers.calculateRatios
import com.tangoplus.facebeauty.vm.InformationViewModel
import com.tangoplus.facebeauty.vm.MainViewModel
import com.tangoplus.facebeauty.vm.MeasureViewModel
import kotlinx.coroutines.launch


class InformationDialogFragment : DialogFragment(), OnFaceStaticCheckListener,
    OnAdapterMoreClickListener {
    private lateinit var bd: FragmentInformationDialogBinding
//    private val mvm : MeasureViewModel by activityViewModels()
    private val ivm : InformationViewModel by activityViewModels()
    private val mvm : MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bd = FragmentInformationDialogBinding.inflate(inflater)
        return bd.root
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
        bd.ibtnIDBack.setOnSingleClickListener {
            dismiss()
            ivm.setSeqIndex(0)
        }

        // double확인해서 비교인지 상세보기인지 판단
        if (mvm.comparisonDoubleItem != null) {
            setComparisonButtons()
        } else {
            setDetailButtons()
        }
        setResult()
        setUserInfo()

    }

    override fun onFaceStaticCheck(drawLineIndex: Int, isChecked: Boolean) {
        val selectedLine = when (drawLineIndex) {
//            0 -> DrawLine.A_GLABELLA_NOSE
//            1 -> DrawLine.A_GLABELLA_NOSE
//            2 -> DrawLine.A_TIP_OF_LIPS
//            3 -> DrawLine.A_GLABELLA_NOSE
//            4 -> DrawLine.A_NOSE_CHIN
//            5 -> DrawLine.A_EARFLAP_NASAL_WING
//            6 -> DrawLine.A_EARFLAP_NASAL_WING
//            7 -> DrawLine.D_EARFLAP_NOSE
//            8 -> DrawLine.D_EARFLAP_NOSE
//            9 -> DrawLine.D_TIP_OF_LIPS_CENTER_LIPS
//            10 -> DrawLine.D_TIP_OF_LIPS_CENTER_LIPS
            else -> DrawLine.A_GLABELLA_NOSE
        }
        when (isChecked) {
            true -> ivm.currentCheckedLines.add(selectedLine)
            false -> ivm.currentCheckedLines.remove(selectedLine)
        }
        Log.v("체크", "$drawLineIndex, $isChecked / $selectedLine ${ivm.currentCheckedLines}")
        setImage()
    }

    private fun setRatioBtn() {
        // TODO 라티오는 따로 뺴버리기 -> 정면에서만 켜지게끔 -> 아니면 자동 off
        bd.btnIDRatio.setOnSingleClickListener {  }
    }



    override fun adapterMoreClicked(isExpanded: Boolean) {
        Log.v("확장", "inFragment = $isExpanded")
        if (!isExpanded) {
            bd.llGDRatioTitle.visibility = View.VISIBLE
            bd.tvGDRatioTitle.visibility = View.VISIBLE
        } else {
            bd.llGDRatioTitle.visibility = View.GONE
            bd.tvGDRatioTitle.visibility = View.GONE
        }
    }
    private fun setRatioCheckSwitch() {
        val df = DecimalFormat("#.#")
        // 값 적기
        val jsonData = mvm.currentResult.value?.results?.getJSONObject(0)
        val coordinates = extractFaceCoordinates(jsonData)
        if (coordinates != null) {
            val vertiIndices = listOf(234, 33, 133, 362, 263, 356)
            val vertiCoordinates = vertiIndices.map { coordinates[it] }
            Log.v("vertiCoordinates", "$vertiCoordinates")
            val horizonIndices = listOf(8, 2, 13, 152)
            val horizonCoordinates = horizonIndices.map { coordinates[it] }
            Log.v("horizonCoordinates", "$horizonCoordinates")
            val vertiText = calculateRatios(vertiCoordinates, true)
            val horizonText = calculateRatios(horizonCoordinates, false)

            bd.tvGDVerti.text = "${vertiText.map { df.format(it) }}"
                .replace("[", "")
                .replace("]", "")
                .replace(", ", " : ")
            bd.tvGDHorizon.text = "${horizonText.map { df.format(it) }}"
                .replace("[", "")
                .replace("]", "")
                .replace(", ", " : ")
        }

        bd.cbGDVerti.setOnCheckedChangeListener { _, isChecked ->
            setRatioLineInImage(true, isChecked)
        }
        bd.cbGDHorizon.setOnCheckedChangeListener { _, isChecked ->
            setRatioLineInImage(false, isChecked)
        }
//        bd.tvGDVerti.setOnSingleClickListener {
//            val balloon = Balloon.Builder(requireContext())
//                .setWidth(BalloonSizeSpec.WRAP)
//                .setHeight(BalloonSizeSpec.WRAP)
//                .setText("모든 비율이 1:1:1:1:1로 일정 할 수록 좋은 비율입니다.")
//                .setTextColorResource(R.color.subColor800)
//                .setTextSize(20f)
//                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
//                .setArrowSize(0)
//                .setMargin(6)
//                .setPadding(12)
//                .setCornerRadius(8f)
//                .setBackgroundColorResource(R.color.white)
//                .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
//                .setLifecycleOwner(viewLifecycleOwner)
//                .setOnBalloonDismissListener {  }
//                .build()
//            bd.tvGDVerti.showAlignTop(balloon)
//            balloon.dismissWithDelay(3000L)
//            balloon.setOnBalloonClickListener { balloon.dismiss() }
//        }
//        bd.tvGDHorizon.setOnSingleClickListener {
//            val balloon = Balloon.Builder(requireContext())
//                .setWidth(BalloonSizeSpec.WRAP)
//                .setHeight(BalloonSizeSpec.WRAP)
//                .setText("눈썹-코끝-입술-턱끝 3:1:2에 가까울수록 하관이 좋은 비율입니다.")
//                .setTextColorResource(R.color.subColor800)
//                .setTextSize(20f)
//                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
//                .setArrowSize(0)
//                .setMargin(6)
//                .setPadding(12)
//                .setCornerRadius(8f)
//                .setBackgroundColorResource(R.color.white)
//                .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
//                .setLifecycleOwner(viewLifecycleOwner)
//                .setOnBalloonDismissListener {  }
//                .build()
//            bd.tvGDHorizon.showAlignTop(balloon)
//            balloon.dismissWithDelay(3000L)
//            balloon.setOnBalloonClickListener { balloon.dismiss() }
//        }
    }

    private fun setRatioLineInImage(isVerti: Boolean, switchedOn: Boolean) {
        when (switchedOn) {
            true -> {
                if (isVerti) ivm.currentCheckedRatioLines.add(DrawRatioLine.A_VERTI)
                else ivm.currentCheckedRatioLines.add(DrawRatioLine.A_HORIZON)
            }
            false -> {
                if (isVerti) ivm.currentCheckedRatioLines.remove(DrawRatioLine.A_VERTI)
                else ivm.currentCheckedRatioLines.remove(DrawRatioLine.A_HORIZON)
            }
        }
        Log.v("비율이미지", "${ivm.currentCheckedRatioLines}")
        setImage()
    }
    private fun setResult() {
        bd.ssiv1.recycle()
        bd.ssiv2.recycle()

        // 비교인지 확인 TODO 여기서 데이터 확인한 후 보내야 함 3분할일지?
        if (mvm.comparisonDoubleItem != null) {

        } else {
            val faceStaticJson0 = mvm.currentResult.value?.results?.getJSONObject(0)?.getJSONObject("data")
            val faceStatic0 = faceStaticJson0.toFaceStatic()
            Log.v("스태틱가져오기", "0: $faceStaticJson0")
            val faceStaticJson1 = mvm.currentResult.value?.results?.getJSONObject(1)?.getJSONObject("data")
            val faceStatic1 = faceStaticJson1.toFaceStatic()
            Log.v("스태틱가져오기", "1: $faceStaticJson1")

            ivm.currentFaceComparision = buildFaceComparisonList(faceStatic0, faceStatic1).toMutableList()
            ivm.currentFaceComparision.apply {
//            add(0, FaceComparisonItem("", 0f, 0f, type = RVItemType.TITLE))
//            add(0, FaceComparisonItem("", 0f, 0f, type = RVItemType.TITLE))
            }
        }
        Log.v("staticDatas", "${ivm.currentFaceComparision}")

        val faceStaticAdapter = FaceStaticRVAdapter(ivm.currentFaceComparision)
        bd.msGD.setOnCheckedChangeListener { _, isChecked ->
            // 리스너 동작
            setLinesInImage(isChecked)
            faceStaticAdapter.setAllChecked(isChecked)
        }

        bd.rvGD2.apply {
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
            val spacingPx = (10 * resources.displayMetrics.density).toInt()
            val specialSpacingPx = (30 * resources.displayMetrics.density).toInt()

            val specialRows = setOf(3) // row index 기준: 0부터 시작
            addItemDecoration(
                GridSpacingItemDecoration(
                spanCount = 3,
                spacingPx = spacingPx,
                includeEdge = true,
                specialRowSpacingPx  = specialSpacingPx,
                specialRows  = specialRows
            )
            )

            faceStaticAdapter.faceStaticCheckListener = this@InformationDialogFragment
            faceStaticAdapter.adapterMoreClickedListener = this@InformationDialogFragment
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
//            FaceComparisonItem("귓바퀴 수평 각도", resting.resting_earflaps_horizontal_angle, occlusal.occlusal_earflaps_horizontal_angle),
            FaceComparisonItem("입술 끝 수평 각도", resting.resting_tip_of_lips_horizontal_angle, occlusal.occlusal_tip_of_lips_horizontal_angle),
//            FaceComparisonItem("미간-코 수직 각도", resting.resting_glabella_nose_vertical_angle, occlusal.occlusal_glabella_nose_vertical_angle),
//            FaceComparisonItem("코-턱 수직 각도", resting.resting_nose_chin_vertical_angle, occlusal.occlusal_nose_chin_vertical_angle),
//            FaceComparisonItem("좌측 귓바퀴-콧망울 수평 각도", resting.resting_left_earflaps_nasal_wing_horizontal_angle, occlusal.occlusal_left_earflaps_nasal_wing_horizontal_angle),
//            FaceComparisonItem("우측 귓바퀴-콧망울 수평 각도", resting.resting_right_earflaps_nasal_wing_horizontal_angle, occlusal.occlusal_right_earflaps_nasal_wing_horizontal_angle),
//            FaceComparisonItem("좌측 귓바퀴-코 거리", resting.resting_left_earflaps_nose_distance, occlusal.occlusal_left_earflaps_nose_distance),
//            FaceComparisonItem("우측 귓바퀴-코 거리", resting.resting_right_earflaps_nose_distance, occlusal.occlusal_right_earflaps_nose_distance),
//            FaceComparisonItem("좌측 입꼬리-입술 중앙 거리", resting.resting_left_tip_of_lips_center_lips_distance, occlusal.occlusal_left_tip_of_lips_center_lips_distance),
//            FaceComparisonItem("우측 입꼬리-입술 중앙 거리", resting.resting_right_tip_of_lips_center_lips_distance, occlusal.occlusal_right_tip_of_lips_center_lips_distance),
        )
    }
    private fun setImage() {
        val seqIndex = ivm.getSeqIndex()
        val isDoubleMode = mvm.comparisonDoubleItem != null

        val (leftSeq, rightSeq) = if (!isDoubleMode) {
            when (seqIndex) {
                0 -> 0 to 1
                1 -> 2 to 3
                else -> 4 to 5
            }
        } else {
            seqIndex to seqIndex
        }

        lifecycleScope.launch {
            if (isDoubleMode) {
                mvm.comparisonDoubleItem?.let { (left, right) ->
                    setImage(this@InformationDialogFragment, left, leftSeq, bd.ssiv1, ivm)
                    setImage(this@InformationDialogFragment, right, rightSeq, bd.ssiv2, ivm)
                }
            } else {
                mvm.currentResult.value?.let { result ->
                    // value가 한 번만 호출되고 재사용됨
                    setImage(this@InformationDialogFragment, result, leftSeq, bd.ssiv1, ivm)
                    setImage(this@InformationDialogFragment, result, rightSeq, bd.ssiv2, ivm)
                }
            }
        }

    }

    private fun setLinesInImage(switchedOn: Boolean) {
        if (switchedOn) {
            if (mvm.comparisonDoubleItem == null) {
                when (ivm.getSeqIndex()) {
                    0 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_CANTHUS_ORAL)
                        ivm.currentCheckedLines.add(DrawLine.A_NASALWINGS_ORAL)
                        ivm.currentCheckedLines.add(DrawLine.E_CHEEKS)
                    }
                    1 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_GLABELLA_NOSE)
                        ivm.currentCheckedLines.add(DrawLine.A_NOSE_JAW)
                    }
                    2 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_BELOW_LIPS)
                        ivm.currentCheckedLines.add(DrawLine.A_SHOULDER)
                        ivm.currentCheckedLines.add(DrawLine.A_EAR)
                        ivm.currentCheckedLines.add(DrawLine.A_NECK)
                    }
                }
            } else {
                when (ivm.getSeqIndex()) {
                    0 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_CANTHUS_ORAL)
                        ivm.currentCheckedLines.add(DrawLine.A_NASALWINGS_ORAL)
                        ivm.currentCheckedLines.add(DrawLine.E_CHEEKS)
                    }
                    1 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_CANTHUS_ORAL)
                        ivm.currentCheckedLines.add(DrawLine.A_NASALWINGS_ORAL)
                        ivm.currentCheckedLines.add(DrawLine.E_CHEEKS)
                    }
                    2 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_GLABELLA_NOSE)
                        ivm.currentCheckedLines.add(DrawLine.A_NOSE_JAW)
                    }
                    3 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_GLABELLA_NOSE)
                        ivm.currentCheckedLines.add(DrawLine.A_NOSE_JAW)
                    }
                    4 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_BELOW_LIPS)
                    }
                    5 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_SHOULDER)
                        ivm.currentCheckedLines.add(DrawLine.A_EAR)
                        ivm.currentCheckedLines.add(DrawLine.A_NECK)
                    }

                }
            }
            ivm.currentCheckedLines.add(DrawLine.A_VERTI)
            ivm.currentCheckedLines.add(DrawLine.A_HORIZON)
        } else {
            ivm.currentCheckedLines.clear()
        }

        setImage()
    }

    private fun showZoomInDialogFragment() {
        val leftSeq = if (ivm.getSeqIndex() == 0 ) 0 else 2
        val rightSeq = if (ivm.getSeqIndex() == 0) 1 else 3
        bd.ssiv1.setOnLongClickListener {
            val zoomInDialog = ZoomInDialogFragment.newInstance(leftSeq)
            zoomInDialog.show(requireActivity().supportFragmentManager, "")
            true
        }
        bd.ssiv2.setOnLongClickListener {
            val zoomInDialog = ZoomInDialogFragment.newInstance(rightSeq)
            zoomInDialog.show(requireActivity().supportFragmentManager, "")
            true
        }
    }
    private fun setComparisonButtons() {
        bd.llIDBottom.visibility = View.VISIBLE
        bd.divIDHorizon.visibility = View.VISIBLE
        val btns = listOf(bd.tvID1, bd.tvID2, bd.tvID3, bd.tvID4, bd.tvID5, bd.tvID6)
        btns.forEachIndexed { indexx, tv ->
            // 초기 색상 설정
            tv.setTextColor(ContextCompat.getColor(requireContext(),
                if (indexx == ivm.getSeqIndex()) R.color.black else R.color.subColor300
            ))

            tv.setOnSingleClickListener {
                ivm.setSeqIndex(indexx)
                Log.v("클릭됨", "${tv.text}, ${ivm.getSeqIndex()}")

                // 모든 버튼 텍스트 색상 갱신
                btns.forEachIndexed { i, t ->
                    t.setTextColor(ContextCompat.getColor(requireContext(),
                        if (i == indexx) R.color.black else R.color.subColor300
                    ))
                }
                setImage()
            }
        }
    }

    private fun setDetailButtons() {
        bd.llIDBottom.visibility = View.GONE
        bd.divIDHorizon.visibility = View.GONE

        bd.tvID1.text = "정면, 교합"
        bd.tvID2.text = "턱 좌우 이동"
        bd.tvID3.text = "입 벌림, 목폄"

        val btns = listOf(bd.tvID1, bd.tvID2, bd.tvID3)
        btns.forEachIndexed { indexx, tv ->
            // 초기 색상 설정
            tv.setTextColor(ContextCompat.getColor(requireContext(),
                    if (indexx == ivm.getSeqIndex()) R.color.black else R.color.subColor300
                )
            )

            tv.setOnSingleClickListener {
                ivm.setSeqIndex(indexx)
                Log.v("클릭됨", "${tv.text}, ${ivm.getSeqIndex()}")

                // 모든 버튼 텍스트 색상 갱신
                btns.forEachIndexed { i, t ->
                    t.setTextColor(ContextCompat.getColor(requireContext(),
                            if (i == indexx) R.color.black else R.color.subColor300
                        )
                    )
                }

                setImage()
            }
        }
    }
    private fun setUserInfo() {
        val name = mvm.currentResult.value?.userName ?: "GUEST"
        val mobile = mvm.currentResult.value?.userMobile ?: ""
        val transformMobile = if (mobile.length == 11 && mobile.startsWith("010")) {
            "${mobile.substring(0, 3)}-${mobile.substring(3, 7)}-${mobile.substring(7)}"
        } else {
            ""
        }
        val info = "$name, $transformMobile"
        bd.tvIdInfo.text = info
    }


}