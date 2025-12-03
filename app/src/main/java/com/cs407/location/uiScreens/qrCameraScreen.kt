package com.cs407.location.uiScreens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.location.viewModels.qrViewModel

@Composable
fun qrCameraScreen(
    viewModel: qrViewModel = viewModel(),
    onQrScanned: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCamPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(hasCamPermission) {
        if (hasCamPermission) {
            viewModel.setUpCamera(context)
        }
    }

    val controller by viewModel.cameraControl.collectAsStateWithLifecycle()

    val qr by viewModel.qrResult.collectAsStateWithLifecycle()

    LaunchedEffect(qr) {
        val value = qr
        if (value != null) {
            // optional debug toast

            onQrScanned(value)
            // If you want to prevent multiple triggers while the code stays in view,
            // you can clear the result in the ViewModel (see note below).
            viewModel.clearResult()
        }
    }

    if (hasCamPermission && controller != null) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            update = { previewView ->
                previewView.controller = controller
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(lifecycleOwner, controller) {
            controller?.bindToLifecycle(lifecycleOwner)
            onDispose {
                controller?.unbind()
            }
        }
    }
}