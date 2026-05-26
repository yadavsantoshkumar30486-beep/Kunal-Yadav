package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.StormCell3D
import com.example.model.StormPreset
import com.example.model.StormType
import com.example.util.NotificationHelper
import com.example.ui.components.Weather3DCanvas
import com.example.ui.theme.*
import androidx.compose.animation.core.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RadarDarkBackground)
                ) { innerPadding ->
                    WeatherTrackerScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherTrackerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val selectedCell by viewModel.selectedCell.collectAsStateWithLifecycle()
    
    // Ambient Immersive Dark Canvas with glowing atmospheric gradients
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBackground)
    ) {
        // High fidelity radial atmospheric glows matching the design's SVG
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = if (viewModel.isHistoricalMode) Color(0xFF00FF66).copy(alpha = 0.05f) else ImmersivePink.copy(alpha = 0.12f),
                radius = this.size.width * 0.55f,
                center = Offset(this.size.width * 0.85f, this.size.height * 0.22f)
            )
            drawCircle(
                color = if (viewModel.isHistoricalMode) Color(0xFF00FFCC).copy(alpha = 0.03f) else ImmersivePurple.copy(alpha = 0.08f),
                radius = this.size.width * 0.75f,
                center = Offset(this.size.width * 0.15f, this.size.height * 0.50f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // -------------------------------------------------------------
            // SECTION 1: Immersive Header (Location Pin, Title, Warning Alert)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location Pin",
                        tint = if (viewModel.isHistoricalMode) RadarGreen else ImmersivePurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val locationName = if (viewModel.isHistoricalMode) {
                            viewModel.currentHistoricalEvent.location
                        } else {
                            when (selectedPreset.id) {
                                "hurricane_hera" -> "Key West, FL"
                                "tornado_vortex" -> "Vortex, CO"
                                "monsoon_nirvana" -> "Kolkata, IN"
                                "arctic_vortex" -> "Siberia Sector"
                                else -> "Recon Zone"
                            }
                        }
                        val systemAlertLabel = if (viewModel.isHistoricalMode) {
                            "Historical Playback: " + viewModel.currentHistoricalEvent.date
                        } else {
                            "Live Severe " + selectedPreset.name + " Warning"
                        }
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = systemAlertLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (viewModel.isHistoricalMode) RadarGreen else Color.LightGray
                        )
                    }
                }

                // Glass-molded Search/AI button
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ImmersiveOverlayHeader),
                    modifier = Modifier
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .size(44.dp)
                        .clickable { viewModel.requestAIBriefing() }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Brief",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Tabs to quickly switch weather simulation scenarios (M3 visual controls)
            ScrollableTabRow(
                selectedTabIndex = StormPreset.presets.indexOf(selectedPreset),
                containerColor = Color.Transparent,
                contentColor = if (viewModel.isHistoricalMode) RadarGreen else ImmersivePurple,
                edgePadding = 24.dp,
                divider = { HorizontalDivider(thickness = 0.5.dp, color = Color(0x11FFFFFF)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                StormPreset.presets.forEach { preset ->
                    val isSelected = preset.id == selectedPreset.id
                    Tab(
                        selected = isSelected,
                        onClick = { 
                            if (!viewModel.isHistoricalMode) {
                                viewModel.applyPreset(preset) 
                            }
                        },
                        enabled = !viewModel.isHistoricalMode,
                        modifier = Modifier.testTag("scenario_tab_${preset.id}"),
                        text = {
                            Text(
                                text = preset.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    if (viewModel.isHistoricalMode) RadarGreen else ImmersivePurple
                                } else Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    )
                }
            }

            // -------------------------------------------------------------
            // SECTION 1B: Typographic Temperature & Status Displays
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val tempDegrees = if (viewModel.isHistoricalMode) {
                    "${viewModel.currentHistoricalEvent.temp}°"
                } else {
                    when (selectedPreset.id) {
                        "hurricane_hera" -> "78°"
                        "tornado_vortex" -> "72°"
                        "monsoon_nirvana" -> "84°"
                        else -> "18°"
                    }
                }
                val condDesc = if (viewModel.isHistoricalMode) {
                    viewModel.currentHistoricalEvent.name
                } else {
                    when (selectedPreset.id) {
                        "hurricane_hera" -> "Cyclonic Eyewall Core"
                        "tornado_vortex" -> "Vortex Suction Core"
                        "monsoon_nirvana" -> "Monsoonal Rain Trough"
                        else -> "Glacial Blizzard Vortex"
                    }
                }
                Text(
                    text = tempDegrees,
                    fontSize = 90.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = Color.White,
                    letterSpacing = (-4).sp,
                    lineHeight = 90.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Condition Status Indicator",
                        tint = if (viewModel.isHistoricalMode) RadarGreen else ImmersivePink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = condDesc,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // -------------------------------------------------------------
            // SECTION 1D: Historical Weather Playback Console (New Feature!)
            // -------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isHistoricalMode) Color(0xFF04140A) else ImmersiveCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .border(
                        1.5.dp, 
                        if (viewModel.isHistoricalMode) RadarGreen.copy(alpha = 0.5f) else Color(0x0EFFFFFF), 
                        RoundedCornerShape(24.dp)
                    )
                    .testTag("historical_console_panel")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Historical Clock",
                                tint = if (viewModel.isHistoricalMode) RadarGreen else ImmersivePurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HISTORICAL METEOROLOGY ANALOGS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.isHistoricalMode) RadarGreen else Color.White,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        // Switch to trigger Historical Mode on/off
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (viewModel.isHistoricalMode) "PLAYBACK: ON" else "PLAYBACK: OFF",
                                color = if (viewModel.isHistoricalMode) RadarGreen else Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Switch(
                                checked = viewModel.isHistoricalMode,
                                onCheckedChange = { isEnabled ->
                                    viewModel.isHistoricalMode = isEnabled
                                    if (isEnabled) {
                                        val hist = viewModel.currentHistoricalEvent
                                        val matchPreset = StormPreset.presets.find { it.id == hist.presetId }
                                        if (matchPreset != null) {
                                            viewModel.applyPreset(matchPreset)
                                        }
                                    }
                                    viewModel.aiAnalysisText = null
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = RadarGreen,
                                    checkedTrackColor = Color(0x3300FF66),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color(0x1DFFFFFF)
                                ),
                                modifier = Modifier.scale(0.7f).testTag("historical_mode_switch")
                            )
                        }
                    }
                    
                    Text(
                        text = "Access registered past extreme storms. Swapping activates retroactive barometric gradients and holographic retro-phosphor radar streamlines in the 3D viewport.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    AnimatedVisibility(visible = viewModel.isHistoricalMode) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "SELECT HISTORICAL RECORD:",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                viewModel.historicalEvents.forEachIndexed { idx, ev ->
                                    val isSelected = viewModel.selectedHistoricalEventIndex == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) RadarGreen.copy(alpha = 0.2f) else Color(0x0EFFFFFF)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) RadarGreen else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.selectedHistoricalEventIndex = idx
                                                val matchPreset = StormPreset.presets.find { it.id == ev.presetId }
                                                if (matchPreset != null) {
                                                    viewModel.applyPreset(matchPreset)
                                                }
                                                viewModel.aiAnalysisText = null
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp)
                                            .testTag("historical_event_tile_${ev.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = ev.date,
                                                color = if (isSelected) RadarGreen else Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = ev.name.take(10) + "..",
                                                color = if (isSelected) Color.White else Color.Gray,
                                                fontSize = 8.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF030704), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("DATE ENCODED", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    Text(viewModel.currentHistoricalEvent.date, color = RadarGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("PRESSURE CORE", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    Text("${viewModel.currentHistoricalEvent.pressure} hPa", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("PEAK SUSTAINED", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    Text("${viewModel.currentHistoricalEvent.winds} mph", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 2: 3D Physics Viewport
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(24.dp))
                    .background(Color(0xE005070B), RoundedCornerShape(24.dp))
            ) {
                // Main Interactive 3D Projection Canvas
                Weather3DCanvas(
                    preset = selectedPreset,
                    orbitAngleX = viewModel.orbitAngleX,
                    orbitAngleY = viewModel.orbitAngleY,
                    scaleZoom = viewModel.scaleZoom,
                    showGridWarping = viewModel.showGridWarping,
                    showWindFlow = viewModel.showWindFlow,
                    showRadarEchoes = viewModel.showRadarEchoes,
                    showLightning = viewModel.showLightningEffects,
                    simSpeedScale = viewModel.simSpeedScale,
                    selectedCell = selectedCell,
                    onCellSelected = { viewModel.selectCell(it) },
                    onAnglesChanged = { p, y ->
                        viewModel.orbitAngleX = p
                        viewModel.orbitAngleY = y
                    },
                    isHistoricalMode = viewModel.isHistoricalMode,
                    historicalDate = if (viewModel.isHistoricalMode) viewModel.currentHistoricalEvent.date else "",
                    modifier = Modifier.testTag("3d_weather_canvas")
                )

                // 3D HUD Diagnostics Overlay legend (Bottom Left)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .background(Color(0xD207090C), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "AXIS: P:${viewModel.orbitAngleX.toInt()}° Y:${viewModel.orbitAngleY.toInt()}°",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(ImmersivePurple, RoundedCornerShape(1)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Wind Particles (${selectedPreset.maxWindsMph} mph)", color = Color.White, fontSize = 9.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(ImmersivePink, RoundedCornerShape(1)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Severe Heat Cores (>55 dBZ)", color = Color.White, fontSize = 9.sp)
                    }
                }

                // 3D Viewport Controls (Top Right Overlay)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Zoom In/Out
                    Row(
                        modifier = Modifier
                            .background(Color(0xBB1C1B1F), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.scaleZoom = (viewModel.scaleZoom - 0.15f).coerceAtLeast(0.6f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp, 2.dp)
                                        .background(Color.White)
                                )
                            }
                        }
                        Text(
                            text = "ZOOM",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { viewModel.scaleZoom = (viewModel.scaleZoom + 0.15f).coerceAtMost(2.0f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Touch Interaction Tip
                    Text(
                        text = "◀ Drag 3D Sphere ▶",
                        color = ImmersivePurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(Color(0xE5111115), RoundedCornerShape(6.dp))
                            .border(0.5.dp, ImmersivePurple.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // SECTION 1C: Secondary Parameter Cards (Wind Speed & Intensity Progress Indicators)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Wind Speed
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ImmersiveCard),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0x0EFFFFFF), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info, // custom clean dynamic wave vector representation
                                contentDescription = "Wind flow icon",
                                tint = ImmersivePurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WIND SPEED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${selectedPreset.maxWindsMph} mph",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // Progress custom filled indicators
                        val windRatio = (selectedPreset.maxWindsMph.toFloat() / 150f).coerceIn(0.1f, 1.0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x15FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(windRatio)
                                    .fillMaxHeight()
                                    .background(ImmersivePurple)
                            )
                        }
                    }
                }

                // Card 2: Intensity Progress Widget
                val maxCellDbz = selectedPreset.stormCells.maxOfOrNull { it.dbz } ?: 45
                val (intensityLevel, fillCount) = when {
                    maxCellDbz >= 60 -> "High" to 3
                    maxCellDbz >= 52 -> "Medium" to 2
                    else -> "Low" to 1
                }
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ImmersiveCard),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0x0EFFFFFF), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Intensity bolt icon",
                                tint = ImmersivePink,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "INTENSITY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = intensityLevel,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // 4 colored segment indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 1..4) {
                                val isFilled = i <= fillCount
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isFilled) ImmersivePink else Color(0x15FFFFFF))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            // SECTION 1E: UNMISSABLE SEVERE WEATHER ALERTS CORRIDOR (New Feature!)
            // -------------------------------------------------------------
            val alertPulseTransition = rememberInfiniteTransition(label = "alert_pulse")
            val alertPulseAlpha by alertPulseTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alert_color_alpha"
            )

            val borderColor = if (viewModel.isHistoricalMode) {
                Color(0xFF00FF66).copy(alpha = alertPulseAlpha)
            } else {
                Color(0xFFEF4444).copy(alpha = alertPulseAlpha)
            }

            val warningLocation = if (viewModel.isHistoricalMode) {
                viewModel.currentHistoricalEvent.location
            } else {
                when (selectedPreset.id) {
                    "hurricane_hera" -> "Key West, FL"
                    "tornado_vortex" -> "Vortex, CO"
                    "monsoon_nirvana" -> "Kolkata, IN"
                    "arctic_vortex" -> "Siberia Sector"
                    else -> "Recon Zone"
                }
            }
            
            val warningType = if (viewModel.isHistoricalMode) {
                viewModel.currentHistoricalEvent.name
            } else {
                selectedPreset.name
            }

            val warningSeverity = if (viewModel.isHistoricalMode) {
                viewModel.currentHistoricalEvent.severity
            } else {
                when (selectedPreset.id) {
                    "hurricane_hera" -> "CRITICAL CAT 5 HURRICANE THREAT"
                    "tornado_vortex" -> "VIOLENT MULTI-VORTEX TORNADO Touchdown"
                    "monsoon_nirvana" -> "SEVERE FLASH FLOOD INUNDATION WARNING"
                    "arctic_vortex" -> "EXTREME WHITE-OUT BLIZZARD WARNING"
                    else -> "SEVERE ATMOSPHERIC HAZARD"
                }
            }

            val warningDuration = if (viewModel.isHistoricalMode) {
                viewModel.currentHistoricalEvent.duration
            } else {
                "Next 12 Hours (Continuous monitoring)"
            }

            val warningAdvice = if (viewModel.isHistoricalMode) {
                viewModel.currentHistoricalEvent.advice
            } else {
                when (selectedPreset.id) {
                    "hurricane_hera" -> listOf(
                        "Mandatory evacuation active for lowlands & coastline.",
                        "Gather critical communication arrays and survival batteries.",
                        "Seek structured masonry coordinates above surge levels."
                    )
                    "tornado_vortex" -> listOf(
                        "Retreat immediately to secure underground storm shelters.",
                        "Cover major vitals with thick cushioning pads to avoid debris.",
                        "Avoid all outer windows and elevated structural zones."
                    )
                    "monsoon_nirvana" -> listOf(
                        "Do not cross running flood channels under any conditions.",
                        "Boil or filter all domestic ingestion fluids.",
                        "Transfer vital resources to second-story positions."
                    )
                    else -> listOf(
                        "Remain sheltered indoors to avoid frostbite spikes.",
                        "Verify clear exterior vents on all furnace infrastructure.",
                        "Maintain steady dripping faucets to prevent line bursting."
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isHistoricalMode) Color(0xFF04180A) else Color(0xFF1E0709)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .border(
                        2.dp,
                        borderColor,
                        RoundedCornerShape(24.dp)
                    )
                    .testTag("severe_weather_alert_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (viewModel.isHistoricalMode) Color(0xFF00FF66).copy(alpha = alertPulseAlpha)
                                    else Color(0xFFEF4444).copy(alpha = alertPulseAlpha)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "METEOROLOGICAL EMERGENCY UPLINK ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (viewModel.isHistoricalMode) Color(0xFF00FF66) else Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = warningSeverity.uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.2).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECTOR: $warningLocation",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "SPAN: $warningDuration",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isHistoricalMode) Color(0xFF00FF66) else Color(0xFFFFB300),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp, 
                        color = if (viewModel.isHistoricalMode) Color(0x3800FF66) else Color(0x38EF4444)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ACTIONABLE COMPLIANCE INSTRUCTIONS (NWS SECURED):",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        warningAdvice.forEach { tip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                  Text(
                                      text = "⚡",
                                      color = if (viewModel.isHistoricalMode) Color(0xFF00FF66) else Color(0xFFFFB300),
                                      fontSize = 11.sp,
                                      modifier = Modifier.padding(end = 6.dp)
                                  )
                                  Text(
                                      text = tip,
                                      fontSize = 11.sp,
                                      color = Color.White,
                                      lineHeight = 15.sp
                                  )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            // SECTION 3: Bottom Sheet Controls Deck (40.dp rounded-t background)
            // -------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                colors = CardDefaults.cardColors(containerColor = ImmersiveCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.9.dp, Color(0x0AFFFFFF), RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    // -------------------------------------------------------------
                    // Hourly Forecast HTML implementation
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hourly Forecast",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Next 24h",
                            color = ImmersivePurple,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dynamically adjust temperatures relative to dynamic presets
                        val basicTempVal = when (selectedPreset.id) {
                            "hurricane" -> 78
                            "tornado" -> 72
                            "monsoon" -> 84
                            else -> 18
                        }
                        val condIcons = listOf(
                            Icons.Default.Warning, // representing dynamic storm
                            Icons.Default.Warning,
                            Icons.Default.Info,    // representing clouds
                            Icons.Default.Info,
                            Icons.Default.Star     // representing clear/star night
                        )

                        HourlyItem(time = "Now", icon = condIcons[0], temp = "${basicTempVal}°", tint = ImmersivePurple)
                        HourlyItem(time = "4PM", icon = condIcons[1], temp = "${basicTempVal - 4}°", tint = ImmersivePurple)
                        HourlyItem(time = "5PM", icon = condIcons[2], temp = "${basicTempVal - 7}°", tint = Color.Gray)
                        HourlyItem(time = "6PM", icon = condIcons[3], temp = "${basicTempVal - 10}°", tint = Color.Gray)
                        HourlyItem(time = "7PM", icon = condIcons[4], temp = "${basicTempVal - 13}°", tint = ImmersivePurple)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0x18FFFFFF))
                    Spacer(modifier = Modifier.height(20.dp))

                    // System dynamic explanation
                    Text(
                        text = "METEOROLOGICAL DIAGNOSTICS",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedPreset.description,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.SansSerif
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Toggle Configurations
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x35030712)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "SIMULATION LAYER TOGGLES",
                                color = ImmersivePurple,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Column
                                Column(modifier = Modifier.weight(1f)) {
                                    ToggleRow(
                                        title = "Pressure Gradient",
                                        checked = viewModel.showGridWarping,
                                        onCheckedChange = { viewModel.showGridWarping = it }
                                    )
                                    ToggleRow(
                                        title = "Streamline Vectors",
                                        checked = viewModel.showWindFlow,
                                        onCheckedChange = { viewModel.showWindFlow = it }
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // Right Column
                                Column(modifier = Modifier.weight(1f)) {
                                    ToggleRow(
                                        title = "Radar Rings",
                                        checked = viewModel.showRadarEchoes,
                                        onCheckedChange = { viewModel.showRadarEchoes = it }
                                    )
                                    ToggleRow(
                                        title = "ESD Discharge",
                                        checked = viewModel.showLightningEffects,
                                        onCheckedChange = { viewModel.showLightningEffects = it }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0x18FFFFFF))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Slider: Physics flow rate scale
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Velocity scale configuration",
                                    tint = ImmersivePink,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                        text = "Wind Field Velocity Ratio: ${"%.1f".format(viewModel.simSpeedScale)}x",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = viewModel.simSpeedScale,
                                onValueChange = { viewModel.simSpeedScale = it },
                                valueRange = 0.2f..2.2f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ImmersivePink,
                                    activeTrackColor = ImmersivePink,
                                    inactiveTrackColor = Color(0x18FFFFFF)
                                ),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("sim_speed_slider")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Storm Cells Telemetry Panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x35030712)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ACTIVE RADAR CELLS (TAP TO FOCUS)",
                                color = ImmersivePurple,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Row list of storm cells
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedPreset.stormCells.forEach { cell ->
                                    val isTargeted = cell == selectedCell
                                    val cellColorAccent = when {
                                        cell.dbz >= 60 -> ImmersivePink
                                        else -> ImmersivePurple
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isTargeted) cellColorAccent.copy(alpha = 0.15f) else Color(0x0EFFFFFF),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isTargeted) cellColorAccent else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel.selectCell(cell) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                            .weight(1f)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = cell.name.take(12),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = if (isTargeted) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "${cell.dbz} dBZ",
                                                color = cellColorAccent,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            selectedCell?.let { cell ->
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0x18FFFFFF))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Display detailed stats
                                Text(
                                    text = "CELL TELEMETRY: ${cell.name.uppercase()}",
                                    color = ImmersivePink,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CellMetric(title = "Core Echo Top", value = "${cell.echoTopKft} kft")
                                    CellMetric(title = "Strike Vol", value = "${cell.lightningStrikesPerMin}/min")
                                    CellMetric(title = "Peak Gusts", value = "${cell.windGustsKts} kts")
                                    CellMetric(title = "Precipitation", value = if (cell.dbz >= 60) "EXTREME" else "HEAVY")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gemini AI Forecaster Console
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF07090C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ImmersivePurple.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "AI forecaster badge icon",
                                        tint = ImmersivePurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GEMINI SYNOPTIC DISPATCHER",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = { viewModel.requestAIBriefing() },
                                    enabled = !viewModel.isAILoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ImmersivePurple,
                                        disabledContainerColor = Color(0xFF1E293B)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("load_ai_briefing_btn")
                                ) {
                                    if (viewModel.isAILoading) {
                                        CircularProgressIndicator(
                                            color = ImmersivePurple,
                                            strokeWidth = 1.5.dp,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "GENERATE BRIEF",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = ImmersivePurple.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            if (viewModel.isAILoading) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        color = ImmersivePurple,
                                        trackColor = ImmersivePurple.copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth().height(4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "DOWNLINKING CLOUD REFLECTIVITIES FROM MODEL...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ImmersivePurple,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else if (viewModel.aiAnalysisText != null) {
                                // Breathtaking Terminal Console Layout
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp)
                                        .background(Color(0xFF030508), RoundedCornerShape(10.dp))
                                        .border(0.5.dp, Color(0x18FFFFFF), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = viewModel.aiAnalysisText!!,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.testTag("ai_briefing_response_text")
                                        )
                                    }
                                }
                            } else {
                                // Empty / Call to Action state
                                Text(
                                    text = "⚡ Press 'GENERATE BRIEF' to launch real-time synoptic model evaluation via server-side Gemini 3.5. Model will ingest active storm dynamics, eyewalls, lightning counts and pressure parameters.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // -------------------------------------------------------------
                    // -------------------------------------------------------------
                    // SECTION 4: SAVED RECON STATIONS & ALERT CENTER (Room DB integration)
                    // -------------------------------------------------------------
                    val favoriteLocations by viewModel.favoriteLocations.collectAsStateWithLifecycle()
                    var newLocName by remember { mutableStateOf("") }
                    var newLocPresetId by remember { mutableStateOf("hurricane_hera") }
                    val expandedStationIds = remember { mutableStateMapOf<Int, Boolean>() }
                    val autoAlertStationIds = remember { mutableStateMapOf<Int, Boolean>() }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF07090C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ImmersivePink.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .testTag("saved_locations_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Saved locations star",
                                        tint = ImmersivePink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SAVED RECON FAVORITE STATIONS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(ImmersivePink.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${favoriteLocations.size} ACTIVE",
                                        color = ImmersivePink,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = ImmersivePink.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))

                            if (favoriteLocations.isEmpty()) {
                                Text(
                                    text = "No saved stations. Use the panel below to establish dynamic radar stations inside SQLite database.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                favoriteLocations.forEach { fav ->
                                    val context = LocalContext.current
                                    val matchPreset = StormPreset.presets.find { it.id == fav.presetId } ?: StormPreset.presets.first()
                                    val isExpanded = expandedStationIds[fav.id] ?: false
                                    val autoAlertsEnabled = autoAlertStationIds[fav.id] ?: true

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                            .background(Color(0xFF030508), RoundedCornerShape(8.dp))
                                            .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                            .testTag("fav_item_${fav.id}")
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = "Fav Pin",
                                                            tint = ImmersivePurple,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = fav.name.uppercase(),
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = fav.note,
                                                        color = Color.Gray,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                Text(
                                                    text = "${fav.customTemp}°F",
                                                    color = ImmersivePink,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "SYS: ${matchPreset.name.replace(" (Cat 5)", "").uppercase()}",
                                                        color = ImmersivePink.copy(alpha = 0.8f),
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = "PRES: ${matchPreset.centralPressureHpa}hPa / WIND: ${matchPreset.maxWindsMph}mph",
                                                        color = Color.LightGray,
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Button(
                                                        onClick = { 
                                                            if (!viewModel.isHistoricalMode) {
                                                                viewModel.applyPreset(matchPreset) 
                                                            }
                                                        },
                                                        enabled = !viewModel.isHistoricalMode,
                                                        colors = ButtonDefaults.buttonColors(containerColor = ImmersivePurple),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(24.dp).testTag("track_3d_btn_${fav.id}")
                                                    ) {
                                                        Text("TRACK 3D", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                                    }
                                                    
                                                    Button(
                                                        onClick = { viewModel.triggerPushNotificationAlert(fav) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300FF66)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(24.dp).testTag("ping_alert_btn_${fav.id}")
                                                    ) {
                                                        Text("PING ALERT", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteFavoriteLocation(fav) },
                                                        modifier = Modifier.size(24.dp).testTag("delete_location_btn_${fav.id}")
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Location", tint = Color.Red, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(thickness = 0.5.dp, color = Color(0x11FFFFFF))
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedStationIds[fav.id] = !isExpanded }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = "Expand toggle icon",
                                                        tint = ImmersivePink,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isExpanded) "COLLAPSE DETAILED COCKPIT FORECAST" else "EXPAND CURRENT CONDITIONS PROFILE & 3-DAY FORECAST",
                                                        color = ImmersivePink,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .background(if (isExpanded) ImmersivePink.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isExpanded) "OPENED" else "CLOSED",
                                                        color = if (isExpanded) ImmersivePink else Color.Gray,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }

                                            AnimatedVisibility(visible = isExpanded) {
                                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                                    Text(
                                                        text = "CURRENT WEATHER TELEMETRY",
                                                        color = Color.Gray,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFF030508), RoundedCornerShape(6.dp))
                                                            .padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column {
                                                            Text("COORDINATES", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                                            val simulatedLat = 24.0 + (fav.id % 15) + (fav.name.length % 5) * 0.13
                                                            val simulatedLon = 80.0 + (fav.id % 20) + (fav.name.length % 7) * 0.17
                                                            Text("${"%.2f".format(simulatedLat)}°N, ${"%.2f".format(simulatedLon)}°W", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        }
                                                        Column {
                                                            Text("BAROMETRIC PRESS", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                                            Text("${matchPreset.centralPressureHpa} hPa", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        }
                                                        Column {
                                                            Text("RADAR REFLECT", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                                            val reflectionStrength = when (matchPreset.type) {
                                                                StormType.TORNADO -> "75 dBZ (Extreme)"
                                                                StormType.HURRICANE -> "62 dBZ (Severe)"
                                                                StormType.MONSOON -> "54 dBZ (Heavy)"
                                                                StormType.BLIZZARD -> "42 dBZ (Moderate)"
                                                            }
                                                            Text(reflectionStrength, color = ImmersivePink, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    Text(
                                                        text = "3-DAY PREDICTIVE OUTLOOK",
                                                        color = Color.Gray,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )
                                                    
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFF030508), RoundedCornerShape(6.dp))
                                                            .padding(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        ForecastDayRow(
                                                            day = "Tomorrow (D1)",
                                                            condition = if (matchPreset.type == StormType.BLIZZARD) "Heavy Snowfall/Wind" else "Extreme Convection",
                                                            wind = "${matchPreset.maxWindsMph} mph",
                                                            tempRange = "${fav.customTemp - 4}°F / ${fav.customTemp + 2}°F"
                                                        )
                                                        ForecastDayRow(
                                                            day = "Next Day (D2)",
                                                            condition = if (matchPreset.type == StormType.BLIZZARD) "Whiteout/Ice Gale" else "Secondary Rainbands",
                                                            wind = "${(matchPreset.maxWindsMph * 0.6).toInt()} mph",
                                                            tempRange = "${fav.customTemp - 7}°F / ${fav.customTemp}°F"
                                                        )
                                                        ForecastDayRow(
                                                            day = "Recovery (D3)",
                                                            condition = "Atmospheric Clearance",
                                                            wind = "12 mph",
                                                            tempRange = "${fav.customTemp - 10}°F / ${fav.customTemp - 2}°F"
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFF051109), RoundedCornerShape(8.dp))
                                                            .border(0.5.dp, Color(0x3300FF66), RoundedCornerShape(8.dp))
                                                            .padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "PUSH AUTO-ALERTS MONITORING",
                                                                color = Color(0xFF00FF66),
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                            Text(
                                                                text = if (autoAlertsEnabled) "Armed: Core pressure and winds auto-trigger push alerts" else "Telemetry alerts muted. Press switch to re-arm.",
                                                                color = Color.LightGray,
                                                                fontSize = 8.sp,
                                                                lineHeight = 11.sp,
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            )
                                                        }
                                                        
                                                        Switch(
                                                            checked = autoAlertsEnabled,
                                                            onCheckedChange = { active ->
                                                                autoAlertStationIds[fav.id] = active
                                                                if (active) {
                                                                    NotificationHelper.showWeatherAlertNotification(
                                                                        context = context,
                                                                        locationName = fav.name,
                                                                        severity = "🚨 PUSH TELEMETRY LISTENER ACTIVATED",
                                                                        message = "Automatic live radar tracking establishes coordinates at ${fav.name.uppercase()}. Emergency alarms dispatch successfully.",
                                                                        notificationId = fav.id + 1000
                                                                    )
                                                                 }
                                                            },
                                                            colors = SwitchDefaults.colors(
                                                                checkedThumbColor = Color(0xFF00FF66),
                                                                checkedTrackColor = Color(0x3300FF66)
                                                            ),
                                                            modifier = Modifier.scale(0.7f).testTag("alert_listener_switch_${fav.id}")
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = ImmersivePink.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Add Favorited Station Form
                            Text(
                                text = "ESTABLISH NEW WEATHER RECON STATION",
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = newLocName,
                                onValueChange = { newLocName = it },
                                placeholder = { Text("E.g., Oklahoma Corridor", color = Color.DarkGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ImmersivePink,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedContainerColor = Color(0xFF030508),
                                    unfocusedContainerColor = Color(0xFF030508)
                                ),
                                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("add_location_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SELECT RADAR SCENARIO SIMULATOR TEMPLATE", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        StormPreset.presets.forEach { pr ->
                                            val isChosen = pr.id == newLocPresetId
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isChosen) ImmersivePink.copy(alpha = 0.3f) else Color(0x11FFFFFF),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        if (isChosen) ImmersivePink else Color(0x11FFFFFF),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable { newLocPresetId = pr.id }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pr.type.name.take(4),
                                                    color = if (isChosen) Color.White else Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (newLocName.isNotBlank()) {
                                            viewModel.addFavoriteLocation(
                                                name = newLocName.trim(),
                                                presetId = newLocPresetId,
                                                temp = (20..95).random()
                                            )
                                            newLocName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ImmersivePink),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .align(Alignment.Bottom)
                                        .testTag("submit_add_location_btn")
                                ) {
                                    Text("ADD", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // -------------------------------------------------------------
                    // Navigation footer bar matching original HTML design (Pill active indicator)
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Navigation Radar Option (Active)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { }
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ImmersiveOverlayHeader)
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Radar View menu selection icon",
                                    tint = ImmersivePurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Radar", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }

                        // Navigation Forecast Option (Inactive)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .alpha(0.5f)
                                .clickable { }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Forecast layout view selector",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Forecast", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }

                        // Navigation Alerts Option (Inactive)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .alpha(0.5f)
                                .clickable { }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Weather alerts catalog view selector",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Alerts", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }

                        // Navigation Settings Option (Inactive)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .alpha(0.5f)
                                .clickable { }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Weather station setup customizer selector",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Settings", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyItem(
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    temp: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        Text(text = time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Icon(imageVector = icon, contentDescription = "Weather status symbol", tint = tint, modifier = Modifier.size(22.dp))
        Text(text = temp, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
    }
}


@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.LightGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RadarCyan,
                checkedTrackColor = Color(0x5600E5FF),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color(0x1DFFFFFF)
            ),
            modifier = Modifier.scale(0.6f) // Keep it small and elegant!
        )
    }
}

@Composable
fun CellMetric(title: String, value: String) {
    Column {
        Text(text = title, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ForecastDayRow(
    day: String,
    condition: String,
    wind: String,
    tempRange: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(text = day, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(text = condition, color = Color.Gray, fontSize = 7.sp)
        }
        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.Start) {
            Text(text = "WIND", color = Color.Gray, fontSize = 6.sp, fontFamily = FontFamily.Monospace)
            Text(text = wind, color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "TEMP RANGE", color = Color.Gray, fontSize = 6.sp, fontFamily = FontFamily.Monospace)
            Text(text = tempRange, color = ImmersivePink, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

