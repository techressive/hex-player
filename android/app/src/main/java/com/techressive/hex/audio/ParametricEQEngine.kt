package com.techressive.hex.audio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class EQMode {
    SIMPLE,
    EXPERT
}

data class EQPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "Custom user preset",
    val bands: List<EQBandConfig>,
    val isCustom: Boolean = false
)

class ParametricEQEngine(context: Context) {
    companion object {
        @Volatile
        private var instance: ParametricEQEngine? = null

        fun getInstance(context: Context): ParametricEQEngine {
            return instance ?: synchronized(this) {
                instance ?: ParametricEQEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("HEX_EQ_PREFS", Context.MODE_PRIVATE)

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _mode = MutableStateFlow(EQMode.SIMPLE)
    val mode: StateFlow<EQMode> = _mode.asStateFlow()

    private val _activePresetName = MutableStateFlow("Flat")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    private val _simpleBands = MutableStateFlow<List<EQBandConfig>>(emptyList())
    val simpleBands: StateFlow<List<EQBandConfig>> = _simpleBands.asStateFlow()

    private val _expertBands = MutableStateFlow<List<EQBandConfig>>(emptyList())
    val expertBands: StateFlow<List<EQBandConfig>> = _expertBands.asStateFlow()

    private val _bands = MutableStateFlow<List<EQBandConfig>>(emptyList())
    val bands: StateFlow<List<EQBandConfig>> = _bands.asStateFlow()

    private val _userPresets = MutableStateFlow<List<EQPreset>>(emptyList())
    val userPresets: StateFlow<List<EQPreset>> = _userPresets.asStateFlow()

    private val _presets = MutableStateFlow<List<EQPreset>>(emptyList())
    val presets: StateFlow<List<EQPreset>> = _presets.asStateFlow()

    init {
        setupDefaultBands()
        loadUserPresets()
        updateAllPresetsList()
        syncActiveBands()
    }

    private fun syncActiveBands() {
        _bands.value = if (_mode.value == EQMode.SIMPLE) _simpleBands.value else _expertBands.value
    }

    private fun updateAllPresetsList() {
        _presets.value = factoryPresets + _userPresets.value
    }

    private fun setupDefaultBands() {
        _simpleBands.value = listOf(
            EQBandConfig(name = "32 Hz", type = FilterType.LOW_SHELF, frequency = 32.0, gainDb = 0.0, q = 0.7),
            EQBandConfig(name = "64 Hz", type = FilterType.PEAKING, frequency = 64.0, gainDb = 0.0, q = 1.0),
            EQBandConfig(name = "250 Hz", type = FilterType.PEAKING, frequency = 250.0, gainDb = 0.0, q = 1.0),
            EQBandConfig(name = "1 kHz", type = FilterType.PEAKING, frequency = 1000.0, gainDb = 0.0, q = 1.0),
            EQBandConfig(name = "4 kHz", type = FilterType.PEAKING, frequency = 4000.0, gainDb = 0.0, q = 1.0),
            EQBandConfig(name = "8 kHz", type = FilterType.PEAKING, frequency = 8000.0, gainDb = 0.0, q = 1.0),
            EQBandConfig(name = "16 kHz", type = FilterType.HIGH_SHELF, frequency = 16000.0, gainDb = 0.0, q = 0.7)
        )

        _expertBands.value = listOf(
            EQBandConfig(name = "Sub Cut", type = FilterType.HIGH_PASS, frequency = 20.0, gainDb = 0.0, q = 0.71),
            EQBandConfig(name = "Low Shelf", type = FilterType.LOW_SHELF, frequency = 80.0, gainDb = 0.0, q = 0.71),
            EQBandConfig(name = "Bass", type = FilterType.PEAKING, frequency = 160.0, gainDb = 0.0, q = 1.2),
            EQBandConfig(name = "Low Mid", type = FilterType.PEAKING, frequency = 400.0, gainDb = 0.0, q = 1.4),
            EQBandConfig(name = "Mid", type = FilterType.PEAKING, frequency = 1000.0, gainDb = 0.0, q = 1.4),
            EQBandConfig(name = "Upper Mid", type = FilterType.PEAKING, frequency = 2500.0, gainDb = 0.0, q = 1.5),
            EQBandConfig(name = "Presence", type = FilterType.PEAKING, frequency = 5000.0, gainDb = 0.0, q = 1.5),
            EQBandConfig(name = "Brilliance", type = FilterType.PEAKING, frequency = 8500.0, gainDb = 0.0, q = 1.8),
            EQBandConfig(name = "High Shelf", type = FilterType.HIGH_SHELF, frequency = 12000.0, gainDb = 0.0, q = 0.71),
            EQBandConfig(name = "Air Cut", type = FilterType.LOW_PASS, frequency = 20000.0, gainDb = 0.0, q = 0.71)
        )
    }

    fun toggleEnabled() {
        _isEnabled.value = !_isEnabled.value
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }

    fun setMode(newMode: EQMode) {
        _mode.value = newMode
        syncActiveBands()
    }

    fun setSimpleBandGain(index: Int, gainDb: Double) {
        val current = _simpleBands.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(gainDb = gainDb.coerceIn(-12.0, 12.0))
            _simpleBands.value = current
            if (_mode.value == EQMode.SIMPLE) syncActiveBands()
        }
    }

    fun setBandGain(index: Int, gainDb: Double) {
        val clamped = gainDb.coerceIn(-12.0, 12.0)
        if (_mode.value == EQMode.SIMPLE) {
            val list = _simpleBands.value.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(gainDb = clamped)
                _simpleBands.value = list
            }
        } else {
            val list = _expertBands.value.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(gainDb = clamped)
                _expertBands.value = list
            }
        }
        syncActiveBands()
    }

