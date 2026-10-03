package com.naresh.lungsdemo.viewmodel

import androidx.lifecycle.ViewModel
import com.naresh.lungsdemo.data.repository.TrainerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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

    fun playSound(soundId: String, soundName: String) {
        viewModelScope.launch {
            startLoading()

            try {
                val response = repository.playSound(soundId)

                if (response.isSuccessful) {
                    selectSound(soundId, soundName)
                    setStatus("Playing $soundName")
                } else {
                    setStatus("Failed to play $soundName")
                }
            } catch (e: Exception) {
                setStatus("Error: ${e.message}")
            } finally {
                stopLoading()
            }
        }
    }

    fun stopSound() {
        viewModelScope.launch {
            startLoading()

            try {
                val response = repository.stopSound()

                if (response.isSuccessful) {
                    clearSelectedSound()
                    setStatus("Sound stopped")
                } else {
                    setStatus("Failed to stop sound")
                }
            } catch (e: Exception) {
                setStatus("Error: ${e.message}")
            } finally {
                stopLoading()
            }
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            startLoading()

            try {
                val response = repository.setVolume(volume)

                if (response.isSuccessful) {
                    setStatus(
                        "Master volume set to ${(volume * 100).toInt()}%"
                    )
                } else {
                    setStatus("Failed to set volume")
                }
            } catch (e: Exception) {
                setStatus("Error: ${e.message}")
            } finally {
                stopLoading()
            }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            startLoading()

            try {
                val response = repository.health()

                if (response.isSuccessful) {
                    setStatus(
                        response.body()?.message
                            ?: "Raspberry Pi Connected"
                    )
                } else {
                    setStatus(
                        "Pi responded with error: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                setStatus(
                    "Cannot connect to Raspberry Pi: ${e.message}"
                )
            } finally {
                stopLoading()
            }
        }
    }
}