package com.husnain.authy.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.utls.DataState
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SyncRepository @Inject constructor(
    private val daoTotp: DaoTotp,
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    private val _syncState = MutableLiveData<DataState<Unit>>()
    val syncState: LiveData<DataState<Unit>> = _syncState

    suspend fun syncTotpData(userId: String) {
        _syncState.postValue(DataState.Loading())
        try {
            val totpList = daoTotp.getAllTotpData()

            if (totpList.isEmpty()) {
                _syncState.postValue(DataState.Error(context.getString(R.string.no_totp_data_found_to_sync)))
                return
            }

            val totpsCollectionRef = firestore.collection("totps").document(userId).collection("totps")

            totpList.forEach { totp ->
                val totpData = hashMapOf(
                    "uid" to totp.uid,
                    "serviceName" to totp.serviceName,
                    "secretKey" to totp.secretKey
                )
                totpsCollectionRef.add(totpData).await()
            }

            _syncState.postValue(DataState.Success(Unit))
        } catch (e: Exception) {
            _syncState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

}
