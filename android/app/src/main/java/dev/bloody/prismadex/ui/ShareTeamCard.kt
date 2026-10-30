package dev.bloody.prismadex.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.bloody.prismadex.data.Build
import dev.bloody.prismadex.data.Pokemon
import dev.bloody.prismadex.data.SavedTeam
import dev.bloody.prismadex.data.gameName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Shareable team card, rebuilt from the user's own reference design
 * (research/share-card-reference-2026-10-29.png): black background, 3x2 grid
 * of rounded cards, per card gender top-left / level top-right, art centred
 * in a soft circle tinted from the primary type, name, type icon(s),
 * ability icon + ability name at the bottom. Rendered DYNAMICALLY from the
 * saved team + the app's bundled art — the reference PNG is never pasted
 * or extracted.
 *
 * Field rules (user, 23:02): GENDER, LEVEL, NAME, TYPING, ABILITY come only
 * from the saved team. A missing field renders EMPTY — never a guessed
 * value, never a fallback default, never placeholder art/names. An empty
 * team renders six empty slots in the same grid; sharing is not blocked
 * with an error.
 *
 * What the saved team actually stores (data/Models.kt):
 * - NAME: Pokemon.name. TYPING: Pokemon.types (all of them, 1 or 2).
 * - ABILITY: Build.ability only, when the user chose one. A null ability
 *   is NOT filled from the calc default table or the species' first
 *   ability — that would present an unchosen ability as the member's.
 * - LEVEL: only Champions (gameId 25) fixes the level at 50
 *   ("nível 50", "IVs e nível não são editáveis neste modo",
 *   TrainingEditor.kt). Other games store no level, so the slot is empty.
 * - GENDER: no per-member gender is stored anywhere in SavedTeam/Build/
 *   Pokemon, and it cannot be derived truthfully from the species ratio.
 *   The top-left slot is therefore left blank. Adding real gender data
 *   needs a per-member gender field + a way to choose it; that is a
 *   data-model decision for the user, not something to invent here.
 * Shares only what the user chose to share: team name, game, members,
 * types, abilities. No account, email, or device data leaves the phone.
 * Game is PLAIN TEXT — no game logos.
 */

/** Level label for the card, or null when the team stores no level. */
internal fun cardLevelLabel(gameId: Int): String? =
    if (gameId == 25) "Nv. 50" else null

/** Ability label for the card: the chosen Build.ability only, else null (empty). */
internal fun cardAbilityLabel(build: Build?): String? =
    build?.ability?.takeIf { it.isNotBlank() }?.abilityDisplayName()

/** Same kebab-case -> Title Case formatting as TrainingEditor.abilityLabel. */
internal fun String.abilityDisplayName(): String =
    split('-').joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

internal fun typeDrawableRes(type: String): Int = when (type) {
    "bug" -> dev.bloody.prismadex.R.drawable.type_bug
    "dark" -> dev.bloody.prismadex.R.drawable.type_dark
    "dragon" -> dev.bloody.prismadex.R.drawable.type_dragon
    "electric" -> dev.bloody.prismadex.R.drawable.type_electric
    "fairy" -> dev.bloody.prismadex.R.drawable.type_fairy
    "fighting" -> dev.bloody.prismadex.R.drawable.type_fighting
    "fire" -> dev.bloody.prismadex.R.drawable.type_fire
    "flying" -> dev.bloody.prismadex.R.drawable.type_flying
    "ghost" -> dev.bloody.prismadex.R.drawable.type_ghost
    "grass" -> dev.bloody.prismadex.R.drawable.type_grass
    "ground" -> dev.bloody.prismadex.R.drawable.type_ground
    "ice" -> dev.bloody.prismadex.R.drawable.type_ice
    "normal" -> dev.bloody.prismadex.R.drawable.type_normal
    "poison" -> dev.bloody.prismadex.R.drawable.type_poison
    "psychic" -> dev.bloody.prismadex.R.drawable.type_psychic
    "rock" -> dev.bloody.prismadex.R.drawable.type_rock
    "steel" -> dev.bloody.prismadex.R.drawable.type_steel
    "water" -> dev.bloody.prismadex.R.drawable.type_water
    else -> dev.bloody.prismadex.R.drawable.type_normal
}

private fun blendArgb(from: Int, to: Int, ratioTo: Float): Int {
    val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * ratioTo).toInt()
    val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * ratioTo).toInt()
    val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * ratioTo).toInt()
    return Color.rgb(r, g, b)
}

