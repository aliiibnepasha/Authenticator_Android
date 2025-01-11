package com.husnain.authy.utls

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.husnain.authy.R
import com.husnain.authy.databinding.BottomSheetDeleteTotpBinding
import com.husnain.authy.databinding.BottomSheetLayoutBinding

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

inline fun View.onClick(crossinline onClick: (View) -> Unit) {
    setOnClickListener {
        onClick(it)
    }
}

fun Fragment.navigate(
    @IdRes destinationId: Int,
    args: Bundle? = null,
) {
    val navController = findNavController()
    val options = NavOptions.Builder().apply {
        setEnterAnim(R.anim.slide_in)
        setExitAnim(R.anim.fade_out)
        setPopEnterAnim(R.anim.fade_in)
        setPopExitAnim(R.anim.slide_out)
    }.build()
    navController.navigate(destinationId, args, options)
}

fun Fragment.popBack(){
    findNavController().popBackStack()
}
fun View.invisible() {
    visibility = View.INVISIBLE
}


fun Activity.startActivity(destinationActivity: Class<*>) {
    val intent = Intent(this, destinationActivity)
    startActivity(intent)
}

fun View.showWithAnimation(duration: Long = 300) {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    this.animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
}

fun View.hideWithAnimation(duration: Long = 300) {
    this.animate()
        .alpha(0f)
        .setDuration(duration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                this@hideWithAnimation.visibility = View.GONE
            }
        })
}
fun Fragment.startActivity(destinationActivity: Class<*>) {
    val intent = Intent(requireActivity(), destinationActivity)
    startActivity(intent)
}

fun Fragment.toast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Activity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}


fun Activity.getColorFromId(colorId: Int): Int {
    return this.resources.getColor(colorId, null)
}

fun Fragment.getColorFromId(colorId: Int): Int {
    return requireContext().resources.getColor(colorId, null)
}

fun Context.getColorFromId(colorId: Int): Int {
    return this.resources.getColor(colorId, null)
}

fun openAppSettings(activity: Activity) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri: Uri = Uri.fromParts("package", activity.packageName, null)
    intent.data = uri
    activity.startActivity(intent)
}


fun showSnackBar(view: View,str: String){
    val snackbar = Snackbar.make(view, str, Snackbar.LENGTH_SHORT)
    val snackbarView = snackbar.view
    val params = snackbarView.layoutParams as ViewGroup.MarginLayoutParams
    params.bottomMargin = 50 // Adjust this value for desired margin (in pixels)
    snackbarView.layoutParams = params

    snackbar.show()
}

fun Context.copyToClip(text: String){
    val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Auth", text)
    clipboard.setPrimaryClip(clip)
}


fun EditText.getTextFromEdit() : String{
    return  this.text.toString()
}

//Bottom Sheet
fun Fragment.showBottomSheetDialog(txtPrimaryButton: String,onPrimaryClick:() -> Unit) {
    val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
    val binding = BottomSheetDeleteTotpBinding.inflate(LayoutInflater.from(requireContext()))
    bottomSheetDialog.setContentView(binding.root)

    binding.tvTop.text = txtPrimaryButton

    binding.lyTopText.setOnClickListener {
        onPrimaryClick.invoke()
        bottomSheetDialog.dismiss()
    }

    binding.lyCancel.setOnClickListener {
        bottomSheetDialog.dismiss()
    }

    bottomSheetDialog.show()
}

enum class OperationType {
    RESTORE,
    DELETE,
    RESTORE_ALL
}


