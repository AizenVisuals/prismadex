package dev.bloody.prismadex.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import dev.bloody.prismadex.data.Pokemon
import dev.bloody.prismadex.data.TrainerCardBackgroundKeys
import dev.bloody.prismadex.data.UserState
import dev.bloody.prismadex.data.gameName
import dev.bloody.prismadex.data.trainerCardBackgroundColors
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trainer card as a shareable image (10:28, item 1).
 *
 * Same sharing pattern as [shareTeamCard]: render to a PNG in the app
 * cache, share through FileProvider. Only saved local data is drawn:
 * trainer name, chosen sprite, chosen team's members (bundled art),
 * registered badge images for that team's game, chosen style and
 * background. A missing sprite, art or badge file renders as an empty
 * slot — never invented art. No template graphics, fonts or watermark
 * are copied: layout follows the app's own Compose card (name, sprite,
 * 3x2 team grid, badge rows) and the background palettes are the app's
 * own theme colours (see TrainerCardDesign.kt). The "padrao" background
 * uses the fixed dark surface colours here because a bitmap has no
 * Material theme; the on-screen preview uses the live surface colour.
 */

suspend fun shareTrainerCardImage(context: Context, state: UserState, catalog: List<Pokemon>) {
    val file = withContext(Dispatchers.IO) {
        val bitmap = renderTrainerCardImage(context, state, catalog)
        val out = File(context.cacheDir, "prismadex-trainer-card.png")
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        out
    }
    val name = state.trainerName.ifBlank { "Cartão de treinador" }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Esse é meu cartão de treinador — $name.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    withContext(Dispatchers.Main) {
        val chooser = Intent.createChooser(intent, "Compartilhar cartão de treinador").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

internal fun renderTrainerCardImage(context: Context, state: UserState, catalog: List<Pokemon>): Bitmap {
    val width = 1080
    val cardTeam = state.cardTeam
    val members = cardTeam.members.mapNotNull { id -> catalog.find { it.id == id } }
    val badgeSet = state.cardBadgeSet(cardTeam.gameId)
    val hasKanto = badgeSet != null && cardTeam.gameId in setOf(3, 4, 10)
    val style = if (state.trainerCardStyle in setOf("classico", "faixa", "compacto")) state.trainerCardStyle else "classico"
    val background = if (state.trainerCardBackground in TrainerCardBackgroundKeys) state.trainerCardBackground else "padrao"
    val firstTypeArgb = members.firstOrNull()?.types?.firstOrNull()?.let { typeColor(it).toArgb().toLong() }
    val (bgTopLong, bgBottomLong, bgInkLong) = trainerCardBackgroundColors(background, firstTypeArgb)
    val bgTop = bgTopLong.toInt()
    val bgBottom = bgBottomLong.toInt()
    val ink = bgInkLong.toInt()
    val sub = Color.argb(204, Color.red(ink), Color.green(ink), Color.blue(ink))
    val cellFill = Color.argb(26, 255, 255, 255)
    val height = if (hasKanto) 1920 else 1560
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background: vertical gradient + faint circle motif (same motif as the
    // on-screen card texture, drawn with the app's own primitives).
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, height.toFloat(), bgTop, bgBottom, Shader.TileMode.CLAMP)
    }
    canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), bgPaint)
    if (background != "padrao") {
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(33, 255, 255, 255) }
        canvas.drawCircle(width * .80f, 130f, 180f, circlePaint)
        canvas.drawCircle(width * .80f, 130f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgTop })
    }

    fun paint(size: Float, bold: Boolean, color: Int, center: Boolean = false) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.color = color
            textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
        }

    fun shrinkToFit(text: String, maxWidth: Float, startSize: Float, bold: Boolean, color: Int, center: Boolean = false): Paint {
        var size = startSize
        var p = paint(size, bold, color, center)
        while (size > 28f && p.measureText(text) > maxWidth) {
            size -= 4f
            p = paint(size, bold, color, center)
        }
        return p
    }

    val nameText = if (state.trainerName.isNotBlank()) state.trainerName else "Cartão de treinador"
    val sprite = runCatching {
        if (state.trainerSprite.isBlank()) null
        else context.assets.open("trainers/${state.trainerSprite}.png").use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
    // Pixel-art sprite: upscale without filtering so it stays crisp.
    val spritePaint = Paint().apply { isFilterBitmap = false }
    fun drawSprite(left: Float, top: Float, size: Float) {
        val bmp = sprite ?: return
        canvas.drawBitmap(bmp, null, RectF(left, top, left + size, top + size), spritePaint)
    }

    val pad = 72f
    var gridTop = 400f
    when (style) {
        "faixa" -> {
            drawSprite(pad, 110f, 280f)
            val textLeft = pad + 280f + 36f
            val maxW = width - pad - textLeft
            val namePaint = shrinkToFit(nameText, maxW, 88f, true, ink)
            canvas.drawText(nameText, textLeft, 250f, namePaint)
            canvas.drawText(gameName(cardTeam.gameId), textLeft, 312f, paint(46f, false, sub))
            canvas.drawText(cardTeam.name, textLeft, 364f, paint(46f, false, sub))
            gridTop = 470f
        }
        "compacto" -> {
            drawSprite((width - 300f) / 2f, 100f, 300f)
            val namePaint = shrinkToFit(nameText, width - pad * 2, 88f, true, ink, center = true)
            canvas.drawText(nameText, width / 2f, 480f, namePaint)
            canvas.drawText("${gameName(cardTeam.gameId)} · ${cardTeam.name}", width / 2f, 538f, paint(46f, false, sub, center = true))
            gridTop = 610f
        }
        else -> {
            val maxW = width - pad * 2 - 240f - 36f
            val namePaint = shrinkToFit(nameText, maxW, 84f, true, ink)
            canvas.drawText(nameText, pad, 212f, namePaint)
            canvas.drawText(gameName(cardTeam.gameId), pad, 272f, paint(46f, false, sub))
            canvas.drawText(cardTeam.name, pad, 324f, paint(46f, false, sub))
            drawSprite(width - pad - 240f, 110f, 240f)
            gridTop = 400f
        }
    }

    // Team grid 3x2: bundled art drawn Fit inside each cell; missing art
    // renders as an outlined empty slot, never a substitute image.
    val gap = 28f
    val cell = (width - pad * 2 - gap * 2) / 3f
    for (row in 0 until 2) {
        for (col in 0 until 3) {
            val left = pad + col * (cell + gap)
            val top = gridTop + row * (cell + gap)
            val rect = RectF(left, top, left + cell, top + cell)
            canvas.drawRoundRect(rect, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cellFill })
            val p = members.getOrNull(row * 3 + col)
            if (p != null) {
                val artPath = if (cardTeam.builds[p.id]?.shiny == true) "art/shiny/${p.id}.webp" else "art/${p.id}.webp"
                val art = runCatching {
                    context.assets.open(artPath).use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
                if (art != null) {
                    val scale = minOf(cell / art.width, cell / art.height)
                    val dw = art.width * scale
                    val dh = art.height * scale
                    canvas.drawBitmap(
                        art, null,
                        RectF(left + (cell - dw) / 2f, top + (cell - dh) / 2f, left + (cell + dw) / 2f, top + (cell + dh) / 2f),
                        Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
                    )
                    art.recycle()
                } else {
                    canvas.drawRoundRect(rect, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = sub; alpha = 90; setStyle(Paint.Style.STROKE); strokeWidth = 3f
                    })
                }
            }
        }
    }
    sprite?.recycle()

    // Badges: images only, no names. Earned at full colour; unearned
    // greyscale + dimmed. Missing file: outlined empty slot.
    var badgesTop = gridTop + cell * 2 + gap + 66f
    fun drawBadgeRow(prefix: String, earned: Set<Int>, top: Float, cellSize: Float, gapPx: Float, count: Int, startIndex: Int = 0) {
        val totalW = count * cellSize + (count - 1) * gapPx
        var left = (width - totalW) / 2f
        for (index in 0 until count) {
            val rect = RectF(left, top, left + cellSize, top + cellSize)
            canvas.drawRoundRect(rect, 28f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cellFill })
            val badgeIndex = startIndex + index
            val won = badgeIndex in earned
            val badge = runCatching {
                context.assets.open("badges/${prefix}-${badgeIndex + 1}.png").use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
            if (badge != null) {
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    isFilterBitmap = true
                    if (!won) {
                        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                        alpha = 115
                    }
                }
                val inset = cellSize * .14f
                canvas.drawBitmap(badge, null, RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset), badgePaint)
                badge.recycle()
            }
            left += cellSize + gapPx
        }
    }
    if (badgeSet != null) {
        val earned = state.cardBadgeIndices(cardTeam.gameId)
        canvas.drawText("INSÍGNIAS", pad, badgesTop, paint(36f, true, ink).apply { letterSpacing = .12f })
        val rowTop = badgesTop + 30f
        if (style == "faixa") {
            drawBadgeRow(badgeSet.prefix, earned, rowTop, 99f, 20f, 8)
            badgesTop = rowTop + 99f
        } else {
            drawBadgeRow(badgeSet.prefix, earned, rowTop, 150f, 24f, 4)
            drawBadgeRow(badgeSet.prefix, earned, rowTop + 150f + 24f, 150f, 24f, 4, startIndex = 4)
            badgesTop = rowTop + 150f * 2 + 24f
        }
        if (hasKanto) {
            canvas.drawText("KANTO · PÓS-JOGO", pad, badgesTop + 58f, paint(36f, true, ink).apply { letterSpacing = .12f })
            drawBadgeRow("kanto", state.kantoPostgameBadges[cardTeam.gameId].orEmpty(), badgesTop + 88f, 150f, 24f, 4)
            drawBadgeRow("kanto", state.kantoPostgameBadges[cardTeam.gameId].orEmpty(), badgesTop + 88f + 174f, 150f, 24f, 4, startIndex = 4)
        }
    } else {
        canvas.drawText("Sem conjunto de insígnias com imagens para este jogo.", pad, badgesTop, paint(40f, false, sub))
    }
    canvas.drawText("PrismaDex", width / 2f, height - 36f, paint(32f, false, sub, center = true))
    return bitmap
}
