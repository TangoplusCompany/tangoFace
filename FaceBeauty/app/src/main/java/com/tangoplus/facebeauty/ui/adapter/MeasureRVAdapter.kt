package com.tangoplus.facebeauty.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.FaceDisplay
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.databinding.RvMeasureComparisonItemBinding
import com.tangoplus.facebeauty.databinding.RvMeasureItemBinding
import com.tangoplus.facebeauty.ui.listener.OnMeasureClickListener
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener
import com.tangoplus.facebeauty.vm.MainViewModel
import java.lang.IllegalArgumentException

class MeasureRVAdapter(private val context: Context, private val faceResults: List<FaceDisplay>, private val mvm : MainViewModel, private val case: Int = 0) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var measureClickListener : OnMeasureClickListener? = null

    inner class MeasureViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val clMI : ConstraintLayout = view.findViewById(R.id.clMI)
        val tvMIDate : TextView = view.findViewById(R.id.tvMIDate)
        val tvMINameMobile : TextView = view.findViewById(R.id.tvMINameMobile)
        val ivMICheck : ImageView = view.findViewById(R.id.ivMICheck)
    }

    inner class MeasureHorizonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMHIDate : TextView = view.findViewById(R.id.tvMHIDate)
        val tvMHINameMobile : TextView = view.findViewById(R.id.tvMHINameMobile)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (case) {
            0 -> {
                val binding = RvMeasureItemBinding.inflate(inflater, parent, false)
                MeasureViewHolder(binding.root)
            }
            1 -> {
                val binding = RvMeasureComparisonItemBinding.inflate(inflater, parent, false)
                MeasureHorizonViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = faceResults[position]

        if (holder is MeasureViewHolder) {
            holder.clMI.background = ContextCompat.getDrawable(context, R.drawable.effect_20)
            holder.ivMICheck.visibility = View.INVISIBLE

            holder.tvMIDate.text = currentItem.regDate?.substring(0,
                currentItem.regDate?.length?.minus(3) ?: 0
            )
            val mergedNameMobile = "${currentItem.userName}${if (currentItem.userMobile?.startsWith("010") == true) ", " + currentItem.userMobile else ""}"
            holder.tvMINameMobile.text = mergedNameMobile

            holder.clMI.setOnSingleClickListener {

                // 비교 상황
                when (mvm.getComparisonState()) {
                    true -> {
                        if (mvm.tempComparisonItems.value != null && mvm.tempComparisonItems.value?.size!! <= 2) {
                            if (currentItem in mvm.tempComparisonItems.value!!) {
                                mvm.removeItem(currentItem)
                                holder.clMI.background = ContextCompat.getDrawable(context, R.drawable.effect_20)
                                holder.ivMICheck.visibility = View.INVISIBLE
                            } else if (mvm.tempComparisonItems.value?.size!! < 2) {
                                mvm.addItem(currentItem)
                                holder.clMI.background = ContextCompat.getDrawable(context, R.drawable.bckgnd_stroke_2_20)
                                holder.ivMICheck.visibility = View.VISIBLE
                            }
                        }
                        Log.v("담은 것들", "${mvm.tempComparisonItems.value?.size}")
                    }
                    false -> {
//                        holder.clMI.background = ContextCompat.getDrawable(context, R.drawable.effect_20)
//                        holder.ivMICheck.visibility = View.INVISIBLE
                        measureClickListener?.onMeasureClick(currentItem.tempServerSn)
                    }
                }

            }
        } else if (holder is MeasureHorizonViewHolder) {
            holder.tvMHIDate.text = currentItem.regDate?.substring(0,
                currentItem.regDate?.length?.minus(3) ?: 0
            )
            val name = "${currentItem.userName}"
            holder.tvMHINameMobile.text = name
        }
    }

    override fun getItemCount(): Int {
        return faceResults.size
    }

    fun resetAllItemsUI() {
        notifyDataSetChanged()
    }

}