package org.boofcv

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView
import boofcv.abst.fiducial.QrCodeDetector
import boofcv.android.camera2.VisualizeCamera2Activity
import boofcv.concurrency.BoofConcurrency
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.misc.MovingAverage
import boofcv.struct.image.GrayU8
import boofcv.struct.image.ImageBase
import boofcv.struct.image.ImageType
import georegression.struct.shapes.Polygon2D_F64
import org.ddogleg.struct.Factory
import org.ddogleg.struct.FastQueue
import java.util.*
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Demonstrates how to detect a QR Code and visualize the results.
 *
 * @see VisualizeCamera2Activity
 * @see boofcv.android.camera2.SimpleCamera2Activity
 *
 * @author Peter Abeles
 */
class QrCodeActivity : VisualizeCamera2Activity() {
    // QR Code detector. Use default configuration
    private val detector: QrCodeDetector<GrayU8> = FactoryFiducial.qrcode(null, GrayU8::class.java)

    // Used to display text info on the display
    private val paintText = Paint()

    private val colorDetected = Paint()

    // Storage for bounds of found QR Codes
    private val foundQR = FastQueue(Factory { Polygon2D_F64() })
    private var message = "" // most recently decoded QR code


    // Used to compute average time in the detector
    private val timeDetection = MovingAverage()

    // where the decoded QR's message is printed
    private var textMessageView: TextView? = null

    // work space for display
    var path = Path()

    init {
        // The default behavior for selecting the camera's resolution is to
        // find the resolution which comes the closest to having this many
        // pixels.
        targetResolution = 1024 * 768
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Java 1.8 issues with older SDK versions
        BoofConcurrency.USE_CONCURRENT = Build.VERSION.SDK_INT >= 24
        setContentView(R.layout.qrcode)
        val surface = findViewById<FrameLayout>(R.id.camera_frame)
        textMessageView = findViewById(R.id.qrmessage)

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

        // Color that detected QR will be painted
        colorDetected.setARGB(0xA0, 0, 0xFF, 0)
        colorDetected.style = Paint.Style.FILL

        // The camera stream will now start after this function is called.
        startCamera(surface, null)
    }

    /**
     * This function is invoked in its own thread and can take as long as you want.
     */
    override fun processImage(image: ImageBase<*>) {
        // Detect the QR Code
        // GrayU8 image was specified in onCreate()
        val elapsedTime = measureNanoTime {
            detector.process(image as GrayU8)
        }
        timeDetection.update(elapsedTime.toDouble()*1e-6)

        // Create a copy of what we will visualize here. In general you want a copy because
        // the UI and image processing is done on two different threads
        synchronized(foundQR) {
            foundQR.reset()
            for( qr in detector.detections ) {
                foundQR.grow().set(qr.bounds)
                message = qr.message
            }
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
        canvas.drawText("detector: %4.1f (ms)".format(timeDetection.average), 180f, 170f, paintText)

        // This line is magical and will save you hours/days of anguish
        // What it does is correctly convert the coordinate system from
        // image pixels that were processed into display coordinates
        // taking in account rotations and weird CCD layouts
        canvas.concat(imageToView)

        // Draw the bounding squares around the QR Codes
        synchronized(foundQR) {
            for (foundIdx in 0 until foundQR.size()) {
                renderPolygon(foundQR[foundIdx], path, canvas, colorDetected)
            }
            if (foundQR.size() > 0)
                textMessageView!!.text = message
        }

        // Pro tip: Run in app fast or release mode for a dramatic speed up!
        // In Android Studio expand "Build Variants" tab on left.
    }

    private fun renderPolygon( s: Polygon2D_F64,  path: Path, canvas: Canvas, paint: Paint? )
    {
        path.reset()
        for (j in 0 until s.size()) {
            val p = s[j]
            if (j == 0)
                path.moveTo(p.x.toFloat(), p.y.toFloat())
            else
                path.lineTo(p.x.toFloat(), p.y.toFloat())
        }
        val p = s[0]
        path.lineTo(p.x.toFloat(), p.y.toFloat())
        path.close()
        canvas.drawPath(path, paint!!)
    }
}