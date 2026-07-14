package ru.kogtie.qr.reader.qr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import ru.kogtie.qr.domain.FiscalReceipt
import ru.kogtie.qr.domain.ReceiptItem
import ru.kogtie.qr.reader.ui.theme.QrreaderTheme

class QrScannerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                QrreaderTheme {
                    QrScannerScreen(viewModel = viewModel())
//                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                        Text("This is the Second Fragment")
//                    }
                }
            }
        }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun QrScannerScreen(
    viewModel: QrScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current

    var hasCameraPermission by remember {
        mutableStateOf(
            isInspectionMode || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !isInspectionMode) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val scannedLink by viewModel.scannedLink.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState is QrScannerUiState.Idle || uiState is QrScannerUiState.Error) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .align(Alignment.Center)
                ) {
                    QrCameraPreview(
                        onQrScanned = { value ->
                            viewModel.onQrScanned(value)
                        }
                    )
                }
                ScannerOverlay()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera permission is required",
                        color = Color.White
                    )
                }
            }
        }

        when (val state = uiState) {
            is QrScannerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is QrScannerUiState.Success -> {
                ReceiptDetails(
                    receipt = state.receipt,
                    onBack = { viewModel.reset() }
                )
            }
            is QrScannerUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Error: ${state.message}", color = Color.Red)
                    Button(onClick = { viewModel.reset() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {}
        }

        if (uiState is QrScannerUiState.Idle && scannedLink != null) {
            Text(
                text = scannedLink!!,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp),
                color = Color.White
            )
        }
    }
}

@Composable
fun ReceiptDetails(
    receipt: FiscalReceipt,
    onBack: () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5), // Bright gray/white background
        contentColor = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack) {
                    Text("Scan Again")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Fiscal Receipt",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )
            }

            HorizontalDivider(color = Color.LightGray)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = receipt.receiptText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        softWrap = false,
                        color = Color.Black
                    )
                }

                item {
                    Text(
                        text = "Structured Items",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                }

                items(receipt.items) { item ->
                    ReceiptItemRow(item)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.LightGray
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ReceiptItemRow(item: ReceiptItem) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${item.quantity} x ${item.unitPrice}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${item.total}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
        if (!item.gtin.isNullOrBlank()) {
            Text(
                text = "GTIN: ${item.gtin}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scannerSize = size.width * 0.7f
        val left = (size.width - scannerSize) / 2
        val top = (size.height - scannerSize) / 2
        val rect = Rect(left, top, left + scannerSize, top + scannerSize)

        // Draw the semi-transparent overlay with a "hole"
        clipPath(
            path = Path().apply {
                addRoundRect(RoundRect(rect, CornerRadius(16.dp.toPx())))
            },
            clipOp = ClipOp.Difference
        ) {
            drawRect(Color.Black.copy(alpha = 0.5f))
        }

        // Draw the white frame
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scannerSize, scannerSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrCameraPreview(
    onQrScanned: (String) -> Unit
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Camera Preview Placeholder",
                color = Color.White
            )
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx)
                ) { imageProxy ->

                    val mediaImage = imageProxy.image

                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val value = barcodes
                                    .firstOrNull()
                                    ?.rawValue

                                if (!value.isNullOrBlank()) {
                                    onQrScanned(value)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    analysis
                )

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}