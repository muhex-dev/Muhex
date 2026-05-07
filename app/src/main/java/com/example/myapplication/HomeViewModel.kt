package com.example.myapplication

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.getInstance(application)
    private val prefs = application.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    var allAppsList by mutableStateOf(emptyList<AppModel>())
    var recentAppsList by mutableStateOf(emptyList<AppModel>())
    var pinnedAppsList by mutableStateOf(emptyList<AppModel>())
    var dockAppsList by mutableStateOf(emptyList<AppModel>())

    // UI State
    var isHomePopupVisible by mutableStateOf(false)
    var isUnifiedSettingsVisible by mutableStateOf(false)
    var unifiedSettingsInitialTab by mutableStateOf(0)
    var isClockSettingsVisible by mutableStateOf(false)
    var isMuhexSettingsVisible by mutableStateOf(false)
    var isQuickMenuVisible by mutableStateOf(false)
    var isFontSettingsVisible by mutableStateOf(false)
    var isPermissionCheckerVisible by mutableStateOf(true)

    var fontPickerTargetKey by mutableStateOf("clock_font_family")
    var fontPickerTitle by mutableStateOf("Clock Font")

    init {
        viewModelScope.launch {
            repository.appsChangedFlow.collectLatest {
                refreshData()
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            allAppsList = repository.getAllApps()
            loadPinnedApps()
            loadDockApps()
        }
    }

    fun loadPinnedApps() {
        viewModelScope.launch {
            pinnedAppsList = repository.getPinnedApps()
        }
    }

    fun loadDockApps() {
        viewModelScope.launch {
            dockAppsList = repository.getDockApps()
        }
    }

    fun updateRecentApps() {
        viewModelScope.launch {
            recentAppsList = repository.getRecentApps()
        }
    }

    fun dismissComposeOverlays() {
        isHomePopupVisible = false
        isUnifiedSettingsVisible = false
        isClockSettingsVisible = false
        isQuickMenuVisible = false
        isFontSettingsVisible = false
    }
}
