package io.github.artcraftandroid.filmcraft.ui.soundcraft

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.artcraftandroid.filmcraft.core.SoundcraftDspParams
import io.github.artcraftandroid.filmcraft.core.SoundcraftMeters
import io.github.artcraftandroid.filmcraft.core.SoundcraftPreset
import io.github.artcraftandroid.filmcraft.core.soundcraftAnalyzeAudio
import io.github.artcraftandroid.filmcraft.core.soundcraftAvailablePlugins
import io.github.artcraftandroid.filmcraft.core.soundcraftDefaultParams
import io.github.artcraftandroid.filmcraft.core.soundcraftPresetParams
import io.github.artcraftandroid.filmcraft.core.soundcraftProcessAudioBuffer
import io.github.artcraftandroid.filmcraft.ui.AppViewModel
import io.github.artcraftandroid.filmcraft.ui.Fc
import io.github.artcraftandroid.filmcraft.ui.FcIconButton
import io.github.artcraftandroid.filmcraft.ui.FcSlider
import io.github.artcraftandroid.filmcraft.ui.FcType
import io.github.artcraftandroid.filmcraft.ui.LucideIcons
import io.github.artcraftandroid.filmcraft.ui.PrimaryButton
import io.github.artcraftandroid.filmcraft.ui.glyph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

private enum class StudioTab { Equalizer, Dynamics, Reverb, Saturation, Plugins }
private val StudioGreen = Color(0xFF22C55E)

