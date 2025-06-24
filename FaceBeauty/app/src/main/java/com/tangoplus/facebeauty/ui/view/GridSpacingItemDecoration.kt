package com.tangoplus.facebeauty.ui.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacingPx: Int,
    private val includeEdge: Boolean,
    private val specialRowSpacingPx: Int = spacingPx,
    private val specialRows: Set<Int> = emptySet()
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val column = position % spanCount
        val row = position / spanCount
        val isSpecialRow = specialRows.contains(row)
        val actualBottomSpacing = if (isSpecialRow) specialRowSpacingPx else spacingPx

        if (includeEdge) {
            outRect.left = spacingPx - column * spacingPx / spanCount
            outRect.right = (column + 1) * spacingPx / spanCount

            if (row == 0) {
                outRect.top = spacingPx
            }

            outRect.bottom = actualBottomSpacing

        } else {
            outRect.left = column * spacingPx / spanCount
            outRect.right = spacingPx - (column + 1) * spacingPx / spanCount

            if (row > 0) {
                outRect.top = spacingPx
            }

            outRect.bottom = if (isSpecialRow) specialRowSpacingPx else 0
        }
    }
}
