package com.husnain.authy.ui.fragment.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.husnain.authy.data.ModelUser
import com.husnain.authy.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VmAuth @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {
    val loginState = repository.loginStatus
    val signUpState = repository.signUpStatus
    val logoutState = repository.logoutState

    fun loginWithEmailPass(user: ModelUser) {
        viewModelScope.launch {
            repository.loginWithEmailPass(user)
        }
    }

    fun signUpWithEmailPass(user: ModelUser) {
        viewModelScope.launch {
            repository.signUpWithEmailPass(user)
        }
    }

    fun logout(){
        viewModelScope.launch {
            repository.logout()
        }
    }
}