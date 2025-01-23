package com.husnain.authy.ui.activities

import androidx.lifecycle.ViewModel
import com.husnain.authy.repositories.TotpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VmMain @Inject constructor(
    private val repository: TotpRepository
): ViewModel() {
    val insertStateForSync = repository.insertStateForSync
}