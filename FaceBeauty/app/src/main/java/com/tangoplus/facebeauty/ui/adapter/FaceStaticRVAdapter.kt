package com.tangoplus.facebeauty.ui.adapter

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import java.text.DecimalFormat
import kotlin.math.abs

class FaceStaticRVAdapter(private val faceComparisonItems: List<FaceComparisonItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var faceStaticCheckListener: OnFaceStaticCheckListener? = null
    var adapterMoreClickedListener : OnAdapterMoreClickListener? = null
    var isExpanded = false
    private val linkedCheckMap = mapOf(

        5 to 6, 6 to 5,
        7 to 8, 8 to 7,
        9 to 10, 10 to 9,

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


        fun bind(item: FaceComparisonItem, position: Int) {
            val df = DecimalFormat("#.##")
            cbFSITitle.text = item.label

            val seq0Data = df.format(item.leftValue) + if (item.label.contains("각도")) "˚" else "cm"
            val seq1Data = df.format(item.rightValue) + if (item.label.contains("각도")) "˚" else "cm"
            tvFSI0.text = seq0Data
            tvFSI1.text = seq1Data
            // TODO 비교 부분 / 입벌린 부분과 턱든부분 데이터 수정 / 하단 멘트 넣기 
            clFSI.setOnSingleClickListener {
                cbFSITitle.performClick()
            }
            tvFSI0.setTypeface(null, Typeface.NORMAL)
            tvFSI1.setTypeface(null, Typeface.NORMAL)
            if (item.leftValue != null && item.rightValue != null) {
                when {
                    item.label.contains("콧망울 수평 각도") -> {
                        Log.v("값들", "콧망울: ${abs(item.leftValue)}, ${abs(item.rightValue)}")
                        if (abs(item.leftValue) > abs(item.rightValue)) {
                            tvFSI1.setTypeface(null, Typeface.BOLD)
                        } else {
                            tvFSI0.setTypeface(null, Typeface.BOLD)
                        }
                    }
                    item.label.contains("수평 각도") -> {
                        Log.v("값들", "수평: ${abs(item.leftValue)}, ${abs(item.rightValue)}")
                        if (abs(item.leftValue) > abs(item.rightValue)) {
                            tvFSI1.setTypeface(null, Typeface.BOLD)
                        } else {
                            tvFSI0.setTypeface(null, Typeface.BOLD)
                        }
                    }
                    item.label.contains("수직 각도") -> {
                        Log.v("값들", "수직: ${abs(item.leftValue)}, ${abs(item.rightValue)}")
                        if (abs(item.leftValue) > abs(item.rightValue)) {
                            tvFSI1.setTypeface(null, Typeface.BOLD)
                        } else {
                            tvFSI0.setTypeface(null, Typeface.BOLD)
                        }
                    }
                    item.label.contains("거리") -> {
                        Log.v("값들", "거리: ${abs(item.leftValue)}, ${abs(item.rightValue)}")

                        if (abs(item.leftValue) < abs(item.rightValue)) {
                            tvFSI1.setTypeface(null, Typeface.BOLD)
                        } else {
                            tvFSI0.setTypeface(null, Typeface.BOLD)
                        }
                    }
                }
            }


            // ✅ 리스너 제거 후 isChecked 세팅 (중복 리스너 방지)
            cbFSITitle.setOnCheckedChangeListener(null)
            cbFSITitle.isChecked = item.isChecked

            // ✅ 리스너 재설정
            cbFSITitle.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                faceStaticCheckListener?.onFaceStaticCheck(position, isChecked)

                // 연결된 아이템 체크 연동
                linkedCheckMap[position]?.let { linkedPos ->
                    val linkedItem = faceComparisonItems[linkedPos]
                    if (linkedItem.isChecked != isChecked) {
                        linkedItem.isChecked = isChecked
                        notifyItemChanged(linkedPos)
                    }
                }
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
