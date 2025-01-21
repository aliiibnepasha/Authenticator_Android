package com.husnain.authy.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.SingleLiveEvent
import javax.inject.Inject

class TotpRepository @Inject constructor(private val daoTotp: DaoTotp,private val context: Context) {
    private val _totpListState = MutableLiveData<DataState<List<EntityTotp>>>()
    val totpListState: LiveData<DataState<List<EntityTotp>>> = _totpListState

    private val _insertState = MutableLiveData<DataState<Unit>>()
    val insertState: LiveData<DataState<Unit>> = _insertState

    private val _deleteState = MutableLiveData<DataState<Nothing>>()
    val deleteState: LiveData<DataState<Nothing>> = _deleteState

    suspend fun fetchAllTotp() {
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
        try {
            daoTotp.insertOrReplaceTotpData(data)
            _insertState.postValue(DataState.Success(Unit))
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
}


