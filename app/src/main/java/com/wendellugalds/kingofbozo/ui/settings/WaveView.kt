package com.wendellugalds.kingofbozo.ui.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class WaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var waveOffset = 0f
    private val waveHeight = 30f
    private val waveLength = 600f

    init {
        paint.style = Paint.Style.FILL
    }

    fun setWaveColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0f) return

        val width = width.toFloat()
        val height = height.toFloat()
        val fillHeight = height * progress
        val top = height - fillHeight

        path.reset()
        
        // Começa um pouco antes da tela para a onda cobrir tudo
        path.moveTo(-20f, top)
        
        // Desenha o topo ondulado
        var x = 0f
        val step = 10f
        while (x <= width + step) {
            val y = top + waveHeight * sin((x + waveOffset) * 2 * Math.PI / waveLength).toFloat()
            path.lineTo(x, y)
            x += step
        }
        
        // Fecha o retângulo na parte de baixo
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        canvas.drawPath(path, paint)

        // Anima o deslocamento da onda
        waveOffset += 10f
        if (waveOffset > waveLength) waveOffset -= waveLength
        
        // Força a atualização contínua para o efeito de animação
        postInvalidateOnAnimation()
    }
}
