package com.husnain.authy.ui.activities

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.husnain.authy.repositories.TotpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VmMain @Inject constructor(
): ViewModel() {
    val isSubscriptionVisible = MutableLiveData(false)

    fun setSubscriptionVisible(isVisible: Boolean) {
        isSubscriptionVisible.value = isVisible
    }
}