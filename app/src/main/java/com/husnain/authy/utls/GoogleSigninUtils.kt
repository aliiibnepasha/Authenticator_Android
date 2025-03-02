package com.husnain.authy.utls

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
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

    fun doGoogleSignIn(
        context: Context,
        scope: CoroutineScope,
        liveData: MutableLiveData<DataState<Nothing>>,
        onSuccess: (name: String, email: String) -> Unit
    ) {
        liveData.postValue(DataState.Loading())
        val credentialManager = androidx.credentials.CredentialManager.create(context)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(getCredentialOptions(context))
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                when (result.credential) {
                    is CustomCredential -> {
                        if (result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(result.credential.data)
                            val googleTokenId = googleIdTokenCredential.idToken
                            val authCredentials = GoogleAuthProvider.getCredential(googleTokenId, null)

                            val user = Firebase.auth.signInWithCredential(authCredentials).await().user
                            user?.let {
                                Log.d("google user data", "${it.displayName}\n ${it.email}")
                                if (it.displayName != null && it.email != null) {
                                    onSuccess.invoke(it.displayName!!, it.email!!)
                                }
                            }
                        }
                    }

                    else -> {

                    }
                }
            } catch (e: NoCredentialException) {
                liveData.value = DataState.Error("You have no google account setup on device.")
                e.printStackTrace()
            } catch (e: GetCredentialException) {
                Log.e("CredentialException", "General error: ${e.localizedMessage}")
                liveData.value = DataState.Error(e.localizedMessage)
                e.printStackTrace()
            }
        }
    }

    private fun getCredentialOptions(context: Context): CredentialOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId("250275436359-5rc135ddgorgpugjvvkb6cq1osihj0mb.apps.googleusercontent.com")
            .build()
    }
}