package dev.bloody.prismadex.ui

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** True when the system "Remove animations" setting is on (animator scale 0). Read once per launch. */
internal val LocalReduceMotion = compositionLocalOf { false }

@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/** Spatial reveal only when motion is allowed; a short fade always carries the state change. */
internal fun expandEnter(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) fadeIn(tween(120))
    else expandVertically(tween(220)) + fadeIn()

internal fun shrinkExit(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) fadeOut(tween(120))
    else shrinkVertically(tween(160)) + fadeOut()

/** Press-feedback target: no scale travel when motion is reduced. */
internal fun pressTarget(pressed: Boolean, reduceMotion: Boolean, amount: Float): Float =
    if (pressed && !reduceMotion) amount else 1f

/** Placement animation for lazy items; skipped when motion is reduced. */
internal fun LazyItemScope.motionPlacement(reduceMotion: Boolean): Modifier =
    if (reduceMotion) Modifier else Modifier.animateItem()

internal fun LazyGridItemScope.motionPlacement(reduceMotion: Boolean): Modifier =
    if (reduceMotion) Modifier else Modifier.animateItem()
