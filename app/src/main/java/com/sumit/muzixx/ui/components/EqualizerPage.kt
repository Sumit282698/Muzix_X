package com.sumit.muzixx.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.viewmodel.MusicViewModel

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerPage(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val isSettingsInit = viewModel.isSettingsInitialized()

    val isEqEnabled = if (isSettingsInit) viewModel.settings.eqEnabled else false
    val bassEnabled = if (isSettingsInit) viewModel.settings.bassEnabled else false
    val bassBoost = if (isSettingsInit) viewModel.settings.bassStrength else 0.0f
    val selectedPresetIndex = if (isSettingsInit) viewModel.settings.eqPresetIndex else 0

    val standardBands = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    val bandValues = remember { mutableStateListOf<Float>() }

    LaunchedEffect(isSettingsInit, viewModel.settings.eqBands.size) {
        if (isSettingsInit && viewModel.settings.eqBands.isNotEmpty()) {
            bandValues.clear()
            bandValues.addAll(viewModel.settings.eqBands)
        } else if (bandValues.isEmpty()) {
            bandValues.addAll(listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
        }
    }

    //Preset mappings
    val presets = listOf("Default", "Classic", "Pop", "Rock", "Jazz", "Custom")
    var selectedPreset by remember(selectedPresetIndex) {
        mutableStateOf(if (selectedPresetIndex in presets.indices && selectedPresetIndex != -1) presets[selectedPresetIndex] else "Custom")
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    var loudness by remember { mutableFloatStateOf(0.0f) }
    var loudnessEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (isEqEnabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isEqEnabled) accentColor else Color.Gray,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = isEqEnabled,
                            onCheckedChange = {
                                viewModel.setEqualizerEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge.copy(all = CornerSize(28.dp)),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        bandValues.forEach { valDb ->
                            Text(
                                text = String.format("%+.1f", valDb),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEqEnabled) Color.White else Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        Canvas(
                            modifier = Modifier.fillMaxSize().pointerInput(isEqEnabled, bandValues.size) {
                                if (!isEqEnabled || bandValues.isEmpty()) return@pointerInput
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    selectedPreset = "Custom"
                                    val colWidth = size.width / standardBands.size
                                    val idx = (change.position.x / colWidth).toInt().coerceIn(0, standardBands.size - 1)
                                    val pctY = (change.position.y / size.height).coerceIn(0f, 1f)

                                    val dbTarget = 15f - (pctY * 30f)
                                    if (idx in 0 until bandValues.size) {
                                        bandValues[idx] = dbTarget
                                        viewModel.setBandLevel(idx, dbTarget) // UPDATES PIPELINE + DISK
                                    }
                                }
                            }
                        ) {
                            val stepX = size.width / (standardBands.size + 1)
                            val pts = mutableListOf<Offset>()

                            for (i in standardBands.indices) {
                                if (i < bandValues.size) {
                                    val x = stepX * (i + 1)
                                    val pctY = (15f - bandValues[i]) / 30f
                                    pts.add(Offset(x, pctY * size.height))
                                }
                            }

                            pts.forEach { pt ->
                                drawLine(Color.Gray.copy(alpha = 0.2f), Offset(pt.x, 0f), Offset(pt.x, size.height), strokeWidth = 2f)
                            }

                            if (pts.isNotEmpty() && isEqEnabled) {
                                val path = Path().apply { moveTo(pts.first().x, pts.first().y) }
                                for (i in 0 until pts.size - 1) {
                                    val p1 = pts[i]
                                    val p2 = pts[i + 1]
                                    path.cubicTo((p1.x + p2.x) / 2, p1.y, (p1.x + p2.x) / 2, p2.y, p2.x, p2.y)
                                }
                                drawPath(path, accentColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                            }

                            pts.forEach { pt ->
                                drawCircle(if (isEqEnabled) accentColor else Color.Gray, radius = 5.dp.toPx(), center = pt)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        standardBands.forEach { b ->
                            Text(b, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(presets.size) { index ->
                    val presetName = presets[index]
                    FilterChip(
                        selected = selectedPreset == presetName,
                        onClick = {
                            selectedPreset = presetName
                            val structuralValues = when (presetName) {
                                "Default" -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                                "Classic" -> listOf(4.5f, 3.0f, 2.0f, 0f, -1.0f, -1.0f, 0f, 1.5f, 3.0f, 4.0f)
                                "Pop" -> listOf(-1.5f, -1.0f, 0f, 2.0f, 4.0f, 4.5f, 3.0f, 1.0f, -1.0f, -1.5f)
                                "Rock" -> listOf(5.0f, 4.0f, 3.0f, 1.5f, -0.5f, -1.0f, 0.5f, 2.5f, 4.0f, 5.0f)
                                "Jazz" -> listOf(3.0f, 2.0f, 1.0f, 1.5f, -1.5f, -1.5f, 0f, 1.5f, 3.0f, 3.5f)
                                else -> null
                            }
                            structuralValues?.let { updatedProfile ->
                                bandValues.clear()
                                bandValues.addAll(updatedProfile)

                                updatedProfile.forEachIndexed { bandIdx, dbVal ->
                                    viewModel.setBandLevel(bandIdx, dbVal)
                                }

                                viewModel.setEqualizerPresetLive(index.toShort())
                            }
                        },
                        label = { Text(presetName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.25f),
                            selectedLabelColor = accentColor
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpressiveKnobCard(
                    title = "Bass Boost",
                    value = bassBoost,
                    isEnabled = bassEnabled,
                    cardBg = surfaceColor,
                    accent = accentColor,
                    onValChange = { viewModel.setBassBoostStrength(it) },
                    onToggle = { viewModel.setBassBoostEnabled(it) },
                    modifier = Modifier.weight(1f)
                )
                ExpressiveKnobCard(
                    title = "Loudness",
                    value = loudness,
                    isEnabled = loudnessEnabled,
                    cardBg = surfaceColor,
                    accent = accentColor,
                    onValChange = { loudness = it },
                    onToggle = { loudnessEnabled = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                shape = MaterialTheme.shapes.extraLarge.copy(all = CornerSize(24.dp)),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Volume", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = viewModel.currentVolume,
                            onValueChange = { newVolume ->
                                viewModel.setMasterVolume(newVolume)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor
                            )
                        )
                    }
                    Text(
                        text = "${(viewModel.currentVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveKnobCard(
    title: String,
    value: Float,
    isEnabled: Boolean,
    cardBg: Color,
    accent: Color,
    onValChange: (Float) -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAccent by animateColorAsState(if (isEnabled) accent else Color.Gray, label = "")

    Card(
        shape = MaterialTheme.shapes.extraLarge.copy(all = CornerSize(24.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .pointerInput(isEnabled) {
                        if (!isEnabled) return@pointerInput
                        detectDragGestures { change, _ ->
                            change.consume()
                            val absolutePercentY = 1f - (change.position.y / size.height)
                            onValChange(absolutePercentY.coerceIn(0f, 1f))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.size(70.dp)) {
                    val sw = 5.dp.toPx()
                    val aSize = Size(size.width - sw, size.height - sw)
                    val tl = Offset(sw / 2, sw / 2)

                    drawArc(Color.Gray.copy(alpha = 0.2f), 140f, 260f, false, tl, aSize, style = Stroke(sw, cap = StrokeCap.Round))
                    drawArc(animatedAccent, 140f, 260f * value, false, tl, aSize, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.3f)
                )
            )
        }
    }
}