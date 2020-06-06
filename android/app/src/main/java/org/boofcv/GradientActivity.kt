package org.boofcv

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.widget.FrameLayout
import boofcv.android.VisualizeImageData
import boofcv.android.camera2.VisualizeCamera2Activity
import boofcv.concurrency.BoofConcurrency
import boofcv.factory.filter.derivative.FactoryDerivative
import boofcv.struct.image.GrayS16
import boofcv.struct.image.GrayU8
import boofcv.struct.image.ImageBase
import boofcv.struct.image.ImageType
import java.util.*

/**
 * Demonstrates how to use the visualize activity. A video stream is opened and the image gradient
 * is found. The gradient is then rendered into a format which can be visualized and displayed
 * on the Android device's screen.
 *
 * This greatly simplifies the process of capturing and visualizing image data from a camera.
 * Internally it uses the camera 2 API. You can customize its behavior by overriding
 * different internal functions. For more details, see the JavaDoc of it's parent classes.
 *
 * @see VisualizeCamera2Activity
 * @see boofcv.android.camera2.SimpleCamera2Activity
 *
 * @author Peter Abeles
 */
class GradientActivity : VisualizeCamera2Activity() {
    // Storage for the gradient
    private val derivX = GrayS16(1, 1)
    private val derivY = GrayS16(1, 1)

    // Storage for image gradient. In general you will want to precompute data structures due
    // to the expense of garbage collection
    private val gradient = FactoryDerivative.three(GrayU8::class.java, GrayS16::class.java)

    // Used to display text info on the display
    private val paintText = Paint()

    init {
        // The default behavior for selecting the camera's resolution is to
        // find the resolution which comes the closest to having this many
        // pixels.
        targetResolution = 640 * 480
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Java 1.8 issues with older SDK versions
        BoofConcurrency.USE_CONCURRENT = Build.VERSION.SDK_INT >= 24
        setContentView(R.layout.gradient)
        val surface = findViewById<FrameLayout>(R.id.camera_frame)

        // By calling this function you are telling the camera library that you wish to process
        // images in a gray scale format. The video stream is typically in YUV420. Color
        // image formats are supported as RGB, YUV, ... etc, color spaces.
        setImageType(ImageType.single(GrayU8::class.java))

        // Configure paint used to display FPS
        paintText.strokeWidth = 4 * displayMetrics.density
        paintText.textSize = 14 * displayMetrics.density
        paintText.textAlign = Paint.Align.LEFT
        paintText.setARGB(0xFF, 0xFF, 0xB0, 0)
        paintText.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        // The camera stream will now start after this function is called.
        startCamera(surface, null)
    }

    /**
     * This is where you specify custom camera settings. See [boofcv.android.camera2.SimpleCamera2Activity]'s
     * JavaDoc for more functions which you can override.
     *
     * @param captureRequestBuilder Used to configure the camera.
     */
    override fun configureCamera(
        device: CameraDevice?,
        characteristics: CameraCharacteristics?,
        captureRequestBuilder: CaptureRequest.Builder
    ) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
    }

    /**
     * During camera initialization this function is called once after the resolution is known.
     * This is a good function to override and predeclare data structres which are dependent
     * on the video feeds resolution.
     */
    override fun onCameraResolutionChange(width: Int, height: Int, sensorOrientation: Int) {
        super.onCameraResolutionChange(width, height, sensorOrientation)
        derivX.reshape(width, height)
        derivY.reshape(width, height)
    }

    /**
     * This function is invoked in its own thread and can take as long as you want.
     */
    override fun processImage(image: ImageBase<*>) {
        // The data type of 'image' was specified in onCreate() function
        // The line below will compute the gradient and store it in two images. One for the
        // gradient along the x-axis and the other along the y-axis
        gradient.process(image as GrayU8, derivX, derivY)
    }

    /**
     * Override the default behavior and colorize gradient instead of converting input image.
     */
    override fun renderBitmapImage(mode: BitmapMode?, image: ImageBase<*>?) {
        when (mode) {
            BitmapMode.UNSAFE -> {
                // this application is configured to use double buffer and could ignore all other modes
                VisualizeImageData.colorizeGradient(derivX, derivY, -1, bitmap, bitmapTmp)
            }
            BitmapMode.DOUBLE_BUFFER -> {
                VisualizeImageData.colorizeGradient(derivX, derivY, -1, bitmapWork, bitmapTmp)
                if (bitmapLock.tryLock()) {
                    try {
                        val tmp = bitmapWork
                        bitmapWork = bitmap
                        bitmap = tmp
                    } finally {
                        bitmapLock.unlock()
                    }
                }
            }
            else -> throw RuntimeException("mode not supported")
        }
    }

    /**
     * Demonstrates how to draw visuals
     */
    override fun onDrawFrame(view: SurfaceView?, canvas: Canvas) {
        super.onDrawFrame(view, canvas)

        // Display info on the image being process and how fast input camera
        // stream (probably in YUV420) is converted into a BoofCV format
        val width = bitmap.width
        val height = bitmap.height
        canvas.drawText("$width x $height Convert: %4.1f (ms)".format(periodConvert.average),0f, 120f, paintText)

        // Pro tip: Run in app fast or release mode for a dramatic speed up!
        // In Android Studio expand "Build Variants" tab on left.
    }
}