@Composable
fun SoundcraftStudioScreen(vm: AppViewModel) {
    BackHandler { vm.back() }

    var params by remember { mutableStateOf(soundcraftDefaultParams()) }
    var selectedPreset by remember { mutableStateOf(SoundcraftPreset.NEUTRAL) }
    var activeTab by remember { mutableStateOf(StudioTab.Equalizer) }
    var meters by remember { mutableStateOf(SoundcraftMeters(-96f, -96f, 0f)) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("SoundCraft Audio Workstation Ready") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Fc.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("screen_soundcraft_studio")
    ) {
        // Studio Header
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FcIconButton(
                glyph = LucideIcons.ChevronLeft.glyph(),
                description = "Back",
                onClick = { vm.back() }
            )
            Text(
                "SoundCraft Studio",
                style = FcType.Header,
                color = Fc.Title,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .clip(CircleShape)
                    .background(Fc.Layer1)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("DAW DSP", style = FcType.Badge, color = Fc.Accent)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Preset Selector Row
            Text("Mastering Presets", style = FcType.Label, color = Fc.Secondary)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip("Clean Flat", selectedPreset == SoundcraftPreset.NEUTRAL) {
                    selectedPreset = SoundcraftPreset.NEUTRAL
                    params = soundcraftDefaultParams()
                }
                PresetChip("Vocal Clarity", selectedPreset == SoundcraftPreset.VOCAL_CLARITY) {
                    selectedPreset = SoundcraftPreset.VOCAL_CLARITY
                    params = soundcraftPresetParams(SoundcraftPreset.VOCAL_CLARITY)
                }
                PresetChip("Warm Tape", selectedPreset == SoundcraftPreset.WARM_TAPE) {
                    selectedPreset = SoundcraftPreset.WARM_TAPE
                    params = soundcraftPresetParams(SoundcraftPreset.WARM_TAPE)
                }
                PresetChip("Podcast Master", selectedPreset == SoundcraftPreset.PODCAST_MASTER) {
                    selectedPreset = SoundcraftPreset.PODCAST_MASTER
                    params = soundcraftPresetParams(SoundcraftPreset.PODCAST_MASTER)
                }
                PresetChip("Bass Punch", selectedPreset == SoundcraftPreset.BASS_PUNCH) {
                    selectedPreset = SoundcraftPreset.BASS_PUNCH
                    params = soundcraftPresetParams(SoundcraftPreset.BASS_PUNCH)
                }
                PresetChip("Spacious Air", selectedPreset == SoundcraftPreset.SPACIOUS_AIR) {
                    selectedPreset = SoundcraftPreset.SPACIOUS_AIR
                    params = soundcraftPresetParams(SoundcraftPreset.SPACIOUS_AIR)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Studio Tabs (EQ, Dynamics, Reverb, Saturation, Plugins)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Fc.Layer1)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StudioTabButton("EQ", activeTab == StudioTab.Equalizer) { activeTab = StudioTab.Equalizer }
                StudioTabButton("Dynamics", activeTab == StudioTab.Dynamics) { activeTab = StudioTab.Dynamics }
                StudioTabButton("Reverb", activeTab == StudioTab.Reverb) { activeTab = StudioTab.Reverb }
                StudioTabButton("Warmth", activeTab == StudioTab.Saturation) { activeTab = StudioTab.Saturation }
                StudioTabButton("Rack", activeTab == StudioTab.Plugins) { activeTab = StudioTab.Plugins }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Content Box
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Fc.Layer1)
                    .border(1.dp, Fc.Hairline, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    StudioTab.Equalizer -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("7-Band Parametric EQ", style = FcType.Header, color = Fc.Title, modifier = Modifier.weight(1f))
                                TogglePill(params.eqEnabled) { params = params.copy(eqEnabled = !params.eqEnabled) }
                            }
                            Spacer(Modifier.height(16.dp))
                            DspSlider("Low Shelf Gain", "${params.lowShelfGainDb.toInt()} dB", (params.lowShelfGainDb + 12f) / 24f) {
                                params = params.copy(lowShelfGainDb = it * 24f - 12f, eqEnabled = true)
                            }
                            DspSlider("Low-Mid Gain (LMF)", "${params.lowMidGainDb.toInt()} dB", (params.lowMidGainDb + 12f) / 24f) {
                                params = params.copy(lowMidGainDb = it * 24f - 12f, eqEnabled = true)
                            }
                            DspSlider("Mid Gain (MF)", "${params.midGainDb.toInt()} dB", (params.midGainDb + 12f) / 24f) {
                                params = params.copy(midGainDb = it * 24f - 12f, eqEnabled = true)
                            }
                            DspSlider("High-Mid Gain (HMF)", "${params.highMidGainDb.toInt()} dB", (params.highMidGainDb + 12f) / 24f) {
                                params = params.copy(highMidGainDb = it * 24f - 12f, eqEnabled = true)
                            }
                            DspSlider("High Shelf Gain (HF)", "${params.highShelfGainDb.toInt()} dB", (params.highShelfGainDb + 12f) / 24f) {
                                params = params.copy(highShelfGainDb = it * 24f - 12f, eqEnabled = true)
                            }
                            DspSlider("High-Pass Filter", "${params.hpfFreqHz.toInt()} Hz", (params.hpfFreqHz - 20f) / 480f) {
                                params = params.copy(hpfFreqHz = 20f + it * 480f, eqEnabled = true)
                            }
                        }
                    }
                    StudioTab.Dynamics -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Studio Compressor & Limiter", style = FcType.Header, color = Fc.Title, modifier = Modifier.weight(1f))
                                TogglePill(params.compEnabled) { params = params.copy(compEnabled = !params.compEnabled) }
                            }
                            Spacer(Modifier.height(16.dp))
                            DspSlider("Threshold", "${params.compThresholdDb.toInt()} dB", (params.compThresholdDb + 60f) / 60f) {
                                params = params.copy(compThresholdDb = it * 60f - 60f, compEnabled = true)
                            }
                            DspSlider("Ratio", "%.1f:1".format(params.compRatio), (params.compRatio - 1f) / 19f) {
                                params = params.copy(compRatio = 1f + it * 19f, compEnabled = true)
                            }
                            DspSlider("Attack", "${params.compAttackMs.toInt()} ms", params.compAttackMs / 100f) {
                                params = params.copy(compAttackMs = it * 100f, compEnabled = true)
                            }
                            DspSlider("Release", "${params.compReleaseMs.toInt()} ms", params.compReleaseMs / 500f) {
                                params = params.copy(compReleaseMs = it * 500f, compEnabled = true)
                            }
                            DspSlider("Makeup Gain", "+${params.compMakeupDb.toInt()} dB", params.compMakeupDb / 20f) {
                                params = params.copy(compMakeupDb = it * 20f, compEnabled = true)
                            }
                        }
                    }
                    StudioTab.Reverb -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Room Reverb", style = FcType.Header, color = Fc.Title, modifier = Modifier.weight(1f))
                                TogglePill(params.reverbEnabled) { params = params.copy(reverbEnabled = !params.reverbEnabled) }
                            }
                            Spacer(Modifier.height(16.dp))
                            DspSlider("Wet/Dry Mix", "${params.reverbMixPercent.toInt()}%", params.reverbMixPercent / 100f) {
                                params = params.copy(reverbMixPercent = it * 100f, reverbEnabled = true)
                            }
                            DspSlider("Room Size", "${params.reverbSizePercent.toInt()}%", params.reverbSizePercent / 100f) {
                                params = params.copy(reverbSizePercent = it * 100f, reverbEnabled = true)
                            }
                            DspSlider("Decay Time", "%.1f s".format(params.reverbDecaySecs), params.reverbDecaySecs / 5f) {
                                params = params.copy(reverbDecaySecs = it * 5f, reverbEnabled = true)
                            }
                            DspSlider("HF Damping", "${(params.reverbDampingHz / 1000f).toInt()} kHz", (params.reverbDampingHz - 1000f) / 15000f) {
                                params = params.copy(reverbDampingHz = 1000f + it * 15000f, reverbEnabled = true)
                            }
                        }
                    }
                    StudioTab.Saturation -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tape & Tube Warmth", style = FcType.Header, color = Fc.Title, modifier = Modifier.weight(1f))
                                TogglePill(params.satEnabled) { params = params.copy(satEnabled = !params.satEnabled) }
                            }
                            Spacer(Modifier.height(16.dp))
                            DspSlider("Harmonic Drive", "${params.satDriveDb.toInt()} dB", params.satDriveDb / 24f) {
                                params = params.copy(satDriveDb = it * 24f, satEnabled = true)
                            }
                            DspSlider("Warmth Mix", "${params.satMixPercent.toInt()}%", params.satMixPercent / 100f) {
                                params = params.copy(satMixPercent = it * 100f, satEnabled = true)
                            }
                        }
                    }
                    StudioTab.Plugins -> {
                        Column {
                            Text("Registered SoundCraft Plugins", style = FcType.Header, color = Fc.Title)
                            Spacer(Modifier.height(8.dp))
                            val available = remember { soundcraftAvailablePlugins() }
                            for (p in available) {
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Fc.Accent))
                                    Spacer(Modifier.width(10.dp))
                                    Text(p, style = FcType.Body, color = Fc.Body)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Studio Level Meters Card
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Fc.Layer1)
                    .border(1.dp, Fc.Hairline, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Studio Meters", style = FcType.Label, color = Fc.Secondary)
                    Spacer(Modifier.height(10.dp))
                    MeterBar("Peak Level", meters.peakDb)
                    Spacer(Modifier.height(8.dp))
                    MeterBar("RMS Level", meters.rmsDb)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dynamic Headroom", style = FcType.BodySmall, color = Fc.Detail)
                        Text("%.1f dB".format(meters.dynamicRangeDb), style = FcType.BodySmall, color = Fc.Title)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action / Processing Button
            PrimaryButton(
                text = if (isProcessing) "Processing with SoundCraft..." else "Test Studio DSP Chain",
                onClick = {
                    if (isProcessing) return@PrimaryButton
                    isProcessing = true
                    statusMessage = "Processing 48kHz audio buffer..."
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            // Synthesize a 48 kHz stereo audio buffer
                            val sr = 48000
                            val durationSec = 1.0f
                            val totalSamples = (sr * durationSec).toInt() * 2 // stereo
                            val input = FloatArray(totalSamples)
                            for (i in 0 until totalSamples step 2) {
                                val t = (i / 2).toFloat() / sr.toFloat()
                                // rich chord (220 Hz A, 440 Hz A, 660 Hz E)
                                val v = (0.4f * sin(2.0 * Math.PI * 220.0 * t) +
                                         0.3f * sin(2.0 * Math.PI * 440.0 * t) +
                                         0.2f * sin(2.0 * Math.PI * 660.0 * t)).toFloat()
                                input[i] = v
                                input[i + 1] = v
                            }

                            // Run through SoundCraft DSP pipeline (pure Rust)
                            val processed = soundcraftProcessAudioBuffer(input.toList(), 2u, sr.toUInt(), params)
                            val m = soundcraftAnalyzeAudio(processed, 2u, sr.toUInt())

                            withContext(Dispatchers.Main) {
                                meters = m
                                isProcessing = false
                                statusMessage = "DSP Processed: Peak %.1f dB, RMS %.1f dB".format(m.peakDb, m.rmsDb)
                                vm.tell(statusMessage)
                            }
                        }
                    }
                },
                enabled = !isProcessing
            )

            Spacer(Modifier.height(12.dp))
            Text(
                statusMessage,
                style = FcType.BodySmall,
                color = Fc.Detail,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Fc.Accent else Fc.Layer1)
            .border(1.dp, if (selected) Fc.Accent else Fc.Hairline, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = FcType.ToolLabel,
            color = if (selected) Color.White else Fc.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun StudioTabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Fc.Raised else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = FcType.ToolLabel,
            color = if (selected) Fc.Title else Fc.Detail,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TogglePill(enabled: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (enabled) StudioGreen.copy(alpha = 0.2f) else Fc.Raised)
            .border(1.dp, if (enabled) StudioGreen else Fc.Hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            if (enabled) "ON" else "BYPASS",
            style = FcType.Badge,
            color = if (enabled) StudioGreen else Fc.Detail,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DspSlider(label: String, valueText: String, fraction: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = FcType.Body, color = Fc.Body)
            Text(valueText, style = FcType.Body, color = Fc.Title, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        FcSlider(fraction.coerceIn(0f, 1f), onChange = onChange)
    }
}

@Composable
private fun MeterBar(label: String, levelDb: Float) {
    val norm = ((levelDb + 60f) / 60f).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = FcType.BodySmall, color = Fc.Detail)
            Text(
                if (levelDb <= -90f) "-∞ dB" else "%.1f dB".format(levelDb),
                style = FcType.BodySmall,
                color = if (levelDb > -1f) Fc.Negative else Fc.Title
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Fc.Raised)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(norm)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            levelDb > -1f -> Fc.Negative
                            levelDb > -6f -> Color(0xFFF59E0B) // Amber
                            else -> StudioGreen
                        }
                    )
            )
        }
    }
}
