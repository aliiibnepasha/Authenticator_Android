package com.husnain.authy.repositories

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelUser
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.GoogleSigninUtils
import com.husnain.authy.utls.SingleLiveEvent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val context: Context,
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

    private val _deleteAccountStatus = SingleLiveEvent<DataState<Nothing>>()
    val deleteAccountStatus: LiveData<DataState<Nothing>> get() = _deleteAccountStatus


    fun deleteUserAccountAndData() {
        _deleteAccountStatus.postValue(DataState.Loading())
        val user = auth.currentUser
        if (user != null) {
            deleteUserData(user.uid) { isDataDeleted, errorMessage ->
                if (isDataDeleted) {
                    deleteFirebaseAccount(user)
                } else {
                    _deleteAccountStatus.postValue(
                        DataState.Error(
                            context.getString(R.string.string_something_went_wrong_please_try_again)
                        )
                    )
                }
            }
        } else {
            _deleteAccountStatus.postValue(DataState.Error(context.getString(R.string.string_no_user_is_currently_logged_in)))
        }
    }

    private fun deleteUserData(userId: String, callback: (Boolean, String?) -> Unit) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.delete()
            .addOnSuccessListener {
                callback(true, null) // User data deleted successfully
            }
            .addOnFailureListener { exception ->
                callback(false, context.getString(R.string.string_something_went_wrong_please_try_again))
            }
    }

    private fun deleteFirebaseAccount(user: FirebaseUser) {
        user.delete()
            .addOnSuccessListener {
                preferenceManager.saveGuestUser(true)
                _deleteAccountStatus.postValue(DataState.Success())
            }
            .addOnFailureListener { exception ->
                _deleteAccountStatus.postValue(
                    DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again))
                )
            }
    }


    fun signUpWithEmailPass(user: ModelUser) {
        _signUpStatus.postValue(DataState.Loading())
        auth.createUserWithEmailAndPassword(user.userEmail, user.userPassword)
            .addOnSuccessListener {
                addUserDataToFirestore(user)
            }
            .addOnFailureListener { exception ->
                if (exception is FirebaseAuthUserCollisionException) {
                    _signUpStatus.value = DataState.Error(context.getString(R.string.string_user_already_exists))
                } else {
                    _signUpStatus.value = DataState.Error(context.getString(R.string.string_authentication_failed))
                }
            }
    }

    private fun addUserDataToFirestore(user: ModelUser) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDocRef = db.collection("users").document(userId)

            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        preferenceManager.saveGuestUser(false)
                        _googleLoginStatus.value = DataState.Success()
                        _signUpStatus.value = DataState.Success()
                    } else {
                        // Document doesn't exist, so insert the user data
                        userDocRef.set(user)
                            .addOnSuccessListener {
                                _googleLoginStatus.value = DataState.Success()
                                _signUpStatus.value = DataState.Success()
                                preferenceManager.saveUserData(user)
                            }
                            .addOnFailureListener {
                                _signUpStatus.value = DataState.Error(it.message!!)
                                _googleLoginStatus.value = DataState.Error(it.message!!)
                            }
                    }
                }
                .addOnFailureListener {
                    _signUpStatus.value = DataState.Error(it.message!!)
                    _googleLoginStatus.value = DataState.Error(it.message!!)
                }
        }
    }


    //Login flow with email and password
    fun loginWithEmailPass(user: ModelUser) {
        _loginStatus.postValue(DataState.Loading())
        auth.signInWithEmailAndPassword(user.userEmail, user.userPassword)
            .addOnSuccessListener {
                preferenceManager.saveUserData(user)
                preferenceManager.saveGuestUser(true)
                _loginStatus.value = DataState.Success()
            }
            .addOnFailureListener { exception ->
                when (exception) {
                    is FirebaseAuthUserCollisionException -> {
                        _loginStatus.value = DataState.Error(context.getString(R.string.string_user_already_exists_with_same_email))
                    }

                    is FirebaseAuthInvalidCredentialsException -> {
                        _loginStatus.value = DataState.Error(context.getString(R.string.string_wrong_credentials))
                    }

                    else -> {
                        _loginStatus.value = DataState.Error(context.getString(R.string.string_repo_authentication_failed))
                    }
                }
            }
    }

    fun logout() {
        _logoutState.postValue(DataState.Loading())
        try {
            auth.signOut()
            preferenceManager.saveGuestUser(true)
            _logoutState.postValue(DataState.Success())
        } catch (exception: Exception) {
            _logoutState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

    fun continueWithGoogle(scope: CoroutineScope, context: Activity) {
        GoogleSigninUtils.doGoogleSignIn(context, scope, _googleLoginStatus) { name, email ->
            addUserDataToFirestore(
                ModelUser(
                    email,
                    "",
                    name
                )
            )
        }
    }
}