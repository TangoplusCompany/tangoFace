package com.tangoplus.facebeauty.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
import com.tangoplus.facebeauty.data.RVItemType
import com.tangoplus.facebeauty.data.db.FaceStatic
import com.tangoplus.facebeauty.databinding.FragmentInformationDialogBinding
import com.tangoplus.facebeauty.ui.view.GridSpacingItemDecoration
import com.tangoplus.facebeauty.ui.view.OnAdapterMoreClickListener
import com.tangoplus.facebeauty.ui.view.OnFaceStaticCheckListener
import com.tangoplus.facebeauty.util.BitmapUtility.extractImageCoordinates
import com.tangoplus.facebeauty.util.BitmapUtility.setImage
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.util.FileUtility.toFaceStatic
import com.tangoplus.facebeauty.util.MathHelpers.calculateRatios
import com.tangoplus.facebeauty.vm.GalleryViewModel
import com.tangoplus.facebeauty.vm.InputViewModel
import com.tangoplus.facebeauty.vm.MeasureViewModel
import kotlinx.coroutines.launch


class InformationDialogFragment : DialogFragment(), OnFaceStaticCheckListener, OnAdapterMoreClickListener {
    private lateinit var binding: FragmentInformationDialogBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private val ivm : InputViewModel by activityViewModels()
    private val gvm : GalleryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInformationDialogBinding.inflate(inflater)
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
        binding.ibtnIDBack.setOnSingleClickListener {
            dismiss()
        }
        showDetailResult()

        // ------# 하단 버튼토글 그룹 시작 #------
        binding.btnToggleGroup.check(R.id.btn1)
        binding.btnToggleGroup.addOnButtonCheckedListener{ _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn1 -> {
                        animateIndicator(true)
                        gvm.setBtnIndex(0)
                    }
                    R.id.btn2 -> {
                        animateIndicator(false)
                        gvm.setBtnIndex(1)
                    }
                }
            }
            setImage()
        }
        binding.btn1.setOnSingleClickListener { binding.btnToggleGroup.check(R.id.btn1) }
        binding.btn2.setOnSingleClickListener { binding.btnToggleGroup.check(R.id.btn2) }
        binding.btnToggleGroup.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                initToggleIndicator()
                binding.btnToggleGroup.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        // ------# 하단 버튼토글 그룹 끝 #------
    }
    private fun initToggleIndicator() {
        val buttonWidth = binding.btnToggleGroup.width / 2
        val params = binding.toggleIndicator.layoutParams
        params.width = buttonWidth - 36
        params.height = binding.btnToggleGroup.height - 36
        binding.toggleIndicator.layoutParams = params

        binding.toggleIndicator.x = 24f
    }

    private fun animateIndicator(toLeft: Boolean) {
        val animator = ValueAnimator.ofFloat(
            binding.toggleIndicator.x,
            if (toLeft) 24f else (binding.btnToggleGroup.width - binding.toggleIndicator.width - 24f)
        )

        animator.addUpdateListener { animation ->
            binding.toggleIndicator.x = animation.animatedValue as Float
        }

        animator.duration = 250
        animator.start()

        animator.addListener(object: Animator.AnimatorListener{
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                when (toLeft) {
                    true -> setToggleText(R.color.subColor800, R.color.subColor300)
                    false -> setToggleText(R.color.subColor300, R.color.subColor800)
                }
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }
    private fun setToggleText(color1: Int, color2: Int) {
        binding.tvbtn1.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color1)))
        binding.tvbtn2.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color2)))
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
        binding.tvGDVerti.setOnSingleClickListener {
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
            binding.tvGDVerti.showAlignTop(balloon)
            balloon.dismissWithDelay(3000L)
            balloon.setOnBalloonClickListener { balloon.dismiss() }
        }
        binding.tvGDHorizon.setOnSingleClickListener {
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
            binding.tvGDHorizon.showAlignTop(balloon)
            balloon.dismissWithDelay(3000L)
            balloon.setOnBalloonClickListener { balloon.dismiss() }
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
    private fun setImage() {
        val leftSeq = if (gvm.getBtnIndex() == 0 ) 0 else 2
        val rightSeq = if (gvm.getBtnIndex() == 0) 1 else 3

        lifecycleScope.launch {
            Log.v("체크", "setImage: ${gvm.currentCheckedRatioLines}")
            gvm.currentResult.value?.let { setImage(this@InformationDialogFragment, it, leftSeq, binding.ssiv1, gvm) }
            gvm.currentResult.value?.let { setImage(this@InformationDialogFragment, it, rightSeq, binding.ssiv2, gvm) }
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

    private fun showZoomInDialogFragment() {
        val leftSeq = if (gvm.getBtnIndex() == 0 ) 0 else 2
        val rightSeq = if (gvm.getBtnIndex() == 0) 1 else 3
        binding.ssiv1.setOnLongClickListener {
            val zoomInDialog = ZoomInDialogFragment.newInstance(leftSeq)
            zoomInDialog.show(requireActivity().supportFragmentManager, "")
            true
        }
        binding.ssiv2.setOnLongClickListener {
            val zoomInDialog = ZoomInDialogFragment.newInstance(rightSeq)
            zoomInDialog.show(requireActivity().supportFragmentManager, "")
            true
        }
    }

}