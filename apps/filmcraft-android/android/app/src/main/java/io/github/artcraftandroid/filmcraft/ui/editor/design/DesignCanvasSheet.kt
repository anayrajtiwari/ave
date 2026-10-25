package io.github.artcraftandroid.filmcraft.ui.editor.design

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.filmcraft.ui.Fc
import io.github.artcraftandroid.filmcraft.ui.FcIconButton
import io.github.artcraftandroid.filmcraft.ui.FcSlider
import io.github.artcraftandroid.filmcraft.ui.FcType
import io.github.artcraftandroid.filmcraft.ui.Glyph
import io.github.artcraftandroid.filmcraft.ui.GlyphIcon
import io.github.artcraftandroid.filmcraft.ui.LucideIcons
import io.github.artcraftandroid.filmcraft.ui.editor.EditorController
import io.github.artcraftandroid.filmcraft.ui.glyph
import kotlin.math.cos
import kotlin.math.sin

enum class DesignTool { Pen, Shapes, Templates, Text }
enum class BackdropMode { Frame, CardColor, Transparent }
enum class ShapeKind { Rectangle, RoundedRect, Circle, Star, Arrow, SpeechBubble }
enum class TemplateKind { None, LowerThird, CinematicQuote, TitleCard, SubscribeBadge }

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
)

data class PlacedShape(
    val kind: ShapeKind,
    val center: Offset,
    val size: Size,
    val fillColor: Color,
    val strokeColor: Color,
    val strokeWidth: Float,
)

private val COLOR_SWATCHES = listOf(
    Color(0xFFFFFFFF), // White
    Color(0xFFFF3B30), // Red
    Color(0xFFFF9500), // Orange
    Color(0xFFFFCC00), // Yellow
    Color(0xFF34C759), // Green
    Color(0xFF00C7BE), // Teal
    Color(0xFF007AFF), // Blue
    Color(0xFF5856D6), // Indigo
    Color(0xFFAF52DE), // Purple
    Color(0xFFFF2D55), // Pink
    Color(0xFF000000), // Black
)

private val CARD_COLORS = listOf(
    Color(0xFF1E293B), // Slate
    Color(0xFF0F172A), // Midnight
    Color(0xFF31103F), // Deep Plum
    Color(0xFF1C2A39), // Navy
    Color(0xFF1B2E24), // Forest
    Color(0xFF27272A), // Zinc
)

