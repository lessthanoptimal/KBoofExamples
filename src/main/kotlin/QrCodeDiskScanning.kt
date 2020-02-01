import boofcv.factory.fiducial.FactoryFiducial
import boofcv.gui.BoofSwingUtil
import boofcv.io.image.UtilImageIO
import boofcv.struct.image.GrayU8

fun main() {
    // Opens a dialog and let's you select a directory
    val directory = BoofSwingUtil.openFileChooser("QR Disk Scanning",BoofSwingUtil.FileTypes.DIRECTORIES) ?: return

    // Create the scanner class
    val detector = FactoryFiducial.qrcode(null,GrayU8::class.java)

    // Walk through the path recursively, finding all image files, load them, scan for QR codes, add results to a map
    val imageToMessages = mutableMapOf<String,List<String>>()
    val time0 = System.currentTimeMillis()
    directory.walk().filter {BoofSwingUtil.isImage(it)}.forEach { f ->
        val image = UtilImageIO.loadImage(f.absolutePath,detector.imageType) ?: return
        detector.process(image)
        imageToMessages[f.absolutePath] = detector.detections.map { it.message }
        println(f.name) // print so we can see something is happening
    }
    val time1 = System.currentTimeMillis()

    // Print a results summary
    val totalMessages = imageToMessages.values.sumBy{it.size}
    println("Found ${imageToMessages.size} images with $totalMessages messages in ${(time1-time0)*1e-3} (s)")
}