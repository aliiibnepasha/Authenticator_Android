package com.husnain.authy.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.NetworkUtils
import com.husnain.authy.utls.SingleLiveEvent
import com.husnain.authy.utls.SyncServiceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TotpRepository @Inject constructor(
    private val daoTotp: DaoTotp,
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val preferenceManager: PreferenceManager
) {
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
            _totpListState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

    suspend fun insertTotp(data: EntityTotp) {
        _insertState.postValue(DataState.Loading())
        _insertStateForSync.postValue(DataState.Loading())
        try {
            daoTotp.insertOrReplaceTotpData(data)
            _insertState.postValue(DataState.Success(Unit))
            SyncServiceUtil.syncIfUserValidForSyncing(context,auth.currentUser?.uid,preferenceManager)
        } catch (e: Exception) {
            _insertState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
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

        try {
            val task = firestore.collection("totps")
                .document(userId)
                .collection("totps")
                .get()
                .await() // Use the extension function to await Firestore result asynchronously

            val documents: QuerySnapshot? = task
            documents?.documents?.mapNotNull { document ->
                val serviceName = document.getString("serviceName") ?: ""
                val secretKey = document.getString("secretKey") ?: ""
                val docId = document.id // Firestore document ID
                val fireStoreUid = document.getLong("uid")?.toInt() ?: 0

                if (serviceName.isNotEmpty() && secretKey.isNotEmpty()) {
                    val entityTotp = EntityTotp(
                        uid = fireStoreUid,
                        serviceName = serviceName,
                        secretKey = secretKey,
                        docId = docId
                    )

                    withContext(Dispatchers.IO) {
                        insertTotp(entityTotp)
                    }
                }
            }

            fetchTotpDataFromRoomDb()
        } catch (e: Exception) {
            _totpListState.postValue(DataState.Error("Failed to fetch Totp data: ${e.message}"))
        }
    }

    private fun isUserValidToFetchDataFromFirebase(): Boolean {
        return when {
            auth.currentUser?.uid != null &&
                    (preferenceManager.isSubscriptionActive() || preferenceManager.isLifeTimeAccessActive()) -> true

            else -> false
        }
    }

}


