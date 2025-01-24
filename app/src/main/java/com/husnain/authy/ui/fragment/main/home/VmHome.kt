package com.husnain.authy.ui.fragment.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.repositories.RecentlyDeletedRepository
import com.husnain.authy.repositories.TotpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VmHome @Inject constructor(
    private val repository: TotpRepository,
    private val recentlyDeletedRepository: RecentlyDeletedRepository
): ViewModel(){
    val totpListState = repository.totpListState
    val deleteState = repository.deleteState
    val insertRecentlyDeletedState = recentlyDeletedRepository.insertState
    var isNavigationTriggered: Boolean = false

    init {
        fetchAllTotp()
    }

    fun fetchAllTotp() {
        viewModelScope.launch {
            repository.fetchTotpData()
        }
    }

    fun deleteTotp(secret: String) {
        viewModelScope.launch {
            repository.deleteTotp(secret)
        }
    }

    fun insertToRecentlyDeleted(data: RecentlyDeleted){
        viewModelScope.launch {
            recentlyDeletedRepository.insertRecentlyDeleted(data)
        }
    }

}