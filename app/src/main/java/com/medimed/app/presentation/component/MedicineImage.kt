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
                contentDescription = null, 
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = com.medimed.app.R.drawable.ic_medicine_placeholder),
                contentDescription = null, 
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


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
            
            e.printStackTrace()
            null
        }
    }
}
