package com.husnain.authy.utls

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.husnain.authy.R
import com.husnain.authy.databinding.DialogSinginBinding

object CustomDialogs {
    fun dialogAuthForAutoSync(
        context: Context,
        inflater: LayoutInflater,
        login: () -> Unit,
        signup: () -> Unit,
    ) {
        val dialog = Dialog(context)
        val binding = DialogSinginBinding.inflate(inflater, null, false)

        dialog.apply {
            setContentView(binding.root)
            setCancelable(true)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val horizontalMargin = context.resources.getDimensionPixelSize(R.dimen.padding_top_20dp)
            val screenWidth = context.resources.displayMetrics.widthPixels
            val dialogWidth = screenWidth - 2 * horizontalMargin

            window?.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT)

            val layoutParams = window?.attributes
            layoutParams?.gravity = Gravity.TOP
            layoutParams?.y = context.resources.getDimensionPixelSize(R.dimen.margin_top_singin_dialog)
            window?.attributes = layoutParams

            binding.btnLogin.setOnClickListener {
                login.invoke()
                dismiss()
            }
            binding.tvSignup.setOnClickListener {
                signup.invoke()
                dismiss()
            }

            show()
        }
    }

}