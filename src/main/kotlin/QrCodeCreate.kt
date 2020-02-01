import boofcv.alg.fiducial.qrcode.QrCodeEncoder
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage
import boofcv.gui.image.ShowImages
import boofcv.io.image.ConvertBufferedImage

fun main() {
    //======================================================================================
    // Create the QrCode data structure with your message.

    val qr = QrCodeEncoder().addAutomatic("Hello World! こんにちは、 世界！").fixate();
    // QrCodeEncoder is very configurable and you can directly control most low parameters too!

    //======================================================================================
    // Render the QR Code into a BoofCV style image. It's also possible to create PDF documents
    // 15 = pixelsPerModule (square)
    val generator = QrCodeGeneratorImage(15).render(qr)
    // It's also possible to generate PDF documents and there is a GUI application available with batch options

    //======================================================================================
    // Convert into a BufferedImage for display and saving to disk
    val buffered = ConvertBufferedImage.convertTo(generator.gray,null);

    ShowImages.showWindow(buffered,"Your QR Code", true);

    // Uncomment to save it to disk as PNG image
    // UtilImageIO.saveImage(buffered,"qrcode.png");
}