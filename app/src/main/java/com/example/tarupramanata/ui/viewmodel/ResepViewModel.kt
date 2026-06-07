package com.example.tarupramanata.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tarupramanata.data.model.DetailResepResponse
import com.example.tarupramanata.repository.TaruPramanaRepository
import kotlinx.coroutines.launch

class ResepViewModel : ViewModel() {
    private val repository = TaruPramanaRepository()

    private val _detailResep = MutableLiveData<Result<DetailResepResponse>>()
    val detailResep: LiveData<Result<DetailResepResponse>> = _detailResep

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadDetailResep(id: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getDetailResep(id)
            _detailResep.value = result
            _isLoading.value = false
        }
    }
}
