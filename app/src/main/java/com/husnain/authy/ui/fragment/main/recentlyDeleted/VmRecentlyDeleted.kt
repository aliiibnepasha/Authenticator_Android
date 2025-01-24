package com.husnain.authy.ui.fragment.main.recentlyDeleted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.repositories.RecentlyDeletedRepository
import com.husnain.authy.repositories.TotpRepository
import com.husnain.authy.utls.OperationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VmRecentlyDeleted @Inject constructor(
    private val repository: RecentlyDeletedRepository,
    private val totpRepository: TotpRepository
): ViewModel(){
    val fetchState = repository.recentlyDeletedListState
    val restoreState = repository.restoreState

    fun fetchAllTotp() {
        viewModelScope.launch {
            totpRepository.fetchTotpData()
        }
    }

    fun fetchRecentlyDeleted(){
        viewModelScope.launch {
            repository.fetchAllRecentlyDeleted()
        }
    }

    fun restoreOrDelete(data: RecentlyDeleted?,operation: OperationType){
        viewModelScope.launch {
            repository.restoreOrDelete(data,operation)
        }
    }

}