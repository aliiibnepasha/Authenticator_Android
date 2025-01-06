package com.husnain.authy.utls

import android.content.Context
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object GoogleSigninUtils {
    private val _signUpStatusWithGoogle = MutableLiveData<DataState<Nothing>>()
    val signUpStatusWithGoogle: LiveData<DataState<Nothing>> = _signUpStatusWithGoogle

    fun doGoogleSingin(
        context: Context,
        scope: CoroutineScope,
    ) {
        _signUpStatusWithGoogle.postValue(DataState.Loading())
        val credentialManager = androidx.credentials.CredentialManager.create(context)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(getCredentialOptions())
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context,request )
                when(result.credential){
                    is CustomCredential ->{
                        if (result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                            val googleTokenId = googleIdTokenCredential.idToken
                            val authCredentials = GoogleAuthProvider.getCredential(googleTokenId,null)

                            val user = Firebase.auth.signInWithCredential(authCredentials).await().user
                            user?.let {
                                    if (it.isAnonymous.not()){
                                        _signUpStatusWithGoogle.value = DataState.Success()
                                    }
                            }
                        }
                    }
                    else ->{

                    }
                }
            } catch (e: NoCredentialException) {
                _signUpStatusWithGoogle.value = DataState.Error("You have no google account setup on device.")
                e.printStackTrace()
            } catch (e: GetCredentialException) {
                _signUpStatusWithGoogle.value = DataState.Error(e.localizedMessage)
                e.printStackTrace()
            }
        }
    }

    private fun getCredentialOptions(): CredentialOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId("159368296018-1puim8g04qetrqk8dhng1q6vdb84u3a0.apps.googleusercontent.com")
            .build()
    }
}