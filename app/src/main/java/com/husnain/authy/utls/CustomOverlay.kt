package com.husnain.authy.utls

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.RequiresApi

class CustomOverlay : LinearLayout {

    private var windowFrame: Bitmap? = null
    // Add this for getting the refrence of linear layout
    private var frameRef : LinearLayout? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )


    fun setFrameReference(linearLayout: LinearLayout){
        frameRef = linearLayout
    }


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (windowFrame == null) {
            createWindowFrame()
        }
        windowFrame?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    override fun isEnabled(): Boolean {
        return false
    }

    override fun isClickable(): Boolean {
        return false
    }

    private fun createWindowFrame() {
        // Convert 20 dp to pixels
        val twentyDp = resources.displayMetrics.density * 50

        // Initialize the bitmap for the overlay
        windowFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val osCanvas = Canvas(windowFrame!!)

        // Draw the dimmed overlay
        val outerRectangle = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(160, 0, 0, 0)  // Dimmed area alpha
        osCanvas.drawRect(outerRectangle, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)

        // Calculate the inner frame position and size
        frameRef?.let { frame ->
            val location = IntArray(2)
            frame.getLocationInWindow(location)  // Get position relative to the window

            // Shift the frame up by 20 dp
            val shiftedY = location[1] - twentyDp

            //  *** ADJUST THE HEIGHT HERE ***
            val originalFrameHeight = frame.height.toFloat()
            val extraHeight = resources.displayMetrics.density * 30 // Example: Add 50dp extra height
            val extendedHeight = originalFrameHeight + extraHeight


            // Create the inner rectangle with full width and the same height as the frame, extended
            val innerRectangle = RectF(
                0f,  // Full width start
                shiftedY.toFloat(),  // Adjusted Y position
                width.toFloat(),  // Full width end
                shiftedY.toFloat() + extendedHeight  //Extended Height
            )

            // Create the clear area
            osCanvas.drawRect(innerRectangle, paint)
        }
    }


    override fun isInEditMode(): Boolean {
        return true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        windowFrame = null
    }
}