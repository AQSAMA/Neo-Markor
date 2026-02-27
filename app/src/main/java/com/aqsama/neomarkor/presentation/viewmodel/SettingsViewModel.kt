package com.aqsama.neomarkor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqsama.neomarkor.data.local.StoragePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val storagePreferences: StoragePreferences,
) : ViewModel() {

    /** 0 = System, 1 = Light, 2 = Dark */
    val themeMode: StateFlow<Int> = storagePreferences.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val accentColor: StateFlow<Int> = storagePreferences.observeAccentColor()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val cornerRadius: StateFlow<Float> = storagePreferences.observeCornerRadius()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 12f)

    val dynamicColor: StateFlow<Boolean> = storagePreferences.observeDynamicColor()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { storagePreferences.setThemeMode(mode) }
    }

    fun setAccentColor(color: Int) {
        viewModelScope.launch { storagePreferences.setAccentColor(color) }
    }

    fun setCornerRadius(radius: Float) {
        viewModelScope.launch { storagePreferences.setCornerRadius(radius) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { storagePreferences.setDynamicColor(enabled) }
    }
}
