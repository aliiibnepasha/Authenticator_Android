package com.husnain.authy.ui.fragment.main.subscription

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.husnain.authy.utls.admob.AdUtils

class VmSubscription : ViewModel() {
    private val _isAdLoaded = MutableLiveData<Boolean?>(null) // null means loading
    val isAdLoaded: LiveData<Boolean?> = _isAdLoaded

    fun loadAd(activity: Activity) {
        _isAdLoaded.value = null
        AdUtils.loadInterstitialAd(activity) { isAdLoaded ->
            _isAdLoaded.value = isAdLoaded
        }
    }
}