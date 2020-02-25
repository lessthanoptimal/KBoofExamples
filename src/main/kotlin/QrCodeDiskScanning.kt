import boofcv.factory.fiducial.FactoryFiducial
import boofcv.gui.BoofSwingUtil
import boofcv.io.image.UtilImageIO
import boofcv.kotlin.loadImage
import boofcv.struct.image.GrayU8
import boofcv.struct.image.ImageType
import kotlin.system.measureTimeMillis

fun main() {
    // Opens a dialog and let's you select a directory
    val directory = BoofSwingUtil.openFileChooser("QR Disk Scanning",BoofSwingUtil.FileTypes.DIRECTORIES) ?: return

    // Create the scanner class
    val detector = FactoryFiducial.qrcode(null,GrayU8::class.java)

    // Walk through the path recursively, finding all image files, load them, scan for QR codes, add results to a map
    val imageToMessages = mutableMapOf<String,List<String>>()
    val elapsedTime = measureTimeMillis {
        directory.walk().filter {UtilImageIO.isImage(it)}.forEach { f ->
            val image = f.absoluteFile.loadImage(ImageType.SB_U8)
            detector.process(image)
            imageToMessages[f.absolutePath] = detector.detections.map { it.message }
            println(f.name) // print so we can see something is happening
        }
    }

    // Print a results summary
    val totalMessages = imageToMessages.values.sumBy{it.size}
    println("\nFound ${imageToMessages.size} images with $totalMessages messages averaging %.2f img/s".
        format(imageToMessages.size/(elapsedTime*1e-3)))
}