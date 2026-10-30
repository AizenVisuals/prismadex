package dev.bloody.prismadex.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import dev.bloody.prismadex.data.Pokemon
import dev.bloody.prismadex.data.SavedTeam
import dev.bloody.prismadex.data.gameName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Shareable team card, rendered with the app's own identity (graphite + lilac,
 * existing type scale spirit) and the game name as PLAIN TEXT — no game logos,
 * no copied artwork beyond the bundled Pokémon art already shipped in the app.
 * Shares only what the user chose to share: team name, game, members, types.
 * No account, email, or device data leaves the phone.
 */
suspend fun shareTeamCard(context: Context, team: SavedTeam, catalog: List<Pokemon>) {
    val members = team.members.mapNotNull { id -> catalog.find { it.id == id } }
    require(members.isNotEmpty()) { "Equipe vazia." }
    val file = withContext(Dispatchers.IO) {
        val bitmap = renderTeamCard(context, team, members)
        val out = File(context.cacheDir, "prismadex-time-${team.id}.png")
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        out
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Esse é meu time Pokémon — ${team.name} (${gameName(team.gameId)}).")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    withContext(Dispatchers.Main) {
        context.startActivity(Intent.createChooser(intent, "Compartilhar time"))
    }
}

private fun renderTeamCard(context: Context, team: SavedTeam, members: List<Pokemon>): Bitmap {
    val width = 1080
    val height = 1420
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val graphite = 0xFF23202F.toInt()
    val cardSurface = 0xFF2E2A42.toInt()
    val lilac = 0xFFB7A6FF.toInt()
    val lilacSoft = 0xFFCFC8F5.toInt()
    val muted = 0xFF9A93B8.toInt()
    val white = 0xFFFFFFFF.toInt()

    fun paint(size: Float, bold: Boolean, color: Int, center: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        this.color = color
        textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
    }

    fun fit(p: Paint, text: String, maxWidth: Float): String {
        var t = text
        while (t.length > 1 && p.measureText(t) > maxWidth) t = t.dropLast(1)
        return if (t.length < text.length && t.isNotEmpty()) t.dropLast(1) + "…" else t
    }

    canvas.drawColor(graphite)
    canvas.drawRect(RectF(0f, 0f, width.toFloat(), 28f), Paint().apply { color = lilac })

    val centerX = width / 2f
    canvas.drawText("ESSE É MEU TIME POKÉMON", centerX, 118f, paint(46f, true, white, center = true))
    canvas.drawText(
        fit(paint(68f, true, white), team.name, width - 96f),
        centerX, 202f, paint(68f, true, white, center = true)
    )
    val formatLabel = if (team.format == "singles") "Individual" else "Duplas"
    canvas.drawText(
        "Jogo: ${gameName(team.gameId)} · $formatLabel",
        centerX, 262f, paint(38f, false, lilacSoft, center = true)
    )
    canvas.drawLine(64f, 300f, (width - 64).toFloat(), 300f, Paint().apply {
        color = lilac; strokeWidth = 3f
    })

    val margin = 32f
    val gap = 24f
    val cardW = (width - margin * 2 - gap) / 2f
    val cardH = 320f
    val top0 = 332f
    members.forEachIndexed { index, p ->
        val col = index % 2
        val row = index / 2
        val left = margin + col * (cardW + gap)
        val top = top0 + row * (cardH + gap)
        val rect = RectF(left, top, left + cardW, top + cardH)
        canvas.drawRoundRect(rect, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardSurface })

        val art = runCatching {
            context.assets.open("art/${p.id}.webp").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        val artSize = 180f
        val artLeft = left + (cardW - artSize) / 2f
        val artTop = top + 20f
        if (art != null) {
            val side = minOf(art.width, art.height)
            val src = Rect((art.width - side) / 2, (art.height - side) / 2, (art.width + side) / 2, (art.height + side) / 2)
            canvas.drawBitmap(art, src, RectF(artLeft, artTop, artLeft + artSize, artTop + artSize), null)
            art.recycle()
        } else {
            canvas.drawCircle(left + cardW / 2f, artTop + artSize / 2f, artSize / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lilac })
            canvas.drawText("?", left + cardW / 2f, artTop + artSize / 2f + 32f, paint(64f, true, graphite, center = true))
        }
        val namePaint = paint(40f, true, white, center = true)
        canvas.drawText(fit(namePaint, p.name, cardW - 32f), left + cardW / 2f, top + 262f, namePaint)
        val typesPaint = paint(30f, false, lilacSoft, center = true)
        canvas.drawText(
            p.types.joinToString(" · ") { typeNames[it] ?: it },
            left + cardW / 2f, top + 302f, typesPaint
        )
    }
    if (members.size < 6) {
        canvas.drawText(
            "${members.size}/6 parceiros",
            centerX, top0 + 3 * (cardH + gap) - gap + 8f, paint(30f, false, muted, center = true)
        )
    }
    canvas.drawText(
        "PrismaDex · feito no aparelho, sem nuvem",
        centerX, height - 28f, paint(28f, false, muted, center = true)
    )
    return bitmap
}
