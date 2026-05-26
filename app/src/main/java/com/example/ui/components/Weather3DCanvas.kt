package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.Particle3D
import com.example.model.StormCell3D
import com.example.model.StormPreset
import com.example.model.StormType
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarGreen
import com.example.ui.theme.RadarVortexPurple
import com.example.ui.theme.RadarWarningRed
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun Weather3DCanvas(
    preset: StormPreset,
    orbitAngleX: Float,
    orbitAngleY: Float,
    scaleZoom: Float,
    showGridWarping: Boolean,
    showWindFlow: Boolean,
    showRadarEchoes: Boolean,
    showLightning: Boolean,
    simSpeedScale: Float,
    selectedCell: StormCell3D?,
    onCellSelected: (StormCell3D) -> Unit,
    onAnglesChanged: (pitch: Float, yaw: Float) -> Unit,
    isHistoricalMode: Boolean = false,
    historicalDate: String = "",
    modifier: Modifier = Modifier
) {
    // Keep local particle buffer cached, updated based on preset & historical overrides
    val MAX_PARTICLES = 220
    val particles = remember(preset, isHistoricalMode, historicalDate) {
        val list = ArrayList<Particle3D>()
        for (i in 0 until MAX_PARTICLES) {
            list.add(generateRandomParticle(preset, i, isHistoricalMode, historicalDate))
        }
        list
    }

    var tick by remember { mutableStateOf(0L) }
    var lightningAlpha by remember { mutableStateOf(0f) }
    var lightningPath by remember { mutableStateOf<Path?>(null) }
    var activeLightningCell by remember { mutableStateOf<StormCell3D?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // Custom Physics & Rotation Update Ticker (approx 60fps)
    LaunchedEffect(preset, simSpeedScale, isHistoricalMode, historicalDate) {
        val speed = simSpeedScale
        while (isActive) {
            withFrameMillis { t ->
                tick = t
                
                // 1. Slow idle rotation if user is not actively dragging
                if (!isDragging) {
                    onAnglesChanged(orbitAngleX, (orbitAngleY + 0.15f) % 360f)
                }

                // 2. Physics simulation loop for active particles
                if (showWindFlow) {
                    particles.forEachIndexed { index, p ->
                        updateParticlePhysics(p, preset, speed, index, tick, isHistoricalMode, historicalDate)
                    }
                }

                // 3. Spontaneous lightning discharge simulation
                if (showLightning) {
                    if (lightningAlpha > 0f) {
                        lightningAlpha -= 0.08f
                        if (lightningAlpha < 0f) lightningAlpha = 0f
                    } else {
                        // High rain cell can discharge lightning
                        val stormCells = preset.stormCells
                        if (stormCells.isNotEmpty() && (t % 150 < 3)) { // Probability trigger
                            val candidateCell = stormCells.random()
                            if (candidateCell.lightningStrikesPerMin > 0) {
                                // Trigger lightning bolt!
                                activeLightningCell = candidateCell
                                val path = Path()
                                val startX = candidateCell.x
                                val startY = -120f // Storm cloud top altitude
                                val startZ = candidateCell.z
                                path.moveTo(startX, startY)
                                
                                var curX = startX
                                var curY = startY
                                var curZ = startZ
                                // Draw lightning zigzag segments downwards to ground
                                while (curY < 100f) {
                                    val nextY = curY + (20f + (0..15).random().toFloat())
                                    val nextX = curX + ((-15..15).random().toFloat())
                                    val nextZ = curZ + ((-15..15).random().toFloat())
                                    path.lineTo(nextX, nextY) 
                                    curX = nextX
                                    curY = nextY
                                    curZ = nextZ
                                }
                                lightningPath = path
                                lightningAlpha = 0.95f
                            }
                        }
                    }
                }
            }
        }
    }

    var canvasSizeWidth by remember { mutableStateOf(0f) }
    var canvasSizeHeight by remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(preset) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    // Drag adjusts yaw and pitch
                    val nextYaw = (orbitAngleY - dragAmount.x * 0.35f) % 360f
                    val nextPitch = (orbitAngleX + dragAmount.y * 0.35f).coerceIn(-80f, 15f)
                    onAnglesChanged(nextPitch, nextYaw)
                }
            }
            .pointerInput(preset) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { _, _ -> },
                    onDragEnd = { }
                )
            }
            .pointerInput(preset) {
                // Handle Tap to select cell
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    if (width > 0 && height > 0) {
                        var closestCell: StormCell3D? = null
                        var closestDist = Float.MAX_VALUE
                        
                        preset.stormCells.forEach { cell ->
                            val projected = project3DTo2D(
                                x = cell.x, y = cell.y, z = cell.z,
                                pitchDeg = orbitAngleX, yawDeg = orbitAngleY,
                                zoom = scaleZoom, cx = width / 2, cy = height / 2
                            )
                            if (projected != null) {
                                val dx = projected.x - offset.x
                                val dy = projected.y - offset.y
                                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                                if (dist < 120f && dist < closestDist) {
                                    closestDist = dist
                                    closestCell = cell
                                }
                            }
                        }

                        closestCell?.let { onCellSelected(it) }
                    }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        canvasSizeWidth = size.width
        canvasSizeHeight = size.height

        // Background color styling based on Mode
        val ambientBackgroundColor = if (isHistoricalMode) Color(0xFF041008) else Color(0xFF0F172A)
        drawCircle(
            color = ambientBackgroundColor,
            radius = minOf(cx, cy) * 0.95f,
            center = Offset(cx, cy),
            blendMode = BlendMode.Screen
        )

        // -------------------------------------------------------------
        // LAYER 1: 3D Warped Atmospheric Pressure Mesh
        // -------------------------------------------------------------
        if (showGridWarping) {
            val gridStep = 30f
            val gridHalf = 150f
            // Historical mode gets a retro green or custom glowing grids
            val gridColor = if (isHistoricalMode) {
                when (historicalDate) {
                    "2005-08-29" -> Color(0x3800FF44) // Katrina Phosphor Green
                    "2013-05-20" -> Color(0x38FF5252) // Tornado Red Alert Outer
                    "2005-07-26" -> Color(0x3800E5FF) // Monsoon Ocean Grid
                    else -> Color(0x3800FFCC)
                }
            } else {
                Color(0x3300E5FF)
            }
            
            // Draw Latitude lines (Grid rows)
            var zVal = -gridHalf
            while (zVal <= gridHalf) {
                val path = Path()
                var first = true
                var xVal = -gridHalf
                while (xVal <= gridHalf) {
                    val yWarp = getAtmosphericGridHeight(xVal, zVal, preset, isHistoricalMode, historicalDate)
                    val p2d = project3DTo2D(xVal, yWarp, zVal, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                    if (p2d != null) {
                        if (first) {
                            path.moveTo(p2d.x, p2d.y)
                            first = false
                        } else {
                            path.lineTo(p2d.x, p2d.y)
                        }
                    }
                    xVal += 15f
                }
                drawPath(path, color = gridColor, style = Stroke(width = 1f))
                zVal += 30f
            }

            // Draw Longitude lines (Grid cols)
            var xVal = -gridHalf
            while (xVal <= gridHalf) {
                val path = Path()
                var first = true
                var zVal = -gridHalf
                while (zVal <= gridHalf) {
                    val yWarp = getAtmosphericGridHeight(xVal, zVal, preset, isHistoricalMode, historicalDate)
                    val p2d = project3DTo2D(xVal, yWarp, zVal, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                    if (p2d != null) {
                        if (first) {
                            path.moveTo(p2d.x, p2d.y)
                            first = false
                        } else {
                            path.lineTo(p2d.x, p2d.y)
                        }
                    }
                    zVal += 15f
                }
                drawPath(path, color = gridColor, style = Stroke(width = 1f))
                xVal += 30f
            }

            // Draw a central sea-level base bounding box
            val cornerP1 = project3DTo2D(-gridHalf, 80f, -gridHalf, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
            val cornerP2 = project3DTo2D(gridHalf, 80f, -gridHalf, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
            val cornerP3 = project3DTo2D(gridHalf, 80f, gridHalf, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
            val cornerP4 = project3DTo2D(-gridHalf, 80f, gridHalf, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
            
            if (cornerP1 != null && cornerP2 != null && cornerP3 != null && cornerP4 != null) {
                drawPath(Path().apply {
                    moveTo(cornerP1.x, cornerP1.y)
                    lineTo(cornerP2.x, cornerP2.y)
                    lineTo(cornerP3.x, cornerP3.y)
                    lineTo(cornerP4.x, cornerP4.y)
                    close()
                }, color = gridColor.copy(alpha = 0.15f), style = Stroke(width = 1.5f))
            }
        }

        // -------------------------------------------------------------
        // LAYER 2: 3D Weather Storm Cell Radar Echoes (Pulsing concentric orbits)
        // -------------------------------------------------------------
        if (showRadarEchoes) {
            preset.stormCells.forEach { cell ->
                val primaryCellColor = if (isHistoricalMode) {
                    when (historicalDate) {
                        "2005-08-29" -> Color(0xFF00FF66) // Katrina Retro green
                        "2013-05-20" -> RadarWarningRed
                        "2005-07-26" -> RadarCyan
                        else -> Color(0xFF00FFCC)
                    }
                } else {
                    when {
                        cell.dbz >= 60 -> RadarWarningRed
                        cell.dbz >= 50 -> RadarAmber
                        cell.dbz >= 40 -> RadarGreen
                        else -> RadarCyan
                    }
                }

                // Draw cell vertical column vector (convective updrafts)
                val topPt = project3DTo2D(cell.x, -70f, cell.z, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                val bottomPt = project3DTo2D(cell.x, 80f, cell.z, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                if (topPt != null && bottomPt != null) {
                    drawLine(
                        color = primaryCellColor.copy(alpha = 0.45f),
                        start = bottomPt,
                        end = topPt,
                        strokeWidth = 2.5f
                    )
                }

                // Draw pulsing concentric 3D radar rings
                val pulseTime = (tick % 2400) / 2400f 
                for (ringIndex in 0..2) {
                    val ringPulse = (pulseTime + ringIndex * 0.33f) % 1.0f
                    val ringRadiusValue = cell.radius * (0.2f + ringPulse * 1.5f)
                    val outAlpha = (1f - ringPulse) * 0.75f
                    
                    val ringPath = Path()
                    var firstRingPt = true
                    for (angStep in 0..16) {
                        val rads = angStep * (2 * PI) / 16
                        val cxCircle = cell.x + ringRadiusValue * cos(rads).toFloat()
                        val czCircle = cell.z + ringRadiusValue * sin(rads).toFloat()
                        val cyWarpCircle = if (showGridWarping) getAtmosphericGridHeight(cxCircle, czCircle, preset, isHistoricalMode, historicalDate) else 80f

                        val rPoint2d = project3DTo2D(cxCircle, cyWarpCircle, czCircle, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                        if (rPoint2d != null) {
                            if (firstRingPt) {
                                ringPath.moveTo(rPoint2d.x, rPoint2d.y)
                                firstRingPt = false
                            } else {
                                ringPath.lineTo(rPoint2d.x, rPoint2d.y)
                            }
                        }
                    }
                    drawPath(
                        path = ringPath,
                        color = primaryCellColor.copy(alpha = outAlpha),
                        style = Stroke(width = if (cell == selectedCell) 3.5f else 1.5f)
                    )
                }

                // Draw the core marker point
                val corePt = project3DTo2D(cell.x, cell.y, cell.z, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                if (corePt != null) {
                    drawCircle(
                        color = primaryCellColor.copy(alpha = 0.3f),
                        radius = 16f,
                        center = corePt
                    )
                    drawCircle(
                        color = primaryCellColor,
                        radius = if (cell == selectedCell) 8f else 5f,
                        center = corePt
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 3: 3D Wind Pattern Vectors (Particle Streamlines)
        // -------------------------------------------------------------
        if (showWindFlow) {
            particles.forEach { p ->
                val p2d = project3DTo2D(p.x, p.y, p.z, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                if (p2d != null) {
                    val zRot = p.x * sin(orbitAngleY * PI / 180f).toFloat() + p.z * cos(orbitAngleY * PI / 180f).toFloat()
                    val distFactor = ((150f - zRot) / 300f).coerceIn(0.1f, 1.0f)
                    
                    val customizedParticleColor = if (isHistoricalMode) {
                        when (historicalDate) {
                            "2005-08-29" -> Color(0xFF00FF66) // Chlorophyll Green
                            "2013-05-20" -> Color(0xFFFF5252) // Thermographic Fire Flame
                            "2005-07-26" -> Color(0xFF00E5FF) // Monsoon Torrent
                            else -> Color(0xFFFFB300) // Blizzard yellow warning crystals
                        }
                    } else {
                        p.color
                    }

                    // Draw particle historical trails
                    if (p.trail.size > 1) {
                        val trailPath = Path()
                        var trailFirst = true
                        p.trail.forEach { (tx, ty, tz) ->
                            val tp2d = project3DTo2D(tx, ty, tz, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                            if (tp2d != null) {
                                if (trailFirst) {
                                    trailPath.moveTo(tp2d.x, tp2d.y)
                                    trailFirst = false
                                } else {
                                    trailPath.lineTo(tp2d.x, tp2d.y)
                                }
                            }
                        }
                        drawPath(
                            path = trailPath,
                            color = customizedParticleColor.copy(alpha = 0.28f * distFactor * (p.life / p.maxLife)),
                            style = Stroke(width = 1.0f + 2.0f * distFactor)
                        )
                    }

                    // Draw primary particle head
                    drawCircle(
                        color = customizedParticleColor.copy(alpha = 0.95f * distFactor * (p.life / p.maxLife)),
                        radius = (1.5f + 2.5f * distFactor),
                        center = p2d
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 4: Spontaneous Real-Time Lightning Flash
        // -------------------------------------------------------------
        if (showLightning && lightningAlpha > 0f && lightningPath != null && activeLightningCell != null) {
            drawRect(
                color = Color.White.copy(alpha = lightningAlpha * 0.18f),
                size = size
            )

            val currentCell = activeLightningCell!!
            val boltColors = if (isHistoricalMode) {
                listOf(Color(0xFF00FF66), Color.White)
            } else {
                listOf(Color(0xFFE0F7FA), Color(0xFFF3E5F5), Color.White)
            }
            
            val boltPath = Path()
            val startPt = project3DTo2D(currentCell.x, -120f, currentCell.z, orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
            if (startPt != null) {
                boltPath.moveTo(startPt.x, startPt.y)
                
                val endPt = project3DTo2D(currentCell.x + ((-20..20).random().toFloat()), 80f, currentCell.z + ((-20..20).random().toFloat()), orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                if (endPt != null) {
                    val midPt1 = project3DTo2D(currentCell.x + ((-10..10).random().toFloat()), -30f, currentCell.z + ((-10..10).random().toFloat()), orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                    val midPt2 = project3DTo2D(currentCell.x + ((-15..15).random().toFloat()), 30f, currentCell.z + ((-15..15).random().toFloat()), orbitAngleX, orbitAngleY, scaleZoom, cx, cy)
                    
                    if (midPt1 != null && midPt2 != null) {
                        boltPath.lineTo(midPt1.x, midPt1.y)
                        boltPath.lineTo(midPt2.x, midPt2.y)
                        boltPath.lineTo(endPt.x, endPt.y)
                    } else {
                        boltPath.lineTo(endPt.x, endPt.y)
                    }
                }
                
                drawPath(
                    path = boltPath,
                    color = boltColors.random().copy(alpha = lightningAlpha),
                    style = Stroke(width = 3.5f)
                )
                drawPath(
                    path = boltPath,
                    color = Color.White,
                    style = Stroke(width = 1.0f)
                )
            }
        }
    }
}

/**
 * Projects a 3D cartesian point (x, y, z) into a 2D screen offset coordinate.
 */
fun project3DTo2D(
    x: Float,
    y: Float,
    z: Float,
    pitchDeg: Float,
    yawDeg: Float,
    zoom: Float,
    cx: Float,
    cy: Float
): Offset? {
    val pitchRad = pitchDeg * PI / 180.0
    val yawRad = yawDeg * PI / 180.0

    // 1. Rotate around Y axis (yaw)
    val cosY = cos(yawRad).toFloat()
    val sinY = sin(yawRad).toFloat()
    val x1 = x * cosY - z * sinY
    val z1 = x * sinY + z * cosY

    // 2. Rotate around X axis (pitch)
    val cosX = cos(pitchRad).toFloat()
    val sinX = sin(pitchRad).toFloat()
    val y2 = y * cosX - z1 * sinX
    val z2 = y * sinX + z1 * cosX

    // 3. Perspective Projection
    val d = 260f * zoom
    val cameraZ = z2 + 255f 
    if (cameraZ < 10f) return null 

    val sx = cx + (x1 * d) / cameraZ
    val sy = cy + (y2 * d) / cameraZ
    
    return Offset(sx, sy)
}

/**
 * Calculates warped depth atmospheric plane. Overridable based on Selected historical event hash.
 */
fun getAtmosphericGridHeight(
    x: Float,
    z: Float,
    preset: StormPreset,
    isHistoricalMode: Boolean = false,
    historicalDate: String = ""
): Float {
    var minHeight = 80f
    
    // Deterministic offset based on Selected Date
    val dateSeedModifier = if (isHistoricalMode) {
        val hash = historicalDate.hashCode()
        // Shifting coordinates based on specific historical date
        (hash % 35).toFloat()
    } else 0f

    preset.stormCells.forEach { cell ->
        val cx = cell.x + dateSeedModifier
        val cz = cell.z + dateSeedModifier

        val dx = x - cx
        val dz = z - cz
        val dist = sqrt(dx * dx + dz * dz)
        
        val pressureFactor = (1030 - preset.centralPressureHpa).toFloat() / 150f 
        val warpStrength = 55f * pressureFactor.coerceIn(0.2f, 2.5f)
        
        val sigma = cell.radius * 2.2f
        val heightOffset = (warpStrength * cos(((dist / sigma).coerceAtMost((PI / 2.0).toFloat())).toDouble())).toFloat()
        
        val currHeight = 84f - if (dist < sigma) heightOffset else 0f
        if (currHeight < minHeight) {
            minHeight = currHeight
        }
    }
    return minHeight
}

/**
 * Generates initial random particle styled bespoke per mode.
 */
fun generateRandomParticle(
    preset: StormPreset,
    index: Int,
    isHistoricalMode: Boolean = false,
    historicalDate: String = ""
): Particle3D {
    val randomAngle = (0..360).random() * PI / 180.0
    val randomRadius = (35..150).random().toFloat()
    val life = (10..100).random().toFloat()
    
    val x = (randomRadius * cos(randomAngle)).toFloat()
    val z = (randomRadius * sin(randomAngle)).toFloat()
    val y = (-100..60).random().toFloat()

    val pColor = when (preset.type) {
        StormType.HURRICANE -> if (index % 3 == 0) RadarCyan else Color(0x9000E676)
        StormType.TORNADO -> if (index % 2 == 0) RadarAmber else RadarWarningRed
        StormType.MONSOON -> if (index % 3 == 0) Color.White else RadarCyan
        StormType.BLIZZARD -> if (index % 2 == 0) Color.White else Color(0xFFE0F7FA)
    }

    return Particle3D(
        x = x,
        y = y,
        z = z,
        life = life,
        maxLife = 100f,
        color = pColor
    )
}

/**
 * Updates wind coordinates along specific stream trajectory fields based on active weather physics.
 */
fun updateParticlePhysics(
    p: Particle3D,
    preset: StormPreset,
    speed: Float,
    index: Int,
    tick: Long,
    isHistoricalMode: Boolean = false,
    historicalDate: String = ""
) {
    p.life -= 1.0f * speed
    p.addTrail()

    val hashOffset = if (isHistoricalMode) historicalDate.hashCode() else 0

    if (p.life <= 0f) {
        p.life = p.maxLife
        p.trail.clear()
        
        // Deterministic reset using date hash
        val seedAngle = if (isHistoricalMode) {
            val offsetAngle = ((hashOffset + index) % 360) * PI / 180f
            offsetAngle
        } else {
            (0..360).random() * PI / 180f
        }

        when (preset.type) {
            StormType.HURRICANE -> {
                val rRadius = (25..160).random().toFloat()
                p.x = (rRadius * cos(seedAngle)).toFloat()
                p.z = (rRadius * sin(seedAngle)).toFloat()
                p.y = (40..80).random().toFloat()
            }
            StormType.TORNADO -> {
                val rRadius = (5..30).random().toFloat()
                p.x = (rRadius * cos(seedAngle)).toFloat()
                p.z = (rRadius * sin(seedAngle)).toFloat()
                p.y = 80f
            }
            StormType.MONSOON -> {
                p.x = -170f
                p.y = (-60..60).random().toFloat()
                p.z = (-120..120).random().toFloat()
            }
            StormType.BLIZZARD -> {
                val rRadius = (20..150).random().toFloat()
                p.x = (rRadius * cos(seedAngle)).toFloat()
                p.z = (rRadius * sin(seedAngle)).toFloat()
                p.y = (-110..80).random().toFloat()
            }
        }
        return
    }

    val kSpeed = 0.55f * speed
    when (preset.type) {
        StormType.HURRICANE -> {
            val r = sqrt(p.x * p.x + p.z * p.z)
            if (r > 5f) {
                val angle = kotlin.math.atan2(p.z.toDouble(), p.x.toDouble())
                // Adjust spin rate using date seed in historical playback
                val dateOscillator = if (isHistoricalMode) sin((tick + hashOffset) * 0.003f) * 0.18f else 0f
                val spinVelocity = (0.05 + 1.2 / sqrt(r.toDouble())) * (1.0f + dateOscillator)
                val nextAngle = angle + spinVelocity * kSpeed
                
                val radialInwardSpeed = if (r > 38f) -0.8f * kSpeed else 0.4f * kSpeed
                val nextRadius = r + radialInwardSpeed
                
                p.x = (nextRadius * cos(nextAngle)).toFloat()
                p.z = (nextRadius * sin(nextAngle)).toFloat()

                if (r < 40f) {
                    p.y -= 2.6f * kSpeed 
                } else {
                    p.y -= 0.5f * kSpeed 
                }
                
                if (p.y < -90f) {
                    p.y = (-110..-80).random().toFloat()
                }
            } else {
                p.life = 0f
            }
        }
        
        StormType.TORNADO -> {
            val heightRatio = (p.y + 120f) / 200f 
            // Narrow and broaden funnel based on date hash
            val targetRadiusBase = if (isHistoricalMode) 10f + (hashOffset % 12) else 6f
            val targetRadius = targetRadiusBase + heightRatio * 75f
            
            val currentRadius = sqrt(p.x * p.x + p.z * p.z)
            val angle = kotlin.math.atan2(p.z.toDouble(), p.x.toDouble())
            
            val radialAdjustment = (targetRadius - currentRadius) * 0.12f * kSpeed
            val spinVelocity = 0.15 + 0.85 / (currentRadius.coerceIn(2f, 100f).toDouble())
            val nextAngle = angle + spinVelocity * kSpeed
            val nextRadius = currentRadius + radialAdjustment
            
            p.x = (nextRadius * cos(nextAngle)).toFloat()
            p.z = (nextRadius * sin(nextAngle)).toFloat()
            
            p.y -= 3.8f * kSpeed 
            
            if (p.y < -120f) {
                p.life = 0f
            }
        }
        
        StormType.MONSOON -> {
            p.x += 4.5f * kSpeed
            
            val sineFrequency = if (isHistoricalMode) 0.022f else 0.015f
            p.y = p.y + sin((p.x + index * 10f) * sineFrequency).toFloat() * 0.4f * speed
            
            preset.stormCells.firstOrNull()?.let { cell ->
                val dx = p.x - cell.x
                val dz = p.z - cell.z
                val dist = sqrt(dx * dx + dz * dz)
                if (dist < 70f) {
                    val pullStrength = (70f - dist) / 70f
                    p.z -= 1.8f * pullStrength * kSpeed
                    p.y -= 0.6f * pullStrength * kSpeed
                }
            }
            
            if (p.x > 170f) {
                p.life = 0f
            }
        }
        
        StormType.BLIZZARD -> {
            val baseRadius = sqrt(p.x * p.x + p.z * p.z)
            val angle = kotlin.math.atan2(p.z.toDouble(), p.x.toDouble())
            
            val spinVelocity = 0.04f + (index % 5) * 0.01f
            // Flip spin directions for certain historical blizzard offsets
            val directionMultiplier = if (isHistoricalMode && hashOffset % 2 == 0) -1f else 1f
            val nextAngle = angle + spinVelocity * kSpeed * directionMultiplier
            
            p.x = (baseRadius * cos(nextAngle)).toFloat()
            p.z = (baseRadius * sin(nextAngle)).toFloat()
            
            p.y += (sin(tick * 0.01f + index).toFloat() * 0.8f - 0.5f) * kSpeed
            
            val noiseX = ((-15..15).random() / 15f) * 0.8f * speed
            val noiseZ = ((-15..15).random() / 15f) * 0.8f * speed
            p.x += noiseX
            p.z += noiseZ
            
            if (p.y > 80f) {
                p.life = 0f
            }
        }
    }
}
