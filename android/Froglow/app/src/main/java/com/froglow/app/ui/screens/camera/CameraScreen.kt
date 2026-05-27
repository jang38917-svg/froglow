package com.froglow.app.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.froglow.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private val ScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)
private val DisabledGreen = Color(0xFFDDEAD6)

private val TopOverlayBg = Color(0xEFFFFEFC)
private val BottomOverlayBg = Color(0xEFFFFEFC)
private val GuideWhite = Color(0xFFFDFBF8)

@Composable
fun CameraScreen(
    onBack: () -> Unit = {},
    onPhotosDone: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    android.util.Log.e("CAMERA_DEBUG", "=== CameraScreen 시작 ===")

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isCameraReady by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val photoUris = remember { mutableStateListOf<String>() }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var previewViewRef: PreviewView? by remember { mutableStateOf(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        android.util.Log.e("CAMERA_DEBUG", "permission result = $granted")
        hasPermission = granted

        if (granted) {
            errorMessage = null
        } else {
            errorMessage = "카메라 권한이 필요합니다."
            isCameraReady = false
        }
    }

    LaunchedEffect(Unit) {
        android.util.Log.e("CAMERA_DEBUG", "LaunchedEffect 권한 체크 hasPermission=$hasPermission")

        if (!hasPermission) {
            android.util.Log.e("CAMERA_DEBUG", "카메라 권한 자동 요청 실행")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(hasPermission, lifecycleOwner, previewViewRef) {
        android.util.Log.e(
            "CAMERA_DEBUG",
            "DisposableEffect 진입 hasPermission=$hasPermission previewViewRef=${previewViewRef != null}"
        )

        if (hasPermission && previewViewRef != null) {
            val future = ProcessCameraProvider.getInstance(context)

            future.addListener(
                {
                    try {
                        val provider = future.get()
                        cameraProvider = provider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewViewRef!!.surfaceProvider)
                        }

                        val frontSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                        val backSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        val selector = when {
                            provider.hasCamera(frontSelector) -> {
                                android.util.Log.e("CAMERA_DEBUG", "전면 카메라 bind 시도")
                                frontSelector
                            }

                            provider.hasCamera(backSelector) -> {
                                android.util.Log.e("CAMERA_DEBUG", "후면 카메라 fallback bind 시도")
                                backSelector
                            }

                            else -> {
                                errorMessage = "사용 가능한 카메라가 없습니다."
                                isCameraReady = false
                                android.util.Log.e("CAMERA_DEBUG", "사용 가능한 카메라 없음")
                                return@addListener
                            }
                        }

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageCapture
                        )

                        isCameraReady = true
                        errorMessage = null
                        android.util.Log.e("CAMERA_DEBUG", "카메라 bind 성공")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isCameraReady = false
                        errorMessage = e.message ?: "카메라를 열 수 없습니다."
                        android.util.Log.e("CAMERA_DEBUG", "카메라 bind 실패: ${e.message}", e)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    val currentStepIndex = photoUris.size.coerceIn(0, 2)
    val currentStepLabel = when (currentStepIndex) {
        0 -> "정면"
        1 -> "왼쪽"
        else -> "오른쪽"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        when {
            !hasPermission -> {
                PermissionContent(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }

            errorMessage != null -> {
                ErrorContent(
                    message = errorMessage ?: "오류가 발생했습니다.",
                    onRetry = {
                        errorMessage = null
                        isCameraReady = false
                        previewViewRef = previewViewRef
                    }
                )
            }

            else -> {
                AndroidView(
                    factory = {
                        PreviewView(it).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        previewViewRef = view
                    }
                )

                CameraSoftGradient()
                CameraGuideOverlay()

                Image(
                    painter = painterResource(id = R.drawable.frog_guide),
                    contentDescription = "촬영 가이드",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 18.dp, bottom = 92.dp)
                        .size(width = 205.dp, height = 132.dp),
                    contentScale = ContentScale.Fit
                )

                TopCameraOverlay(
                    photoCount = photoUris.size,
                    currentStep = currentStepLabel,
                    onBack = onBack
                )

                BottomCaptureOverlay(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    photoCount = photoUris.size,
                    isCapturing = isCapturing,
                    onRetake = {
                        if (photoUris.isNotEmpty()) {
                            photoUris.removeAt(photoUris.lastIndex)
                        }
                    },
                    onCapture = {
                        if (!isCameraReady || isCapturing || photoUris.size >= 3) {
                            return@BottomCaptureOverlay
                        }

                        isCapturing = true

                        val photoFile = File(
                            context.cacheDir,
                            "frog_${
                                SimpleDateFormat(
                                    "yyyyMMdd_HHmmss",
                                    Locale.KOREA
                                ).format(System.currentTimeMillis())
                            }.jpg"
                        )

                        val outputOptions =
                            ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    photoUris.add(Uri.fromFile(photoFile).toString())
                                    isCapturing = false
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    exception.printStackTrace()
                                    errorMessage = exception.message ?: "촬영 실패"
                                    isCapturing = false
                                }
                            }
                        )
                    },
                    onUse = {
                        if (photoUris.size == 3) {
                            onPhotosDone(photoUris.toList())
                        }
                    }
                )

                if (!isCameraReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x33000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSoftGradient() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0x55FFFDF8),
                        0.18f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1.00f to Color(0x66FFFDF8)
                    )
                )
            )
    )
}

