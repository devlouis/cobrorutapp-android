package com.pe.cobrorutapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pe.cobrorutapp.core.domain.preferences.ThemeMode
import com.pe.cobrorutapp.core.domain.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val preferencesReady: Boolean = false,
    val isSaving: Boolean = false,
    val preferencesError: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.themeMode.catch { error ->
                if (error !is IOException) throw error
                _uiState.update { it.copy(preferencesError = true) }
            }.collect { mode ->
                _uiState.update { it.copy(themeMode = mode, preferencesReady = true) }
            }
        }
    }

    fun cycleTheme() {
        val state = _uiState.value
        if (!state.preferencesReady || state.isSaving) return
        val next = when (state.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        _uiState.update { it.copy(isSaving = true, preferencesError = false) }
        viewModelScope.launch {
            try {
                preferences.setThemeMode(next)
            } catch (_: IOException) {
                _uiState.update { it.copy(preferencesError = true) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
