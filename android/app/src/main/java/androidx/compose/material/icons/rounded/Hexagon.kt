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

public val Icons.Rounded.Hexagon: ImageVector
    get() {
        if (_hexagon != null) {
            return _hexagon!!
        }
        _hexagon = materialIcon(name = "Rounded.Hexagon") {
            materialPath {
                moveTo(16.05f, 3.0f)
                horizontalLineTo(7.95f)
                curveTo(7.24f, 3.0f, 6.58f, 3.38f, 6.22f, 4.0f)
                lineToRelative(-4.04f, 7.0f)
                curveToRelative(-0.36f, 0.62f, -0.36f, 1.38f, 0.0f, 2.0f)
                lineToRelative(4.04f, 7.0f)
                curveToRelative(0.36f, 0.62f, 1.02f, 1.0f, 1.73f, 1.0f)
                horizontalLineToRelative(8.09f)
                curveToRelative(0.71f, 0.0f, 1.37f, -0.38f, 1.73f, -1.0f)
                lineToRelative(4.04f, -7.0f)
                curveToRelative(0.36f, -0.62f, 0.36f, -1.38f, 0.0f, -2.0f)
                lineToRelative(-4.04f, -7.0f)
                curveTo(17.42f, 3.38f, 16.76f, 3.0f, 16.05f, 3.0f)
                close()
            }
        }
        return _hexagon!!
    }

private var _hexagon: ImageVector? = null
