import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
//import javafx.scene.canvas.GraphicsContext;

public class Camera extends Application{
    private static int startY;
    private static int endY;
    private static int resultY =0;
    private static int resultX =0;
    private static int preY = 0;
    private static int preX = 0;
    private static int setScreenSize = 0;
    private static int setScreenX1 = 0;
    private static int setScreenY1 = 0;
    private static int setScreenX2 = 0;
    private static int setScreenY2 = 0;
    private static int setScreenX3 = 0;
    private static int setScreenY3 = 0;
    private static int setScreenX4 = 0;
    private static int setScreenY4 = 0;
    private static int screenHeight = 480;
    private static int screenWidth = 640;
    private static int clkTime = 0;
    

    Mat webcam_image;
    BufferedImage temp;
    VideoCapture capture;
    Canvas canvas;
    private boolean isShowPreview = false;
    private boolean isShowDraw = true;
    private static boolean isLightOn = false;
	private DaemonThread myThread = null;
    static boolean CHECK_WHITE = false;
    Color lineColor = Color.BLACK;
    WritableImage wImage;
    boolean isInit = true;
    boolean mouceMode = false;
    boolean isClick = false;

    // Create a constructor method
    public Camera() {
        super();
    }
    /**
     * Matの画像を１マスずつアクセスできるBufferedImageに変換する
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

    /*
     * BufferedImageに１マスずつアクセスしているメソッド
     *
     *
     */
    public static void indexCoordinate(BufferedImage image) {
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
                // 輝度が250より大きい所を光源と判断しています
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
            if(setScreenSize<4&&isLightOn==false){
            	switch(setScreenSize){
            	    case 0:
            	    	setScreenX1 = resultX;
            	    	setScreenY1 = resultY;
            	    	break;
            	    case 1:
            	    	setScreenX2 = resultX;
            	    	setScreenY2 = resultY;
            	    	break;
            	    case 2:
            	    	setScreenX3 = resultX;
            	    	setScreenY3 = resultY;
            	    	break;
            	    case 3:
            	    	setScreenX4 = resultX;
            	    	setScreenY4 = resultY;
            	    	break;
            	}
                //setScreenSize++;
            }
            isLightOn=true;
            System.out.println("setScreenSize"+setScreenSize);
        }else{
    	    resultX = 0;
    	    resultY = 0;
        	preX = resultX;
        	preY = resultY;
            isLightOn=false;
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
        canvas  = new Canvas(screenWidth , screenHeight);
        root.getChildren().add( canvas );

        // Load the native library.
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        webcam_image = new Mat();
        capture = new VideoCapture(0);
        capture.set(Videoio.CAP_PROP_FPS, 30);
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, screenWidth);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, screenHeight);
        // シーンを作成
        Scene   scene   = new Scene( root );

        // ウィンドウ表示
        primaryStage.setScene( scene );
        primaryStage.show();
        primaryStage.setOnCloseRequest(req -> {
            mouceMode = false;
        	Platform.exit();
        });

        myThread = new DaemonThread(); //create object of threat class
        Thread t = new Thread(myThread);
        t.setDaemon(true);
        myThread.runnable = true;
        t.start();                 //start thrad
    }


    private void drawCanvas(BufferedImage image, String strFps) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
		if (isInit)
        {
			
			try {
				BufferedImage bi = ImageIO.read(new File(getClass().getResource("/test.png").getPath()));
				Image img = SwingFXUtils.toFXImage(bi,null);
				//screenHeight = (int)img.getHeight();
				//screenWidth = (int)img.getWidth();
				gc.drawImage(img, 80, 0);
            	isInit = false;
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
        }
    	if (isShowPreview) {
    		gc.drawImage(SwingFXUtils.toFXImage(image,null), 0, 0);
    	}
    	if (isShowDraw) {
	    	// グラフィクス・コンテキストを取得し、
	    	// キャンバスに描写
    		if(resultX<50&&resultX!=0){
    			if(resultY<50){
    				gc.clearRect(0, 0, screenWidth, screenHeight);
    				isInit = true;
    			}else if(resultY<100){
    				lineColor = Color.BROWN;
    			}else if(resultY<150){
    				lineColor = Color.CYAN;

    			}else if(resultY<200){
    				lineColor = Color.RED;

    			}else if(resultY<250){
    				lineColor = Color.BLACK;

    			}else if(resultY<300){
    				if (mouceMode){
    					mouceMode = false;
    				}else {
        				mouceMode = true;
    				}
    			}
    		}
    		if(!mouceMode){
	    		if(wImage != null){
	    		    gc.drawImage(wImage, 0, 0);
	    		}
	    		gc.setLineWidth(5);
	    		gc.setStroke(lineColor);
		        if(CHECK_WHITE){
		        	gc.strokeLine( preX,preY,resultX ,resultY );
		        }
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
	        gc.setFill( Color.BLACK );
	        gc.fillRect( 0 ,250  , 50 , 50 );
	        gc.setFill( Color.WHITE );
	        gc.fillRect( 2 ,252  , 46 , 46 );
    	}
		gc.setLineWidth(1);
//        gc.strokeText("clear", 20, 20);
		gc.strokeText(strFps, 20, 20);
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
                            if(setScreenSize>3){
                            	//変換元座標設定
                            	float srcPoint[] = new float[]{
                            			setScreenX1, setScreenY1,
                            			setScreenX2, setScreenY2,
                            			setScreenX3, setScreenY3,
                            			setScreenX4, setScreenY4};
                            	Mat srcPointMat = new Mat(4,2,CvType.CV_32F);
                            	srcPointMat.put(0, 0,srcPoint );
                            	//変換先座標設定
                            	float dstPoint[] = new float[]{0, 0, 0, 480, 640, 480, 640, 0 };
                            	Mat dstPointMat = new Mat(4,2,CvType.CV_32F);
                            	dstPointMat.put(0, 0,dstPoint );
                            	//変換行列作成
                            	Mat r_mat = Imgproc.getPerspectiveTransform(srcPointMat, dstPointMat);
                            	//図形変換処理//                            	Mat dstMat = new Mat(mat.rows(),mat.cols(),mat.type());
                            	Imgproc.warpPerspective(frame, frame, r_mat, frame.size(),Imgproc.INTER_LINEAR);
                            }
                            	buff = matToBufferedImage(frame);
                    		indexCoordinate(buff);
                    		drawCanvas(buff, String.valueOf(fps));
                    		if(wImage != null){
                    		    canvas.snapshot(null, wImage);
                    		}
                            cnt++;
                            if(mouceMode){
                                Robot robo = new Robot();
                                PointerInfo pi = MouseInfo.getPointerInfo();
                                Point mousePoint = pi.getLocation();
								if(CHECK_WHITE){
                                	robo.mouseMove(mousePoint.x+resultX-preX, mousePoint.y+resultY-preY);
                                    if(clkTime<15&&isClick){
                                    	robo.mousePress(InputEvent.BUTTON1_MASK);
                                    	robo.mouseRelease(InputEvent.BUTTON1_MASK);
                                    }
                                    clkTime = 0;
                                    isClick = false;
                                    
                                }else {
                                    clkTime++;
                                    isClick = true;
                                }
                                	
                                
                            }
	                    } catch (Exception ex) {
	                        System.out.printf("Error %s", ex.toString());
	                    }
                    }
            	}
            }
        }
    }
}