suspend fun shareTeamCard(context: Context, team: SavedTeam, catalog: List<Pokemon>) {
    // Slot order is the team's member order; an id missing from the catalog
    // renders as an empty slot instead of shifting the others.
    val slots: List<Pokemon?> = team.members.take(6).map { id -> catalog.find { it.id == id } }
    val file = withContext(Dispatchers.IO) {
        val bitmap = renderTeamCard(context, team, slots)
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
        // Flags must be on the CHOOSER intent itself: LocalContext here is a
        // wrapper, not the Activity, so startActivity from it requires
        // FLAG_ACTIVITY_NEW_TASK (verified on device: without it the sheet
        // never opens and the error surfaces in the UI instead of crashing).
        val chooser = Intent.createChooser(intent, "Compartilhar time").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

private fun renderTeamCard(context: Context, team: SavedTeam, slots: List<Pokemon?>): Bitmap {
    val width = 1260
    val height = 1280
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val black = 0xFF000000.toInt()
    val white = 0xFFFFFFFF.toInt()
    val softWhite = 0xFFF2F0FA.toInt()
    val abilityColor = 0xFFD9D4EC.toInt()
    val muted = 0xFF9A93B8.toInt()
    val emptyFill = 0xFF121212.toInt()

    fun paint(size: Float, bold: Boolean, color: Int, center: Boolean = false) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.color = color
            textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
        }

    // Shrink-to-fit for names: never cut a name without first shrinking it.
    fun namePaintFor(text: String, maxWidth: Float): Paint {
        var size = 44f
        var p = paint(size, true, white, center = true)
        while (size > 28f && p.measureText(text) > maxWidth) {
            size -= 2f
            p = paint(size, true, white, center = true)
        }
        return p
    }

    fun fit(p: Paint, text: String, maxWidth: Float): String {
        var t = text
        while (t.length > 1 && p.measureText(t) > maxWidth) t = t.dropLast(1)
        return if (t.length < text.length && t.isNotEmpty()) t.dropLast(1) + "…" else t
    }

    canvas.drawColor(black)

    val margin = 36f
    val gap = 24f
    val cardW = (width - margin * 2 - gap * 2) / 3f
    val cardH = 560f
    val top0 = 36f
    val levelLabel = cardLevelLabel(team.gameId)

    for (index in 0 until 6) {
        val p = slots.getOrNull(index)
        val col = index % 3
        val row = index / 3
        val left = margin + col * (cardW + gap)
        val top = top0 + row * (cardH + gap)
        val rect = RectF(left, top, left + cardW, top + cardH)
        if (p == null) {
            // Empty slot: outline only. No "+", no text, no placeholder art.
            canvas.drawRoundRect(rect, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = emptyFill })
            canvas.drawRoundRect(
                rect, 40f, 40f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = muted; alpha = 90; style = Paint.Style.STROKE; strokeWidth = 2.5f
                }
            )
            continue
        }
        val typeArgb = typeColor(p.types.firstOrNull() ?: "normal").toArgb()
        val fill = blendArgb(typeArgb, 0xFF101010.toInt(), 0.80f)
        val circleColor = blendArgb(typeArgb, 0xFF000000.toInt(), 0.28f)
        canvas.drawRoundRect(rect, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
        canvas.drawRoundRect(
            rect, 40f, 40f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = white; alpha = 150; style = Paint.Style.STROKE; strokeWidth = 3f
            }
        )

        // Gender (top-left): no per-member gender is stored, so this corner
        // stays empty. The position is reserved by the layout, not filled
        // with a guessed symbol.
        // Level (top-right): only for Champions teams (fixed level 50).
        levelLabel?.let {
            canvas.drawText(it, left + cardW - 28f, top + 76f, paint(40f, false, softWhite).apply {
                textAlign = Paint.Align.RIGHT
            })
        }

        // Art in a soft tinted circle. Missing bundled art (the 4 known
        // IDs) leaves the circle empty — no "?" placeholder art.
        val cx = left + cardW / 2f
        val circleR = 120f
        val circleCy = top + 232f
        canvas.drawCircle(cx, circleCy, circleR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = circleColor })
        val art = runCatching {
            context.assets.open("art/${p.id}.webp").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (art != null) {
            val artSize = 258f
            val side = minOf(art.width, art.height)
            val src = Rect(
                (art.width - side) / 2, (art.height - side) / 2,
                (art.width + side) / 2, (art.height + side) / 2
            )
            canvas.drawBitmap(
                art, src,
                RectF(cx - artSize / 2f, circleCy - artSize / 2f, cx + artSize / 2f, circleCy + artSize / 2f),
                null
            )
            art.recycle()
        }

        // Name: shrink-to-fit, never swapped between members.
        val namePaint = namePaintFor(p.name, cardW - 32f)
        canvas.drawText(p.name, cx, top + 432f, namePaint)

        // Typing: ALL types, as the app's own vector type icons.
        val iconSize = 56f
        val iconGap = 20f
        val totalIcons = p.types.size * iconSize + (p.types.size - 1) * iconGap
        var iconLeft = cx - totalIcons / 2f
        val iconTop = top + 458f
        p.types.forEach { type ->
            val drawable = ContextCompat.getDrawable(context, typeDrawableRes(type))
            drawable?.let {
                it.setBounds(
                    iconLeft.toInt(), iconTop.toInt(),
                    (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt()
                )
                it.draw(canvas)
            }
            iconLeft += iconSize + iconGap
        }

        // Ability: chosen Build.ability only. Missing -> blank row.
        cardAbilityLabel(team.builds[p.id])?.let { ability ->
            val aPaint = paint(32f, false, abilityColor)
            val diamond = 20f
            val dGap = 12f
            val textW = aPaint.measureText(ability)
            val rowW = diamond + dGap + textW
            val rowLeft = cx - rowW / 2f
            val rowCy = top + 538f
            val diamondPath = Path().apply {
                moveTo(rowLeft + diamond / 2f, rowCy - diamond / 2f)
                lineTo(rowLeft + diamond, rowCy)
                lineTo(rowLeft + diamond / 2f, rowCy + diamond / 2f)
                lineTo(rowLeft, rowCy)
                close()
            }
            canvas.drawPath(
                diamondPath,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = abilityColor; style = Paint.Style.STROKE; strokeWidth = 3f
                }
            )
            canvas.drawText(ability, rowLeft + diamond + dGap, rowCy + 11f, aPaint)
        }
    }

    // Quiet footer below the grid (the one addition to the reference, so the
    // shared image is self-contained): team name + game as plain text +
    // format. The grid itself follows the reference exactly.
    val formatLabel = if (team.format == "singles") "Singles · individual" else "Doubles · duplas"
    val footerPaint = paint(30f, false, muted, center = true)
    canvas.drawText(
        fit(footerPaint, "${team.name} · ${gameName(team.gameId)} · $formatLabel", width - 72f),
        width / 2f, 1246f, footerPaint
    )
    return bitmap
}
