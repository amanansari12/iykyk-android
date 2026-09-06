package com.amanansari.iykyk.ui.screen

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.amanansari.iykyk.data.model.ProcessingPhase
import com.amanansari.iykyk.data.model.ProcessingUiState
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
import kotlin.time.Duration.Companion.milliseconds

private val ProcessingWarning = Color(0xFFD78203)

@Composable
fun ProcessingScreen(
    uri: Uri,
    onCancel: () -> Unit,
    onCompleted: () -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel()
) {

    val cancelAndReturnHome = {
        viewModel.cancelProcessing()
        onCancel()
    }

    /*
     * Navigate to Results when processing completes.
     */
    LaunchedEffect(
        viewModel.processingUiState.isCompleted
    ) {
        if (viewModel.processingUiState.isCompleted) {
            onCompleted()
        }
    }

    /*
     * Automatic failure countdown.
     *
     * When Face Detection or Face Embeddings produces zero results,
     * the ViewModel sets isProcessFailed = true and the countdown
     * begins here.
     */
    LaunchedEffect(
        viewModel.processingUiState.isProcessFailed
    ) {
        if (viewModel.processingUiState.isProcessFailed) {

            for (seconds in 10 downTo 1) {

                viewModel.updateFailureCountdown(seconds)

                delay(1000.milliseconds)
            }

            viewModel.cancelProcessing()
            onCancel()
        }
    }

    /*
     * Android system back.
     */
    BackHandler {
        cancelAndReturnHome()
    }

    /*
     * Start processing when this screen receives a URI.
     */
    LaunchedEffect(uri) {
        viewModel.updateUri(uri)
        viewModel.startProcessing()

    }

    ProcessingScreenContent(
        uiState = viewModel.processingUiState,
        onCancel = cancelAndReturnHome,
        videoMs = viewModel.videoMetadata?.durationMs ?: 0L,
    )

}
/* -------------------------------------------------------------------------- */
/* Main screen                                                                */
/* -------------------------------------------------------------------------- */

