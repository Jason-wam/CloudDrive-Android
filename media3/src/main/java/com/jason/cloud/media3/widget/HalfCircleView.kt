package com.jason.cloud.media3.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class HalfCircleView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var pointA = PointF(0f, 0f)
    private var pointB = PointF(0f, 0f)
    private var pointC = PointF(0f, 0f)
    private val paint = Paint()

    init {
        paint.color = Color.parseColor("#20FFFFFF")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pointA = PointF(0f, 0f)
        pointB = PointF(w.toFloat(), h.toFloat() / 2)
        pointC = PointF(0f, h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val circleCenter = calculateCircleCenter(pointA, pointB, pointC)
        val centerX = circleCenter.x
        val centerY = circleCenter.y
        println("圆心坐标：($centerX, $centerY)")
        val radius = abs(centerX - pointB.x)

        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    private fun calculateCircleCenter(pointA: PointF, pointB: PointF, pointC: PointF): PointF {
        // 计算边AB和边AC的中点
        val midAB = PointF((pointA.x + pointB.x) / 2f, (pointA.y + pointB.y) / 2f)
        val midAC = PointF((pointA.x + pointC.x) / 2f, (pointA.y + pointC.y) / 2f)

        // 计算直线AB和直线AC的斜率
        val slopeAB = (pointB.y - pointA.y) / (pointB.x - pointA.x)
        val slopeAC = (pointC.y - pointA.y) / (pointC.x - pointA.x)

        // 计算垂直平分线的斜率
        val slopeMidAB = -1 / slopeAB
        val slopeMidAC = -1 / slopeAC

        // 计算中点与垂直平分线方程的截距
        val interceptAB = midAB.y - slopeMidAB * midAB.x
        val interceptAC = midAC.y - slopeMidAC * midAC.x

        // 计算圆心的x坐标
        val centerX = (interceptAC - interceptAB) / (slopeMidAB - slopeMidAC)

        // 计算圆心的y坐标
        val centerY = slopeMidAB * (centerX - midAB.x) + midAB.y

        return PointF(centerX, centerY)
    }


}