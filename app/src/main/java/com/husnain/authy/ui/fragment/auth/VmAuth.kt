package com.husnain.authy.ui.fragment.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.husnain.authy.data.models.ModelUser
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
    val googleLoginState = repository.googleLoginStatus
    val deleteAccountState = repository.deleteAccountStatus
    var isNavigationTriggered: Boolean = false

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

    fun continueWithGoogle(context: Activity){
        repository.continueWithGoogle(viewModelScope,context)
    }


    fun deleteAccount(){
        viewModelScope.launch {
            repository.deleteUserAccountAndData()
        }
    }
}