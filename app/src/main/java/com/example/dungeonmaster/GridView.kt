package com.example.dungeonmaster

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

class GridView(context : Context, val size : Int) : View(context) {

    private var itemW = 0
    private var itemH = 0

    private var grid : Array<Array<ImageView>> = Array(size){
        Array(size){
            ImageView(context)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.YELLOW)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        itemW = w / size
        itemH = itemW

        for (i in 0 until size){
            for (j in 0 until size){
                val imageView = grid[i][j]
                imageView.layoutParams = FrameLayout.LayoutParams(itemW, itemH)
                imageView.setBackgroundColor(Color.RED)
                imageView.x = (i * itemW).toFloat() + 2f
                imageView.y = (i * itemH).toFloat() + 2f
            }
        }
    }

}