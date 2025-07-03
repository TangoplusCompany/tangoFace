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
//        private val llFSI : LinearLayout = view.findViewById(R.id.llFSI)
//        private val llFSI0 : LinearLayout = view.findViewById(R.id.llFSI0)
//        private val llFSI1 : LinearLayout = view.findViewById(R.id.llFSI1)
//        private val llFSI2 : LinearLayout = view.findViewById(R.id.llFSI2)
//        private val tvNorm0: TextView = view.findViewById(R.id.tvNorm0)
//        private val tvNorm1: TextView = view.findViewById(R.id.tvNorm1)
//        private val tvNorm2: TextView = view.findViewById(R.id.tvNorm2)
//        private val tvComment0: TextView = view.findViewById(R.id.tvComment0)
//        private val tvComment1: TextView = view.findViewById(R.id.tvComment1)
//        private val tvComment2: TextView = view.findViewById(R.id.tvComment2)

        fun bind(item: FaceComparisonItem, position: Int) {
            val df = DecimalFormat("#.##")
            cbFSITitle.text = item.label

            val seq0Data = df.format(item.restingValue) + if (item.label.contains("각도")) "˚" else "cm"
            val seq1Data = df.format(item.occlusalValue) + if (item.label.contains("각도")) "˚" else "cm"
            tvFSI0.text = seq0Data
            tvFSI1.text = seq1Data

            clFSI.setOnSingleClickListener {
                cbFSITitle.performClick()
            }
            tvFSI0.setTypeface(null, Typeface.NORMAL)
            tvFSI1.setTypeface(null, Typeface.NORMAL)

//            if (position > 5) {
//                llFSI0.visibility = View.GONE
//                llFSI1.visibility = View.GONE
//                llFSI2.visibility = View.GONE
//            } else {
//                llFSI0.visibility = View.VISIBLE
//                llFSI1.visibility = View.VISIBLE
//                llFSI2.visibility = View.VISIBLE
//
//                val normAndComments = getNormAndComments(position - 2)
//                tvNorm0.text = normAndComments[0].first
//                tvNorm1.text = normAndComments[1].first
//                tvNorm2.text = normAndComments[2].first
//                tvComment0.text = normAndComments[0].second
//                tvComment1.text = normAndComments[1].second
//                tvComment2.text = normAndComments[2].second
//            }



            when {
                item.label.contains("콧망울 수평 각도") -> {
                    Log.v("값들", "콧망울: ${abs(item.restingValue)}, ${abs(item.occlusalValue)}")
                    if (abs(item.restingValue) > abs(item.occlusalValue)) {
                        tvFSI1.setTypeface(null, Typeface.BOLD)
                    } else {
                        tvFSI0.setTypeface(null, Typeface.BOLD)
                    }
                }
                item.label.contains("수평 각도") -> {
                    Log.v("값들", "수평: ${abs(item.restingValue)}, ${abs(item.occlusalValue)}")
                    if (abs(item.restingValue) > abs(item.occlusalValue)) {
                        tvFSI1.setTypeface(null, Typeface.BOLD)
                    } else {
                        tvFSI0.setTypeface(null, Typeface.BOLD)
                    }
                }
                item.label.contains("수직 각도") -> {
                    Log.v("값들", "수직: ${abs(item.restingValue)}, ${abs(item.occlusalValue)}")
                    if (abs(item.restingValue) > abs(item.occlusalValue)) {
                        tvFSI1.setTypeface(null, Typeface.BOLD)
                    } else {
                        tvFSI0.setTypeface(null, Typeface.BOLD)
                    }
                }
                item.label.contains("거리") -> {
                    Log.v("값들", "거리: ${abs(item.restingValue)}, ${abs(item.occlusalValue)}")

                    if (abs(item.restingValue) < abs(item.occlusalValue)) {
                        tvFSI1.setTypeface(null, Typeface.BOLD)
                    } else {
                        tvFSI0.setTypeface(null, Typeface.BOLD)
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

//    private fun getNormAndComments(case: Int) : List<Pair<String, String>> {
//        val lipsAndEarFlapNorms = listOf("±0.8° 이내", "±0.8° ~ 1.5°", "±1.6° 이상")
//        val eyeNorms = listOf("±0.6° 이내", "±0.7° ~ 1.2°", "±1.3° 이상")
//        val foreheadNorms = listOf("±0.5° 이내", "±0.6° ~ 1.5°", "±1.6° 이상")
//
//        val eyeComments = listOf("눈높이 균형 양호, 안와 대칭", "좌우 눈 높이 약간 다름, 안면 비대칭 초기에 보임", "한쪽 눈이 명확히 높거나 낮아 보임, 안와 위치 이상 또는 두개골 비대칭 가능성")
//        val earFlapComments = listOf("귀 높이 거의 같음, 정렬 양호", "좌우 귀 높이 약간 차이, 두개골·경추 정렬 영향 가능성", "귀 높이 차이 명확, 경추 틀어짐 또는 두개골 기울기 의심")
//        val lipsComments = listOf("자연스러운 대칭, 정렬 양호", "경미한 비대칭, 습관 또는 초기 이상 가능성, 두개골·경추 정렬 영향 가능성", "명확한 비대칭, 턱관절·골격 문제 가능성")
//        val foreheadComments = listOf("거의 수직, 중심선 잘 유지됨", "중심선 약간 기울어 있음, 비대칭 초기 경향", "명백한 중심선 기울기, 안면 비대칭 또는 상부 경추 회전 가능성 있음")
//
//
//        return when (case) {
//            0 -> eyeNorms.zip(eyeComments)
//            1 -> lipsAndEarFlapNorms.zip(earFlapComments)
//            2 -> lipsAndEarFlapNorms.zip(lipsComments)
//            else -> foreheadNorms.zip(foreheadComments)
//        }
//    }
}
