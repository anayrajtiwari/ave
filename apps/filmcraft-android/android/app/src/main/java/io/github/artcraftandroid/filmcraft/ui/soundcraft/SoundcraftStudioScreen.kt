package io.github.artcraftandroid.filmcraft.ui.soundcraft

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.artcraftandroid.filmcraft.ui.GlyphIcon
import io.github.artcraftandroid.filmcraft.ui.LucideIcons
import io.github.artcraftandroid.filmcraft.ui.PrimaryButton
import io.github.artcraftandroid.filmcraft.ui.editor.PickKind
import io.github.artcraftandroid.filmcraft.ui.editor.VoiceRecorder
import io.github.artcraftandroid.filmcraft.ui.glyph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

private enum class StudioTab { Equalizer, Dynamics, Reverb, Saturation, Plugins }
private val StudioGreen = Color(0xFF22C55E)

@Composable
fun SoundcraftStudioScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var params by remember { mutableStateOf(soundcraftDefaultParams()) }
    var selectedPreset by remember { mutableStateOf(SoundcraftPreset.NEUTRAL) }
    var activeTab by remember { mutableStateOf(StudioTab.Equalizer) }
    var meters by remember { mutableStateOf(SoundcraftMeters(-96f, -96f, 0f)) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready to import audio or record with microphone") }

    // Audio file & recording state
    var currentAudioFile by remember { mutableStateOf<File?>(null) }
    var audioTitle by remember { mutableStateOf("No audio loaded") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingRecorder by remember { mutableStateOf<VoiceRecorder?>(null) }
    var recordingDurationSec by remember { mutableLongStateOf(0L) }
    var recordingLevel by remember { mutableFloatStateOf(0f) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }

    // Playback state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Clean up media player and recorder on exit
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            recordingRecorder?.stop()
            recordingRecorder = null
            recordingJob?.cancel()
        }
    }

    BackHandler {
        mediaPlayer?.stop()
        recordingRecorder?.stop()
        vm.back()
    }

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
                onClick = {
                    mediaPlayer?.stop()
                    recordingRecorder?.stop()
                    vm.back()
                }
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
            // Audio Source & Recording Card
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Fc.Layer1)
                    .border(1.dp, Fc.Hairline, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Source", style = FcType.Label, color = Fc.Secondary)
                        if (isRecording) {
                            Text(
                                "● REC ${recordingDurationSec}s",
                                style = FcType.Badge,
                                color = Fc.Recording,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Buttons: Import File & Record Mic
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Import Audio File Button
                        Box(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Fc.Raised)
                                .clickable(role = Role.Button) {
                                    vm.pick(PickKind.FilesAudio) { uris ->
                                        val uri = uris.firstOrNull() ?: return@pick
                                        scope.launch(Dispatchers.IO) {
                                            runCatching {
                                                val dest = File(context.cacheDir, "soundcraft_import_${System.currentTimeMillis()}.wav")
                                                context.contentResolver.openInputStream(uri)?.use { input ->
                                                    dest.outputStream().use { output -> input.copyTo(output) }
                                                }
                                                val floats = loadSamplesFromAudioFile(dest)
                                                val m = soundcraftAnalyzeAudio(floats.toList(), 1u, 48000u)
                                                withContext(Dispatchers.Main) {
                                                    currentAudioFile = dest
                                                    audioTitle = uri.lastPathSegment ?: "Imported Audio"
                                                    meters = m
                                                    statusMessage = "Loaded audio: ${floats.size / 48000}s, Peak: %.1f dB".format(m.peakDb)
                                                    vm.tell("Audio loaded into SoundCraft Studio")
                                                }
                                            }.onFailure {
                                                withContext(Dispatchers.Main) {
                                                    vm.tell("Could not load audio file: ${it.message}")
                                                }
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlyphIcon(LucideIcons.Music.glyph(), Fc.Accent, size = 20.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Import Audio", style = FcType.ToolLabel, color = Fc.Title)
                            }
                        }

                        // Record Microphone Button
                        Box(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isRecording) Fc.Recording else Fc.Raised)
                                .clickable(role = Role.Button) {
                                    if (isRecording) {
                                        // Stop Recording
                                        recordingRecorder?.stop()
                                        recordingRecorder = null
                                        isRecording = false
                                        recordingJob?.cancel()
                                        currentAudioFile?.let { recorded ->
                                            scope.launch(Dispatchers.IO) {
                                                val floats = loadSamplesFromAudioFile(recorded)
                                                val m = soundcraftAnalyzeAudio(floats.toList(), 1u, 48000u)
                                                withContext(Dispatchers.Main) {
                                                    audioTitle = "Voice Recording (${recordingDurationSec}s)"
                                                    meters = m
                                                    statusMessage = "Recording saved: ${floats.size / 48000}s"
                                                    vm.tell(statusMessage)
                                                }
                                            }
                                        }
                                    } else {
                                        // Start Recording
                                        vm.requestMicrophone {
                                            val recFile = File(context.cacheDir, "soundcraft_rec_${System.currentTimeMillis()}.wav")
                                            val recorder = VoiceRecorder(recFile)
                                            if (recorder.start()) {
                                                currentAudioFile = recFile
                                                recordingRecorder = recorder
                                                isRecording = true
                                                recordingDurationSec = 0L
                                                statusMessage = "Recording microphone input..."
                                                recordingJob = scope.launch {
                                                    val start = System.currentTimeMillis()
                                                    while (isRecording) {
                                                        recordingDurationSec = (System.currentTimeMillis() - start) / 1000
                                                        recordingLevel = recorder.level
                                                        delay(100)
                                                    }
                                                }
                                            } else {
                                                vm.tell("Could not initialize microphone recorder")
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlyphIcon(
                                    LucideIcons.Mic.glyph(),
                                    if (isRecording) Color.White else Fc.Recording,
                                    size = 20.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isRecording) "Stop Rec" else "Record Mic",
                                    style = FcType.ToolLabel,
                                    color = if (isRecording) Color.White else Fc.Title
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Audio Track Info & Transport Play/Stop
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Fc.Raised)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(audioTitle, style = FcType.Body, color = Fc.Title, maxLines = 1)
                            Text(
                                if (currentAudioFile != null) "Ready for SoundCraft DSP mastering" else "Tap Import or Record to begin",
                                style = FcType.BodySmall,
                                color = Fc.Detail
                            )
                        }

                        if (currentAudioFile != null) {
                            // Play / Stop Button
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Fc.Accent)
                                    .clickable(role = Role.Button) {
                                        if (isPlaying) {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            isPlaying = false
                                        } else {
                                            currentAudioFile?.let { file ->
                                                runCatching {
                                                    mediaPlayer?.release()
                                                    mediaPlayer = MediaPlayer().apply {
                                                        setDataSource(file.absolutePath)
                                                        setOnCompletionListener { isPlaying = false }
                                                        prepare()
                                                        start()
                                                    }
                                                    isPlaying = true
                                                }.onFailure {
                                                    vm.tell("Playback error: ${it.message}")
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isPlaying) "■" else "▶",
                                    style = FcType.Body,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

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

            // Action / Processing Button: Master Audio Track
            PrimaryButton(
                text = if (isProcessing) "Mastering with SoundCraft DSP..." else "Apply DSP & Master Audio",
                onClick = {
                    if (isProcessing) return@PrimaryButton
                    isProcessing = true
                    statusMessage = "Processing through SoundCraft DSP rack..."
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            val targetFile = currentAudioFile
                            val inputSamples: FloatArray = if (targetFile != null && targetFile.exists()) {
                                loadSamplesFromAudioFile(targetFile)
                            } else {
                                // synthesize 48kHz test chord
                                val sr = 48000
                                val total = sr * 2
                                FloatArray(total) { i ->
                                    val t = i.toFloat() / sr
                                    (0.4f * sin(2.0 * Math.PI * 220.0 * t) +
                                     0.3f * sin(2.0 * Math.PI * 440.0 * t) +
                                     0.2f * sin(2.0 * Math.PI * 660.0 * t)).toFloat()
                                }
                            }

                            if (inputSamples.isEmpty()) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    vm.tell("No audio samples to process")
                                }
                                return@withContext
                            }

                            // Run through SoundCraft pure-Rust DSP engine
                            val processed = soundcraftProcessAudioBuffer(inputSamples.toList(), 1u, 48000u, params)
                            val m = soundcraftAnalyzeAudio(processed, 1u, 48000u)

                            // Save mastered WAV file
                            val outFile = File(context.cacheDir, "soundcraft_mastered_${System.currentTimeMillis()}.wav")
                            saveFloatArrayToWav(outFile, processed.toFloatArray(), 48000)

                            withContext(Dispatchers.Main) {
                                meters = m
                                currentAudioFile = outFile
                                audioTitle = "Mastered Audio (${processed.size / 48000}s)"
                                isProcessing = false
                                statusMessage = "Mastered: Peak %.1f dB, RMS %.1f dB".format(m.peakDb, m.rmsDb)
                                vm.tell("SoundCraft mastering complete! Ready to play.")
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

/** Loads 16-bit PCM WAV samples into a float array normalized -1.0..1.0. */
private fun loadSamplesFromAudioFile(file: File): FloatArray {
    val bytes = file.readBytes()
    if (bytes.size <= 44) return FloatArray(0)
    val numSamples = (bytes.size - 44) / 2
    val floats = FloatArray(numSamples)
    val bb = ByteBuffer.wrap(bytes, 44, numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until numSamples) {
        floats[i] = bb.short.toFloat() / 32768.0f
    }
    return floats
}

/** Writes a float array of PCM samples (-1.0..1.0) into a standard 16-bit mono WAV file. */
private fun saveFloatArrayToWav(file: File, samples: FloatArray, sampleRate: Int) {
    file.parentFile?.mkdirs()
    val raf = RandomAccessFile(file, "rw")
    raf.setLength(0)
    val dataSize = samples.size * 2
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray())
        putInt(dataSize + 36)
        put("WAVEfmt ".toByteArray())
        putInt(16) // PCM format header length
        putShort(1) // PCM format
        putShort(1) // Mono
        putInt(sampleRate)
        putInt(sampleRate * 2) // Byte rate
        putShort(2) // Block align
        putShort(16) // Bits per sample
        put("data".toByteArray())
        putInt(dataSize)
    }
    raf.write(header.array())

    val buf = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (s in samples) {
        val clamped = (s.coerceIn(-1.0f, 1.0f) * 32767.0f).toInt().toShort()
        buf.putShort(clamped)
    }
    raf.write(buf.array())
    raf.close()
}
