import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

//import javafx.scene.canvas.GraphicsContext;

public class Camera extends Application{
    private static int startY;
    private static int endY;
    private static int resultY =0;
    private static int resultX =0;
    private static int preY = 0;
    private static int preX = 0;
    Mat webcam_image;
    BufferedImage temp;
    VideoCapture capture;
    Canvas canvas;
    private boolean isShowPreview = false;
    private boolean isShowDraw = true;
	private DaemonThread myThread = null;
    static boolean CHECK_WHITE = false;
    Color lineColor = Color.BLACK;

    // Create a constructor method
    public Camera() {
        super();
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
        BufferedImage image = new BufferedImage(cols, rows, type);
        image.getRaster().setDataElements(0, 0, cols, rows, data);
        return image;
    }

    public static void toGrayScale(BufferedImage image) {
        WritableRaster raster = image.getRaster();

        int[] pixelBuffer = new int[raster.getNumDataElements()];

        startY = 0;
        endY = 0;
        CHECK_WHITE = false;
        for (int y = 0; y < raster.getHeight(); y++) {

            for (int x = 0; x < raster.getWidth(); x++) {
                raster.getPixel(x, y, pixelBuffer);

                // 単純平均法((R+G+B)/3)でグレースケール化したときの輝度取得
                int pixelAvg = (pixelBuffer[0] + pixelBuffer[1] + pixelBuffer[2]) / 3;
                // RGBをすべてに同値を設定することでグレースケール化する
                if(pixelAvg > 250){
                	preX = resultX;
                	preY = resultY;
                    resultX=x;
                    startY=y;
                    for(int pixelAvg2 = pixelAvg;pixelAvg2 > 250;y++){
                        if(y >=raster.getHeight()){
                            break;
                        }
                        pixelAvg2 = (pixelBuffer[0] + pixelBuffer[1] + pixelBuffer[2]) / 3;
                        raster.getPixel(x, y, pixelBuffer);
                    }
                    endY=y;
                    resultY = (startY + endY)/2;
                    CHECK_WHITE = true;
                    if(preX == 0 && preY == 0){
                    	preX = resultX;
                    	preY = resultY;
                    }
                    break;
                }
            }
            if(CHECK_WHITE){
                break;
            }
        }
        if(CHECK_WHITE){
            System.out.println("marker is "+resultX+","+resultY+".");
        }else{
    	    resultX = 0;
    	    resultY = 0;
        	preX = resultX;
        	preY = resultY;
        }
    }

    public static void main(String arg[]) {
        launch( arg );
        return;
    }
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        // シーングラフを作成
        Group   root    = new Group();
        // キャンバスを作成
        canvas  = new Canvas(640, 480);
        root.getChildren().add( canvas );

        // Load the native library.
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        webcam_image = new Mat();
        capture = new VideoCapture(0);
        // シーンを作成
        Scene   scene   = new Scene( root );

        // ウィンドウ表示
        primaryStage.setScene( scene );
        primaryStage.show();

        if (true) {
	        myThread = new DaemonThread(); //create object of threat class
	        Thread t = new Thread(myThread);
	        t.setDaemon(true);
	        myThread.runnable = true;
	        t.start();                 //start thrad
        } else {
	        if( capture.isOpened()) {
	                Timeline timer = new Timeline(new KeyFrame( Duration.millis(30),new EventHandler<ActionEvent>(){
	                        @Override
	                        public void handle(ActionEvent event){
	                                capture.read(webcam_image);
	                            if( !webcam_image.empty() ) {
	                        temp = matToBufferedImage(webcam_image);
	//                        toGrayScale(temp);
	                        drawCanvas(temp);
	                       ;
	                    } else {
	                        System.out.println(" --(!) No captured frame -- ");
	                    }
	                        }
	                }));

	            timer.setCycleCount(Timeline.INDEFINITE);
	            timer.play();
	        }
        }
    }

    private void drawCanvas(BufferedImage image) {
    	drawCanvas(image, "0");
    }

    private void drawCanvas(BufferedImage image, String strFps) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
    	if (isShowPreview) {
    		gc.drawImage(SwingFXUtils.toFXImage(image,null), 0, 0);
    	}
    	if (isShowDraw) {
	    	// グラフィクス・コンテキストを取得し、
	    	// キャンバスに描写
    		if(resultX<50&&resultX!=0){
    			if(resultY<50){
    				  gc.clearRect(0, 0, 640, 480);
    			}else if(resultY<100){
    				lineColor = Color.BROWN;
    			}else if(resultY<150){
    				lineColor = Color.CYAN;
    				
    			}else if(resultY<200){
    				lineColor = Color.RED;
    				
    			}else if(resultY<250){
    				lineColor = Color.BLACK;
    				
    			}
    		}
    		gc.setLineWidth(5);
    		gc.setStroke(lineColor);
	        if(CHECK_WHITE){
	        	gc.strokeLine( preX,preY,resultX ,resultY );
	        }
	        gc.setFill( Color.WHITE );
	        gc.fillRect( 0 ,0  , 50 , 50 );
	        gc.setFill( Color.BROWN );
	        gc.fillRect( 0 ,50  , 50 , 50 );
	        gc.setFill( Color.CYAN );
	        gc.fillRect( 0 ,100  , 50 , 50 );
	        gc.setFill( Color.RED);
	        gc.fillRect( 0 ,150  , 50 , 50 );
	        gc.setFill( Color.BLACK);
	        gc.fillRect( 0 ,200  , 50 , 50 );
    	}
		gc.setLineWidth(1);
        gc.strokeText("clear", 20, 20);
    }

    class DaemonThread implements Runnable {
        Mat frame = new Mat();
    	double startTime,nowTime, diffTime;
    	int fps = 0;
    	int cnt = 0;
    	int oldcnt = 0;
        MatOfByte mem = new MatOfByte();
    	final double f = (1000 /Core.getTickFrequency());

        protected volatile boolean runnable = false;

        @Override
        public void run() {
            synchronized (this) {
            	startTime = Core.getTickCount();
            	while (runnable) {
                    if (capture.grab()) {
                        try {
                        	capture.retrieve(frame);

                            nowTime = Core.getTickCount();
                            diffTime = (int)((nowTime- startTime)*f);

                            if (diffTime >= 1000) {
                             startTime = nowTime;
                             fps = cnt - oldcnt;
                             oldcnt = cnt;
                            }
                            BufferedImage buff;
                            if (true) {
                            	buff = matToBufferedImage(frame);
                            } else {
                            	Imgcodecs.imencode(".bmp", frame, mem);
	                            Image im = ImageIO.read(new ByteArrayInputStream(mem.toArray()));
	                    		buff = (BufferedImage) im;
                            }
                    		toGrayScale(buff);
                    		drawCanvas(buff, String.valueOf(fps));
                            cnt++;
	                    } catch (Exception ex) {
	                        System.out.printf("Error %s", ex.toString());
	                    }
                    }
            	}
            }
        }
    }
}


