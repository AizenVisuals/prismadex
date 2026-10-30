package dev.bloody.prismadex.data

/**
 * Trainer-card styles and backgrounds + trainer-sprite display names.
 *
 * Pure Kotlin (no Android/Compose dependency) so unit tests can cover it.
 *
 * STYLES and BACKGROUNDS are ORIGINAL to PrismaDex: layout ideas only
 * (name prominent, trainer sprite, 3x2 team grid, 8 badge slots, selectable
 * background, preview + image export) from the common trainer-card
 * convention described in template guides — no template graphics, fonts,
 * watermark or artwork is copied. Background colours reuse the app's own
 * Eeveelution theme palettes (see SettingsScreen.AppThemes, dark variants) plus a
 * plain graphite option; the "type" background is tinted from the card
 * team's first type colour at render time. The "padrao" background keeps
 * the previous look (app surface) for existing saved state.
 *
 * DISPLAY NAMES: the verified trainer manifest
 * (assets/trainers/credit-manifest.json) carries file keys + artist
 * credits, but NO canonical character display names. Names shown in the
 * picker are therefore deterministic formatting of the file key only
 * (e.g. "acerola-masters2" -> "Acerola Masters 2"), NOT verified
 * official character names. The picker footer says so; never present a
 * formatted key as a confirmed character name.
 */

/** Selectable card arrangements. */
data class TrainerCardStyle(val key: String, val name: String, val detail: String)

val TrainerCardStyles = listOf(
    TrainerCardStyle("classico", "Clássico", "Nome e sprite no topo, time 3×2, insígnias em 2 fileiras."),
    TrainerCardStyle("faixa", "Faixa", "Cabeçalho largo com sprite grande, time 3×2, insígnias numa fileira."),
    TrainerCardStyle("compacto", "Cartão", "Sprite grande centralizado, nome central, time 3×2, insígnias em 2 fileiras."),
)

val TrainerCardStyleKeys: Set<String> = TrainerCardStyles.map { it.key }.toSet()

/** Selectable card backgrounds. Colours are ARGB longs (0xAARRGGBB). */
data class TrainerCardBackground(val key: String, val name: String, val top: Long, val bottom: Long, val ink: Long)

val TrainerCardBackgrounds = listOf(
    TrainerCardBackground("padrao", "Padrão do app", 0xFF1A1C29L, 0xFF1A1C29L, 0xFFF2F0FAL),
    TrainerCardBackground("aurora", "Aurora", 0xFF2A2350L, 0xFF10121BL, 0xFFF2F0FAL),
    TrainerCardBackground("carmim", "Carmim", 0xFF4A2233L, 0xFF1B1017L, 0xFFF2F0FAL),
    TrainerCardBackground("oceano", "Oceano", 0xFF17394DL, 0xFF0B1720L, 0xFFF2F0FAL),
    TrainerCardBackground("jade", "Jade", 0xFF1F3D31L, 0xFF0C1915L, 0xFFF2F0FAL),
    TrainerCardBackground("ambar", "Âmbar", 0xFF4A3A1EL, 0xFF1B160EL, 0xFFF2F0FAL),
    TrainerCardBackground("perola", "Pérola", 0xFF33334AL, 0xFF15151BL, 0xFFF2F0FAL),
    TrainerCardBackground("escuro", "Grafite", 0xFF23232EL, 0xFF101014L, 0xFFF2F0FAL),
    TrainerCardBackground("tipo", "Cor do tipo", 0xFF1A1C29L, 0xFF1A1C29L, 0xFFF2F0FAL),
)

val TrainerCardBackgroundKeys: Set<String> = TrainerCardBackgrounds.map { it.key }.toSet()

/**
 * Background colours for a card. For "tipo", [typeArgb] (the first type's
 * ARGB colour, or null) tints the top of the gradient; the card never
 * renders without a background colour.
 */
fun trainerCardBackgroundColors(key: String, typeArgb: Long? = null): Triple<Long, Long, Long> {
    val base = TrainerCardBackgrounds.firstOrNull { it.key == key } ?: TrainerCardBackgrounds.first()
    if (key == "tipo" && typeArgb != null) {
        val r = (typeArgb shr 16) and 0xFF
        val g = (typeArgb shr 8) and 0xFF
        val b = typeArgb and 0xFF
        val tintedTop = 0xFF000000L + (r * 55 / 100 + 30) * 65536 + (g * 55 / 100 + 30) * 256 + (b * 55 / 100 + 30)
        return Triple(tintedTop, base.bottom, base.ink)
    }
    return Triple(base.top, base.bottom, base.ink)
}

/** Format a trainer file key for display. Deterministic key formatting only — see file header. */
fun trainerSpriteDisplayName(key: String): String {
    if (key.isBlank()) return ""
    return key.split("-").joinToString(" ") { segment ->
        val lower = segment.lowercase()
        when {
            lower.startsWith("gen") -> {
                val rest = lower.removePrefix("gen")
                val digits = rest.takeWhile { it.isDigit() }
                val suffix = rest.drop(digits.length).uppercase()
                "Gen $digits" + if (suffix.isNotEmpty()) " $suffix" else ""
            }
            lower.startsWith("masters") -> {
                val num = lower.removePrefix("masters")
                "Masters" + if (num.isNotEmpty()) " $num" else ""
            }
            lower == "lgpe" -> "LGPE"
            lower == "usum" -> "USUM"
            lower == "rse" -> "RSE"
            lower == "rb" -> "RB"
            lower == "xy" -> "XY"
            lower == "dp" -> "DP"
            lower == "rs" -> "RS"
            lower == "jp" -> "JP"
            lower == "v" -> "V"
            lower == "s" -> "S"
            else -> segment.replaceFirstChar { it.uppercaseChar() }
        }
    }.trim()
}

/** Search text for a sprite: display name + raw key, lowercased. */
fun trainerSpriteSearchText(key: String): String =
    (trainerSpriteDisplayName(key) + " " + key).lowercase()