@Composable
fun DesignCanvasSheet(ed: EditorController) {
    BackHandler { ed.designOpen = false }

    var tool by remember { mutableStateOf(DesignTool.Pen) }
    var backdropMode by remember { mutableStateOf(if (ed.designBackdrop != null) BackdropMode.Frame else BackdropMode.CardColor) }
    var cardColor by remember { mutableStateOf(CARD_COLORS[0]) }
    var activeColor by remember { mutableStateOf(COLOR_SWATCHES[1]) }
    var strokeWidth by remember { mutableFloatStateOf(8f) }
    var selectedShape by remember { mutableStateOf(ShapeKind.RoundedRect) }
    var selectedTemplate by remember { mutableStateOf(TemplateKind.None) }

    val paths = remember { mutableStateListOf<DrawPath>() }
    val shapes = remember { mutableStateListOf<PlacedShape>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var titleText by remember { mutableStateOf("CHAPTER ONE") }
    var subtitleText by remember { mutableStateOf("A New Vision") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Fc.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("sheet_design_canvas"),
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---- TOP ACTION BAR ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FcIconButton(LucideIcons.X.glyph(), "Close", { ed.designOpen = false })
                    Spacer(Modifier.width(8.dp))
                    Text("Design Studio", style = FcType.Header, color = Fc.Title)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Undo button
                    if (paths.isNotEmpty() || shapes.isNotEmpty()) {
                        FcIconButton(LucideIcons.Undo2.glyph(), "Undo", {
                            if (paths.isNotEmpty()) paths.removeAt(paths.size - 1)
                            else if (shapes.isNotEmpty()) shapes.removeAt(shapes.size - 1)
                        })
                    }

                    // Commit button: Insert Still Segment
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Fc.Accent)
                            .clickable {
                                val bmp = renderCanvasToBitmap(
                                    width = 1080,
                                    height = 1920,
                                    backdrop = if (backdropMode == BackdropMode.Frame) ed.designBackdrop else null,
                                    bgColor = if (backdropMode == BackdropMode.CardColor) cardColor else Color.Transparent,
                                    paths = paths,
                                    shapes = shapes,
                                    template = selectedTemplate,
                                    title = titleText,
                                    subtitle = subtitleText,
                                )
                                ed.insertDesignStill(bmp, asOverlay = false, replace = false)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Insert Still", style = FcType.BodySmall, fontWeight = FontWeight.Bold, color = Fc.Black)
                    }

                    // Commit button: Add as Overlay
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Fc.Raised)
                            .clickable {
                                val bmp = renderCanvasToBitmap(
                                    width = 1080,
                                    height = 1920,
                                    backdrop = null,
                                    bgColor = Color.Transparent,
                                    paths = paths,
                                    shapes = shapes,
                                    template = selectedTemplate,
                                    title = titleText,
                                    subtitle = subtitleText,
                                )
                                ed.insertDesignStill(bmp, asOverlay = true, replace = false)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text("Add Overlay", style = FcType.BodySmall, color = Fc.Title)
                    }
                }
            }

            // ---- BACKDROP SELECTOR ROW ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BackdropChip("Frame", backdropMode == BackdropMode.Frame, enabled = ed.designBackdrop != null) {
                    backdropMode = BackdropMode.Frame
                }
                BackdropChip("Card", backdropMode == BackdropMode.CardColor) {
                    backdropMode = BackdropMode.CardColor
                }
                BackdropChip("Transparent", backdropMode == BackdropMode.Transparent) {
                    backdropMode = BackdropMode.Transparent
                }
            }

            // ---- INTERACTIVE DRAWING CANVAS ----
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                val t = ed.timeline
                val aspect = if (t != null && t.height.toInt() > 0) t.width.toFloat() / t.height.toFloat() else (9f / 16f)

                Box(
                    Modifier
                        .aspectRatio(aspect, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Fc.Detail, RoundedCornerShape(12.dp))
                        .background(
                            when (backdropMode) {
                                BackdropMode.Frame -> Color.Black
                                BackdropMode.CardColor -> cardColor
                                BackdropMode.Transparent -> Color(0x33FFFFFF)
                            }
                        )
                        .pointerInput(tool, activeColor, strokeWidth) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (tool == DesignTool.Pen) {
                                        currentPath = listOf(offset)
                                    } else if (tool == DesignTool.Shapes) {
                                        shapes.add(
                                            PlacedShape(
                                                kind = selectedShape,
                                                center = offset,
                                                size = Size(140.dp.toPx(), 100.dp.toPx()),
                                                fillColor = activeColor.copy(alpha = 0.85f),
                                                strokeColor = Color.White,
                                                strokeWidth = 3f,
                                            )
                                        )
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (tool == DesignTool.Pen) {
                                        currentPath = currentPath + change.position
                                    }
                                },
                                onDragEnd = {
                                    if (tool == DesignTool.Pen && currentPath.isNotEmpty()) {
                                        paths.add(DrawPath(currentPath, activeColor, strokeWidth))
                                        currentPath = emptyList()
                                    }
                                }
                            )
                        },
                ) {
                    // Frame Backdrop
                    if (backdropMode == BackdropMode.Frame && ed.designBackdrop != null) {
                        Image(
                            bitmap = ed.designBackdrop!!,
                            contentDescription = "Frozen Frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }

                    // Native Compose Canvas rendering drawing and shapes
                    Canvas(Modifier.fillMaxSize()) {
                        // Render stored paths
                        paths.forEach { p ->
                            drawSmoothPath(p.points, p.color, p.strokeWidth)
                        }

                        // Render active drawing path
                        if (currentPath.isNotEmpty()) {
                            drawSmoothPath(currentPath, activeColor, strokeWidth)
                        }

                        // Render vector shapes
                        shapes.forEach { shape ->
                            drawVectorShape(shape)
                        }

                        // Render layout template
                        drawTemplatePreview(selectedTemplate, size, titleText, subtitleText)
                    }
                }
            }

            // ---- TOOLBAR & CONTROLS ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Fc.Raised)
                    .padding(vertical = 8.dp)
            ) {
                // Tool Tabs: Pen, Shapes, Templates
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToolTab("Pen", LucideIcons.Paintbrush.glyph(), tool == DesignTool.Pen) { tool = DesignTool.Pen }
                    ToolTab("Shapes", io.github.artcraftandroid.filmcraft.ui.Glyphs.Shapes, tool == DesignTool.Shapes) { tool = DesignTool.Shapes }
                    ToolTab("Templates", io.github.artcraftandroid.filmcraft.ui.Glyphs.Template, tool == DesignTool.Templates) { tool = DesignTool.Templates }
                }

                Spacer(Modifier.height(8.dp))

                when (tool) {
                    DesignTool.Pen -> {
                        // Color swatches + Stroke width
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            COLOR_SWATCHES.forEach { c ->
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(2.dp, if (activeColor == c) Fc.Accent else Color.Transparent, CircleShape)
                                        .clickable { activeColor = c }
                                )
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Thickness", style = FcType.BodySmall, color = Fc.Body, modifier = Modifier.width(70.dp))
                            FcSlider(
                                fraction = ((strokeWidth - 2f) / 34f).coerceIn(0f, 1f),
                                onChange = { strokeWidth = 2f + it * 34f },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    DesignTool.Shapes -> {
                        // Shape selectors
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ShapeKind.values().forEach { sk ->
                                val active = selectedShape == sk
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) Fc.Accent else Fc.Raised)
                                        .clickable { selectedShape = sk }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        sk.name,
                                        style = FcType.BodySmall,
                                        color = if (active) Fc.Black else Fc.Title,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                        Text(
                            "Tap on canvas to place shape",
                            style = FcType.BodySmall,
                            color = Fc.Secondary,
                            modifier = Modifier.padding(start = 16.dp, top = 6.dp)
                        )
                    }
                    DesignTool.Templates -> {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TemplateKind.values().forEach { tk ->
                                val active = selectedTemplate == tk
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) Fc.Accent else Fc.Raised)
                                        .clickable { selectedTemplate = tk }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        if (tk == TemplateKind.None) "Clear" else tk.name,
                                        style = FcType.BodySmall,
                                        color = if (active) Fc.Black else Fc.Title,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun BackdropChip(label: String, active: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Fc.Accent else Fc.Raised)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = FcType.BodySmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (!enabled) Fc.Detail else if (active) Fc.Black else Fc.Title,
        )
    }
}

