package com.husnain.authy.ui.fragment.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.repositories.TotpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VmSettings @Inject constructor(
    private val repository: TotpRepository,
): ViewModel(){
    val insertState = repository.insertState

    fun insertSecretData(data: EntityTotp) {
        viewModelScope.launch {
            repository.insertTotp(data)
        }
    }
}