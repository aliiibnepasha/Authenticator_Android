package com.husnain.authy.utls

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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

        // Convert 14 dp to pixels for corner radius
        val cornerRadius = resources.displayMetrics.density * 14

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

            // Create the inner rectangle with the updated vertical position
            val innerRectangle = RectF(
                location[0].toFloat(),
                shiftedY.toFloat(),  // Adjusted Y position
                location[0].toFloat() + frame.width.toFloat(),
                shiftedY.toFloat() + frame.height.toFloat()
            )

            // Create the inner rounded rectangle path
            val path = Path()
            path.addRoundRect(
                innerRectangle,
                cornerRadius,
                cornerRadius,
                Path.Direction.CW
            )

            // Draw the clear area (inside the frame)
            osCanvas.drawPath(path, paint)


            // Optionally, draw a border around the frame
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            strokePaint.strokeWidth = 5f
            strokePaint.color = Color.WHITE
            strokePaint.style = Paint.Style.STROKE
            osCanvas.drawPath(path,strokePaint)
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