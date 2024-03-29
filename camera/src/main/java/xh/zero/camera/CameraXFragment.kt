package xh.zero.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.viewbinding.ViewBinding
import timber.log.Timber
import xh.zero.camera.utils.StorageUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * CameraX相机
 */
abstract class CameraXFragment<VIEW: ViewBinding> : BaseCameraFragment<VIEW>() {

    private var displayId: Int = -1
    private var cameraProvider: ProcessCameraProvider? = null
//    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
//    private lateinit var windowManager: WindowManager
    private var camera: Camera? = null

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var surfaceExecutor: ExecutorService

    // 照片输出路径
//    private lateinit var outputDirectory: File

//    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var bitmapBuffer: Bitmap
    private var rotateBitmap: Bitmap? = null
    private var imageRotationDegrees: Int = 0
    var isStopAnalysis = false

    protected abstract val initialExposureIndex: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        surfaceExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        isStopAnalysis = true
        cameraExecutor.apply {
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }
        surfaceExecutor.apply {
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize our background executor
//        cameraExecutor = Executors.newSingleThreadExecutor()
//        surfaceExecutor = Executors.newSingleThreadExecutor()
        // Determine the output directory
//        outputDirectory = getOutputDirectory(requireContext())

//        windowManager = WindowManager(view.context)

//        getSurfaceView().setOnSurfaceCreated { sfTexture ->
//            surfaceTexture = sfTexture
//            setSurfaceBufferSize(surfaceRatio, surfaceTexture)
//            displayId = getSurfaceView().display.displayId
//            setupCamera()
//        }

        // 手动对焦
        getSurfaceView().setOnGestureDetect(object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val x = e.x ?: 0f
                val y = e.y ?: 0f

                val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                    getSurfaceView().width.toFloat(), getSurfaceView().height.toFloat()
                )

                val autoFocusPoint = factory.createPoint(x, y)
                try {
                    camera?.cameraControl?.startFocusAndMetering(
                        FocusMeteringAction.Builder(
                            autoFocusPoint,
                            FocusMeteringAction.FLAG_AF
                        ).apply {
                            //focus only when the user tap the preview
                            disableAutoCancel()
                            onFocusTap(x, y)
                        }.build()
                    )
                } catch (e: CameraInfoUnavailableException) {
                    Log.d("ERROR", "cannot access camera", e)
                }
                return true
            }
        })
    }

    override fun onSurfaceCreated() {
        setupCamera()
    }

    abstract fun onFocusTap(x: Float, y: Float)

    /**
     * 设置曝光值
     */
    fun setExposure(index: Int) {
        camera?.apply {
            val range = cameraInfo.exposureState.exposureCompensationRange
            Timber.d("exposure size: $range")
            if (range.contains(index)) {
                cameraControl.setExposureCompensationIndex(index)
                val ev = cameraInfo.exposureState.exposureCompensationStep.toFloat() * index
                Timber.i("EV: $ev")
            }
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
//            lensFacing = when {
//                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
//                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
//                else -> throw IllegalStateException("Back and front camera are unavailable")
//            }
            getSurfaceView().holder.setFixedSize(getSurfaceView().width, getSurfaceView().height)
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     * 构建相机用例
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
//        val metrics = windowManager.getCurrentWindowMetrics().bounds
//        Timber.d("Screen metrics: ${metrics.width()} x ${metrics.height()}")
//        windowManager.maximumWindowMetrics.bounds

        val screenAspectRatio = aspectRatio(aspectRatio.width, aspectRatio.height)
        Timber.d("Preview aspect ratio: $screenAspectRatio")

        val rotation = getSurfaceView().display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder()
            // 开启前后置摄像头
//            .requireLensFacing(lensFacing)
            // 开启特定Id的摄像头
            .addCameraFilter { cameraList ->
                cameraList.filter { cameraInfo ->
                    Camera2CameraInfo.from(cameraInfo).cameraId == cameraId
                }
            }
            .build()

        // Preview 用例
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture 用例
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .setJpegQuality(100)
            .build()

        if (isAnalysis) {
            // ImageAnalysis 用例
            imageAnalyzer = ImageAnalysis.Builder()
                // We request aspect ratio but no resolution
//            .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                // 大分辨率
//            .setTargetResolution(Size(960, 1280))
                .setTargetResolution(captureSize)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            imageRotationDegrees = image.imageInfo.rotationDegrees
                            bitmapBuffer = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                        }
                        Timber.d("ImageAnalysis: capture: ($captureSize) image ${image.width} x ${image.height}, rotation: ${image.imageInfo.rotationDegrees}")
                        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
                        // 拿到的图片是逆时针转了90度的图，这里修正它
                        val matrix = Matrix()
                        matrix.postRotate(imageRotationDegrees.toFloat())
                        rotateBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
                        // 监听线程关闭的消息
                        if (isStopAnalysis) {
                            return@Analyzer
                        }
                        onAnalysisImage(rotateBitmap!!)

                    })
                }
        }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = if (isAnalysis) {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)
            } else {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
            }

            setExposure(initialExposureIndex)

            // Attach the viewfinder's surface provider to preview use case
