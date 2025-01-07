package com.husnain.authy.repositories

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.husnain.authy.data.ModelUser
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.GoogleSigninUtils
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val preferenceManager: PreferenceManager,
) {
    private val _signUpStatus = MutableLiveData<DataState<Nothing>>()
    val signUpStatus: LiveData<DataState<Nothing>> = _signUpStatus

    private val _googleLoginStatus = MutableLiveData<DataState<Nothing>>()
    val googleLoginStatus: LiveData<DataState<Nothing>> = _googleLoginStatus

    private val _loginStatus = MutableLiveData<DataState<Nothing>>()
    val loginStatus: LiveData<DataState<Nothing>> = _loginStatus

    private val _logoutState = MutableLiveData<DataState<Nothing>>()
    val logoutState: LiveData<DataState<Nothing>> = _logoutState

    fun signUpWithEmailPass(user: ModelUser) {
        _signUpStatus.postValue(DataState.Loading())
        auth.createUserWithEmailAndPassword(user.userEmail, user.userPassword)
            .addOnSuccessListener {
                addUserDataToFirestore(user)
            }
            .addOnFailureListener { exception ->
                if (exception is FirebaseAuthUserCollisionException) {
                    _signUpStatus.value = DataState.Error("User already exists.")
                } else {
                    _signUpStatus.value = DataState.Error("Authentication failed.")
                }
            }
    }

    private fun addUserDataToFirestore(user: ModelUser) {
        db.collection("users").document(auth.currentUser?.uid!!).set(user)
            .addOnSuccessListener {
                _signUpStatus.value = DataState.Success()
                preferenceManager.saveUserData(user)
            }
            .addOnFailureListener {
                _signUpStatus.value = DataState.Error(it.message!!)
            }
    }

    //Login flow with email and password
    fun loginWithEmailPass(user: ModelUser) {
        _loginStatus.postValue(DataState.Loading())
        auth.signInWithEmailAndPassword(user.userEmail, user.userPassword)
            .addOnSuccessListener {
                preferenceManager.saveUserData(user)
                _loginStatus.value = DataState.Success()
            }
            .addOnFailureListener { exception ->
                if (exception is FirebaseAuthInvalidCredentialsException) {
                    _loginStatus.value = DataState.Error("Wrong credentials Or not signed up.")
                } else {
                    _loginStatus.value = DataState.Error("Authentication failed.")
                }
            }
    }

    fun logout() {
        _logoutState.postValue(DataState.Loading())
        try {
            auth.signOut()
            _logoutState.postValue(DataState.Success())
        } catch (exception: Exception) {
            _logoutState.postValue(DataState.Error("Logout failed"))
        }
    }

    fun continueWithGoogle(scope: CoroutineScope,context: Activity) {
        GoogleSigninUtils.doGoogleSignIn(context, scope, _googleLoginStatus)
    }
}