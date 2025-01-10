package com.husnain.authy.ui.fragment.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.repositories.TotpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VmHome @Inject constructor(
    private val repository: TotpRepository
): ViewModel(){
    val totpListState = repository.totpListState
    val insertState = repository.insertState

    init {
        fetchAllTotp()
    }

    private fun fetchAllTotp() {
        viewModelScope.launch {
            repository.fetchAllTotp()
        }
    }

    fun insertSecretData(data: EntityTotp) {
        viewModelScope.launch {
            repository.insertTotp(data)
        }
    }

}