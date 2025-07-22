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
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignTop
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.DrawLine
import com.tangoplus.facebeauty.data.DrawRatioLine
import com.tangoplus.facebeauty.data.FaceComparisonItem
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


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
    override fun onDestroyView() {
        super.onDestroyView()
        mvm.comparisonDoubleItem = null
        ivm.currentCheckedLines.clear()
        if (ivm.getRatioState() != DrawRatioLine.A_NONE) ivm.setAllOrNone()
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
            setComparisonUI()
        } else {
            setDetailButtons()
            setUserInfo()
        }
        setResult()
        setRatioBtn()
    }

    override fun onFaceStaticCheck(drawLineIndex: Int, isChecked: Boolean) {
        val selectedLine = if (mvm.comparisonDoubleItem == null) {
            when (ivm.getSeqIndex()) {
                0 -> {
                    when (drawLineIndex) {
                        0 -> DrawLine.A_CANTHUS
                        1 -> DrawLine.A_TIP_OF_LIPS
                        2 -> DrawLine.A_CHIN
                        3 -> DrawLine.A_CANTHUS_ORAL
                        4 -> DrawLine.A_CANTHUS_ORAL
                        5 -> DrawLine.A_NASALWINGS_ORAL
                        6 -> DrawLine.A_NASALWINGS_ORAL
                        7 -> DrawLine.E_CHEEKS
                        8 -> DrawLine.E_CHEEKS
                        else -> DrawLine.A_CANTHUS
                    }
                }
                1 -> {
                    // 0 ~ 3 item 전부 동시에 체크
                    DrawLine.A_NOSE_JAW
                }
                2 -> {
                    when (drawLineIndex) {
                        0 -> DrawLine.A_BELOW_LIPS
                        1 -> DrawLine.A_BELOW_LIPS
                        2 -> DrawLine.A_SHOULDER
                        3 -> DrawLine.A_EAR
                        else -> DrawLine.A_NECK
                    }
                }
                else -> DrawLine.A_GLABELLA_NOSE
            }
        } else {
            when (ivm.getSeqIndex()) {
                0, 1 -> {
                    when (drawLineIndex) {
                        0 -> DrawLine.A_CANTHUS
                        1 -> DrawLine.A_TIP_OF_LIPS
                        2 -> DrawLine.A_CHIN
                        3 -> DrawLine.A_CANTHUS_ORAL
                        4 -> DrawLine.A_CANTHUS_ORAL
                        5 -> DrawLine.A_NASALWINGS_ORAL
                        6 -> DrawLine.A_NASALWINGS_ORAL
                        7 -> DrawLine.E_CHEEKS
                        8 -> DrawLine.E_CHEEKS
                        else -> DrawLine.A_CANTHUS
                    }
                }
                2, 3 -> {
                    // 0 ~ 3 item 전부 동시에 체크
                    DrawLine.A_NOSE_JAW
                }
                4 -> {
                    DrawLine.A_BELOW_LIPS
                }
                5 -> {
                    when (drawLineIndex) {
                        0 -> DrawLine.A_SHOULDER
                        1 -> DrawLine.A_EAR
                        else -> DrawLine.A_NECK
                    }
                }
                else -> DrawLine.A_GLABELLA_NOSE
            }
        }
        when (isChecked) {
            true -> ivm.currentCheckedLines.add(selectedLine)
            false -> ivm.currentCheckedLines.remove(selectedLine)
        }
        Log.v("체크", "$drawLineIndex, $isChecked / $selectedLine ${ivm.currentCheckedLines}")
        setImage()
    }

    private fun setRatioBtn() {
        bd.btnIDRatio.setOnSingleClickListener {
            ivm.setRatioState()
            ivm.getRatioState()
            setImage()
        }
        bd.btnIDRatio.setOnLongClickListener {
            ivm.setAllOrNone()
            ivm.getRatioState()
            setImage()
            true
        }
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
//            Log.v("vertiCoordinates", "$vertiCoordinates")
            val horizonIndices = listOf(8, 2, 13, 152)
            val horizonCoordinates = horizonIndices.map { coordinates[it] }
//            Log.v("horizonCoordinates", "$horizonCoordinates")
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
        bd.tvGDVerti.setOnSingleClickListener {
            val balloon = Balloon.Builder(requireContext())
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText("모든 비율이 1:1:1:1:1로 일정 할 수록 좋은 비율입니다.")
                .setTextColorResource(R.color.subColor800)
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
            bd.tvGDVerti.showAlignTop(balloon)
            balloon.dismissWithDelay(3000L)
            balloon.setOnBalloonClickListener { balloon.dismiss() }
        }
        bd.tvGDHorizon.setOnSingleClickListener {
            val balloon = Balloon.Builder(requireContext())
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText("눈썹-코끝-입술-턱끝 3:1:2에 가까울수록 하관이 좋은 비율입니다.")
                .setTextColorResource(R.color.subColor800)
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
            bd.tvGDHorizon.showAlignTop(balloon)
            balloon.dismissWithDelay(3000L)
            balloon.setOnBalloonClickListener { balloon.dismiss() }
        }
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
        setImage()
    }

    private fun buildFaceComparisonList(isComparison: Boolean, leftJA: JSONArray?, rightJA: JSONArray?): List<FaceComparisonItem>? {

        val (leftSeq, rightSeq) = if (!isComparison) {
            when (ivm.getSeqIndex()) {
                0 -> 0 to 1
                1 -> 2 to 3
                else -> 4 to 5
            }
        } else {
            ivm.getSeqIndex() to ivm.getSeqIndex()
        }
        if (leftJA != null && rightJA != null) {
            val leftValue = leftJA.getJSONObject(leftSeq).getJSONObject("data")
            val rightValue = rightJA.getJSONObject(rightSeq).getJSONObject("data")
            return if (isComparison) {
                when (ivm.getSeqIndex()) {
                    0 -> {
                        listOf(
                            FaceComparisonItem("양쪽 눈", leftValue.getDouble("resting_eye_horizontal_angle").toFloat(), rightValue.getDouble("resting_eye_horizontal_angle").toFloat()),
//                            FaceComparisonItem("양쪽 눈썹", leftValue.getDouble("resting_eyebrow_horizontal_angle").toFloat(), rightValue.getDouble("resting_eyebrow_horizontal_angle").toFloat()),
                            FaceComparisonItem("양쪽 입술", leftValue.getDouble("resting_tip_of_lips_horizontal_angle").toFloat(), rightValue.getDouble("resting_tip_of_lips_horizontal_angle").toFloat()),
                            FaceComparisonItem("턱 끝", leftValue.getDouble("resting_tip_of_chin_horizontal_angle").toFloat(), rightValue.getDouble("resting_tip_of_chin_horizontal_angle").toFloat()),
                            FaceComparisonItem("왼쪽 눈매 - 입술 끝", leftValue.getDouble("resting_canthus_oral_left_vertical_angle").toFloat(), rightValue.getDouble("resting_canthus_oral_left_vertical_angle").toFloat()),
                            FaceComparisonItem("오른쪽 눈매 - 입술 끝", leftValue.getDouble("resting_canthus_oral_right_vertical_angle").toFloat(), rightValue.getDouble("resting_canthus_oral_right_vertical_angle").toFloat()),

                            FaceComparisonItem("왼쪽 코끝 - 입술 끝", leftValue.getDouble("resting_nasal_wing_tip_of_lips_left_vertical_angle").toFloat(), rightValue.getDouble("resting_nasal_wing_tip_of_lips_left_vertical_angle").toFloat()),
                            FaceComparisonItem("오른쪽 코끝 - 입술 끝", leftValue.getDouble("resting_nasal_wing_tip_of_lips_right_vertical_angle").toFloat(), rightValue.getDouble("resting_nasal_wing_tip_of_lips_right_vertical_angle").toFloat()),
                            FaceComparisonItem("왼쪽 볼 너비", leftValue.getDouble("resting_left_cheeks_extent").toFloat(), rightValue.getDouble("resting_left_cheeks_extent").toFloat()),
                            FaceComparisonItem("오른쪽 볼 너비", leftValue.getDouble("resting_right_cheeks_extent").toFloat(), rightValue.getDouble("resting_right_cheeks_extent").toFloat()),
                        )
                    }
                    1 -> {
                        listOf(
                            FaceComparisonItem("양쪽 눈", leftValue.getDouble("occlusal_eye_horizontal_angle").toFloat(), rightValue.getDouble("occlusal_eye_horizontal_angle").toFloat()),
//                            FaceComparisonItem("양쪽 눈썹", leftValue.getDouble("occlusal_eyebrow_horizontal_angle").toFloat(), rightValue.getDouble("occlusal_eyebrow_horizontal_angle").toFloat()),
                            FaceComparisonItem("양쪽 입술", leftValue.getDouble("occlusal_tip_of_lips_horizontal_angle").toFloat(), rightValue.getDouble("occlusal_tip_of_lips_horizontal_angle").toFloat()),
                            FaceComparisonItem("턱 끝", leftValue.getDouble("occlusal_tip_of_chin_horizontal_angle").toFloat(), rightValue.getDouble("occlusal_tip_of_chin_horizontal_angle").toFloat()),
                            FaceComparisonItem("왼쪽 눈매 - 입술 끝", leftValue.getDouble("occlusal_canthus_oral_left_vertical_angle").toFloat(), rightValue.getDouble("occlusal_canthus_oral_left_vertical_angle").toFloat()),
                            FaceComparisonItem("오른쪽 눈매 - 입술 끝", leftValue.getDouble("occlusal_canthus_oral_right_vertical_angle").toFloat(), rightValue.getDouble("occlusal_canthus_oral_right_vertical_angle").toFloat()),
                            FaceComparisonItem("왼쪽 코끝 - 입술 끝", leftValue.getDouble("occlusal_nasal_wing_tip_of_lips_left_vertical_angle").toFloat(), rightValue.getDouble("occlusal_nasal_wing_tip_of_lips_left_vertical_angle").toFloat()),
                            FaceComparisonItem("오른쪽 코끝 - 입술 끝", leftValue.getDouble("occlusal_nasal_wing_tip_of_lips_right_vertical_angle").toFloat(), rightValue.getDouble("occlusal_nasal_wing_tip_of_lips_right_vertical_angle").toFloat()),
                            FaceComparisonItem("왼쪽 볼 너비", leftValue.getDouble("occlusal_left_cheeks_extent").toFloat(), rightValue.getDouble("occlusal_left_cheeks_extent").toFloat()),
                            FaceComparisonItem("오른쪽 볼 너비", leftValue.getDouble("occlusal_right_cheeks_extent").toFloat(), rightValue.getDouble("occlusal_right_cheeks_extent").toFloat()),
                        )
                    }
                    2 -> {
                        listOf(
                            FaceComparisonItem("코 - 턱", leftValue.getDouble("jaw_left_tilt_nose_chin_vertical_angle").toFloat(), rightValue.getDouble("jaw_left_tilt_nose_chin_vertical_angle").toFloat()),
                            FaceComparisonItem("양 입술", leftValue.getDouble("jaw_left_tilt_tip_of_lips_horizontal_angle").toFloat(), rightValue.getDouble("jaw_left_tilt_tip_of_lips_horizontal_angle").toFloat()),
                            FaceComparisonItem("왼쪽 중간 턱", leftValue.getDouble("jaw_left_tilt_left_mandibular_distance").toFloat(), rightValue.getDouble("jaw_left_tilt_left_mandibular_distance").toFloat()),
                            FaceComparisonItem("오른쪽 중간 턱", leftValue.getDouble("jaw_left_tilt_right_mandibular_distance").toFloat(), rightValue.getDouble("jaw_left_tilt_right_mandibular_distance").toFloat()),
                        )
                    }
                    3 -> {
                        listOf(
                            FaceComparisonItem("코 - 턱", leftValue.getDouble("jaw_right_tilt_nose_chin_vertical_angle").toFloat(), rightValue.getDouble("jaw_right_tilt_nose_chin_vertical_angle").toFloat()),
                            FaceComparisonItem("양 입술", leftValue.getDouble("jaw_right_tilt_tip_of_lips_horizontal_angle").toFloat(), rightValue.getDouble("jaw_right_tilt_tip_of_lips_horizontal_angle").toFloat()),
                            FaceComparisonItem("왼쪽 중간 턱", leftValue.getDouble("jaw_right_tilt_left_mandibular_distance").toFloat(), rightValue.getDouble("jaw_right_tilt_left_mandibular_distance").toFloat()),
                            FaceComparisonItem("오른쪽 중간 턱", leftValue.getDouble("jaw_right_tilt_right_mandibular_distance").toFloat(), rightValue.getDouble("jaw_right_tilt_right_mandibular_distance").toFloat()),
                        )
                    }
                    4 -> {
                        listOf(
                            FaceComparisonItem("입 높이", leftValue.getDouble("jaw_opening_lips_distance").toFloat(), rightValue.getDouble("jaw_opening_lips_distance").toFloat()),
                            FaceComparisonItem("입 각도", leftValue.getDouble("jaw_opening_lips_vertical_angle").toFloat(), rightValue.getDouble("jaw_opening_lips_vertical_angle").toFloat()),
                        )
                    }
                    else -> {
                        listOf(
                            FaceComparisonItem("양 어깨", leftValue.getDouble("neck_extention_shoulder_horizontal_angle").toFloat(), rightValue.getDouble("neck_extention_shoulder_horizontal_angle").toFloat()),
                            FaceComparisonItem("양 귀", leftValue.getDouble("neck_extention_ear_horizontal_angle").toFloat(), rightValue.getDouble("neck_extention_ear_horizontal_angle").toFloat()),
                            FaceComparisonItem("목 각도", leftValue.getDouble("neck_extention_neck_vertical_angle").toFloat(), rightValue.getDouble("neck_extention_neck_vertical_angle").toFloat()),

                            )
                    }
                }
            } else {
                when (ivm.getSeqIndex()) {
                    0 -> {
                        listOf(
                            FaceComparisonItem("양쪽 눈", leftValue.getDouble("resting_eye_horizontal_angle").toFloat(), rightValue.getDouble("occlusal_eye_horizontal_angle").toFloat()),
//                            FaceComparisonItem("양쪽 눈썹", leftValue.getDouble("resting_eyebrow_horizontal_angle").toFloat(), rightValue.getDouble("occlusal_eyebrow_horizontal_angle").toFloat()),
                            FaceComparisonItem("양쪽 입술", leftValue.getDouble("resting_tip_of_lips_horizontal_angle").toFloat(), rightValue.getDouble("occlusal_tip_of_lips_horizontal_angle").toFloat()),
                            FaceComparisonItem("턱 끝", leftValue.getDouble("resting_tip_of_chin_horizontal_angle").toFloat(), rightValue.getDouble("occlusal_tip_of_chin_horizontal_angle").toFloat()),
                            FaceComparisonItem("왼쪽 눈매 - 입술 끝", leftValue.getDouble("resting_canthus_oral_left_vertical_angle").toFloat(), rightValue.getDouble("occlusal_canthus_oral_left_vertical_angle").toFloat()),
                            FaceComparisonItem("오른쪽 눈매 - 입술 끝", leftValue.getDouble("resting_canthus_oral_right_vertical_angle").toFloat(), rightValue.getDouble("occlusal_canthus_oral_right_vertical_angle").toFloat()),
                            FaceComparisonItem("왼쪽 코끝 - 입술 끝", leftValue.getDouble("resting_nasal_wing_tip_of_lips_left_vertical_angle").toFloat(), rightValue.getDouble("occlusal_nasal_wing_tip_of_lips_left_vertical_angle").toFloat()),
                            FaceComparisonItem("오른쪽 코끝 - 입술 끝", leftValue.getDouble("resting_nasal_wing_tip_of_lips_right_vertical_angle").toFloat(), rightValue.getDouble("occlusal_nasal_wing_tip_of_lips_right_vertical_angle").toFloat()),
                            FaceComparisonItem("왼쪽 볼 너비", leftValue.getDouble("resting_left_cheeks_extent").toFloat(), rightValue.getDouble("occlusal_left_cheeks_extent").toFloat()),
                            FaceComparisonItem("오른쪽 볼 너비", leftValue.getDouble("resting_right_cheeks_extent").toFloat(), rightValue.getDouble("occlusal_right_cheeks_extent").toFloat()),
                        )
                    }
                    1 -> {
                        listOf(
                            FaceComparisonItem("코 - 턱", leftValue.getDouble("jaw_left_tilt_nose_chin_vertical_angle").toFloat(), rightValue.getDouble("jaw_right_tilt_nose_chin_vertical_angle").toFloat()),
                            FaceComparisonItem("양 입술", leftValue.getDouble("jaw_left_tilt_tip_of_lips_horizontal_angle").toFloat(), rightValue.getDouble("jaw_right_tilt_tip_of_lips_horizontal_angle").toFloat()),
                            FaceComparisonItem("왼쪽 중간 턱", leftValue.getDouble("jaw_left_tilt_left_mandibular_distance").toFloat(), rightValue.getDouble("jaw_right_tilt_left_mandibular_distance").toFloat()),
                            FaceComparisonItem("오른쪽 중간 턱", leftValue.getDouble("jaw_left_tilt_right_mandibular_distance").toFloat(), rightValue.getDouble("jaw_right_tilt_right_mandibular_distance").toFloat()),
                        )
                    }
                    else -> {
                        listOf(
                            FaceComparisonItem("입 높이", leftValue.getDouble("jaw_opening_lips_distance").toFloat(), 0f),
                            FaceComparisonItem("입 각도", leftValue.getDouble("jaw_opening_lips_vertical_angle").toFloat(), 0f),
                            FaceComparisonItem("양 어깨", rightValue.getDouble("neck_extention_shoulder_horizontal_angle").toFloat(), 0f),
                            FaceComparisonItem("양 귀", rightValue.getDouble("neck_extention_ear_horizontal_angle").toFloat(), 0f),
                            FaceComparisonItem("목 각도", rightValue.getDouble("neck_extention_neck_vertical_angle").toFloat(), 0f),
                        )
                    }
                }
            }
        }
        return  null
    }

    private fun setResult() {
        bd.ssiv1.recycle()
        bd.ssiv2.recycle()
        Log.v("setResult", "setResult started")
        val isComparison = mvm.comparisonDoubleItem != null
        if (isComparison) {
            val leftResult = mvm.comparisonDoubleItem?.first
            val rightResult = mvm.comparisonDoubleItem?.second
            val result = buildFaceComparisonList(true, leftResult?.results, rightResult?.results)?.toMutableList()
            if (result != null) {
                ivm.currentFaceComparision = result
            }
        } else {
            val faceStaticJson = mvm.currentResult.value?.results
            val result = buildFaceComparisonList(false, faceStaticJson, faceStaticJson)?.toMutableList()
            if (result != null) {
                ivm.currentFaceComparision = result
            }
            Log.v("setResult", "result: $result")

        }
        Log.v("staticDatas", "${ivm.currentFaceComparision}")
        val basicInfo = if (isComparison) {
            Pair(mvm.comparisonDoubleItem?.first?.regDate!!, mvm.comparisonDoubleItem?.second?.regDate!!)
        } else null
        val faceStaticAdapter = FaceStaticRVAdapter(ivm.currentFaceComparision, basicInfo, ivm.getSeqIndex())
        bd.msGD.setOnCheckedChangeListener { _, isChecked ->
            // 리스너 동작
            setLinesInImage(isChecked)
            faceStaticAdapter.setAllChecked(isChecked)
            if (!isChecked) {
                ivm.currentCheckedLines.clear()
            }
        }
        setImage()
        showZoomInDialogFragment()
        setRatioCheckSwitch()


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
                    bd.tvIdDate1.apply {
                        visibility = View.VISIBLE
                        text = left.regDate?.substring(0, 11)
                    }
                    bd.tvIdDate2.apply {
                        visibility = View.VISIBLE
                        text = right.regDate?.substring(0, 11)
                    }

                }

            } else {
                mvm.currentResult.value?.let { result ->
                    // value가 한 번만 호출되고 재사용됨
                    setImage(this@InformationDialogFragment, result, leftSeq, bd.ssiv1, ivm)
                    setImage(this@InformationDialogFragment, result, rightSeq, bd.ssiv2, ivm)
                }
                bd.tvIdDate1.visibility = View.GONE
                bd.tvIdDate2.visibility = View.GONE

            }
            bd.btnIDRatio.visibility = View.VISIBLE
        }
    }

    private fun setLinesInImage(switchedOn: Boolean) {
        if (switchedOn) {
            if (mvm.comparisonDoubleItem == null) {
                when (ivm.getSeqIndex()) {
                    0 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_CANTHUS)
                        ivm.currentCheckedLines.add(DrawLine.A_TIP_OF_LIPS)
                        ivm.currentCheckedLines.add(DrawLine.A_CHIN)

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
                        ivm.currentCheckedLines.add(DrawLine.A_CANTHUS)
                        ivm.currentCheckedLines.add(DrawLine.A_TIP_OF_LIPS)
                        ivm.currentCheckedLines.add(DrawLine.A_CHIN)


                        ivm.currentCheckedLines.add(DrawLine.E_CHEEKS)
                    }
                    1 -> {
                        ivm.currentCheckedLines.add(DrawLine.A_CANTHUS)
                        ivm.currentCheckedLines.add(DrawLine.A_TIP_OF_LIPS)
                        ivm.currentCheckedLines.add(DrawLine.A_CHIN)

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
        } else {
            ivm.currentCheckedLines.clear()
        }
        setImage()
    }

    private fun showZoomInDialogFragment() { // 0 , 1, 2인데
        val index = ivm.getSeqIndex()
        val isComparison = mvm.comparisonDoubleItem != null

        val (leftSeq, rightSeq) = if (mvm.comparisonDoubleItem == null) {
            val left = index * 2
            val right = left + 1
            left to right
        } else {
            index to index
        }
        bd.ssiv1.setOnLongClickListener {
            Log.v("seq와left", "$isComparison")
            val zoomInDialog = if (!isComparison) {
                ZoomInDialogFragment.newInstance(leftSeq, null)
            } else {
                ZoomInDialogFragment.newInstance(leftSeq, true)
            }
            zoomInDialog.show(requireActivity().supportFragmentManager, "")
            true
        }
        bd.ssiv2.setOnLongClickListener {
            Log.v("seq와left", "$isComparison")

            val zoomInDialog = if (!isComparison) {
                ZoomInDialogFragment.newInstance(rightSeq, null)
            } else {
                ZoomInDialogFragment.newInstance(rightSeq, false)
            }
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
                ivm.currentCheckedLines.clear()
                if (bd.msGD.isChecked) bd.msGD.isChecked = false

                // 모든 버튼 텍스트 색상 갱신
                btns.forEachIndexed { i, t ->
                    t.setTextColor(ContextCompat.getColor(requireContext(),
                        if (i == indexx) R.color.black else R.color.subColor300
                    ))
                }
                setImage()
                setResult()
            }
        }
    }

    private fun setComparisonUI() {
        bd.tvIdInfo.visibility = View.GONE
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
                // checkbox 해제
                ivm.currentCheckedLines.clear()
                if (bd.msGD.isChecked) bd.msGD.isChecked = false

                // 모든 버튼 텍스트 색상 갱신
                btns.forEachIndexed { i, t ->
                    t.setTextColor(ContextCompat.getColor(requireContext(),
                            if (i == indexx) R.color.black else R.color.subColor300
                        )
                    )

                }
                setImage()
                setResult()
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