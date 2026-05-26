package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Particle3D(
    var x: Float,
    var y: Float,
    var z: Float,
    var life: Float,
    var maxLife: Float,
    val color: Color,
    val trail: MutableList<Triple<Float, Float, Float>> = mutableListOf()
) {
    fun addTrail() {
        trail.add(Triple(x, y, z))
        if (trail.size > 8) {
            trail.removeAt(0)
        }
    }
}

data class StormCell3D(
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float,
    val dbz: Int, // Radar reflectivity in decibels (e.g., 55 dBZ is heavy rain/hail)
    val echoTopKft: Int, // Height in thousands of feet
    val lightningStrikesPerMin: Int,
    val windGustsKts: Int
)

enum class StormType {
    HURRICANE,    // Circular horizontal spiral
    TORNADO,      // Inward and upward tight rising funnel
    MONSOON,      // Streamlines with a localized shear bend
    BLIZZARD      // Turbulent chaotic cold vortex
}

data class StormPreset(
    val id: String,
    val name: String,
    val type: StormType,
    val description: String,
    val centralPressureHpa: Int,
    val maxWindsMph: Int,
    val stormCells: List<StormCell3D>,
    val defaultAngleX: Float = -20f,
    val defaultAngleY: Float = 45f
) {
    companion object {
        val presets = listOf(
            StormPreset(
                id = "hurricane_hera",
                name = "Hurricane Hera (Cat 5)",
                type = StormType.HURRICANE,
                description = "Massive cyclonic depression active over tropical waters. Showing a distinct 3D cloud wall, high altitude outflow, and secondary convective rainbands.",
                centralPressureHpa = 912,
                maxWindsMph = 165,
                stormCells = listOf(
                    StormCell3D("Eyewall Core East", x = 30f, y = 0f, z = 0f, radius = 22f, dbz = 62, echoTopKft = 58, lightningStrikesPerMin = 18, windGustsKts = 155),
                    StormCell3D("Eyewall Core West", x = -30f, y = 0f, z = 0f, radius = 22f, dbz = 60, echoTopKft = 55, lightningStrikesPerMin = 14, windGustsKts = 148),
                    StormCell3D("Feeder Band Alpha", x = 110f, y = -20f, z = 90f, radius = 35f, dbz = 45, echoTopKft = 42, lightningStrikesPerMin = 6, windGustsKts = 90),
                    StormCell3D("Feeder Band Beta", x = -120f, y = -10f, z = -100f, radius = 40f, dbz = 48, echoTopKft = 46, lightningStrikesPerMin = 8, windGustsKts = 95)
                ),
                defaultAngleX = -25f,
                defaultAngleY = 35f
            ),
            StormPreset(
                id = "tornado_vortex",
                name = "Supercell Tornado 'Vortex'",
                type = StormType.TORNADO,
                description = "VIOLENT rotating col of air in contact with ground. Multi-vortex structure exhibiting severe vertical velocity gradient and thermal updraft shear.",
                centralPressureHpa = 885,
                maxWindsMph = 230,
                stormCells = listOf(
                    StormCell3D("Funnel Touchpoint", x = 0f, y = 100f, z = 0f, radius = 15f, dbz = 75, echoTopKft = 65, lightningStrikesPerMin = 45, windGustsKts = 210),
                    StormCell3D("Mesocyclone Wall", x = 10f, y = -40f, z = -10f, radius = 55f, dbz = 52, echoTopKft = 50, lightningStrikesPerMin = 28, windGustsKts = 115),
                    StormCell3D("Rear Flank Downdraft", x = -50f, y = 20f, z = 40f, radius = 30f, dbz = 40, echoTopKft = 35, lightningStrikesPerMin = 5, windGustsKts = 85)
                ),
                defaultAngleX = -12f,
                defaultAngleY = 55f
            ),
            StormPreset(
                id = "monsoon_nirvana",
                name = "Jetstream Monsoon 'Nirvana'",
                type = StormType.MONSOON,
                description = "Widespread high-momentum flow hitting coastal mountains. Severe high-altitude wind shear causing rapid local squalls and convective triggers.",
                centralPressureHpa = 988,
                maxWindsMph = 85,
                stormCells = listOf(
                    StormCell3D("Orograhic Squall Line A", x = -40f, y = 10f, z = -20f, radius = 45f, dbz = 54, echoTopKft = 40, lightningStrikesPerMin = 12, windGustsKts = 75),
                    StormCell3D("Coastal Flood Cell B", x = 60f, y = 30f, z = 50f, radius = 35f, dbz = 47, echoTopKft = 34, lightningStrikesPerMin = 5, windGustsKts = 60)
                ),
                defaultAngleX = -32f,
                defaultAngleY = -45f
            ),
            StormPreset(
                id = "arctic_vortex",
                name = "Polar Blizzard Vortex",
                type = StormType.BLIZZARD,
                description = "Brutal deep arctic cold core air sink. Strong low-level flow fields carrying massive dry crystal precipitation on highly tilted orbital trajectories.",
                centralPressureHpa = 945,
                maxWindsMph = 105,
                stormCells = listOf(
                    StormCell3D("Polar Core Sink", x = 0f, y = -10f, z = 0f, radius = 60f, dbz = 35, echoTopKft = 22, lightningStrikesPerMin = 0, windGustsKts = 90),
                    StormCell3D("Sub-Zero Squall Alpha", x = 70f, y = 0f, z = -70f, radius = 25f, dbz = 42, echoTopKft = 24, lightningStrikesPerMin = 2, windGustsKts = 80)
                ),
                defaultAngleX = -30f,
                defaultAngleY = 120f
            )
        )
    }
}
