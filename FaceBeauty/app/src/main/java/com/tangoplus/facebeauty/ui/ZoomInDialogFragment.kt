package com.tangoplus.facebeauty.ui

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.databinding.FragmentZoomInDialogBinding
import com.tangoplus.facebeauty.util.BitmapUtility.setImage
import com.tangoplus.facebeauty.vm.InformationViewModel
import com.tangoplus.facebeauty.vm.MainViewModel
import com.tangoplus.facebeauty.vm.MeasureViewModel
import kotlinx.coroutines.launch

class ZoomInDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentZoomInDialogBinding
    private val ivm : InformationViewModel by activityViewModels()
    private val mvm : MainViewModel by activityViewModels()

    companion object {
    private const val ARG_SEQ = "arguement_sequence"
        fun newInstance(seq: Int) : ZoomInDialogFragment {

            val fragment = ZoomInDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SEQ, seq)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentZoomInDialogBinding.inflate(inflater)
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FlexableDialogFragment)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seq = arguments?.getInt(ARG_SEQ) ?: 0

        lifecycleScope.launch {
            mvm.currentResult.value?.let { setImage(this@ZoomInDialogFragment, it, seq, binding.ssivZID, ivm, true) }
        }
    }
}