package com.husnain.authy.repositories

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoRecentlyDeleted
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.OperationType
import com.husnain.authy.utls.SyncServiceUtil
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RecentlyDeletedRepository @Inject constructor(
    private val daoRecentlyDeleted: DaoRecentlyDeleted,
    private val daoTotp: DaoTotp,
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferenceManager: PreferenceManager
) {
    private val _recentlyDeletedListState = MutableLiveData<DataState<List<RecentlyDeleted>>>()
    val recentlyDeletedListState: LiveData<DataState<List<RecentlyDeleted>>> = _recentlyDeletedListState

    private val _insertState = MutableLiveData<DataState<Unit>>()
    val insertState: LiveData<DataState<Unit>> = _insertState

    private val _restoreState = MutableLiveData<DataState<Unit>>()
    val restoreState: LiveData<DataState<Unit>> = _restoreState

    suspend fun fetchAllRecentlyDeleted() {
        _recentlyDeletedListState.postValue(DataState.Loading())
        try {
            val data = daoRecentlyDeleted.getAllRecentlyDeletedData()
            _recentlyDeletedListState.postValue(DataState.Success(data))
        } catch (e: Exception) {
            _recentlyDeletedListState.postValue(
                DataState.Error(
                    context.getString(R.string.string_something_went_wrong_please_try_again)
                )
            )
        }
    }

    suspend fun insertRecentlyDeleted(data: RecentlyDeleted) {
        _insertState.postValue(DataState.Loading())
        try {
            daoRecentlyDeleted.insertOrReplaceRecentlyDeletedData(data)
            _insertState.postValue(DataState.Success(Unit))
        } catch (e: Exception) {
            e.message?.let { Log.d(Constants.TAG, it) }
            _insertState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

    private suspend fun insertTotp(data: EntityTotp) {
        try {
            daoTotp.insertOrReplaceTotpData(data)
            _restoreState.postValue(DataState.Success(Unit))
        } catch (e: Exception) {
            e.message?.let { Log.d(Constants.TAG, it) }
            _restoreState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

    suspend fun restoreOrDelete(data: RecentlyDeleted?, operation: OperationType) {
        _restoreState.postValue(DataState.Loading())

        try {
            when (operation) {
                OperationType.RESTORE -> {
                    daoRecentlyDeleted.deleteRecentBySecret(data!!.secret)
                    insertTotp(EntityTotp(0, data.name, data.secret))
                    _restoreState.postValue(DataState.Success(Unit))
                    SyncServiceUtil.syncIfUserValidForSyncing(context,auth.currentUser?.uid,preferenceManager)
                }

                OperationType.RESTORE_ALL -> {
                    val allRecentlyDeleted = daoRecentlyDeleted.getAllRecentlyDeletedData()
                    allRecentlyDeleted.forEach { item ->
                        insertTotp(EntityTotp(0, item.name, item.secret))
                    }
                    daoRecentlyDeleted.clearAllRecentlyDeletedTable()
                    _restoreState.postValue(DataState.Success(Unit))
                    SyncServiceUtil.syncIfUserValidForSyncing(context,auth.currentUser?.uid,preferenceManager)
                }

                OperationType.PERMANENTLY_DELETE -> {
                    if (data != null) {
                        deleteDocumentFromFirebase(data)
                    }
                }

                OperationType.DELETE_ALL -> {
                    deleteAllDocumentsFromFirebase()
                }
            }
        } catch (e: Exception) {
            e.message?.let { Log.d(Constants.TAG, it) }
            _restoreState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }


    private suspend fun deleteDocumentFromFirebase(data: RecentlyDeleted) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val collectionRef = firestore.collection("totps")
                    .document(userId)
                    .collection("totps")

                val querySnapshot = collectionRef.get().await()
                var documentDeleted = false

                querySnapshot.documents.firstOrNull { document ->
                    document.getString("secretKey") == data.secret
                }?.let { document ->
                    document.reference.delete().await()
                    documentDeleted = true
                    Log.d(Constants.TAG, "Document deleted from Firestore")
                }

                if (!documentDeleted) {
                    Log.d(Constants.TAG, "Matching document not found in Firestore")
                }

                daoRecentlyDeleted.deleteRecentBySecret(data.secret)
                _restoreState.postValue(DataState.Success(Unit))
            } catch (e: Exception) {
                Log.d(Constants.TAG, e.message ?: "Unknown error")
                _restoreState.postValue(
                    DataState.Error(
                        context.getString(R.string.string_something_went_wrong_please_try_again)
                    )
                )
            }
        } else {
            daoRecentlyDeleted.deleteRecentBySecret(data.secret)
            _restoreState.postValue(DataState.Success())
        }
    }

    private suspend fun deleteAllDocumentsFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val allRecentlyDeleted = daoRecentlyDeleted.getAllRecentlyDeletedData()
                val collectionRef = firestore.collection("totps")
                    .document(userId)
                    .collection("totps")

                val querySnapshot = collectionRef.get().await()

                allRecentlyDeleted.forEach { item ->
                    querySnapshot.documents.firstOrNull { document ->
                        document.getString("secretKey") == item.secret
                    }?.let { document ->
                        document.reference.delete().await()
                    }
                }

                daoRecentlyDeleted.clearAllRecentlyDeletedTable()
                _restoreState.postValue(DataState.Success(Unit))
            } catch (e: Exception) {
                Log.d(Constants.TAG, e.message ?: "Unknown error")
                _restoreState.postValue(
                    DataState.Error(
                        context.getString(R.string.string_something_went_wrong_please_try_again)
                    )
                )
            }
        } else {
            daoRecentlyDeleted.clearAllRecentlyDeletedTable()
            _restoreState.postValue(DataState.Success())
        }
    }
}
