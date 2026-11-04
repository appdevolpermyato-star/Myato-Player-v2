package com.example.ui.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepTimerManager(
    private val onTimerExpired: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow<Long?>(null)
    val remainingSeconds: StateFlow<Long?> = _remainingSeconds.asStateFlow()

    fun setTimer(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return

        val totalSeconds = minutes * 60L
        _remainingSeconds.value = totalSeconds

        timerJob = scope.launch {
            var current = totalSeconds
            while (current > 0) {
                delay(1000)
                current -= 1
                _remainingSeconds.value = current
            }
            _remainingSeconds.value = null
            onTimerExpired()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = null
    }

    fun isTimerActive(): Boolean = _remainingSeconds.value != null
}
