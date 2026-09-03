package com.amanansari.iykyk

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun uriToFilename(
    context: Context,
    uri: Uri
): String? {
    return context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getString(
                cursor.getColumnIndexOrThrow(
                    OpenableColumns.DISPLAY_NAME
                )
            )
        } else {
            null
        }
    }
}