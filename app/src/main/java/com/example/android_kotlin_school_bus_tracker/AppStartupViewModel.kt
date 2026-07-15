package com.example.android_kotlin_school_bus_tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_kotlin_school_bus_tracker.data.AuthRepository
import com.example.android_kotlin_school_bus_tracker.data.BusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StartupState {
    object Loading : StartupState()
    data class Ready(val busId: String) : StartupState()
    data class Error(val message: String) : StartupState()
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val busRepository: BusRepository
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            _state.value = StartupState.Loading
            try {
                val busId = authRepository.getOrCreateUid()
                busRepository.ensureBusExists(busId)
                _state.value = StartupState.Ready(busId)
            } catch (e: Exception) {
                _state.value = StartupState.Error(e.message ?: "Startup failed")
            }
        }
    }
}