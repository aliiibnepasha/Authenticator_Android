package com.husnain.authy.utls

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.husnain.authy.R

class LoadingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        const val SCALE = 1.0f
        const val ALPHA = 255
    }

    private val scaleFloats = FloatArray(8) { SCALE }
    private val alphas = IntArray(8) { ALPHA }
    private val animators = mutableListOf<ValueAnimator>()
    private var indicatorColor: Int = Color.BLACK  // Default color is black

    init {
        // Load custom attributes from XML
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LoadingView,
            0, 0
        ).apply {
            try {
                // Fetch the custom color attribute from XML, default is black
                indicatorColor = getColor(R.styleable.LoadingView_indicatorColor, Color.BLACK)
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paint = Paint()
        paint.isAntiAlias = true

        val radius = width / 10f
        paint.color = indicatorColor  // Use the custom color for the indicator

        for (i in 0 until 8) {
            canvas.save()
            // Calculate position in circular layout
            val point = circleAt(width, height, width / 2.5f - radius, i * (Math.PI / 4))
            canvas.translate(point.x, point.y)
            canvas.scale(scaleFloats[i], scaleFloats[i])
            canvas.rotate(i * 45f)  // Apply rotation for each line

            paint.alpha = alphas[i]
            // Draw a rounded rectangle (line effect)
            val rectF = RectF(-radius, -radius / 1.5f, radius * 1.5f, radius / 1.5f)
            canvas.drawRoundRect(rectF, 5f, 5f, paint)
            canvas.restore()
        }
    }

    // Start the animations
    fun start(viewToHideIf: View? = null) {
        viewToHideIf?.gone()
        visibility = VISIBLE
        val delays = intArrayOf(0, 120, 240, 360, 480, 600, 720, 780)

        for (i in 0 until 8) {
            val index = i

            // Scale Animation
            val scaleAnim = ValueAnimator.ofFloat(1f, 0.4f, 1f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                startDelay = delays[i].toLong()
                addUpdateListener { animation ->
                    scaleFloats[index] = animation.animatedValue as Float
                    postInvalidate()  // Redraw view on animation update
                }
            }

            // Alpha Animation
            val alphaAnim = ValueAnimator.ofInt(255, 77, 255).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                startDelay = delays[i].toLong()
                addUpdateListener { animation ->
                    alphas[index] = animation.animatedValue as Int
                    postInvalidate()  // Redraw view on animation update
                }
            }

            // Add to list of animators for future control
            animators.add(scaleAnim)
            animators.add(alphaAnim)

            scaleAnim.start()
            alphaAnim.start()
        }
    }

    // Stop the animations
    fun stop(view: View? = null) {
        for (animator in animators) {
            if (animator.isRunning) {
                animator.cancel()  // Cancel the animator
            }
        }
        animators.clear()  // Clear the list of animators

        // Make the view gone after stopping the animations
        visibility = GONE
        view?.visible()
    }

    /**
     * Calculate the position of a point on a circle with center (a, b) and radius R,
     * where the angle is α.
     */
    private fun circleAt(width: Int, height: Int, radius: Float, angle: Double): Point {
        val x = (width / 2f + radius * Math.cos(angle)).toFloat()
        val y = (height / 2f + radius * Math.sin(angle)).toFloat()
        return Point(x, y)
    }

    data class Point(val x: Float, val y: Float)

    // Set a custom color for the indicator
    fun setIndicatorColor(color: Int) {
        indicatorColor = color
        invalidate()  // Redraw the view with the new color
    }
}
