package com.tangoplus.facebeauty.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.facebeauty.R
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.databinding.RvMeasureItemBinding
import com.tangoplus.facebeauty.ui.view.OnMeasureClickListener
import com.tangoplus.facebeauty.util.FileUtility.setOnSingleClickListener

class MeasureRVAdapter(private val context: Context, private val faceResults: List<FaceResult>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var measureClickListener : OnMeasureClickListener? = null

    inner class MeasureViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val clMI : ConstraintLayout = view.findViewById(R.id.clMI)
        val tvMIDate : TextView = view.findViewById(R.id.tvMIDate)
        val tvMINameMobile : TextView = view.findViewById(R.id.tvMINameMobile)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureItemBinding.inflate(inflater, parent, false)
        return MeasureViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = faceResults[position]

        if (holder is MeasureViewHolder) {
            holder.tvMIDate.text = currentItem.regDate?.substring(0,
                currentItem.regDate?.length?.minus(3) ?: 0
            )
            val mergedNameMobile = "${currentItem.userName}${if (currentItem.userMobile?.startsWith("010") == true) ", " + currentItem.userMobile else ""}"
            holder.tvMINameMobile.text = mergedNameMobile

            holder.clMI.setOnSingleClickListener {
                measureClickListener?.onMeasureClick(currentItem.tempServerSn)
            }
        }
    }

    override fun getItemCount(): Int {
        return faceResults.size
    }

//    private fun maskingName(name: String?) : String{
//        return (if (name != "GUEST") {
//            name?.replaceRange(1, name.length - 2, "*")
//        } else {
//            "GUEST"
//        }).toString()
//    }
//    private fun maskingMobile(mobile: String?) : String? {
//        val maskedMobile = mobile?.replaceRange(4, 6, "*")
//            ?.replaceRange(8, 10, "*")
//        return maskedMobile
//    }
}