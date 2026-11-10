package com.techressive.hex.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin
import kotlin.random.Random

object SpectrumAnalyzer {
    private val _bands = MutableStateFlow(FloatArray(32))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    private var isPlaying = false
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    operator fun invoke(scope: CoroutineScope? = null): SpectrumAnalyzer = this

    fun startSimulation() = updatePlaybackState(true)
    fun stopSimulation() = updatePlaybackState(false)

    fun updatePlaybackState(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            startLoop()
        } else {
            stopLoop()
        }
    }

    private fun startLoop() {
        if (job != null && job?.isActive == true) return
        job = scope.launch {
            var phase = 0f
            while (isActive && isPlaying) {
                val newBands = FloatArray(32)
                for (i in 0 until 32) {
                    val base = (sin(phase + i * 0.4f) + 1f) * 0.4f
                    val noise = Random.nextFloat() * 0.2f
                    newBands[i] = (base + noise).coerceIn(0.05f, 0.95f)
                }
                _bands.value = newBands
                phase += 0.25f
                // 30 fps (~33ms) throttled loop for battery optimization
                delay(33L)
            }
        }
    }

    private fun stopLoop() {
        job?.cancel()
        job = null
        // Zero out visualizer bins immediately when paused
        _bands.value = FloatArray(32)
    }
}
