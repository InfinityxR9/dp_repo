package com.naresh.lungsdemo.viewmodel

import com.naresh.lungsdemo.model.TrainerMode
import com.naresh.lungsdemo.model.LungSound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.naresh.lungsdemo.data.repository.TrainerRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class TrainerViewModel(
    private val repository: TrainerRepository
) : ViewModel() {

    private val _selectedSound = MutableStateFlow<LungSound?>(null)
    val selectedSound: StateFlow<LungSound?> = _selectedSound.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()


    private val _currentStatus = MutableStateFlow<String?>(null)
    val currentStatus: StateFlow<String?> =
        _currentStatus.asStateFlow()


    // null = not checked yet
    // true = connected
    // false = not connected
    private val _isPiConnected = MutableStateFlow<Boolean?>(null)
    val isPiConnected: StateFlow<Boolean?> =
        _isPiConnected.asStateFlow()

    private val _trainerMode = MutableStateFlow(TrainerMode.TRAINING)
    val trainerMode: StateFlow<TrainerMode> = _trainerMode.asStateFlow()

    fun setTrainerMode(mode: TrainerMode) {
        _trainerMode.value = mode
    }

    private fun selectSound(soundId: String, soundName: String) {
        _selectedSound.value = LungSound(
            id = soundId,
            name = soundName
        )
    }

    private fun clearSelectedSound() {
        _selectedSound.value = null
    }

    private fun startLoading() {
        _isLoading.value = true
    }

    private fun stopLoading() {
        _isLoading.value = false
    }


    private fun setStatus(status: String?) {
        _currentStatus.value = status
    }


    fun playSound(
        soundId: String,
        soundName: String
    ) {
        viewModelScope.launch {

            startLoading()

            try {

                val response =
                    repository.playSound(soundId)

                if (response.isSuccessful) {

                    selectSound(
                        soundId,
                        soundName
                    )

                    setStatus(
                        "Playing $soundName"
                    )

                } else {

                    setStatus(
                        "Failed to play $soundName"
                    )
                }

            } catch (e: Exception) {

                setStatus(
                    "Error: ${e.message}"
                )

            } finally {

                stopLoading()
            }
        }
    }


    fun stopSound() {

        viewModelScope.launch {

            startLoading()

            try {

                val response =
                    repository.stopSound()

                if (response.isSuccessful) {

                    clearSelectedSound()

                    setStatus(
                        "Sound stopped"
                    )

                } else {

                    setStatus(
                        "Failed to stop sound"
                    )
                }

            } catch (e: Exception) {

                setStatus(
                    "Error: ${e.message}"
                )

            } finally {

                stopLoading()
            }
        }
    }


    fun setVolume(
        volume: Float
    ) {

        viewModelScope.launch {

            startLoading()

            try {

                val response =
                    repository.setVolume(volume)

                if (response.isSuccessful) {

                    setStatus(
                        "Master volume set to ${(volume * 100).toInt()}%"
                    )

                } else {

                    setStatus(
                        "Failed to set volume"
                    )
                }

            } catch (e: Exception) {

                setStatus(
                    "Error: ${e.message}"
                )

            } finally {

                stopLoading()
            }
        }
    }


    fun checkConnection() {

        viewModelScope.launch {

            startLoading()

            try {

                val response =
                    repository.health()

                if (response.isSuccessful) {

                    _isPiConnected.value = true

                    setStatus(
                        response.body()?.message
                            ?: "Raspberry Pi Connected"
                    )

                } else {

                    _isPiConnected.value = false

                    setStatus(
                        "Pi responded with error: ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                _isPiConnected.value = false

                setStatus(
                    "Cannot connect to Raspberry Pi: ${e.message}"
                )

            } finally {

                stopLoading()
            }
        }
    }
}