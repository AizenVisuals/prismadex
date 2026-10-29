/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Rounded.CatchingPokemon: ImageVector
    get() {
        if (_catchingPokemon != null) {
            return _catchingPokemon!!
        }
        _catchingPokemon = materialIcon(name = "Rounded.CatchingPokemon") {
            materialPath {
                moveTo(14.5f, 12.0f)
                curveToRelative(0.0f, 1.38f, -1.12f, 2.5f, -2.5f, 2.5f)
                curveToRelative(-1.38f, 0.0f, -2.5f, -1.12f, -2.5f, -2.5f)
                reflectiveCurveToRelative(1.12f, -2.5f, 2.5f, -2.5f)
                curveTo(13.38f, 9.5f, 14.5f, 10.62f, 14.5f, 12.0f)
                close()
                moveTo(22.0f, 12.0f)
                curveToRelative(0.0f, 5.52f, -4.48f, 10.0f, -10.0f, 10.0f)
                curveTo(6.48f, 22.0f, 2.0f, 17.52f, 2.0f, 12.0f)
                reflectiveCurveTo(6.48f, 2.0f, 12.0f, 2.0f)
                curveTo(17.52f, 2.0f, 22.0f, 6.48f, 22.0f, 12.0f)
                close()
                moveTo(20.0f, 12.0f)
                horizontalLineToRelative(-4.0f)
                curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f)
                curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                horizontalLineTo(4.0f)
                curveToRelative(0.0f, 4.41f, 3.59f, 8.0f, 8.0f, 8.0f)
                curveTo(16.41f, 20.0f, 20.0f, 16.41f, 20.0f, 12.0f)
                close()
            }
        }
        return _catchingPokemon!!
    }

private var _catchingPokemon: ImageVector? = null
