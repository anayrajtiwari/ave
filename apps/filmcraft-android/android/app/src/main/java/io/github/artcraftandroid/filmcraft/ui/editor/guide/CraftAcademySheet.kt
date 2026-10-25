package io.github.artcraftandroid.filmcraft.ui.editor.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.filmcraft.ui.Fc
import io.github.artcraftandroid.filmcraft.ui.FcIconButton
import io.github.artcraftandroid.filmcraft.ui.FcType
import io.github.artcraftandroid.filmcraft.ui.Glyph
import io.github.artcraftandroid.filmcraft.ui.GlyphIcon
import io.github.artcraftandroid.filmcraft.ui.Glyphs
import io.github.artcraftandroid.filmcraft.ui.LucideIcons
import io.github.artcraftandroid.filmcraft.ui.editor.EditorController
import io.github.artcraftandroid.filmcraft.ui.glyph

data class AcademyLesson(
    val title: String,
    val subtitle: String,
    val steps: List<String>,
    val glyph: Glyph,
    val accent: Color,
)

private val LESSONS = listOf(
    AcademyLesson(
        title = "Fixing or Designing Over a Frame",
        subtitle = "Don't like a specific frame or want an intermission card?",
        steps = listOf(
            "1. Scrub the playhead to the frame you want to replace or design over.",
            "2. Tap 'Design' on the toolbar (or select a clip and tap 'Freeze & Design').",
            "3. The paused frame loads as your canvas plate. Use the vector pen or shapes to draw, annotate, or mask.",
            "4. Tap 'Insert Still' to insert the design directly into your sequence with automatic ripple editing!",
        ),
        glyph = Glyphs.Palette,
        accent = Color(0xFFE2C974),
    ),
    AcademyLesson(
        title = "1-Tap Cinematic Motion & Animations",
        subtitle = "Bring still photos, titles, and video clips to life instantly.",
        steps = listOf(
            "1. Tap any clip on your timeline (video, still image, or text).",
            "2. Tap 'Motion' in the toolbar.",
            "3. Select 'Pop / Bounce' for punchy spring entrances, or 'Ken Burns' for a slow cinematic pan and zoom.",
            "4. Adjust the duration slider (0.2s - 2.5s) to match the rhythm of your cut.",
        ),
        glyph = Glyphs.Move,
        accent = Color(0xFF60A5FA),
    ),
    AcademyLesson(
        title = "Keyframing Mastery (EffectCraft)",
        subtitle = "Animate scale, rotation, and position over time.",
        steps = listOf(
            "1. In the Motion panel, switch to the 'Keyframe Studio' tab.",
            "2. Scrub the playhead to where you want the motion to start, adjust Scale/Rotation, and tap '+ Add Keyframe'.",
            "3. Move the playhead forward by 1 or 2 seconds, change the parameters, and tap '+ Add Keyframe' again.",
            "4. Press Play: AVE smoothly interpolates between your keyframes in real time!",
        ),
        glyph = LucideIcons.WandSparkles.glyph(),
        accent = Color(0xFFA78BFA),
    ),
    AcademyLesson(
        title = "Lower-Thirds & Floating Overlays",
        subtitle = "Add graphics that play on top of your video footage.",
        steps = listOf(
            "1. Tap 'Design' and select the 'Transparent' backdrop mode.",
            "2. Under Templates, choose 'LowerThird' or 'SubscribeBadge', or draw custom callouts with the vector pen.",
            "3. Tap 'Add Overlay' at the top right.",
            "4. The graphic is placed on an overlay lane directly above your playhead!",
        ),
        glyph = Glyphs.Template,
        accent = Color(0xFF34D399),
    ),
)

@Composable
fun CraftAcademySheet(ed: EditorController) {
    BackHandler { ed.academyOpen = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(Fc.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("sheet_craft_academy"),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyphs.GraduationCap, Fc.Accent, size = 26.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Craft Academy", style = FcType.Header, color = Fc.Title)
                }
                FcIconButton(LucideIcons.X.glyph(), "Close", { ed.academyOpen = false })
            }

            Text(
                "Learn how to master video editing, vector design, and kinetic motion inside AVE.",
                style = FcType.BodySmall,
                color = Fc.Secondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            // Lesson Cards
            LESSONS.forEach { lesson ->
                LessonCard(lesson)
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Dismiss Button
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Fc.Accent)
                    .clickable { ed.academyOpen = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Got It, Let's Create!", style = FcType.Body, fontWeight = FontWeight.Bold, color = Fc.Black)
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: AcademyLesson) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Fc.Raised)
            .border(1.dp, lesson.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(lesson.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(lesson.glyph, lesson.accent, size = 20.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(lesson.title, style = FcType.Body, fontWeight = FontWeight.Bold, color = Fc.Title)
                Text(lesson.subtitle, style = FcType.BodySmall, fontSize = 12.sp, color = Fc.Secondary)
            }
        }

        Spacer(Modifier.height(10.dp))

        lesson.steps.forEach { step ->
            Text(
                step,
                style = FcType.BodySmall,
                color = Fc.Body,
                lineHeight = 18.sp,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}
