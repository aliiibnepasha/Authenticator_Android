package com.husnain.authy.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.husnain.authy.data.room.daos.DaoRecentlyDeleted
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.utls.DataState
import javax.inject.Inject

class RecentlyDeletedRepository @Inject constructor(
    private val daoRecentlyDeleted: DaoRecentlyDeleted
) {
    private val _recentlyDeletedListState = MutableLiveData<DataState<List<RecentlyDeleted>>>()
    val recentlyDeletedListState: LiveData<DataState<List<RecentlyDeleted>>> =
        _recentlyDeletedListState

    private val _insertState = MutableLiveData<DataState<Unit>>()
    val insertState: LiveData<DataState<Unit>> = _insertState

    suspend fun fetchAllRecentlyDeleted() {
        _recentlyDeletedListState.postValue(DataState.Loading())
        try {
            val data = daoRecentlyDeleted.getAllRecentlyDeletedData()
            _recentlyDeletedListState.postValue(DataState.Success(data))
        } catch (e: Exception) {
            _recentlyDeletedListState.postValue(
                DataState.Error(
                    e.message ?: "Failed to fetch data"
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
            _insertState.postValue(DataState.Error(e.message ?: "Failed to insert data"))
        }
    }
}
