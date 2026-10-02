package com.naresh.lungsdemo.viewmodel

import androidx.lifecycle.ViewModel
import com.naresh.lungsdemo.data.repository.TrainerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TrainerViewModel(
    private val repository: TrainerRepository
) : ViewModel() {

    private val _selectedSoundId = MutableStateFlow<String?>(null)
    val selectedSoundId: StateFlow<String?> = _selectedSoundId

    private val _selectedSoundName = MutableStateFlow<String?>(null)
    val selectedSoundName: StateFlow<String?> = _selectedSoundName

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun selectSound(soundId: String, soundName: String) {
        _selectedSoundId.value = soundId
        _selectedSoundName.value = soundName
    }

    fun clearSelectedSound() {
        _selectedSoundId.value = null
        _selectedSoundName.value = null
    }

    fun startLoading() {
        _isLoading.value = true
    }

    fun stopLoading() {
        _isLoading.value = false
    }

    private val _currentStatus = MutableStateFlow<String?>(null)
    val currentStatus: StateFlow<String?> = _currentStatus

    fun setStatus(status: String?) {
        _currentStatus.value = status
    }
}