    fun setBandGain(index: Int, gainDb: Float) = setBandGain(index, gainDb.toDouble())

    fun setBandFrequency(index: Int, freq: Double) {
        val clamped = freq.coerceIn(20.0, 20000.0)
        if (_mode.value == EQMode.EXPERT) {
            val list = _expertBands.value.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(frequency = clamped)
                _expertBands.value = list
                syncActiveBands()
            }
        }
    }

    fun setBandQ(index: Int, q: Double) {
        val clamped = q.coerceIn(0.1, 10.0)
        if (_mode.value == EQMode.EXPERT) {
            val list = _expertBands.value.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(q = clamped)
                _expertBands.value = list
                syncActiveBands()
            }
        }
    }

    fun resetToFlat() {
        _simpleBands.value = _simpleBands.value.map { it.copy(gainDb = 0.0) }
        _expertBands.value = _expertBands.value.map { it.copy(gainDb = 0.0) }
    }

    val factoryPresets: List<EQPreset>
        get() = listOf(
            EQPreset(
                id = "flat",
                name = "Audiophile Flat / Bypass",
                description = "Bit-perfect uncolored frequency response straight from master audio.",
                bands = _simpleBands.value.map { it.copy(gainDb = 0.0) }
            ),
            EQPreset(
                id = "harman",
                name = "Harman Target Curve",
                description = "Controlled sub-bass warmth and vocal clarity.",
                bands = listOf(
                    EQBandConfig(name = "32 Hz", type = FilterType.LOW_SHELF, frequency = 32.0, gainDb = 4.5, q = 0.7),
                    EQBandConfig(name = "64 Hz", type = FilterType.PEAKING, frequency = 64.0, gainDb = 3.2, q = 1.0),
                    EQBandConfig(name = "250 Hz", type = FilterType.PEAKING, frequency = 250.0, gainDb = 0.8, q = 1.0),
                    EQBandConfig(name = "1 kHz", type = FilterType.PEAKING, frequency = 1000.0, gainDb = 0.0, q = 1.0),
                    EQBandConfig(name = "4 kHz", type = FilterType.PEAKING, frequency = 4000.0, gainDb = 2.5, q = 1.0),
                    EQBandConfig(name = "8 kHz", type = FilterType.PEAKING, frequency = 8000.0, gainDb = 1.0, q = 1.0),
                    EQBandConfig(name = "16 kHz", type = FilterType.HIGH_SHELF, frequency = 16000.0, gainDb = 0.5, q = 0.7)
                )
            ),
            EQPreset(
                id = "warm_tube",
                name = "Analog Tube & Vinyl",
                description = "Rich harmonic low-end warmth with gentle silky roll-off.",
                bands = listOf(
                    EQBandConfig(name = "32 Hz", type = FilterType.LOW_SHELF, frequency = 32.0, gainDb = 2.0, q = 0.7),
                    EQBandConfig(name = "64 Hz", type = FilterType.PEAKING, frequency = 64.0, gainDb = 3.0, q = 1.0),
                    EQBandConfig(name = "250 Hz", type = FilterType.PEAKING, frequency = 250.0, gainDb = 1.5, q = 1.0),
                    EQBandConfig(name = "1 kHz", type = FilterType.PEAKING, frequency = 1000.0, gainDb = 0.0, q = 1.0),
                    EQBandConfig(name = "4 kHz", type = FilterType.PEAKING, frequency = 4000.0, gainDb = -0.5, q = 1.0),
                    EQBandConfig(name = "8 kHz", type = FilterType.PEAKING, frequency = 8000.0, gainDb = -1.5, q = 1.0),
                    EQBandConfig(name = "16 kHz", type = FilterType.HIGH_SHELF, frequency = 16000.0, gainDb = -2.5, q = 0.7)
                )
            ),
            EQPreset(
                id = "hd650_correction",
                name = "Sennheiser HD600/HD650",
                description = "AutoEQ sub-bass extension and veil lift.",
                bands = listOf(
                    EQBandConfig(name = "Sub Lift", type = FilterType.LOW_SHELF, frequency = 60.0, gainDb = 5.0, q = 0.71),
                    EQBandConfig(name = "Mid Bass", type = FilterType.PEAKING, frequency = 180.0, gainDb = -1.5, q = 1.4),
                    EQBandConfig(name = "Mids", type = FilterType.PEAKING, frequency = 1200.0, gainDb = 0.0, q = 1.0),
                    EQBandConfig(name = "Veil Lift", type = FilterType.PEAKING, frequency = 3200.0, gainDb = 2.0, q = 1.8),
                    EQBandConfig(name = "Treble Air", type = FilterType.HIGH_SHELF, frequency = 10000.0, gainDb = 2.5, q = 0.71)
                )
            )
        )

    fun loadPreset(preset: EQPreset) {
        _activePresetName.value = preset.name
        if (preset.bands.size == _simpleBands.value.size) {
            val updated = _simpleBands.value.mapIndexed { idx, band ->
                band.copy(gainDb = preset.bands[idx].gainDb)
            }
            _simpleBands.value = updated
        } else {
            _expertBands.value = preset.bands
            _mode.value = EQMode.EXPERT
        }
        syncActiveBands()
    }

    fun applyPreset(preset: EQPreset) = loadPreset(preset)

    fun saveCurrentAsPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val currentBands = if (_mode.value == EQMode.SIMPLE) _simpleBands.value else _expertBands.value
        val preset = EQPreset(
            name = trimmed,
            description = "Custom user preset (${if (_mode.value == EQMode.SIMPLE) "7-Band" else "Parametric"})",
            bands = currentBands,
            isCustom = true
        )
        _userPresets.value = _userPresets.value + preset
        _activePresetName.value = preset.name
        persistUserPresets()
        updateAllPresetsList()
    }

    fun deleteUserPreset(id: String) {
        _userPresets.value = _userPresets.value.filter { it.id != id && it.name != id }
        persistUserPresets()
        updateAllPresetsList()
    }

    fun deleteCustomPreset(nameOrId: String) = deleteUserPreset(nameOrId)

    private fun persistUserPresets() {
        val jsonArray = JSONArray()
        for (preset in _userPresets.value) {
            val obj = JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("description", preset.description)
                val bandsArray = JSONArray()
                for (b in preset.bands) {
                    val bObj = JSONObject().apply {
                        put("name", b.name)
                        put("type", b.type.name)
                        put("freq", b.frequency)
                        put("gain", b.gainDb)
                        put("q", b.q)
                    }
                    bandsArray.put(bObj)
                }
                put("bands", bandsArray)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("HEX_USER_PRESETS_JSON", jsonArray.toString()).apply()
    }

    private fun loadUserPresets() {
        val raw = prefs.getString("HEX_USER_PRESETS_JSON", null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<EQPreset>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val desc = obj.optString("description", "")
                val bandsArray = obj.getJSONArray("bands")
                val bands = mutableListOf<EQBandConfig>()
                for (j in 0 until bandsArray.length()) {
                    val bObj = bandsArray.getJSONObject(j)
                    bands.add(
                        EQBandConfig(
                            name = bObj.getString("name"),
                            type = FilterType.valueOf(bObj.getString("type")),
                            frequency = bObj.getDouble("freq"),
                            gainDb = bObj.getDouble("gain"),
                            q = bObj.optDouble("q", 1.0)
                        )
                    )
                }
                list.add(EQPreset(id = id, name = name, description = desc, bands = bands, isCustom = true))
            }
            _userPresets.value = list
        } catch (e: Exception) {
            // Ignore parse errors on corrupted prefs
        }
    }
}
