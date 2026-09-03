package com.amanansari.iykyk.ui.screen

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amanansari.iykyk.ui.component.SelectedVideoDialog
import com.amanansari.iykyk.ui.theme.Background
import com.amanansari.iykyk.ui.theme.IykykTheme
import com.amanansari.iykyk.ui.theme.Primary
import com.amanansari.iykyk.ui.theme.Secondary
import com.amanansari.iykyk.ui.theme.SurfaceContainerHigh
import com.amanansari.iykyk.ui.theme.SurfaceContainerLow
import com.amanansari.iykyk.ui.theme.SurfaceContainerMid
import com.amanansari.iykyk.ui.theme.Tertiary
import com.amanansari.iykyk.ui.theme.TextHighEmphasis
import com.amanansari.iykyk.ui.theme.TextMidEmphasis
import com.amanansari.iykyk.uriToFilename

@Composable
fun HomeScreen(
    onVideoSelected: (Uri) -> Unit = {}
) {

    val cardShape = RoundedCornerShape(48.dp)

    val infiniteTransition =
        rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )


    val interactionSource = remember {
        MutableInteractionSource()
    }

    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(200),
        label = "buttonScale"
    )

    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    /*
    * Launches Android's system Photo Picker, filtered to videos only.
    * No READ_MEDIA_VIDEO / storage permission needed - the picker grants
    * temporary, scoped access to whatever the user selects.
    * */

    val context = LocalContext.current

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->

        selectedUri = uri
    }


    selectedUri?.let { uri ->

        val videoName = uriToFilename(
            LocalContext.current,
            uri
        )

        SelectedVideoDialog(
            videoName = videoName ?: "Unknown video",
            onCancel = {
                selectedUri = null
            },
            onEdit = {
                pickVideoLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.VideoOnly
                    )
                )
            },
            onProceed = {
                onVideoSelected(uri)
            }
        )
    }



    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "Find Everyone in Your Video.",
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Pick a video and iykyk will find each person, count their appearances, and create a collage.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextMidEmphasis
            )
        }

        //> Main video selection card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                shape = cardShape,
                color = Color.Transparent,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SurfaceContainerHigh,
                                    SurfaceContainerMid
                                )
                            )
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    //> Video icon with pulse animation

                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {



                        // Pulse ring
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .scale(pulseScale)
                                .background(
                                    color = Primary.copy(alpha = pulseAlpha),
                                    shape = CircleShape
                                )
                        )

                        // Outer circle
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = SurfaceContainerHigh,
                            shadowElevation = 8.dp
                        ) {

                            Box(
                                contentAlignment = Alignment.Center
                            ) {

                                // Inner gradient circle
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Primary,
                                                    SurfaceContainerMid
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = "Select video",
                                        tint = TextHighEmphasis,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    //> Card title

                    Text(
                        text = "Select a video",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHighEmphasis
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    //> Card description

                    Text(
                        text = "Choose a portrait video from your device",
                        fontSize = 12.sp,
                        color = TextMidEmphasis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    //> Choose Video button

                    Button(
                        onClick = {
                            pickVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .scale(buttonScale)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape
                            ),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = TextHighEmphasis
                        ),

                        interactionSource = interactionSource,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = TextHighEmphasis,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = "Choose Video",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextHighEmphasis
                            )
                        }
                    }
                }
            }
        }

        item {
            //> -----------------------------------------
            //> Privacy badge
            //> -----------------------------------------

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ){
                Surface(

                    shape = CircleShape,
                    color = SurfaceContainerLow,

                    ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "✓",
                            color = Secondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "Processed entirely on your device",
                            color = TextHighEmphasis,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "·",
                            color = TextMidEmphasis,
                            fontSize = 11.sp
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "100% PRIVATE",
                            color = TextMidEmphasis,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(32.dp)
            )

            //> -----------------------------------------
            //> How it works
            //> -----------------------------------------

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "HOW IT WORKS",
                        color = Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "ON-DEVICE",
                        color = TextMidEmphasis,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                // Step 01
                HowItWorksItem(
                    number = "01",
                    title = "Detect",
                    description = "Find faces in the video.",
                    accentColor = Primary
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                // Step 02
                HowItWorksItem(
                    number = "02",
                    title = "Recognize",
                    description = "Group appearances belonging to the same person.",
                    accentColor = Secondary
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                // Step 03
                HowItWorksItem(
                    number = "03",
                    title = "Create",
                    description = "Select the best shots and build the collage.",
                    accentColor = Tertiary
                )
            }
        }
    }
}

@Composable
private fun HowItWorksItem(
    number: String,
    title: String,
    description: String,
    accentColor: Color
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerMid
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Number / icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = number,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = number,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        text = title,
                        color = TextHighEmphasis,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = description,
                    color = TextMidEmphasis,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {

    IykykTheme {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background
        ) {

            HomeScreen()
        }
    }
}