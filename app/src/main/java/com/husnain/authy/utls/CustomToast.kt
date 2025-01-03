package com.husnain.authy.utls

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.husnain.authy.R

object CustomToast {
    private var toast: Toast? = null

    fun Fragment.showCustomToast(strMessage: String) {
        activity?.let { mContext ->
            val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val layout: View = inflater.inflate(R.layout.toast_custom, mContext.findViewById(R.id.toast_container))

            // Set the message
            val text: TextView = layout.findViewById(R.id.toast_txv_message)
            text.text = strMessage

            // Cancel previous toast if exists
            toast?.cancel()

            toast = Toast(mContext)
            val marginToast = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                45f, // Use float for the value
                mContext.resources.displayMetrics
            )

            toast?.apply {
                setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, marginToast.toInt())
                setDuration(Toast.LENGTH_SHORT)
                view = layout
                show()
            }
        }
    }
}
