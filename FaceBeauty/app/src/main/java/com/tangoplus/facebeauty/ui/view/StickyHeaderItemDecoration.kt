package com.tangoplus.facebeauty.ui.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderItemDecoration(
    private val isHeader : (itemPosition : Int) -> Boolean,
    private val getHeaderTitle: (itemPosition : Int) -> String
) : RecyclerView.ItemDecoration() {
    private val headerHeight = 64
    private val headerPaint = Paint().apply {
        color = "#F6F6F6".toColorInt() // 빨간색으로 확실히 보이게 #FF5449
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = "#000000".toColorInt()
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

//    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
//        super.getItemOffsets(outRect, view, parent, state)
//        val position = parent.getChildAdapterPosition(view)
//
//        if (position != RecyclerView.NO_POSITION && isHeader(position)) {
//            outRect.top = headerHeight
//        }
//    }
override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    super.getItemOffsets(outRect, view, parent, state)
    val position = parent.getChildAdapterPosition(view)

    if (position != RecyclerView.NO_POSITION) {
        val layoutManager = parent.layoutManager as? GridLayoutManager
        if (layoutManager != null) {
            val spanSizeLookup = layoutManager.spanSizeLookup
            val spanSize = spanSizeLookup.getSpanSize(position)

            when {
                // 첫 번째 헤더(0번째)는 top margin 없음
                position == 0 && isHeader(position) && spanSize == layoutManager.spanCount -> {
                    outRect.top = 0
                }

                // 나머지 헤더들만 top margin 적용
                isHeader(position) && spanSize == layoutManager.spanCount -> {
                    outRect.top = headerHeight
                }

                // 첫 번째 헤더 바로 다음 아이템들은 margin 없음
                isFirstRowAfterFirstHeader(position, layoutManager) -> {
                    outRect.top = 0
                }

                // 다른 헤더들 다음 아이템들만 margin 적용
                isFirstRowAfterOtherHeaders(position, layoutManager) -> {
                    outRect.top = headerHeight
                }
            }
        }
    }
}

    private fun isFirstRowAfterFirstHeader(position: Int, layoutManager: GridLayoutManager): Boolean {
        // 0번째가 헤더인지 확인
        if (!isHeader(0)) return false

        // 현재 위치가 첫 번째 헤더 다음 첫 번째 행인지 확인
        var currentSpan = 0
        for (i in 1 until position) {
            val spanSize = layoutManager.spanSizeLookup.getSpanSize(i)
            currentSpan += spanSize
            if (currentSpan >= layoutManager.spanCount) {
                return false // 이미 첫 번째 행을 넘어섰음
            }
        }
        return position > 0 && currentSpan < layoutManager.spanCount
    }

    private fun isFirstRowAfterOtherHeaders(position: Int, layoutManager: GridLayoutManager): Boolean {
        // 바로 위 헤더 찾기
        var headerPosition = -1
        for (i in position - 1 downTo 0) {
            if (isHeader(i) && layoutManager.spanSizeLookup.getSpanSize(i) == layoutManager.spanCount) {
                headerPosition = i
                break
            }
        }

        // 헤더가 없거나 첫 번째 헤더면 false
        if (headerPosition == -1 || headerPosition == 0) return false

        // 헤더 다음 첫 번째 행인지 확인
        var currentSpan = 0
        for (i in headerPosition + 1 until position) {
            val spanSize = layoutManager.spanSizeLookup.getSpanSize(i)
            currentSpan += spanSize
            if (currentSpan >= layoutManager.spanCount) {
                return false
            }
        }
        return true
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val childCount = parent.childCount
        if (childCount == 0) return

        // 각 아이템의 헤더 그리기
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)

            if (position != RecyclerView.NO_POSITION && isHeader(position)) {
                val title = getHeaderTitle(position)

                // 헤더 위치 계산
                val left = parent.paddingLeft.toFloat()
                val right = (parent.width - parent.paddingRight).toFloat()
                val top = (child.top - headerHeight).toFloat()
                val bottom = child.top.toFloat()

                // 배경 그리기
                c.drawRect(left, top, right, bottom, headerPaint)

                // 텍스트 그리기
                val textY = top + (headerHeight / 2f) + (textPaint.textSize / 3f)
                c.drawText(title, left + 32f, textY, textPaint)
            }
        }
    }
}