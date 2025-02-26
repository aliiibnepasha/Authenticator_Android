package com.husnain.authy.utls

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.biometric.BiometricManager
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.husnain.authy.R

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

@SuppressLint("ClickableViewAccessibility")
fun Fragment.setupKeyboardDismissListener(view: View) {
    view.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            hideKeyboard()
        }
        false
    }
}

fun Fragment.hideKeyboard() {
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val currentFocus = activity?.currentFocus
    if (currentFocus != null) {
        imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }
}

fun Fragment.isBiometricSupported(): Boolean {
    val biometricManager = BiometricManager.from(requireContext())
    return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS -> true
        else -> false
    }
}

//enums

enum class OperationType {
    RESTORE,
    PERMANENTLY_DELETE,
    RESTORE_ALL,
    DELETE_ALL
}


enum class DelayOption {
    IMMEDIATELY,
    AFTER_15S,
    AFTER_30S,
    AFTER_50S,
    AFTER_1M;

    // Method to get the display text from resources
    fun getDisplayText(context: Context): String {
        return when (this) {
            IMMEDIATELY -> context.getString(R.string.delay_immediately)
            AFTER_15S -> context.getString(R.string.delay_after_15s)
            AFTER_30S -> context.getString(R.string.delay_after_30s)
            AFTER_50S -> context.getString(R.string.delay_after_50s)
            AFTER_1M -> context.getString(R.string.delay_after_1m)
        }
    }
}


