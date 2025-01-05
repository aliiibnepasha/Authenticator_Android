package com.husnain.authy.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.husnain.authy.data.room.DaoTotp
import com.husnain.authy.data.room.EntityTotp
import com.husnain.authy.utls.DataState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TotpRepository @Inject constructor(private val daoTotp: DaoTotp) {
    private val _totpListState = MutableLiveData<DataState<List<EntityTotp>>>()
    val totpListState: LiveData<DataState<List<EntityTotp>>> = _totpListState

    private val _insertState = MutableLiveData<DataState<Unit>>()
    val insertState: LiveData<DataState<Unit>> = _insertState

    suspend fun fetchAllTotp() {
        _totpListState.postValue(DataState.Loading())
        withContext(Dispatchers.IO) {
            try {
                val data = daoTotp.getAllTotpData()
                _totpListState.postValue(DataState.Success(data))
            } catch (e: Exception) {
                _totpListState.postValue(DataState.Error(e.message ?: "Failed to fetch data"))
            }
        }
    }

    suspend fun insertTotp(data: EntityTotp) {
        _insertState.postValue(DataState.Loading())
        withContext(Dispatchers.IO) {
            try {
                daoTotp.insertOrReplaceTotpData(data)
                _insertState.postValue(DataState.Success(Unit))
            } catch (e: Exception) {
                _insertState.postValue(DataState.Error(e.message ?: "Failed to insert data"))
            }
        }
    }
}