@Composable
private fun ToolTab(label: String, glyph: Glyph, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Fc.Accent.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (active) Fc.Accent else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GlyphIcon(glyph, if (active) Fc.Accent else Fc.Title, size = 16.dp)
        Text(
            label,
            style = FcType.BodySmall,
            color = if (active) Fc.Accent else Fc.Title,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun DrawScope.drawSmoothPath(points: List<Offset>, color: Color, strokeWidth: Float) {
    if (points.size < 2) return
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        path.quadraticTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
    }
    drawPath(path, color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawVectorShape(shape: PlacedShape) {
    val cx = shape.center.x
    val cy = shape.center.y
    val w = shape.size.width
    val h = shape.size.height

    when (shape.kind) {
        ShapeKind.Rectangle -> {
            drawRect(shape.fillColor, topLeft = Offset(cx - w / 2, cy - h / 2), size = shape.size)
            drawRect(shape.strokeColor, topLeft = Offset(cx - w / 2, cy - h / 2), size = shape.size, style = Stroke(shape.strokeWidth))
        }
        ShapeKind.RoundedRect -> {
            drawRoundRect(shape.fillColor, topLeft = Offset(cx - w / 2, cy - h / 2), size = shape.size, cornerRadius = CornerRadius(24f, 24f))
            drawRoundRect(shape.strokeColor, topLeft = Offset(cx - w / 2, cy - h / 2), size = shape.size, cornerRadius = CornerRadius(24f, 24f), style = Stroke(shape.strokeWidth))
        }
        ShapeKind.Circle -> {
            val r = (w.coerceAtLeast(h)) / 2f
            drawCircle(shape.fillColor, radius = r, center = shape.center)
            drawCircle(shape.strokeColor, radius = r, center = shape.center, style = Stroke(shape.strokeWidth))
        }
        ShapeKind.Star -> {
            val starPath = Path()
            val outerR = w / 2f
            val innerR = outerR * 0.45f
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = (i * 36.0 - 90.0) * Math.PI / 180.0
                val px = (cx + r * cos(angle)).toFloat()
                val py = (cy + r * sin(angle)).toFloat()
                if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
            }
            starPath.close()
            drawPath(starPath, shape.fillColor)
            drawPath(starPath, shape.strokeColor, style = Stroke(shape.strokeWidth))
        }
        ShapeKind.Arrow -> {
            val p = Path()
            p.moveTo(cx - w / 2, cy - h / 6)
            p.lineTo(cx + w / 6, cy - h / 6)
            p.lineTo(cx + w / 6, cy - h / 2)
            p.lineTo(cx + w / 2, cy)
            p.lineTo(cx + w / 6, cy + h / 2)
            p.lineTo(cx + w / 6, cy + h / 6)
            p.lineTo(cx - w / 2, cy + h / 6)
            p.close()
            drawPath(p, shape.fillColor)
            drawPath(p, shape.strokeColor, style = Stroke(shape.strokeWidth))
        }
        ShapeKind.SpeechBubble -> {
            val p = Path()
            val left = cx - w / 2
            val top = cy - h / 2
            p.moveTo(left + 24f, top)
            p.lineTo(left + w - 24f, top)
            p.quadraticTo(left + w, top, left + w, top + 24f)
            p.lineTo(left + w, top + h - 24f)
            p.quadraticTo(left + w, top + h, left + w - 24f, top + h)
            p.lineTo(left + 48f, top + h)
            p.lineTo(left + 24f, top + h + 28f)
            p.lineTo(left + 28f, top + h)
            p.lineTo(left + 24f, top + h)
            p.quadraticTo(left, top + h, left, top + h - 24f)
            p.lineTo(left, top + 24f)
            p.quadraticTo(left, top, left + 24f, top)
            p.close()
            drawPath(p, shape.fillColor)
            drawPath(p, shape.strokeColor, style = Stroke(shape.strokeWidth))
        }
    }
}

private fun DrawScope.drawTemplatePreview(template: TemplateKind, size: Size, title: String, subtitle: String) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
    }

    when (template) {
        TemplateKind.LowerThird -> {
            val h = size.height * 0.14f
            val top = size.height * 0.76f
            val w = size.width * 0.88f
            val left = (size.width - w) / 2f

            drawRoundRect(
                Color(0xCC000000),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(16f, 16f),
            )
            drawRect(
                Color(0xFFE2C974),
                topLeft = Offset(left, top),
                size = Size(8f, h),
            )

            drawContext.canvas.nativeCanvas.apply {
                paint.textSize = size.width * 0.045f
                paint.isFakeBoldText = true
                drawText(title, left + 24f, top + h * 0.42f, paint)
                paint.textSize = size.width * 0.032f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.LTGRAY
                drawText(subtitle, left + 24f, top + h * 0.78f, paint)
            }
        }
        TemplateKind.CinematicQuote -> {
            val w = size.width * 0.85f
            val h = size.height * 0.35f
            val left = (size.width - w) / 2f
            val top = (size.height - h) / 2f

            drawRoundRect(
                Color(0xBB111827),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(20f, 20f),
            )
            drawRoundRect(
                Color(0xFFE2C974),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(3f),
            )

            drawContext.canvas.nativeCanvas.apply {
                paint.textSize = size.width * 0.05f
                paint.isFakeBoldText = true
                paint.textAlign = android.graphics.Paint.Align.CENTER
                drawText("“$title”", size.width / 2f, top + h * 0.45f, paint)
                paint.textSize = size.width * 0.032f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.LTGRAY
                drawText("— $subtitle", size.width / 2f, top + h * 0.72f, paint)
            }
        }
        TemplateKind.TitleCard -> {
            val top = size.height * 0.38f
            drawRect(Color(0xD9000000), topLeft = Offset(0f, top), size = Size(size.width, size.height * 0.24f))
            drawRect(Color(0xFFE2C974), topLeft = Offset(size.width * 0.1f, top), size = Size(size.width * 0.8f, 4f))

            drawContext.canvas.nativeCanvas.apply {
                paint.textSize = size.width * 0.065f
                paint.isFakeBoldText = true
                paint.textAlign = android.graphics.Paint.Align.CENTER
                drawText(title, size.width / 2f, top + size.height * 0.12f, paint)
                paint.textSize = size.width * 0.035f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.LTGRAY
                drawText(subtitle, size.width / 2f, top + size.height * 0.19f, paint)
            }
        }
        TemplateKind.SubscribeBadge -> {
            val w = size.width * 0.72f
            val h = size.height * 0.11f
            val left = (size.width - w) / 2f
            val top = size.height * 0.80f

            drawRoundRect(Color(0xFFFF0000), topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(h / 2f, h / 2f))

            drawContext.canvas.nativeCanvas.apply {
                paint.textSize = size.width * 0.045f
                paint.isFakeBoldText = true
                paint.textAlign = android.graphics.Paint.Align.CENTER
                drawText("SUBSCRIBE", size.width / 2f, top + h * 0.62f, paint)
            }
        }
        TemplateKind.None -> {}
    }
}

