package com.husnain.authy.repositories

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoRecentlyDeleted
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.NetworkUtils
import com.husnain.authy.utls.SingleLiveEvent
import com.husnain.authy.utls.SyncServiceUtil
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TotpRepository @Inject constructor(
    private val daoTotp: DaoTotp,
    private val daoRecentlyDeleted: DaoRecentlyDeleted,
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val preferenceManager: PreferenceManager
) {
    val firebaseAnalytics = Firebase.analytics
    private val _totpListState = MutableLiveData<DataState<List<EntityTotp>>>()
    val totpListState: LiveData<DataState<List<EntityTotp>>> = _totpListState

    private val _insertState = SingleLiveEvent<DataState<Unit>>()
    val insertState: SingleLiveEvent<DataState<Unit>> = _insertState

    private val _insertStateForSync = MutableLiveData<DataState<Unit>>()
    val insertStateForSync: MutableLiveData<DataState<Unit>> = _insertStateForSync

    private val _deleteState = MutableLiveData<DataState<Nothing>>()
    val deleteState: LiveData<DataState<Nothing>> = _deleteState

    private suspend fun fetchTotpDataFromRoomDb() {
        _totpListState.postValue(DataState.Loading())
        try {
            val data = daoTotp.getAllTotpData()
            _totpListState.postValue(DataState.Success(data))
        } catch (e: Exception) {
            e.message?.let { Log.d(Constants.TAG, it) }
            _totpListState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

    suspend fun insertTotp(data: EntityTotp) {
        _insertState.postValue(DataState.Loading())
        _insertStateForSync.postValue(DataState.Loading())
        try {
            daoTotp.insertOrReplaceTotpData(data)
            logTOTPAdded()
            _insertState.postValue(DataState.Success(Unit))
            SyncServiceUtil.syncIfUserValidForSyncing(context,auth.currentUser?.uid,preferenceManager)
        } catch (e: Exception) {
            _insertState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

    private fun logTOTPAdded() {
        val bundle = Bundle().apply {
            putString("totp_status", "success")
        }
        firebaseAnalytics.logEvent("totp_added", bundle)
    }

    suspend fun deleteTotp(secret: String) {
        _deleteState.postValue(DataState.Loading())
        try {
            daoTotp.deleteTotpById(secret)
            _deleteState.postValue(DataState.Success())
        } catch (e: Exception) {
            e.message?.let {
                _deleteState.postValue(DataState.Error(it))
            }
        }
    }

    suspend fun fetchTotpData() {
        if (isUserValidToFetchDataFromFirebase()) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    fetchTotpFromFirestore(userId)
                } else {
                    fetchTotpDataFromRoomDb()
                }
            } else {
                fetchTotpDataFromRoomDb()
            }
        } else {
            fetchTotpDataFromRoomDb()
        }
    }

    private suspend fun fetchTotpFromFirestore(userId: String) {
        _totpListState.postValue(DataState.Loading())
        val totpCollectionName = "totps"
        try {
            val documents = firestore.collection(totpCollectionName)
                .document(userId)
                .collection(totpCollectionName)
                .get()
                .await()

            documents?.documents?.forEach { document ->
                processFirebaseDocument(document)
            }

            fetchTotpDataFromRoomDb()
        } catch (e: Exception) {
            _totpListState.postValue(DataState.Error("Failed to fetch Totp data: ${e.message}"))
        }
    }

    private suspend fun processFirebaseDocument(document: DocumentSnapshot) {
        val serviceName = document.getString("serviceName").orEmpty()
        val secretKey = document.getString("secretKey").orEmpty()
        val fireStoreUid = document.getLong("uid")?.toInt() ?: 0
        val docId = document.id

        if (serviceName.isNotBlank() && secretKey.isNotBlank() && !daoRecentlyDeleted.isRecentlyDeleted(secretKey)) {
            val entityTotp = EntityTotp(
                uid = fireStoreUid,
                serviceName = serviceName,
                secretKey = secretKey,
                docId = docId
            )
            daoTotp.insertOrReplaceTotpData(entityTotp)
        }
    }


    private fun isUserValidToFetchDataFromFirebase(): Boolean {
        return when {
            auth.currentUser?.uid != null &&
                    (preferenceManager.isSubscriptionActive()) -> true

            else -> false
        }
    }

}


