import boofcv.alg.distort.brown.LensDistortionBrown
import boofcv.alg.geo.PerspectiveOps
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.gui.fiducial.VisualizeFiducial
import boofcv.gui.image.ImagePanel
import boofcv.gui.image.ShowImages
import boofcv.io.webcamcapture.UtilWebcamCapture
import boofcv.kotlin.asGrayU8
import boofcv.struct.image.GrayU8
import georegression.struct.point.Point2D_F64
import georegression.struct.se.Se3_F64
import java.awt.Font

fun main() {
    // I have a Logitech BRIO and the spec says 90 degree FOV so that's what I will use
    // if you care about accurate results calibrate your camera. This will be Less Than Optimal...
    val fieldOfViewDegrees = 90.0
    // How wise the QR Codes are. For demonstration purposes I'm using a larger one that's 16cm
    val markerWidth = 16.0

    // Open a webcam and create the detector
    val webcam = UtilWebcamCapture.openDefault(800, 600)
    val detector = FactoryFiducial.qrcode3D(null, GrayU8::class.java)

    // Create the panel used to display the image and
    val gui = ImagePanel()
    gui.preferredSize =  webcam.viewSize

    // "Guess" the camera parameters based on the FOV. This won't correct lens distortion and will be approximate
    val intrinsics = PerspectiveOps.createIntrinsic(
        webcam.viewSize.width, webcam.viewSize.height, fieldOfViewDegrees)

    // Specify the 3D geometric information
    detector.setMarkerWidth(markerWidth)
    detector.setLensDistortion(LensDistortionBrown(intrinsics),intrinsics.width, intrinsics.height)

    ShowImages.showWindow(gui, "QR Code 3D", true)

    // Transform from qrcode to camera
    val qr2cam = Se3_F64()
    // storage for center of the QR code in pixels
    val centerPixel = Point2D_F64()

    while (true) {
        // Load the image from the webcam
        val image = webcam.image ?: break

        // Convert to gray scale and detect QR codes inside
        detector.detect(image.asGrayU8())

        // Draw where boxes around the QR Codes
        val g2 = image.createGraphics()
        g2.font = Font("Serif", Font.BOLD, 24)

        for ( idx in 0 until detector.totalFound() ) {
            if( !detector.getFiducialToCamera(idx,qr2cam) )
                continue

            // Draw a 3D cube to show that it's estimating it's 3D pose
            VisualizeFiducial.drawCube(qr2cam, intrinsics,markerWidth,4, g2)

            // Draw how far away it is
            detector.getCenter(idx,centerPixel)
            val distance = qr2cam.T.norm()
            VisualizeFiducial.drawLabel(centerPixel,"%4.1f".format(distance),g2)
        }

        // Update the display
        gui.setImageRepaint(image)
    }
}