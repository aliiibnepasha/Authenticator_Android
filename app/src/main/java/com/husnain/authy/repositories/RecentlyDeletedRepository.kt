package com.husnain.authy.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.PrimaryKey
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoRecentlyDeleted
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.OperationType
import com.husnain.authy.utls.SingleLiveEvent
import javax.inject.Inject

class RecentlyDeletedRepository @Inject constructor(
    private val daoRecentlyDeleted: DaoRecentlyDeleted,
    private val daoTotp: DaoTotp,
    private val context: Context
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
            _insertState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

    suspend fun restoreOrDelete(data: RecentlyDeleted?, operation: OperationType) {
        _restoreState.postValue(DataState.Loading())

        try {
            when (operation) {
                OperationType.RESTORE -> {
                    // Restore the individual item
                    daoRecentlyDeleted.deleteRecentBySecret(data!!.secret)
                    insertTotp(EntityTotp(0, data.name, data.secret))
                }

                OperationType.DELETE -> {
                    // Delete the individual item
                    daoRecentlyDeleted.deleteRecentBySecret(data!!.secret)
                    _restoreState.postValue(DataState.Success(Unit))
                }

                OperationType.RESTORE_ALL -> {
                    // Fetch all recently deleted items and restore them
                    val allRecentlyDeleted = daoRecentlyDeleted.getAllRecentlyDeletedData()
                    allRecentlyDeleted.forEach { item ->
                        insertTotp(EntityTotp(0, item.name, item.secret))
                    }
                    daoRecentlyDeleted.clearAllRecentlyDeletedTable()
                    _restoreState.postValue(DataState.Success(Unit))
                }

                OperationType.DELETE_ALL -> {
                    daoRecentlyDeleted.clearAllRecentlyDeletedTable()
                    _restoreState.postValue(DataState.Success(Unit))
                }
            }
        } catch (e: Exception) {
            _restoreState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

    private suspend fun insertTotp(data: EntityTotp) {
        try {
            daoTotp.insertOrReplaceTotpData(data)
            _restoreState.postValue(DataState.Success(Unit))
        } catch (e: Exception) {
            _restoreState.postValue(DataState.Error(context.getString(R.string.string_something_went_wrong_please_try_again)))
        }
    }

}
