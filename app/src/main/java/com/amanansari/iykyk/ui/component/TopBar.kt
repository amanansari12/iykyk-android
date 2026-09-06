package com.amanansari.iykyk.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amanansari.iykyk.ui.theme.Primary

@Composable
fun TopBar(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    showSavedIcon: Boolean = false,
    onSavedClick: () -> Unit = {}
){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp)

    ) {

        if (showBackButton) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { onBackClick() },
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back"
            )
        }

        Column(
            modifier = Modifier.align(
                alignment = if (showBackButton) {
                    Alignment.Center
                } else {
                    Alignment.CenterStart
                }),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Start),
                text = "IYKYK",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary.copy(alpha = 0.5f)
            )

            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (showSavedIcon) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { onSavedClick() },
                imageVector = Icons.Outlined.CollectionsBookmark,
                contentDescription = "Saved collages"
            )
        }

    }
}