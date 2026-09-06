package com.amanansari.iykyk.ui.screen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.amanansari.iykyk.data.local.SavedCollageEntity
import com.amanansari.iykyk.ui.theme.Background
import com.amanansari.iykyk.ui.theme.HairlineChrome
import com.amanansari.iykyk.ui.theme.IykykTheme
import com.amanansari.iykyk.ui.theme.Primary
import com.amanansari.iykyk.ui.theme.Secondary
import com.amanansari.iykyk.ui.theme.SurfaceContainerHigh
import com.amanansari.iykyk.ui.theme.SurfaceContainerLow
import com.amanansari.iykyk.ui.theme.SurfaceContainerMid
import com.amanansari.iykyk.ui.theme.TextHighEmphasis
import com.amanansari.iykyk.ui.theme.TextMidEmphasis
import com.amanansari.iykyk.ui.viewmodel.SavedCollagesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SavedCollagesScreen(
    viewModel: SavedCollagesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val savedCollages by viewModel.savedCollages.collectAsState()

    SavedCollagesContent(
        savedCollages = savedCollages,
        onDelete = { collage -> viewModel.deleteCollage(collage) },
        onShare = { collage ->
            val uri = viewModel.getShareableUri(collage) ?: return@SavedCollagesContent false
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share your collage"))
            true
        }
    )
}

@Composable
fun SavedCollagesContent(
    savedCollages: List<SavedCollageEntity>,
    onDelete: (SavedCollageEntity) -> Unit,
    onShare: (SavedCollageEntity) -> Boolean
) {
    var previewCollage by remember { mutableStateOf<SavedCollageEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<SavedCollageEntity?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200.milliseconds)
            toastMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        if (savedCollages.isEmpty()) {
            EmptyLibraryState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = "Your collages",
                            color = TextHighEmphasis,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${savedCollages.size} saved on this device",
                            color = TextMidEmphasis,
                            fontSize = 13.sp
                        )
                    }
                }

                items(savedCollages, key = { it.id }) { collage ->
                    SavedCollageCard(
                        collage = collage,
                        onClick = { previewCollage = collage },
                        onDeleteClick = { pendingDelete = collage }
                    )
                }
            }
        }

        toastMessage?.let { message ->
            ToastBubble(
                message = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }

    previewCollage?.let { collage ->
        FullscreenSavedCollageDialog(
            collage = collage,
            onDismiss = { previewCollage = null },
            onShare = {
                if (onShare(collage)) {
                    toastMessage = "Opening share sheet"
                }
            },
            onDeleteClick = {
                previewCollage = null
                pendingDelete = collage
            }
        )
    }

    pendingDelete?.let { collage ->
        DeleteConfirmationDialog(
            onConfirm = {
                onDelete(collage)
                pendingDelete = null
                toastMessage = "Collage deleted"
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

//> -----------------------------------------
//> Empty state
//> -----------------------------------------

@Composable
private fun EmptyLibraryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceContainerMid),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CollectionsBookmark,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = TextMidEmphasis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No collage saved",
            color = TextHighEmphasis,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Collages you save from the Results screen will show up here.",
            color = TextMidEmphasis,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

//> -----------------------------------------
//> Grid card
//> -----------------------------------------

@Composable
private fun SavedCollageCard(
    collage: SavedCollageEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val bitmap = rememberBitmapFromFile(collage.filePath)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, HairlineChrome, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerHigh)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Saved collage",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete collage",
                    modifier = Modifier.size(16.dp),
                    tint = TextHighEmphasis
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
                text = "${collage.identityCount} ${if (collage.identityCount == 1) "person" else "people"}",
                color = TextHighEmphasis,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = formatSavedDate(collage.createdAt),
                color = TextMidEmphasis,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

//> -----------------------------------------
//> Fullscreen preview + share/delete
//> -----------------------------------------

@Composable
private fun FullscreenSavedCollageDialog(
    collage: SavedCollageEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val bitmap = rememberBitmapFromFile(collage.filePath)

    Dialog(onDismissRequest = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainerLow)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Collage fullscreen preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextHighEmphasis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = SurfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerLow,
                            contentColor = TextHighEmphasis
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onShare,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = TextHighEmphasis
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

//> -----------------------------------------
//> Delete confirmation
//> -----------------------------------------

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerMid,
        titleContentColor = TextHighEmphasis,
        textContentColor = TextMidEmphasis,
        title = { Text("Delete this collage?") },
        text = {
            Text(
                "This only removes it from your in-app Saved library. " +
                        "Any copy already saved to your gallery is not affected."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Secondary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMidEmphasis)
            }
        }
    )
}

//> -----------------------------------------
//> Toast
//> -----------------------------------------

@Composable
private fun ToastBubble(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = SurfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message, color = TextHighEmphasis, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

//> -----------------------------------------
//> Helpers
//> -----------------------------------------

@Composable
private fun rememberBitmapFromFile(filePath: String): Bitmap? {
    val state = produceState<Bitmap?>(initialValue = null, filePath) {
        value = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(filePath)
            } catch (e: Exception) {
                null
            }
        }
    }
    return state.value
}

private fun formatSavedDate(timestampMs: Long): String {
    val formatter = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}

@Preview(showBackground = true)
@Composable
private fun SavedCollagesEmptyPreview() {
    IykykTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            SavedCollagesContent(
                savedCollages = emptyList(),
                onDelete = {},
                onShare = { true }
            )
        }
    }
}
