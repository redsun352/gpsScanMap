package com.arkeoscan.phone.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.arkeoscan.core.common.model.AppSettings
import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.common.model.DistanceUnit
import com.arkeoscan.core.common.model.GpsAccuracyMode
import com.arkeoscan.core.common.model.GridResolution
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Ayarlar şu an için in-memory tutulur (Activity/process yaşam döngüsü boyunca
 * geçerlidir). Kalıcılık istenirse Jetpack DataStore (Preferences) ile
 * kolayca genişletilebilir; AppSettings data class'ı zaten bunun için
 * tasarlandı (core-common modülünde, framework-bağımsız).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setGpsAccuracyMode(mode: GpsAccuracyMode) {
        _settings.update { it.copy(gpsAccuracyMode = mode) }
    }

    fun setGridResolution(resolution: GridResolution) {
        _settings.update { it.copy(gridResolution = resolution) }
    }

    fun setColorScale(scale: ColorScale) {
        _settings.update { it.copy(colorScale = scale) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        _settings.update { it.copy(distanceUnit = unit) }
    }
}
