package com.cs407.location.viewModels


import android.content.Context
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class qrViewModel : ViewModel() {

    private var _cameraControl = MutableStateFlow<LifecycleCameraController?>(null)
    val cameraControl = _cameraControl.asStateFlow()

    private var _qrResult = MutableStateFlow<String?>(null)
    val qrResult get() = _qrResult.asStateFlow()


    fun setUpCamera(context: Context) {
        // Only set up once
        if (_cameraControl.value != null) return
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()

        val barcodeScanner = BarcodeScanning.getClient(options)

        val controller = LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)

            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                MlKitAnalyzer(
                    listOf(barcodeScanner),
                    CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context)
                ) { result ->
                    val barcodes = result.getValue(barcodeScanner)
                    barcodes?.firstOrNull()?.rawValue?.let { value ->
                        viewModelScope.launch { _qrResult.emit(value) }
                    }
                }
            )
        }
        _cameraControl.value = controller
    }
}