fun renderCanvasToBitmap(
    width: Int,
    height: Int,
    backdrop: ImageBitmap?,
    bgColor: Color,
    paths: List<DrawPath>,
    shapes: List<PlacedShape>,
    template: TemplateKind,
    title: String,
    subtitle: String,
): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    // Fill background
    if (bgColor != Color.Transparent) {
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (bgColor.alpha * 255).toInt(),
                (bgColor.red * 255).toInt(),
                (bgColor.green * 255).toInt(),
                (bgColor.blue * 255).toInt(),
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    }

    // Draw backdrop if present
    backdrop?.let {
        val srcBmp = it.asAndroidBitmap()
        canvas.drawBitmap(srcBmp, null, android.graphics.Rect(0, 0, width, height), null)
    }

    // Draw paths with Android Canvas
    val pathPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }

    paths.forEach { p ->
        pathPaint.color = android.graphics.Color.argb(
            (p.color.alpha * 255).toInt(),
            (p.color.red * 255).toInt(),
            (p.color.green * 255).toInt(),
            (p.color.blue * 255).toInt(),
        )
        pathPaint.strokeWidth = p.strokeWidth * (width.toFloat() / 360f)
        val ap = android.graphics.Path()
        if (p.points.size > 1) {
            val sx = width.toFloat() / 360f
            val sy = height.toFloat() / 640f
            ap.moveTo(p.points[0].x * sx, p.points[0].y * sy)
            for (i in 1 until p.points.size) {
                ap.lineTo(p.points[i].x * sx, p.points[i].y * sy)
            }
            canvas.drawPath(ap, pathPaint)
        }
    }

    return bmp
}
