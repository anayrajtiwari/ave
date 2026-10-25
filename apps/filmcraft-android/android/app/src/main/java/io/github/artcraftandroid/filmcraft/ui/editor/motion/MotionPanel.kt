package io.github.artcraftandroid.filmcraft.ui.editor.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.filmcraft.core.ClipInfo
import io.github.artcraftandroid.filmcraft.core.MotionInfo
import io.github.artcraftandroid.filmcraft.ui.Fc
import io.github.artcraftandroid.filmcraft.ui.FcSlider
import io.github.artcraftandroid.filmcraft.ui.FcType
import io.github.artcraftandroid.filmcraft.ui.Glyph
import io.github.artcraftandroid.filmcraft.ui.GlyphIcon
import io.github.artcraftandroid.filmcraft.ui.Glyphs
import io.github.artcraftandroid.filmcraft.ui.LucideIcons
import io.github.artcraftandroid.filmcraft.ui.ToolPanel
import io.github.artcraftandroid.filmcraft.ui.editor.EditorController
import io.github.artcraftandroid.filmcraft.ui.glyph

data class MotionPreset(
    val id: String,
    val label: String,
    val description: String,
    val glyph: Glyph,
)

private val MOTION_PRESETS = listOf(
    MotionPreset("none", "None", "Reset motion", LucideIcons.RotateCcw.glyph()),
    MotionPreset("pop", "Pop / Bounce", "Spring overshoot scale", LucideIcons.WandSparkles.glyph()),
    MotionPreset("fade_in", "Fade In", "Smooth entrance", Glyphs.Checker),
    MotionPreset("fade_out", "Fade Out", "Smooth exit", Glyphs.NoTransition),
    MotionPreset("ken_burns", "Ken Burns", "Cinematic pan & zoom", LucideIcons.Film.glyph()),
    MotionPreset("zoom_in", "Zoom In", "Scale 50% → 100%", Glyphs.Transition),
    MotionPreset("zoom_out", "Zoom Out", "Scale 160% → 100%", LucideIcons.SwatchBook.glyph()),
    MotionPreset("slide_left", "Slide Left", "Enter from right", LucideIcons.ChevronLeft.glyph()),
    MotionPreset("slide_right", "Slide Right", "Enter from left", LucideIcons.ChevronRight.glyph()),
    MotionPreset("slide_up", "Slide Up", "Enter from bottom", LucideIcons.ChevronUp.glyph()),
    MotionPreset("slide_down", "Slide Down", "Enter from top", LucideIcons.ChevronDown.glyph()),
    MotionPreset("pulse", "Pulse", "Rhythmic breathing", LucideIcons.WandSparkles.glyph()),
    MotionPreset("shake", "Camera Shake", "Action camera jitter", Glyphs.Move),
)

