package com.tangoplus.facebeauty.ui.adapter

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.FaceComparisonItem
import com.tangoplus.facebeauty.data.RVItemType

import com.tangoplus.facebeauty.databinding.RvFaceStaticItemBinding
import com.tangoplus.facebeauty.ui.listener.OnAdapterMoreClickListener
import com.tangoplus.facebeauty.ui.listener.OnFaceStaticCheckListener
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.vm.InformationViewModel
import com.tangoplus.facebeauty.vm.MainViewModel
import java.text.DecimalFormat
import kotlin.math.abs

class FaceStaticRVAdapter(private val faceComparisonItems: List<FaceComparisonItem>, private val basicInfo : Pair<String, String>? = null, private val seqValue: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var faceStaticCheckListener: OnFaceStaticCheckListener? = null
    var adapterMoreClickedListener : OnAdapterMoreClickListener? = null
    var isExpanded = false
    private val linkedCheckDetailMap = listOf(
        mapOf(
            3 to listOf(4), 4 to listOf(3),
            5 to listOf(6), 6 to listOf(5),
            7 to listOf(8), 8 to listOf(7)
        ),
        mapOf(
            0 to listOf(1, 2, 3),
            1 to listOf(0, 2, 3),
            2 to listOf(0, 1, 3),
            3 to listOf(0, 1, 2)
        ),
        mapOf(
            0 to listOf(1),
            1 to listOf(0)
        )
    )
    private val linkedCheckComparisonMap = listOf(
        mapOf(
            3 to listOf(4), 4 to listOf(3),
            5 to listOf(6), 6 to listOf(5),
            7 to listOf(8), 8 to listOf(7)
        ),
        mapOf(
            3 to listOf(4), 4 to listOf(3),
            5 to listOf(6), 6 to listOf(5),
            7 to listOf(8), 8 to listOf(7)
        ),
        mapOf(
            0 to listOf(1, 2, 3),
            1 to listOf(0, 2, 3),
            2 to listOf(0, 1, 3),
            3 to listOf(0, 1, 2)
        ),
        mapOf(
            0 to listOf(1, 2, 3),
            1 to listOf(0, 2, 3),
            2 to listOf(0, 1, 3),
            3 to listOf(0, 1, 2)
        ),
        mapOf(
            0 to listOf(1),
            1 to listOf(0)
        ),
        mapOf(
            0 to listOf(0),
            1 to listOf(1),
            2 to listOf(2)
        )

    )
    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_MORE = 1
    }
    private val visibleList: List<FaceComparisonItem>
        get() = if (isExpanded) faceComparisonItems else faceComparisonItems.take(3)

    inner class FSViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val clFSI: ConstraintLayout = view.findViewById(R.id.clFSI)
        private val cbFSITitle: MaterialCheckBox = view.findViewById(R.id.cbFSITitle)
        private val tvFSI0: TextView = view.findViewById(R.id.tvFSI0)
        private val tvFSI1: TextView = view.findViewById(R.id.tvFSI1)
        private val tvFSITitle0 : TextView = view.findViewById(R.id.tvFSITitle0)
        private val tvFSITitle1 : TextView = view.findViewById(R.id.tvFSITitle1)
        private val cvFSIState : CardView = view.findViewById(R.id.cvFSIState)

        fun bind(item: FaceComparisonItem, position: Int) {
            val df = DecimalFormat("#.##")
            cbFSITitle.text = item.label

            // state 확인
            val notation = when (item.label) {
                in listOf("왼쪽 중간 턱", "오른쪽 중간 턱", "입 높이") -> { "cm" }
                in listOf("왼쪽 볼 너비", "오른쪽 볼 너비") -> "㎠"
                else -> "˚"
            }
            val seq0Data = df.format(item.leftValue) + notation
            val seq1Data = df.format(item.rightValue) + notation
            tvFSI0.text = seq0Data
            tvFSI1.text = seq1Data

            if (basicInfo != null) {
                val leftTitle = "왼쪽 ${basicInfo.first.substring(5, 10).replace("-", "월 ")}일"
                tvFSITitle0.text =  leftTitle
                val rightTitle = "오른쪽 ${basicInfo.second.substring(5, 10).replace("-", "월 ")}일"
                tvFSITitle1.text = rightTitle

                // 비교일 때
                cbFSITitle.setOnCheckedChangeListener(null)
                cbFSITitle.isChecked = item.isChecked

                cbFSITitle.setOnCheckedChangeListener { _, isChecked ->
                    item.isChecked = isChecked
                    faceStaticCheckListener?.onFaceStaticCheck(position, isChecked)

                    // 연결된 아이템 체크 연동
                    linkedCheckComparisonMap[seqValue][position]?.forEach { linkedPos ->
                        val linkedItem = faceComparisonItems[linkedPos]
                        if (linkedItem.isChecked != isChecked) {
                            linkedItem.isChecked = isChecked
                            notifyItemChanged(linkedPos)
                        }
                    }
                }

            } else {
                // 상세보기일 때
                val leftTitle = when (seqValue) {
                    0 -> "안정 상태"
                    1 -> "턱 왼쪽 쏠림"
                    else -> if (item.label in listOf("입 높이", "입 각도")) "입 벌림" else "턱 폄"
                }
                tvFSITitle0.text = leftTitle
                val rightTitle = when (seqValue) {
                    0 -> "교합 상태"
                    else -> "턱 오른쪽 쏠림"
                }
                tvFSITitle1.text =  rightTitle

                clFSI.setOnSingleClickListener {
                    cbFSITitle.performClick()
                }
                tvFSI0.setTypeface(null, Typeface.NORMAL)
                tvFSI1.setTypeface(null, Typeface.NORMAL)
                if (item.leftValue != null && item.rightValue != null) {
                    when {
                        item.label in listOf("볼 너비", "중간 턱", "입 높이") -> {
//                            Log.v("값들", "양쪽: ${abs(item.leftValue)}, ${abs(item.rightValue)}")
                            if (abs(item.leftValue) < abs(item.rightValue)) {
                                tvFSI1.setTypeface(null, Typeface.BOLD)
                            } else {
                                tvFSI0.setTypeface(null, Typeface.BOLD)
                            }
                        }
                        else -> { // item.label.contains("거리")
//                            Log.v("값들", "거리: ${abs(item.leftValue)}, ${abs(item.rightValue)}")

                            if (abs(item.leftValue) > abs(item.rightValue)) {
                                tvFSI1.setTypeface(null, Typeface.BOLD)
                            } else {
                                tvFSI0.setTypeface(null, Typeface.BOLD)
                            }
                        }
                    }
                }
                cbFSITitle.setOnCheckedChangeListener(null)
                cbFSITitle.isChecked = item.isChecked

                cbFSITitle.setOnCheckedChangeListener { _, isChecked ->
                    item.isChecked = isChecked
                    faceStaticCheckListener?.onFaceStaticCheck(position, isChecked)

                    // 연결된 아이템 체크 연동
                    linkedCheckDetailMap[seqValue][position]?.forEach { linkedPos ->
                        val linkedItem = faceComparisonItems[linkedPos]
                        if (linkedItem.isChecked != isChecked) {
                            linkedItem.isChecked = isChecked
                            notifyItemChanged(linkedPos)
                        }
                    }
                }

            }
            // 입 벌림, 턱 들어올림 일 때 오른쪾 아이템 없애기
            if (item.label in listOf("입 높이", "입 각도", "양 어깨", "양 귀", "목 각도")) {
                tvFSITitle1.text = ""
                tvFSI1.text = ""
            }
        }
    }
    inner class FSMIViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val button = view.findViewById<TextView>(R.id.tvFSMIMore)
        fun bind(isExpanded: Boolean, onClick: () -> Unit) {
            button.text = if (isExpanded) "접기" else "더보기"
            button.setOnSingleClickListener {
                onClick()
                adapterMoreClickedListener?.adapterMoreClicked(isExpanded)
                Log.v("확장", "$isExpanded")
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MORE -> {
                val view = inflater.inflate(R.layout.rv_face_static_more_item, parent, false)
                FSMIViewHolder(view)
            }

            else -> {
                val binding = RvFaceStaticItemBinding.inflate(inflater, parent, false)
                FSViewHolder(binding.root)
            }
        }
    }

    override fun getItemCount(): Int {
        return visibleList.size + 1 // 마지막은 무조건 "더보기" 버튼
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FSViewHolder) {
            val item = faceComparisonItems[position]
            holder.bind(item, position)
        } else if (holder is FSMIViewHolder) {
            holder.bind(isExpanded) {
                isExpanded = !isExpanded
                notifyDataSetChanged()
            }
        }
    }
    override fun getItemViewType(position: Int): Int {
        return if (position == visibleList.size) TYPE_MORE else TYPE_NORMAL
    }
    fun setAllChecked(checked: Boolean) {
        faceComparisonItems.forEach { item ->
            // NORMAL 타입만 체크되도록 (제목/빈칸은 무시)
            if (item.type == RVItemType.NORMAL) {
                item.isChecked = checked
            }
        }
        notifyDataSetChanged()
    }

}
