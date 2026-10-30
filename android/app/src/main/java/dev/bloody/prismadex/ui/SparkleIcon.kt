package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The detail star stays yellow (10:50): the sparkle shape is drawn in
 *  this yellow; the active/inactive states differ only in opacity. */
internal val ShinyYellow = Color(0xFFFFC107)

/** 4-point sparkle from the 10:50 reference: concave-sided star, drawn as
 *  a vector so it stays sharp at any size. Same position/behavior as the
 *  old 5-point star; only the shape and quality change. */
internal val SparkleStar: ImageVector = ImageVector.Builder("sparkle_star", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
        moveTo(12f, 1.5f)
        quadTo(13.8f, 10.2f, 22.5f, 12f)
        quadTo(13.8f, 13.8f, 12f, 22.5f)
        quadTo(10.2f, 13.8f, 1.5f, 12f)
        quadTo(10.2f, 10.2f, 12f, 1.5f)
        close()
    }
}.build()

/** Stroke-only variant of the exact same 4-point path: transparent fill.
 *  The tint passed at the call site recolours the stroke, so the same
 *  vector draws the white halo and the soft charcoal outline. Stroke is
 *  1.7 (was 2.4): thinner, so the button reads lighter, not heavy black. */
internal val SparkleStarOutline: ImageVector = ImageVector.Builder("sparkle_star_outline", 24.dp, 24.dp, 24f, 24f).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(12f, 1.5f)
        quadTo(13.8f, 10.2f, 22.5f, 12f)
        quadTo(13.8f, 13.8f, 12f, 22.5f)
        quadTo(10.2f, 13.8f, 1.5f, 12f)
        quadTo(10.2f, 10.2f, 12f, 1.5f)
        close()
    }
}.build()

/** Soft outline + halo colours for the shiny toggle (13:49, user:
 * "muito preto"). Outline is charcoal gray 0xFF40464E, NOT pure black,
 * with a thin 1.7 stroke; the halo is solid white at 25.5dp, only just
 * larger than the 24dp outline, so it separates the stroke from dark
 * type-colour cards without a hard white ring. No alpha blends. */
private val SoftSparkleOutline = Color(0xFF40464E)
private val SoftSparkleHalo = Color.White

/**
 * Shiny toggle icon (13:40 user: outline + colour when clicked; 13:49
 * user: too black, soften it). Inactive: SOFT charcoal outline only
 * (transparent fill) over a slightly larger solid-white halo, so the
 * stroke reads on dark type-colour cards and on light backgrounds
 * alike without looking heavy black. Active: YELLOW (#FFC107) fill
 * with the same soft charcoal outline on top. The uploaded
 * sparkle-duotone.svg was checked as a shape reference only; no path
 * was imported - both vectors are the app's own 10:50 sparkle path.
 * Same 48dp IconButton target and same toggle behavior at every call
 * site; only the icon rendering changes.
 */
@Composable
internal fun ShinySparkleIcon(active: Boolean, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (active) {
            LIcon(SparkleStar, contentDescription, tint = ShinyYellow, modifier = Modifier.size(24.dp))
            LIcon(SparkleStarOutline, null, tint = SoftSparkleOutline, modifier = Modifier.size(24.dp))
        } else {
            LIcon(SparkleStarOutline, null, tint = SoftSparkleHalo, modifier = Modifier.size(25.5.dp))
            LIcon(SparkleStarOutline, contentDescription, tint = SoftSparkleOutline, modifier = Modifier.size(24.dp))
        }
    }
}
