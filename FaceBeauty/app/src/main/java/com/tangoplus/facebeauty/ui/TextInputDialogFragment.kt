package com.tangoplus.facebeauty.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.databinding.FragmentTextInputDialogBinding
import com.tangoplus.facebeauty.ui.view.TypingAnimationHelper
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.util.NativeLib
import com.tangoplus.facebeauty.util.PreferenceUtility
import com.tangoplus.facebeauty.util.SecurityUtility.generateCustomUUID
import com.tangoplus.facebeauty.vm.InputViewModel
import com.tangoplus.facebeauty.vm.MeasureViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern
import kotlin.math.hypot

class TextInputDialogFragment : DialogFragment() {
    private lateinit var binding:  FragmentTextInputDialogBinding
    private val ivm : InputViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private var touchX: Float = 0f
    private var touchY: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextInputDialogBinding.inflate(inflater)
        isCancelable = false
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false) // 외부 터치 무시
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                showSkipDialog()
                true // 이벤트 소비
            } else {
                false
            }
        }
        return dialog
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기 설정: 이름 입력창이 나오고, 키보드가 올라옴
        binding.etTIDMobile.visibility = View.INVISIBLE
        binding.btnTIDSave.visibility = View.INVISIBLE

        setGuideAnimation(binding.etTIDName)
        lifecycleScope.launch {
            TypingAnimationHelper().startTypingAnimation(binding.tvTIDGuide, "안녕하세요\n분석을 위해 성함을 입력해주세요")
        }
        binding.tvTIDGuide.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                touchX = event.x
                touchY = event.y
            }
            false
        }
        binding.etTIDName.postDelayed({
            binding.etTIDName.requestFocus()
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etTIDName, InputMethodManager.SHOW_IMPLICIT)
        }, 250)

        // 이름
        val nameTextWatcher = namePatternCheck(ivm, binding.etTIDName)
        binding.etTIDName.addTextChangedListener(nameTextWatcher)

        // 비밀번호 넣기
        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
        binding.etTIDMobile.addTextChangedListener(object: TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {

                if (isFormatting) return
                isFormatting = true
                val cleaned = s.toString().replace("-", "")
                val maxDigits = 11
                val limited = if (cleaned.length > maxDigits) cleaned.substring(0, maxDigits) else cleaned

                val formatted = when {
                    limited.length <= 3 -> limited
                    limited.length <= 7 -> "${limited.substring(0, 3)}-${limited.substring(3)}"
                    else -> "${limited.substring(0, 3)}-${limited.substring(3, 7)}-${limited.substring(7)}"
                }

                // 기존 입력과 다를 때만 업데이트
                if (s.toString() != formatted && s != null) {
                    binding.etTIDMobile.setText(formatted) // setText를 사용하여 확실하게 변경
                    binding.etTIDMobile.setSelection(formatted.length) // 커서를 마지막 위치로 이동
                }

                isFormatting = false
                ivm.mobileCondition.value = mobilePatternCheck.matcher(binding.etTIDMobile.text.toString()).find()
                if (ivm.mobileCondition.value == true) {
                    binding.etTIDMobile.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)
                    ivm.mobileValue.value = binding.etTIDMobile.text.toString().replace("-", "")
                } else {
                    binding.etTIDMobile.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.deleteColor)
                }
            }
        })

        binding.etTIDName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                if (ivm.nameCondition.value == true) {
                    setGuideAnimation(binding.etTIDMobile)
                    binding.etTIDMobile.requestFocus()
                    lifecycleScope.launch {
                        TypingAnimationHelper().startTypingAnimation(binding.tvTIDGuide, "감사합니다\n마지막으로 휴대폰 번호를 입력해주세요")
                    }
                }
                return@setOnEditorActionListener true
            }
            false
        }

        ivm.inputCondition.observe(viewLifecycleOwner) { bothTrue ->
            if (bothTrue) {
                if (!ivm.isShownBtn) {
                    setGuideAnimation(binding.btnTIDSave, 1)
                    ivm.isShownBtn = true

                }
                binding.btnTIDSave.isEnabled = true
            } else {
                binding.btnTIDSave.isEnabled = false
            }

        }

        binding.btnTIDSave.setOnSingleClickListener {
            startDismissWithRipple()
            val key = NativeLib.getSecretKey()
            Log.d("SecretKey", "My Key: $key")

            val uuid = generateCustomUUID(ivm.nameValue.value, ivm.mobileValue.value, key)
            Log.v("createdUUID", uuid)
            mvm.currentUUID = uuid
        }

        binding.tvTIDSkip.setOnSingleClickListener {
            showSkipDialog()
        }
        binding.etTIDName.setText("")
        binding.etTIDMobile.setText("")
    }

    private fun setGuideAnimation(vv: View, case: Int = 0) {

        // case 1: 버튼 아래-위 슬라이드 else: view 위-아래 슬라이드
        val slideCase = when (case) {
            1 -> 100f
            else -> -100f
        }
        val slideDuration = when (case) {
            1 -> 500L
            else -> 600L
        }
        val slide = TranslateAnimation(0f, 0f, slideCase, 0f)
        slide.apply {
            duration = slideDuration
            interpolator = AccelerateDecelerateInterpolator()
            fillAfter = true
        }
        val fadeCase = when (case) {
            1 -> 0L
            else -> 600L
        }
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = fadeCase
        }
        val animationSet = AnimationSet(true).apply {
            addAnimation(slide)
            addAnimation(fadeIn)
            fillAfter = true
        }
        vv.apply {
            visibility = View.VISIBLE
            animation = animationSet
        }
    }
    private fun namePatternCheck(ivm: InputViewModel, input: EditText) : TextWatcher {
        val nameRegex = "^[가-힣]{2,5}$|^[a-zA-Z]{2,20}$"
        val namePatternCheck = Pattern.compile(nameRegex)
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                ivm.nameCondition.value = namePatternCheck.matcher(input.text.toString()).find()
//                Log.v("이름컨디션", "${ivm.nameCondition.value}, ${input.text}")
//                Log.v("보스", "${ivm.inputCondition.value}, ${ivm.nameCondition.value}, ${ivm.mobileCondition.value}")
                if (ivm.nameCondition.value == true) {
                    input.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)
                    ivm.nameValue.value = input.text.toString()
                } else {
                    input.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.deleteColor)
                }
            }
        }
    }
    private fun startDismissWithRipple() {
        val rootView = binding.flTID

        val finalRadius = hypot(rootView.width.toDouble(), rootView.height.toDouble()).toFloat()

        val anim = ViewAnimationUtils.createCircularReveal(
            rootView,
            touchX.toInt(),
            touchY.toInt(),
            finalRadius,
            0f
        )
        anim.duration = 300
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                dismissAllowingStateLoss()
                ivm.isFinishInput = true
            }
        })
        anim.start()
    }

    private fun showSkipDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage("개인정보 입력을 건너뛰시겠습니까?")
            setPositiveButton("예") { _, _ ->

                ivm.nameValue.value = "GUEST"
                ivm.mobileValue.value = (PreferenceUtility(requireContext()).getLastTempServerSn() + 1).toString() // 측정 결과 저장 전이니 일단 +1 한 temp_server_sn을 mobile로 uuid생성
                val key = NativeLib.getSecretKey()
                val uuid = generateCustomUUID(ivm.nameValue.value, ivm.mobileValue.value, key)
                Log.v("createdUUID", uuid)
                mvm.currentUUID = uuid
                ivm.isFinishInput = true
                dismiss()
            }
            setNeutralButton("기록으로 이동") { _, _ ->
                ivm.nameValue.value = "GUEST"
                ivm.mobileValue.value = (PreferenceUtility(requireContext()).getLastTempServerSn() + 1).toString() // 측정 결과 저장 전이니 일단 +1 한 temp_server_sn을 mobile로 uuid생성
                val key = NativeLib.getSecretKey()
                val uuid = generateCustomUUID(ivm.nameValue.value, ivm.mobileValue.value, key)
                Log.v("createdUUID", uuid)
                mvm.currentUUID = uuid
                dismiss()

                val galleryDialog = GalleryDialogFragment()
                galleryDialog.show(requireActivity().supportFragmentManager, "")
            }
            setNegativeButton("아니오") { _, _ -> }
            show()
        }
    }
}