@Composable
private fun TopCameraOverlay(
    photoCount: Int,
    currentStep: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepIndicator(
            currentStep = currentStep,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = TopOverlayBg,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = PrimaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = TopOverlayBg,
                border = BorderStroke(1.dp, CardBorder),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "$photoCount/3",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = PrimaryText,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0xCFFFFEFC),
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepChip(
                text = "정면",
                selected = currentStep == "정면",
                modifier = Modifier.weight(1f)
            )

            StepDivider()

            StepChip(
                text = "왼쪽",
                selected = currentStep == "왼쪽",
                modifier = Modifier.weight(1f)
            )

            StepDivider()

            StepChip(
                text = "오른쪽",
                selected = currentStep == "오른쪽",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StepDivider() {
    Text(
        text = "›",
        modifier = Modifier.width(18.dp),
        color = CardBorder,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StepChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) GreenPrimary else Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else SecondaryText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BottomCaptureOverlay(
    modifier: Modifier = Modifier,
    photoCount: Int,
    isCapturing: Boolean,
    onRetake: () -> Unit,
    onCapture: () -> Unit,
    onUse: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = BottomOverlayBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalButton(
                onClick = onRetake,
                enabled = photoCount > 0 && !isCapturing,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFF1EBDD),
                    contentColor = SecondaryText,
                    disabledContainerColor = Color(0xFFF3F1EC),
                    disabledContentColor = SecondaryText.copy(alpha = 0.42f)
                ),
                contentPadding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 7.dp
                )
            ) {
                Text(
                    text = "다시찍기",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            CaptureButton(
                enabled = !isCapturing && photoCount < 3,
                onClick = onCapture
            )

            Button(
                onClick = onUse,
                enabled = photoCount == 3 && !isCapturing,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = GreenSoft,
                    disabledContentColor = SecondaryText.copy(alpha = 0.55f)
                ),
                contentPadding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 7.dp
                )
            ) {
                Text(
                    text = "사용하기",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(66.dp),
        shape = CircleShape,
        color = Color(0xF0FFFEFC),
        border = BorderStroke(2.dp, GreenPrimary),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(49.dp),
                shape = CircleShape,
                color = if (enabled) GreenPrimary else DisabledGreen,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = enabled) {
                            onClick()
                        }
                )
            }
        }
    }
}

@Composable
private fun PermissionContent(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "카메라 권한이 필요합니다.",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "피부 진단을 위해 카메라 접근을 허용해주세요.",
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("권한 허용")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "카메라를 열 수 없습니다.",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("다시 시도")
                }
            }
        }
    }
}

@Composable
private fun CameraGuideOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val minFaceWidth = 205.dp.toPx()
        val maxFaceWidth = 260.dp.toPx()

        val faceWidth = (w * 0.56f).coerceIn(minFaceWidth, maxFaceWidth)
        val faceHeight = faceWidth * 1.23f

        val faceLeft = (w - faceWidth) / 2f
        val faceTop = h * 0.23f

        drawRoundRect(
            color = GuideWhite.copy(alpha = 0.93f),
            topLeft = Offset(faceLeft, faceTop),
            size = Size(faceWidth, faceHeight),
            style = Stroke(
                width = 3.8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            ),
            cornerRadius = CornerRadius(999f, 999f)
        )

        drawLine(
            color = GuideWhite.copy(alpha = 0.78f),
            start = Offset(w / 2f, faceTop - 20f),
            end = Offset(w / 2f, faceTop + faceHeight + 20f),
            strokeWidth = 2.8f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = GuideWhite.copy(alpha = 0.78f),
            start = Offset(faceLeft - 20f, faceTop + faceHeight / 2f),
            end = Offset(faceLeft + faceWidth + 20f, faceTop + faceHeight / 2f),
            strokeWidth = 2.8f,
            cap = StrokeCap.Round
        )
    }
}