@Composable
fun MotionPanel(
    ed: EditorController,
    clip: ClipInfo,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Presets, 1: Keyframe Studio
    var animDuration by remember { mutableDoubleStateOf(0.8) }
    var selectedPreset by remember { mutableStateOf("none") }
    var motionInfo by remember { mutableStateOf<MotionInfo?>(null) }

    LaunchedEffect(clip.id, ed.timeline?.revision) {
        motionInfo = ed.motionInfo(clip.id)
    }

    ToolPanel("Motion & Animation", onClose, modifier = modifier.testTag("panel_motion")) {
        // Tab selector
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Fc.Raised),
            horizontalArrangement = Arrangement.Center,
        ) {
            val tabs = listOf("Presets", "Keyframe Studio")
            tabs.forEachIndexed { index, title ->
                val active = selectedTab == index
                Box(
                    Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Fc.Accent else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title,
                        style = FcType.BodySmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) Fc.Black else Fc.Title,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectedTab == 0) {
            // ---- PRESETS TAB ----
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Duration: ${"%.1f".format(animDuration)}s", style = FcType.BodySmall, color = Fc.Body)
                    Text("Applies to clip start / end", style = FcType.BodySmall, color = Fc.Secondary)
                }

                FcSlider(
                    fraction = ((animDuration.toFloat() - 0.2f) / 2.3f).coerceIn(0f, 1f),
                    onChange = { animDuration = (0.2f + it * 2.3f).toDouble() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )

                Spacer(Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(MOTION_PRESETS) { p ->
                        val active = selectedPreset == p.id
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Fc.Accent.copy(alpha = 0.2f) else Fc.Raised)
                                .border(1.dp, if (active) Fc.Accent else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedPreset = p.id
                                    ed.setMotionPreset(clip.id, p.id, animDuration)
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            GlyphIcon(p.glyph, if (active) Fc.Accent else Fc.Title, size = 20.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                p.label,
                                style = FcType.BodySmall,
                                fontSize = 11.sp,
                                color = if (active) Fc.Accent else Fc.Title,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        } else {
            // ---- KEYFRAME STUDIO TAB ----
            val clipDurationTicks = clip.duration
            val clipStartTicks = clip.start
            val relTicks = (ed.playhead - clipStartTicks).coerceIn(0L, clipDurationTicks)
            val relMs = (relTicks / 254_016_000).coerceAtLeast(0L)

            var kfScale by remember { mutableDoubleStateOf(motionInfo?.scale ?: 100.0) }
            var kfRotation by remember { mutableDoubleStateOf(motionInfo?.rotation ?: 0.0) }
            var kfOpacity by remember { mutableDoubleStateOf(motionInfo?.opacity ?: 100.0) }

            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                // Keyframe timeline visualizer
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Time: ${"%.2f".format(relTicks.toDouble() / 254_016_000_000.0)}s", style = FcType.BodySmall, color = Fc.Accent)
                    val kfCount = motionInfo?.keyframes?.size ?: 0
                    Text("$kfCount keyframes", style = FcType.BodySmall, color = Fc.Secondary)
                }

                Spacer(Modifier.height(4.dp))

                // Diamond scrubber bar
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Fc.Raised)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // Playhead line
                    val frac = if (clipDurationTicks > 0) (relTicks.toFloat() / clipDurationTicks.toFloat()).coerceIn(0f, 1f) else 0f
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = (frac * 280).dp)
                            .size(3.dp, 20.dp)
                            .background(Fc.Accent)
                    )

                    // Diamonds for keyframes
                    motionInfo?.keyframes?.forEach { kf ->
                        val kfFrac = if (clipDurationTicks > 0) ((kf.timeMs * 254_016_000L).toFloat() / clipDurationTicks.toFloat()).coerceIn(0f, 1f) else 0f
                        Box(
                            Modifier
                                .padding(start = (kfFrac * 280).dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700))
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Keyframe buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Fc.Accent)
                            .clickable {
                                ed.addMotionKeyframe(
                                    clip = clip.id,
                                    timeOffsetMs = relMs,
                                    scale = kfScale,
                                    rotation = kfRotation,
                                    opacity = kfOpacity,
                                )
                                ed.tellUser("Keyframe added at current frame")
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+ Add Keyframe", style = FcType.BodySmall, fontWeight = FontWeight.Bold, color = Fc.Black)
                    }

                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Fc.Raised)
                            .clickable {
                                ed.clearMotion(clip.id)
                                ed.tellUser("Keyframes cleared")
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Reset", style = FcType.BodySmall, color = Fc.Secondary)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Live Parameter Sliders for active keyframe
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Scale: ${kfScale.toInt()}%", style = FcType.BodySmall, color = Fc.Title, modifier = Modifier.width(90.dp))
                    FcSlider(
                        fraction = ((kfScale.toFloat() - 20f) / 230f).coerceIn(0f, 1f),
                        onChange = { kfScale = (20f + it * 230f).toDouble() },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Rotation: ${kfRotation.toInt()}°", style = FcType.BodySmall, color = Fc.Title, modifier = Modifier.width(90.dp))
                    FcSlider(
                        fraction = ((kfRotation.toFloat() + 180f) / 360f).coerceIn(0f, 1f),
                        onChange = { kfRotation = (-180f + it * 360f).toDouble() },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Opacity: ${kfOpacity.toInt()}%", style = FcType.BodySmall, color = Fc.Title, modifier = Modifier.width(90.dp))
                    FcSlider(
                        fraction = (kfOpacity.toFloat() / 100f).coerceIn(0f, 1f),
                        onChange = { kfOpacity = (it * 100f).toDouble() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
