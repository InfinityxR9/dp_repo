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
}