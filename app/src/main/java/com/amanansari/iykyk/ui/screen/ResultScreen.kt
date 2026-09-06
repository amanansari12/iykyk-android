package com.amanansari.iykyk.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.amanansari.iykyk.data.model.PersonResult
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
import com.amanansari.iykyk.ui.viewmodel.ProcessingViewModel
import kotlinx.coroutines.delay
import androidx.core.graphics.createBitmap

@Composable
fun ResultScreen(
    onCancel: () -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel()

) {

    val cancelAndReturnHome = {
        viewModel.cancelProcessing()
        onCancel()
    }

    BackHandler {
        cancelAndReturnHome()
    }



    ResultScreenContent(
        personResults = viewModel.personResults,
        collageBitmap = viewModel.collageBitmap,
        videoDurationMs = viewModel.videoMetadata?.durationMs ?: 0L,
        onSaveToGallery = {
            // TODO: MediaStore save — wire when we build export logic
        },
        onShare = {
            // TODO: ACTION_SEND intent — wire when we build export logic
        }
    )
}

@Composable
fun ResultScreenContent(
    personResults: List<PersonResult>,
    collageBitmap: Bitmap?,
    videoDurationMs: Long,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit
) {
    val sortedPeople = remember(personResults) {
        personResults.sortedBy { it.clusterId }
    }

    val totalAppearances = remember(sortedPeople) {
        sortedPeople.sumOf { it.appearanceCount }
    }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showFullscreenPreview by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200)
            toastMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {

            item { Spacer(modifier = Modifier.height(4.dp)) }

            //> -----------------------------------------
            //> Header block
            //> -----------------------------------------
            item {
                HeaderBlock(
                    peopleCount = sortedPeople.size,
                    totalAppearances = totalAppearances,
                    videoDurationMs = videoDurationMs
                )
            }

            //> -----------------------------------------
            //> Detected faces carousel
            //> -----------------------------------------
            item {
                DetectedFacesSection(people = sortedPeople)
            }

            //> -----------------------------------------
            //> Collage preview
            //> -----------------------------------------
            item {
                CollagePreviewSection(
                    collageBitmap = collageBitmap,
                    onExpandClick = { showFullscreenPreview = true }
                )
            }

            //> -----------------------------------------
            //> Inference summary
            //> -----------------------------------------
            item {
                InferenceSummaryCard(
                    identities = sortedPeople.size,
                    moments = totalAppearances
                )
            }

            // Room for the sticky bottom bar so the last card isn't hidden behind it
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }

        BottomActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            onSaveClick = {
                onSaveToGallery()
                toastMessage = "Saved to Photos"
            },
            onShareClick = {
                onShare()
                toastMessage = "Opening share sheet"
            }
        )

        toastMessage?.let { message ->
            ToastBubble(
                message = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 92.dp)
            )
        }
    }

    if (showFullscreenPreview && collageBitmap != null) {
        FullscreenCollageDialog(
            bitmap = collageBitmap,
            onDismiss = { showFullscreenPreview = false }
        )
    }
}

//> -----------------------------------------
//> Header block
//> -----------------------------------------

@Composable
private fun HeaderBlock(
    peopleCount: Int,
    totalAppearances: Int,
    videoDurationMs: Long
) {
    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                shape = CircleShape,
                color = SurfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% ON-DEVICE",
                        color = Secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextMidEmphasis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDuration(videoDurationMs),
                    color = TextMidEmphasis,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your people",
            color = TextHighEmphasis,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "$peopleCount unique ${if (peopleCount == 1) "person" else "people"} found across $totalAppearances appearances",
            color = TextMidEmphasis,
            fontSize = 14.sp
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

//> -----------------------------------------
//> Detected faces carousel
//> -----------------------------------------

@Composable
private fun DetectedFacesSection(people: List<PersonResult>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DETECTED FACES",
                    color = Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = SurfaceContainerHigh
                ) {
                    Text(
                        text = "${people.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = TextHighEmphasis,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = "SWIPE",
                color = TextMidEmphasis,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(people, key = { it.clusterId }) { person ->
                val personNumber = people.indexOf(person) + 1
                PersonCard(person = person, personNumber = personNumber)
            }
        }
    }
}

