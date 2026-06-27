package com.medimed.app.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.medimed.app.presentation.theme.LocalDimens
import java.io.File

/**
 * Displays a medicine image from a local file path, falling back to a placeholder
 * icon if the image is missing, corrupt, or the path is null/blank.
 *
 * @param imagePath Absolute path to the image file on internal storage, or null.
 * @param modifier Standard compose modifier.
 * @param backgroundColor Background color shown behind/around the image.
 * @param medicineName Optional medicine name used for accessibility descriptions.
 *                     When provided, screen readers will announce "Image of [name]"
 *                     instead of the generic "Medicine image".
 */
@Composable
fun MedicineImage(
    imagePath: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    medicineName: String? = null
) {
    val bitmap = rememberBitmapFromPath(imagePath)
    val accessibilityLabel = if (medicineName != null) "Image of $medicineName" else "Medicine image"

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .semantics { contentDescription = accessibilityLabel },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null, // Described by parent Box semantics
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = com.medimed.app.R.drawable.ic_medicine_placeholder),
                contentDescription = null, // Described by parent Box semantics
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Remembers a decoded [ImageBitmap] from a file path. Returns null if:
 * - The path is null or blank
 * - The file does not exist (e.g., deleted or moved)
 * - The file is corrupt or cannot be decoded as a bitmap
 *
 * The result is cached by path so the same file is not decoded on every recomposition.
 */
@Composable
fun rememberBitmapFromPath(path: String?): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    return remember(path) {
        try {
            val file = File(path)
            if (file.exists() && file.canRead() && file.length() > 0) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            // Corrupt file, OOM, or IO error — gracefully fall back to placeholder
            e.printStackTrace()
            null
        }
    }
}