//            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            preview?.setSurfaceProvider { request ->
                val surface = Surface(surfaceTexture)
                request.provideSurface(surface, surfaceExecutor) { result ->
                    surface.release()
                    surfaceTexture.release()
                    // 0: success
                    Timber.d("surface used result: ${result.resultCode}")
                }
            }
            observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            Timber.e("Use case binding failed: $exc")
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            // 相机状态监听
            val msg = when (cameraState.type) {
                CameraState.Type.PENDING_OPEN -> "CameraState: Pending Open"
                CameraState.Type.OPENING -> "CameraState: Opening"
                CameraState.Type.OPEN -> {
                    // Setup Camera resources and begin processing
                    onOpened()
                    "CameraState: Open"
                }
                CameraState.Type.CLOSING -> "CameraState: Closing"
                CameraState.Type.CLOSED -> "CameraState: Closed"
            }

            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

            // 相机错误处理
            cameraState.error?.let { error ->
                val errMsg: String? = when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> "Stream config error"
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> "Camera in use"
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> "Other recoverable error"
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> "Camera disabled"
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> "Fatal error"
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> "Do not disturb mode enabled"
                    else -> null
                }
                onError(errMsg)
            }
        }
    }

    override fun capture(callback: CaptureCallback) {

        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->

            // Create output file to hold the image
            val photoFile = createFile(requireContext(), PHOTO_EXTENSION)

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {

                // Mirror image when using the front camera
//                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Timber.e("Photo capture failed: ${exc.message}")
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        requireActivity().runOnUiThread {
                            callback(savedUri.path)
                        }
                        Timber.d("Photo capture succeeded: ${savedUri.path}")

                        // We can only change the foreground Drawable using API level 23+ API
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Update the gallery thumbnail with latest picture taken
//                            setGalleryThumbnail(savedUri)
                        }

                        // Implicit broadcasts will be ignored for devices running API level >= 24
                        // so if you only target API level 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            requireActivity().sendBroadcast(
                                Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(savedUri.toFile().absolutePath),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Timber.d("Image capture scanned into media store: $uri")
                        }
                    }
                })

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Display flash animation to indicate that photo was captured
//                fragmentCameraBinding.root.postDelayed({
//                    fragmentCameraBinding.root.foreground = ColorDrawable(Color.WHITE)
//                    fragmentCameraBinding.root.postDelayed(
//                        { fragmentCameraBinding.root.foreground = null }, ANIMATION_FAST_MILLIS)
//                }, ANIMATION_SLOW_MILLIS)
            }
        }
    }

    companion object {
        private const val PHOTO_EXTENSION = "jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private const val IMAGE_ROTATE = 90f
    }
}