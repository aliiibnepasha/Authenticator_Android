package com.husnain.authy.utls

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
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





