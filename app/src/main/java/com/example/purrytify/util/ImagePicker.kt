package com.example.purrytify.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberImagePicker(
    onImagePicked: (Uri) -> Unit
): Pair<() -> Unit, Uri?> {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            onImagePicked(it)
        }
    }
    
    val pickImage = {
        launcher.launch("image/*")
    }
    
    return Pair(pickImage, imageUri)
}