package com.pollub.samoloty.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class Grid : View {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val paint = Paint()

    init {

        paint.apply {
            color = Color.BLUE
            strokeWidth = 5f
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {

            canvas.drawColor(Color.TRANSPARENT)

            val w = width
            val h = height

            for (i in 2..8 step 2) {
                canvas.drawLine(w * i / 10f, h * 0.9f, w * i / 10f, w * 0.1f, paint)
            }
        }

    }


}