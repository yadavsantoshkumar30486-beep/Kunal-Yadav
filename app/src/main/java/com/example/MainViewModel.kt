package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApi
import com.example.database.AppDatabase
import com.example.database.FavoriteLocation
import com.example.database.FavoriteLocationRepository
import com.example.model.StormCell3D
import com.example.model.StormPreset
import com.example.model.StormType
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoricalEvent(
    val id: String,
    val name: String,
    val date: String,
    val location: String,
    val presetId: String,
    val severity: String,
    val duration: String,
    val temp: Int,
    val pressure: Int,
    val winds: Int,
    val description: String,
    val advice: List<String>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repo = FavoriteLocationRepository(db.favoriteLocationDao())

    // Historical Weather Event dataset
    val historicalEvents = listOf(
        HistoricalEvent(
            id = "katrina_2005",
            name = "Hurricane Katrina (Cat 5)",
            date = "2005-08-29",
            location = "Gulf Coast Corridor",
            presetId = "hurricane_hera",
            severity = "CRITICAL CATEGORY 5 SURGE WARNING",
            duration = "18 Hours expected",
            temp = 83,
            pressure = 902,
            winds = 175,
            description = " landfall event. Extreme sea-surface temperatures in the Gulf Loop generated unprecedented convective eyewall velocities, driving massive barometric grid depletion.",
            advice = listOf(
                "Immediate evacuation of low-lying coastal flood zones.",
                "Seek secure dry shelters above 18-foot storm surge limits.",
                "Verify and carry operational portable satellite radios and lanterns."
            )
        ),
        HistoricalEvent(
            id = "moore_2013",
            name = "Moore EF5 Super-Vortex",
            date = "2013-05-20",
            location = "Moore, Oklahoma",
            presetId = "tornado_vortex",
            severity = "VIOLENT EF5 TORNADO EMERGENCY",
            duration = "4 Hours expected",
            temp = 74,
            pressure = 878,
            winds = 210,
            description = "Violent multi-vortex suction core touching down on city corridor. Thermal updraft shears exceeding 200 mph with rapid core rotation profiles.",
            advice = listOf(
                "Retreat instantly to custom subterranean bunkers or heavy safe rooms.",
                "Cover head and neck with robust padding or mattresses to shield debris.",
                "Stay completely away from loose brick walls and exterior window panes."
            )
        ),
        HistoricalEvent(
            id = "mumbai_2005",
            name = "Mumbai Monsoon Deluge",
            date = "2005-07-26",
            location = "Greater Mumbai Sector",
            presetId = "monsoon_nirvana",
            severity = "FLASH FLOOD CATACLYSM WARNING",
            duration = "24 Hours expected",
            temp = 80,
            pressure = 974,
            winds = 88,
            description = "Extreme monsoonal low pressure hit offshore coastal elevations, triggering record high-altitude convection and severe atmospheric water column discharge.",
            advice = listOf(
                "Strictly avoid crossing active or static flooded expressways.",
                "Move all high-value operational gear and food to vertical attic zones.",
                "Utilize backup emergency power supplies and treat all drinking fluids."
            )
        ),
        HistoricalEvent(
            id = "blizzard_1993",
            name = "Storm of the Century",
            date = "1993-03-12",
            location = "Eastern US Spine",
            presetId = "arctic_vortex",
            severity = "EXTREME WIND-CHILL BLIZZARD ALERT",
            duration = "36 Hours expected",
            temp = 10,
            pressure = 960,
            winds = 98,
            description = "Deep brutal polar air mass sink merging with warm coastal moisture. Extreme wind-chill values combined with heavy snow blinding white-out gradients.",
            advice = listOf(
                "Retain absolute shelter indoors; frostbite risks trigger within minutes.",
                "Confirm clear exhaust paths for heating appliances to avoid carbon hazard.",
                "Keep water faucets dripping slowly to prevent structural pipeline ruptures."
            )
        )
    )

    // Current operating states for Historical Mode
    var isHistoricalMode by mutableStateOf(false)
    var selectedHistoricalEventIndex by mutableStateOf(0)

    val currentHistoricalEvent: HistoricalEvent
        get() = historicalEvents[selectedHistoricalEventIndex]

    // Expose favorites reactively from Room DB as StateFlow
    val favoriteLocations: StateFlow<List<FavoriteLocation>> = repo.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedPreset = MutableStateFlow(StormPreset.presets.first())
    val selectedPreset: StateFlow<StormPreset> = _selectedPreset.asStateFlow()

    private val _selectedCell = MutableStateFlow<StormCell3D?>(null)
    val selectedCell: StateFlow<StormCell3D?> = _selectedCell.asStateFlow()

    var orbitAngleX by mutableStateOf(-20f)
    var orbitAngleY by mutableStateOf(45f)
    var scaleZoom by mutableStateOf(1.2f)

    var showGridWarping by mutableStateOf(true)
    var showWindFlow by mutableStateOf(true)
    var showRadarEchoes by mutableStateOf(true)
    var showLightningEffects by mutableStateOf(true)
    
    var simSpeedScale by mutableStateOf(1.0f)
    var isAILoading by mutableStateOf(false)
    var aiAnalysisText by mutableStateOf<String?>(null)

    init {
        // Initialize Notification channel
        NotificationHelper.initNotificationChannel(application)
        
        // Initialize view angle for first preset
        applyPreset(StormPreset.presets.first())
        
        // Inject a default favorite if list is empty to assist first-time user interaction
        viewModelScope.launch {
            repo.allFavorites.collect { list ->
                if (list.isEmpty()) {
                    repo.insert(
                        FavoriteLocation(
                            name = "Miami Coastal Front",
                            presetId = "hurricane_hera",
                            note = "Category 5 Core Evacuation Warnings Active",
                            customTemp = 84
                        )
                    )
                    repo.insert(
                        FavoriteLocation(
                            name = "Oklahoma Tornado Corridor",
                            presetId = "tornado_vortex",
                            note = "Severe Mesh Funnel Tracking",
                            customTemp = 72
                        )
                    )
                }
            }
        }
    }

    fun applyPreset(preset: StormPreset) {
        _selectedPreset.value = preset
        _selectedCell.value = preset.stormCells.firstOrNull()
        orbitAngleX = preset.defaultAngleX
        orbitAngleY = preset.defaultAngleY
        aiAnalysisText = null // Reset AI analysis on scenario change
    }

    fun selectCell(cell: StormCell3D) {
        _selectedCell.value = cell
    }

    // Save location to Room
    fun addFavoriteLocation(name: String, presetId: String, temp: Int = 72) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val preset = StormPreset.presets.find { it.id == presetId } ?: StormPreset.presets.first()
            val noteText = "Tracking ${preset.type.name.lowercase().replaceFirstChar { it.uppercase() }} Structure"
            repo.insert(
                FavoriteLocation(
                    name = name,
                    presetId = presetId,
                    note = noteText,
                    customTemp = temp
                )
            )
        }
    }

    // Delete location from Room
    fun deleteFavoriteLocation(location: FavoriteLocation) {
        viewModelScope.launch {
            repo.delete(location)
        }
    }

    // Trigger local push notification for favorited locations
    fun triggerPushNotificationAlert(location: FavoriteLocation) {
        val app = getApplication<Application>()
        val preset = StormPreset.presets.find { it.id == location.presetId } ?: StormPreset.presets.first()
        
        val alertType = when (preset.type) {
            StormType.HURRICANE -> "🚨 CATEGORY 5 HURRICANE WARNING"
            StormType.TORNADO -> "🚨 SEVERE TORNADO TOUCHDOWN ALERT"
            StormType.MONSOON -> "🚨 FLASH MONSOON FLOODING WARNING"
            StormType.BLIZZARD -> "🚨 EXTREME BLIZZARD WHITE-OUT ALERTIMA"
        }
        
        val detailedMsg = "Severe tracking telemetry recorded at ${location.name}. Core pressure dipping to ${preset.centralPressureHpa} hPa with eyewall winds sweeping at ${preset.maxWindsMph} mph. Emergency shelter protocols requested."
        
        NotificationHelper.showWeatherAlertNotification(
            context = app,
            locationName = location.name,
            severity = alertType,
            message = detailedMsg,
            notificationId = location.id
        )
    }

    fun requestAIBriefing() {
        val preset = _selectedPreset.value
        isAILoading = true
        aiAnalysisText = null

        val cellDetailsList = preset.stormCells.joinToString("; ") {
            "${it.name} (radar: ${it.dbz}dBZ, tops: ${it.echoTopKft}Kft, lightning: ${it.lightningStrikesPerMin}/min, gusts: ${it.windGustsKts}kts)"
        }

        val histContext = if (isHistoricalMode) {
            val ev = currentHistoricalEvent
            "Disaster Profile: ${ev.name} on ${ev.date} in ${ev.location}. Peak intensity registers at ${ev.pressure} hPa pressure, ${ev.winds} mph sustained wind fields."
        } else null

        val finalName = if (isHistoricalMode) currentHistoricalEvent.name else preset.name
        val finalPressure = if (isHistoricalMode) currentHistoricalEvent.pressure else preset.centralPressureHpa
        val finalWinds = if (isHistoricalMode) currentHistoricalEvent.winds else preset.maxWindsMph

        viewModelScope.launch {
            try {
                val response = GeminiApi.getStormAnalysis(
                    stormName = finalName,
                    pressureHpa = finalPressure,
                    windsMph = finalWinds,
                    cellDetails = cellDetailsList,
                    isGridWarping = showGridWarping,
                    activeLightningRate = preset.stormCells.sumOf { it.lightningStrikesPerMin },
                    historicalContext = histContext
                )
                aiAnalysisText = response
            } catch (e: Exception) {
                aiAnalysisText = "Encountered coordinate error during synoptic telemetry uplink. Standby for reboot."
            } finally {
                isAILoading = false
            }
        }
    }
}
