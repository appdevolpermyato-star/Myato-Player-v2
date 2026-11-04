package com.example.ui.music

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerBandInfo(
    val index: Short,
    val centerFreq: Int, // in milliHertz (e.g. 60000 = 60Hz)
    val minLevel: Short, // in milliBels (e.g. -1500 = -15dB)
    val maxLevel: Short, // in milliBels (e.g. 1500 = +15dB)
    val currentLevel: Short
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bassBoost: Int = 400, // 0 - 1000
    val virtualizer: Int = 300, // 0 - 1000
    val currentPreset: String = "Pop",
    val presets: List<String> = listOf("Rock", "Pop", "Jazz", "Clássico", "Eletrônica", "Hip-Hop", "Flat", "Personalizado"),
    val bands: List<EqualizerBandInfo> = emptyList()
)

class AudioEffectManager {

    companion object {
        private const val TAG = "AudioEffectManager"
        val instance by lazy { AudioEffectManager() }
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun initEffects(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            release()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoost.toShort())
                }
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.virtualizer.toShort())
                }
            }

            refreshBandsFromHardware()
            applyCurrentPreset(_state.value.currentPreset)
            Log.d(TAG, "Audio effects initialized successfully for session: $audioSessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize hardware audio effects: ${e.message}")
            // Create fallback virtual bands for UI if emulator lacks hardware DSP
            setupFallbackBands()
        }
    }

    private fun setupFallbackBands() {
        val fallbackFrequencies = listOf(60000, 230000, 910000, 3600000, 14000000)
        val bands = fallbackFrequencies.mapIndexed { index, freq ->
            EqualizerBandInfo(
                index = index.toShort(),
                centerFreq = freq,
                minLevel = -1500,
                maxLevel = 1500,
                currentLevel = 0
            )
        }
        _state.value = _state.value.copy(bands = bands)
    }

    private fun refreshBandsFromHardware() {
        val eq = equalizer ?: run {
            setupFallbackBands()
            return
        }
        val numBands = eq.numberOfBands
        val range = eq.bandLevelRange
        val minLevel: Short = if (range != null && range.isNotEmpty()) range[0] else (-1500).toShort()
        val maxLevel: Short = if (range != null && range.size > 1) range[1] else 1500.toShort()

        val bandList = mutableListOf<EqualizerBandInfo>()
        for (i in 0 until numBands) {
            val band = i.toShort()
            val centerFreq = eq.getCenterFreq(band)
            val currentLevel = eq.getBandLevel(band)
            bandList.add(
                EqualizerBandInfo(
                    index = band,
                    centerFreq = centerFreq,
                    minLevel = minLevel,
                    maxLevel = maxLevel,
                    currentLevel = currentLevel
                )
            )
        }
        _state.value = _state.value.copy(bands = bandList)
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle effects: ${e.message}")
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(bassBoost = clamped)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set bass boost: ${e.message}")
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(virtualizer = clamped)
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set virtualizer: ${e.message}")
        }
    }

    fun setBandLevel(bandIndex: Short, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, level)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set band level on hardware: ${e.message}")
        }
        val updatedBands = _state.value.bands.map {
            if (it.index == bandIndex) it.copy(currentLevel = level) else it
        }
        _state.value = _state.value.copy(
            bands = updatedBands,
            currentPreset = "Personalizado"
        )
    }

    fun setPreset(presetName: String) {
        _state.value = _state.value.copy(currentPreset = presetName)
        applyCurrentPreset(presetName)
    }

    private fun applyCurrentPreset(presetName: String) {
        // Preset profile multipliers from -1500 to +1500 mB
        val multipliers = when (presetName) {
            "Rock" -> listOf(600, 300, -200, 400, 700)
            "Pop" -> listOf(-200, 300, 600, 300, -200)
            "Jazz" -> listOf(400, 200, -100, 200, 500)
            "Clássico" -> listOf(500, 300, -200, 300, 400)
            "Eletrônica" -> listOf(800, 500, -100, 400, 800)
            "Hip-Hop" -> listOf(900, 600, 0, 200, 600)
            "Flat" -> listOf(0, 0, 0, 0, 0)
            else -> return // For "Personalizado", preserve manual levels
        }

        val updatedBands = _state.value.bands.mapIndexed { index, band ->
            val targetLevel = multipliers.getOrNull(index) ?: 0
            val clamped = targetLevel.coerceIn(band.minLevel.toInt(), band.maxLevel.toInt()).toShort()
            try {
                equalizer?.setBandLevel(band.index, clamped)
            } catch (_: Exception) {}
            band.copy(currentLevel = clamped)
        }

        _state.value = _state.value.copy(bands = updatedBands)
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
