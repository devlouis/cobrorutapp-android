package com.pe.cobrorutapp.probe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pe.cobrorutapp.core.domain.probe.NotificationProbeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationProbeViewModel @Inject constructor(private val repository: NotificationProbeRepository) : ViewModel() {
    val state = repository.state
    fun startCapture() { viewModelScope.launch(Dispatchers.IO) { repository.startSession() } }
    fun stopCapture() { viewModelScope.launch(Dispatchers.IO) { repository.stopSession() } }
    fun clearSamples() { viewModelScope.launch(Dispatchers.IO) { repository.clear() } }
}