@Composable
private fun ProcessingScreenContent(
    uiState: ProcessingUiState,
    onCancel: () -> Unit,
    videoMs : Long
) {

    Spacer(modifier = Modifier.height(16.dp))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            ProcessingHeader(
                isProcessing = uiState.isProcessing,
                isCompleted = uiState.isCompleted,
                hasError = uiState.isProcessFailed
            )
        }

        item {
            CurrentPhaseCard(
                uiState = uiState
            )
        }

        item {
            ProcessingPipeline(
                currentPhase = uiState.phase,
                progress = uiState.progress,
                isCompleted = uiState.isCompleted,
                isProcessFailed = uiState.isProcessFailed
            )
        }

        item {
            SecurityAuditCard(
                uiState = uiState,
                videoMs = videoMs
            )
        }

        item {
            ProcessingControls(
                isProcessing = uiState.isProcessing,
                onCancel = onCancel
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Processing header                                                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun ProcessingHeader(
    isProcessing: Boolean,
    isCompleted: Boolean,
    hasError: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {

        ProcessingStatusPill(
            isProcessing = isProcessing,
            isCompleted = isCompleted,
            hasError = hasError
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = when {
                hasError -> "Processing failed"
                isCompleted -> "Your collage is ready"
                else -> "Building your people map"
            },
            color = TextHighEmphasis,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = when {
                hasError ->
                    "The video could not be processed."

                isCompleted ->
                    "Your video has been analyzed successfully."

                else ->
                    "Analyzing your video locally to identify people and appearances without sending a single byte to the cloud."
            },
            color = TextMidEmphasis,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Processing status pill                                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun ProcessingStatusPill(
    isProcessing: Boolean,
    isCompleted: Boolean,
    hasError: Boolean
) {
    val statusColor = when {
        hasError -> ProcessingWarning
        else -> Secondary
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "processing_status"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_alpha"
    )

    Row(
        modifier = Modifier
            .background(
                color = SurfaceContainerHigh,
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = statusColor.copy(
                        alpha = if (isProcessing) {
                            pulseAlpha
                        } else {
                            1f
                        }
                    ),
                    shape = CircleShape
                )
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = when {
                hasError -> "PROCESSING FAILED"
                isCompleted -> "PROCESSING COMPLETE"
                else -> "ON-DEVICE PROCESSING"
            },
            color = statusColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Current phase card                                                         */
/* -------------------------------------------------------------------------- */

@Composable
private fun CurrentPhaseCard(
    uiState: ProcessingUiState
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "progress"
    )

    val currentProgressPercent =
        (animatedProgress * 100).toInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            HairlineChrome
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "CURRENT PHASE",
                    color = TextMidEmphasis,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )

                Text(
                    text = "$currentProgressPercent%",
                    color = if (uiState.isProcessFailed) {
                        ProcessingWarning
                    } else {
                        Primary
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedContent(
                targetState = uiState.phase,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(250)
                    ) togetherWith fadeOut(
                        animationSpec = tween(150)
                    )
                },
                label = "current_phase_text"
            ) { phase ->

                Column {

                    Text(
                        text = if (uiState.isProcessFailed) {
                            "Process Failed"
                        } else {
                            phase.displayName()
                        },
                        color = if (uiState.isProcessFailed) {
                            ProcessingWarning
                        } else {
                            TextHighEmphasis
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = uiState.message.ifBlank {
                            phase.defaultMessage()
                        },
                        color = TextMidEmphasis,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    if (uiState.isProcessFailed) {

                        Text(
                            text = "Returning to Home in ${uiState.failureCountdown}s",
                            color = ProcessingWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = SurfaceContainerHigh,
                        shape = RoundedCornerShape(50)
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .background(
                            brush = if (uiState.isProcessFailed) {

                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ProcessingWarning,
                                        ProcessingWarning.copy(
                                            alpha = 0.75f
                                        )
                                    )
                                )

                            } else {

                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Primary,
                                        Primary.copy(alpha = 0.9f),
                                        Secondary
                                    )
                                )
                            },
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Processing pipeline                                                        */
/* -------------------------------------------------------------------------- */

@Composable
private fun ProcessingPipeline(
    currentPhase: ProcessingPhase?,
    progress: Float,
    isCompleted: Boolean,
    isProcessFailed: Boolean
) {
    val phases = remember {
        ProcessingPhase.entries
    }

    val currentIndex = when {
        isCompleted -> phases.lastIndex
        currentPhase == null -> -1
        else -> phases.indexOf(currentPhase)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            HairlineChrome
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "PROCESSING PIPELINE",
                    color = TextHighEmphasis,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = if (isProcessFailed) {
                        "${(currentIndex + 1).coerceAtLeast(0)} OF ${phases.size} FAILED"
                    } else {
                        "${(currentIndex + 1).coerceAtLeast(0)} OF ${phases.size}"
                    },
                    color = if (isProcessFailed) {
                        ProcessingWarning
                    } else {
                        Secondary
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            phases.forEachIndexed { index, phase ->

                PipelineRow(
                    phase = phase,
                    index = index,
                    currentIndex = currentIndex,
                    currentProgress = if (
                        index == currentIndex
                    ) {
                        progress
                    } else {
                        null
                    },
                    isProcessFailed = isProcessFailed
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Pipeline row                                                               */
/* -------------------------------------------------------------------------- */

@Composable
private fun PipelineRow(
    phase: ProcessingPhase,
    index: Int,
    currentIndex: Int,
    currentProgress: Float?,
    isProcessFailed: Boolean
) {
    val isFailed =
        isProcessFailed && index == currentIndex

    val isCompleted =
        index < currentIndex

    val isActive =
        index == currentIndex && !isFailed

    val titleColor by animateColorAsState(
        targetValue = when {
            isFailed -> ProcessingWarning
            isActive -> Primary
            isCompleted -> TextHighEmphasis
            else -> TextMidEmphasis.copy(alpha = 0.6f)
        },
        animationSpec = tween(250),
        label = "pipeline_title_color"
    )

    val infiniteTransition = rememberInfiniteTransition(
        label = "pipeline_$index"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) {
            1.15f
        } else {
            1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 850,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pipeline_scale"
    )

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {

                when {

                    /*
                     * Failed phase
                     */
                    isFailed -> {

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = ProcessingWarning,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = "!",
                                color = Background,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    /*
                     * Completed phase
                     */
                    isCompleted -> {

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = Secondary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Background,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    /*
                     * Active phase
                     */
                    isActive -> {

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .scale(pulseScale)
                                .background(
                                    color = Primary.copy(
                                        alpha = 0.16f
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        color = Primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            color = SurfaceContainerLow,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }

                    /*
                     * Pending phase
                     */
                    else -> {

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = SurfaceContainerHigh,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = TextMidEmphasis.copy(
                                            alpha = 0.35f
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = phase.displayName(),
                    color = titleColor,
                    fontSize = 14.sp,
                    fontWeight = if (
                        isActive || isFailed
                    ) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    }
                )

                when {
                    isFailed -> {

                        Text(
                            text = "Process Failed",
                            color = ProcessingWarning,
                            fontSize = 9.sp,
                            letterSpacing = 0.4.sp
                        )
                    }

                    isActive && currentProgress != null -> {

                        Text(
                            text = "Active · ${(currentProgress * 100).toInt()}% complete",
                            color = Primary,
                            fontSize = 9.sp,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }

            when {

                isFailed -> {

                    PipelineStatusChip(
                        text = "Failed",
                        color = ProcessingWarning
                    )
                }

                isCompleted -> {

                    PipelineStatusChip(
                        text = "Done",
                        color = Secondary
                    )
                }

                isActive -> {

                    PipelineStatusChip(
                        text = "Inference",
                        color = Primary
                    )
                }

                else -> {

                    Text(
                        text = "QUEUED",
                        color = TextMidEmphasis.copy(
                            alpha = 0.45f
                        ),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        if (index < ProcessingPhase.entries.lastIndex) {

            Box(
                modifier = Modifier
                    .padding(start = 11.dp)
                    .width(2.dp)
                    .height(10.dp)
                    .background(
                        if (index < currentIndex) {
                            Secondary.copy(alpha = 0.45f)
                        } else {
                            SurfaceContainerHigh
                        }
                    )
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Pipeline status chip                                                       */
/* -------------------------------------------------------------------------- */

@Composable
private fun PipelineStatusChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 4.dp
            )
    ) {

        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Security audit                                                             */
/* -------------------------------------------------------------------------- */

@Composable
private fun SecurityAuditCard(
    uiState: ProcessingUiState,
    videoMs: Long
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            HairlineChrome
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "SECURITY & SYSTEM AUDIT",
                    color = TextHighEmphasis,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = "HARDWARE ISOLATED",
                        color = Secondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                AuditCell(
                    title = "MODE",
                    value = "LOCAL ON-DEVICE",
                    modifier = Modifier.weight(1f)
                )

                AuditCell(
                    title = "VIDEO DURATION",
                    value = formatDuration(videoMs),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                AuditCell(
                    title = "PIPELINE STATUS",
                    value = when {
                        uiState.isProcessFailed ->
                            "FAILED"

                        uiState.isCompleted ->
                            "COMPLETE"

                        else ->
                            "PROCESSING"
                    },
                    valueColor = when {
                        uiState.isProcessFailed ->
                            ProcessingWarning

                        uiState.isCompleted ->
                            Secondary

                        else ->
                            Secondary
                    },
                    modifier = Modifier.weight(1f)
                )

                AuditCell(
                    title = "NETWORK EMISSION",
                    value = "ZERO CLOUD SYNC",
                    valueColor = Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Audit cell                                                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun AuditCell(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextHighEmphasis
) {
    Column(
        modifier = modifier
            .background(
                color = SurfaceContainerMid,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {

        Text(
            text = title,
            color = TextMidEmphasis,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Bottom controls                                                            */
/* -------------------------------------------------------------------------- */

@Composable
private fun ProcessingControls(
    isProcessing: Boolean,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            color = SurfaceContainerHigh
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = isProcessing,
                        onClick = onCancel
                    )
                    .padding(
                        horizontal = 18.dp,
                        vertical = 13.dp
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel processing",
                    tint = if (isProcessing) {
                        TextHighEmphasis
                    } else {
                        TextMidEmphasis.copy(alpha = 0.45f)
                    }
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Cancel Processing",
                    color = if (isProcessing) {
                        TextHighEmphasis
                    } else {
                        TextMidEmphasis.copy(alpha = 0.45f)
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = SurfaceContainerHigh
        ) {

            IconButton(
                onClick = { }
            ) {

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Processing information",
                    tint = TextMidEmphasis
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Helpers                                                                    */
/* -------------------------------------------------------------------------- */

private fun ProcessingPhase?.displayName(): String {
    return when (this) {

        ProcessingPhase.METADATA_EXTRACTION ->
            "Metadata Extraction"

        ProcessingPhase.FRAME_EXTRACTION ->
            "Frame Extraction"

        ProcessingPhase.FACE_DETECTION ->
            "Face Detection"

        ProcessingPhase.FACE_EMBEDDING ->
            "Face Embeddings"

        ProcessingPhase.CLUSTERING ->
            "Clustering"

        ProcessingPhase.APPEARANCE_COUNTING ->
            "Appearance Counting"

        ProcessingPhase.BEST_SHOT_SELECTION ->
            "Best Shot Selection"

        ProcessingPhase.COLLAGE_GENERATION ->
            "Collage Generation"

        ProcessingPhase.COMPLETED ->
            "Processing Complete"

        null ->
            "Preparing..."
    }
}

private fun ProcessingPhase?.defaultMessage(): String {
    return when (this) {

        ProcessingPhase.METADATA_EXTRACTION ->
            "Reading video metadata..."

        ProcessingPhase.FRAME_EXTRACTION ->
            "Preparing frame extraction..."

        ProcessingPhase.FACE_DETECTION ->
            "Detecting faces across extracted frames..."

        ProcessingPhase.FACE_EMBEDDING ->
            "Generating face embeddings..."

        ProcessingPhase.CLUSTERING ->
            "Grouping similar face embeddings..."

        ProcessingPhase.APPEARANCE_COUNTING ->
            "Counting continuous appearances..."

        ProcessingPhase.BEST_SHOT_SELECTION ->
            "Selecting the strongest representative shot..."

        ProcessingPhase.COLLAGE_GENERATION ->
            "Creating the final collage..."

        ProcessingPhase.COMPLETED ->
            "Processing completed successfully."

        null ->
            "Preparing video processing..."
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000

    return when {
        totalSeconds < 60 -> {
            "$totalSeconds sec"
        }

        totalSeconds < 3600 -> {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "%02d min %02d sec".format(minutes, seconds)
        }

        else -> {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            "%02d hr %02d min %02d sec".format(hours, minutes, seconds)
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Preview                                                                    */
/* -------------------------------------------------------------------------- */

@Preview(
    showBackground = true,
    backgroundColor = 0xFF101419
)
@Composable
private fun ProcessingScreenPreview() {

    IykykTheme {

        ProcessingScreenContent(
            uiState = ProcessingUiState(
                phase = ProcessingPhase.FACE_DETECTION,
                progress = 0.64f,
                message = "Detecting faces across extracted frames...",
                isProcessing = true
            ),
            onCancel = {},
            videoMs = 1000L
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF101419
)
@Composable
private fun ProcessingFailurePreview() {

    IykykTheme {

        ProcessingScreenContent(
            uiState = ProcessingUiState(
                phase = ProcessingPhase.FACE_DETECTION,
                progress = 1f,
                message = "No faces were detected in the video.",
                isProcessing = false,
                isProcessFailed = true,
                failureCountdown = 10,
                error = "Process Failed"
            ),
            onCancel = {},
            videoMs = 1000L
        )
    }
}