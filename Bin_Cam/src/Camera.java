import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.awt.image.WritableRaster;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class Camera extends JPanel {
    private static final long serialVersionUID = 1L;
    private BufferedImage image;
    private static LookupOp lookUpTable;

    // Create a constructor method
    public Camera() {
        super();
        Camera.lookUpTable = this.getLookUpTable();
    }
    private BufferedImage getimage() {
        return image;
    }
    private void setimage(BufferedImage newimage) {
        image = newimage;
        return;
    }
    /**
     * Converts/writes a Mat into a BufferedImage.
     *
     * @param matrix Mat of type CV_8UC3 or CV_8UC1
     * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
     */
    public static BufferedImage matToBufferedImage(Mat matrix) {
        int cols = matrix.cols();
        int rows = matrix.rows();
        int elemSize = (int)matrix.elemSize();
        byte[] data = new byte[cols * rows * elemSize];
        int type;
        matrix.get(0, 0, data);
        switch (matrix.channels()) {
        case 1:
            type = BufferedImage.TYPE_BYTE_GRAY;
            break;
        case 3:
            type = BufferedImage.TYPE_3BYTE_BGR;
            // bgr to rgb
            byte b;
            for(int i = 0; i < data.length; i = i + 3) {
                b = data[i];
                data[i] = data[i+2];
                data[i + 2] = b;
            }
            break;
        default:
            return null;
        }
        BufferedImage image2 = new BufferedImage(cols, rows, type);
        image2.getRaster().setDataElements(0, 0, cols, rows, data);
        return image2;
    }
    public void paintComponent(Graphics g) {
        BufferedImage temp = getimage();
        if(temp != null) {
            g.drawImage(temp,10,10,temp.getWidth(),temp.getHeight(), this);
        }
    }
    
    public static void toGrayScale(BufferedImage image) {

        WritableRaster raster = image.getRaster();

        int[] pixelBuffer = new int[raster.getNumDataElements()];

        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < raster.getWidth(); x++) {
                // ピクセルごとに処理

                raster.getPixel(x, y, pixelBuffer);

                // 単純平均法((R+G+B)/3)でグレースケール化したときの輝度取得
                int pixelAvg = (pixelBuffer[0] + pixelBuffer[1] + pixelBuffer[2]) / 3;
                // RGBをすべてに同値を設定することでグレースケール化する
                pixelBuffer[0] = pixelAvg;
                pixelBuffer[1] = pixelAvg;
                pixelBuffer[2] = pixelAvg;

                raster.setPixel(x, y, pixelBuffer);
            }
        }
    }

    public LookupOp getLookUpTable() {

        byte[] lookUpTable = new byte[256];

        for (int i = 0; i < 256; i++) {
            // 閾値により、白・黒どちらを返すか決定
            if (i > 125) {
                lookUpTable[i] = (byte) 255;
            } else {
                lookUpTable[i] = (byte) 0;
            }
        }
        return new LookupOp(new ByteLookupTable(0, lookUpTable), null);
    }

    public static void main(String arg[]) {
        // Load the native library.
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        JFrame frame = new JFrame("BasicPanel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400,400);
        Camera panel = new Camera();
        frame.setContentPane(panel);
        frame.setVisible(true);
        Mat webcam_image = new Mat();
        BufferedImage temp;
        VideoCapture capture = new VideoCapture(0);

        if( capture.isOpened()) {
            while( true ) {
                capture.read(webcam_image);
                if( !webcam_image.empty() ) {
//                    Imgproc.threshold(webcam_image,webcam_image, 0, 255.0, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
                    Imgproc.resize(webcam_image, webcam_image, new Size(webcam_image.size().width,webcam_image.size().height));
                    frame.setSize(webcam_image.width()+40,webcam_image.height()+60);
                    temp = matToBufferedImage(webcam_image);
                    toGrayScale(temp);
                    temp = lookUpTable.filter(temp, null);
                    panel.setimage(temp);
                    panel.repaint();
                } else {
                    System.out.println(" --(!) No captured frame -- ");
                }
            }
        }
        return;
    }
}
