import boofcv.alg.distort.brown.LensDistortionBrown
import boofcv.alg.geo.PerspectiveOps
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.gui.fiducial.VisualizeFiducial
import boofcv.gui.image.ImagePanel
import boofcv.gui.image.ShowImages
import boofcv.io.image.ConvertBufferedImage
import boofcv.io.webcamcapture.UtilWebcamCapture
import boofcv.struct.image.GrayU8
import georegression.struct.se.Se3_F64
import java.awt.BasicStroke
import java.awt.Color

fun main() {
    val fieldOfViewDegrees = 90.0
    val markerWidth = 10.0

    // Open a webcam and create the detector
    val webcam = UtilWebcamCapture.openDefault(800, 600)
    val detector = FactoryFiducial.qrcode3D(null, GrayU8::class.java)

    // Create the panel used to display the image and
    val gui = ImagePanel()
    gui.preferredSize =  webcam.viewSize

    val intrinsics = PerspectiveOps.createIntrinsic(
        webcam.viewSize.width, webcam.viewSize.height, 90.0)

    detector.setMarkerWidth(markerWidth)
    detector.setLensDistortion(LensDistortionBrown(intrinsics),intrinsics.width, intrinsics.height)

    ShowImages.showWindow(gui, "Gradient", true)

    val fid2cam = Se3_F64()

    while (true) {
        // Load the image from the webcam
        val image = webcam.image

        // Convert to gray scale and detect QR codes inside
        val gray = ConvertBufferedImage.convertFrom(image, null as GrayU8?)
        detector.detect(gray)

        // Draw where boxes around the QR Codes
        val g2 = image.createGraphics()
        g2.color = Color.RED
        g2.stroke = BasicStroke(4.0f)
        for ( idx in 0 until detector.totalFound() ) {
            if( !detector.getFiducialToCamera(idx,fid2cam) )
                continue
            VisualizeFiducial.drawCube(fid2cam, intrinsics,markerWidth,4, g2)
        }

        // Update the display
        gui.setImageRepaint(image)
    }
}