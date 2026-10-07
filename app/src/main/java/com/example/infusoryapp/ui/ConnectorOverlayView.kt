package com.example.infusoryapp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.infusoryapp.R

data class LineConnection(
    val nodeX: Float,
    val nodeY: Float,
    val labelX: Float,
    val labelY: Float
)

class ConnectorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val lineConnections = mutableListOf<LineConnection>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.connector_line)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.border_interactive)
        style = Paint.Style.FILL
    }

    private val outerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_white)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun setConnections(connections: List<LineConnection>) {
        lineConnections.clear()
        lineConnections.addAll(connections)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (conn in lineConnections) {
            // Draw connector line
            canvas.drawLine(conn.nodeX, conn.nodeY, conn.labelX, conn.labelY, linePaint)

            // Draw anchor dot on 3D node position
            canvas.drawCircle(conn.nodeX, conn.nodeY, 8f, dotPaint)
            canvas.drawCircle(conn.nodeX, conn.nodeY, 12f, outerDotPaint)
        }
    }
}