@Composable
private fun PersonCard(person: PersonResult, personNumber: Int) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, HairlineChrome, RoundedCornerShape(20.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerHigh)
        ) {
            Image(
                bitmap = person.representativeFace.asImageBitmap(),
                contentDescription = "Person $personNumber",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            CountBadge(
                count = person.appearanceCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
                text = "Person %02d".format(personNumber),
                color = TextHighEmphasis,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "${person.appearanceCount} appearances",
                color = TextMidEmphasis,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Primary
    ) {
        Text(
            text = "×$count",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = TextHighEmphasis,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

//> -----------------------------------------
//> Collage preview
//> -----------------------------------------

@Composable
private fun CollagePreviewSection(
    collageBitmap: Bitmap?,
    onExpandClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Your collage",
                    color = TextHighEmphasis,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "One moment for everyone.",
                    color = TextMidEmphasis,
                    fontSize = 13.sp
                )
            }

            Surface(
                shape = CircleShape,
                color = SurfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Story Ready (9:16)",
                        color = Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Primary.copy(alpha = 0.35f),
                    spotColor = Primary.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, HairlineChrome, RoundedCornerShape(28.dp))
        ) {

            if (collageBitmap != null) {
                Image(
                    bitmap = collageBitmap.asImageBitmap(),
                    contentDescription = "Generated collage",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "Collage not ready",
                    modifier = Modifier.align(Alignment.Center),
                    color = TextMidEmphasis,
                    fontSize = 13.sp
                )
            }

            // Watermark badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = CircleShape,
                color = SurfaceContainerHigh.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "IYKYK",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Primary.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Expand affordance
            IconButton(
                onClick = onExpandClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fullscreen,
                    contentDescription = "Expand collage",
                    tint = TextHighEmphasis
                )
            }
        }
    }
}

@Composable
private fun FullscreenCollageDialog(bitmap: Bitmap, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Collage fullscreen preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

//> -----------------------------------------
//> Inference summary
//> -----------------------------------------

@Composable
private fun InferenceSummaryCard(identities: Int, moments: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceContainerMid)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INFERENCE SUMMARY",
                color = TextMidEmphasis,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Secondary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zero cloud sync",
                    color = Secondary,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(value = "$identities", label = "Identities", valueColor = Primary, modifier = Modifier.weight(1f))
            StatTile(value = "$moments", label = "Moments", valueColor = Secondary, modifier = Modifier.weight(1f))
            StatTile(value = "100%", label = "Private", valueColor = TextHighEmphasis, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainerHigh)
            .padding(12.dp)
    ) {
        Text(text = value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextMidEmphasis, fontSize = 12.sp)
    }
}

//> -----------------------------------------
//> Sticky bottom actions + toast
//> -----------------------------------------

@Composable
private fun BottomActionBar(
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = SurfaceContainerHigh,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = onSaveClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceContainerLow,
                    contentColor = TextHighEmphasis
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save to Gallery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onShareClick,
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

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    val dummyBitmap = createBitmap(200, 250)

    val dummyPeople = listOf(
        PersonResult(clusterId = 1, appearanceCount = 4, representativeFace = dummyBitmap),
        PersonResult(clusterId = 2, appearanceCount = 3, representativeFace = dummyBitmap),
        PersonResult(clusterId = 3, appearanceCount = 5, representativeFace = dummyBitmap)
    )

    IykykTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            ResultScreenContent(
                personResults = dummyPeople,
                collageBitmap = dummyBitmap,
                videoDurationMs = 30_000L,
                onSaveToGallery = {},
                onShare = {}
            )
        }
    }
}