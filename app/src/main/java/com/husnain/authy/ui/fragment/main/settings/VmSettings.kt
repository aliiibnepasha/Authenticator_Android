package com.husnain.authy.ui.fragment.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.repositories.AuthRepository
import com.husnain.authy.repositories.TotpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VmSettings @Inject constructor(
    private val totpRepository: TotpRepository,
    private val authRepository: AuthRepository
): ViewModel(){
    val insertState = totpRepository.insertState
    val logoutState = authRepository.logoutState
    val deleteAccountState = authRepository.deleteAccountStatus

    fun logout(){
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun deleteAccount(){
        viewModelScope.launch {
            authRepository.deleteUserAccountAndData()
        }
    }

}