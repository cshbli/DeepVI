package com.huawei.vi.androiddemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;


import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.opencv.android.OpenCVLoader;
import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowImageClassifier;
import org.tensorflow.demo.TensorFlowObjectDetectionAPIModel;
import org.tensorflow.demo.TensorFlowYoloDetector;
import org.tensorflow.demo.env.ImageUtils;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import com.huawei.vi.androidlib.ViUtils;
import com.huawei.vi.androidlib.caffe.CaffeMobile;

import com.huawei.hivision.hiscan.HiScan;
import com.huawei.hivision.HiObjectScan;
import com.huawei.hivision.hiscanlite.HiScanLite;

public class MainActivity extends AppCompatActivity implements CNNListener {
    private static final String TAG = "MainActivity";
    
    private static final int RESULT_EXTERNAL_STORAGE = 2;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    
    private static String[] RAW_920_CLASSES;
    private static String[] NJNET_CLASSES;
    private static String[] NJNET_THRESHOLD; 
    private static String[] IMAGENET_CLASSES;
    private static String[] SCENE_CLASSES;
    private static String[] SCENE_CLASS10;
    private int FROM_ASSET = 1;

        
    private RecognitionScoreView ivCaptured;
    private TextView tvLabel;
    private Uri fileUri;
    private ProgressDialog dialog;
    private Bitmap bmp;
    private CaffeMobile caffeObject;
    
    private float[] objectProbs;
    private int[] mappingResults;
    private float[] mappingProbs;
    
    String modelPath;    
    
    private Toolbar toolbar;

    private static final int MODEL_SCANNER_MOBILENET_0_5_224 = 0;
    private static final int MODEL_SCANNER_MOBILENET_0_5_224_LITE = 1;
    private static final int MODEL_SSD_MOBILENET = 2;

    final CharSequence modelList[] = {"Scanner MobileNet_0.5_224",
                                      "Scanner MoibleNet_0.5_224 Lite",
                                      "Object Detection SSD MobileNet"};
    private int modelSelected = MODEL_SCANNER_MOBILENET_0_5_224;
    private int prevModelSelected = MODEL_SCANNER_MOBILENET_0_5_224;

    private HiScan scanner;
    private List<HiScan.Recognition> scannerResults;
    private List<Classifier.Recognition> tfResults;

    private HiObjectScan objectScanner;
    private List<HiObjectScan.Recognition> objectScannerResults;

    private HiScanLite scannerLite;

    Detector<Face> safeFaceDetector;
    public SparseArray<Face> mFaces;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.e(TAG, "OpenCV Initialization error");
        }
    }

    public void addPicToGallery(Context context, Uri contentUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }
    
    public void initToolBar() {
    	// Find the toolbar view inside the activity layout
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        
        //toolbar.setTitle(R.string.toolbarTitle);

        // Sets the Toolbar to act as the ActionBar for this Activity window
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);
 
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				switch (menuItem.getItemId()) {
					case R.id.miGallery:
						initPrediction();
		                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		                startActivityForResult(i, REQUEST_IMAGE_SELECT);
		                return true;
		                
					case R.id.miCamera:
						initPrediction();		                
		                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
		                Intent i2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		                i2.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
		                startActivityForResult(i2, REQUEST_IMAGE_CAPTURE);
		                return true;
		                
					case R.id.action_settings:
						showModelSelectionDialog();
				}

				return false;
			}
		});
    }

    private void switchModel() {
    	// ImageNet image mean values: B: 104.007, G: 116.669, R: 122.679
        float[] meanValues = {104.007f, 116.669f, 122.679f};
        
    	switch (modelSelected) {
            case MODEL_SCANNER_MOBILENET_0_5_224:
                scanner = new HiScan(this);
                break;

            case MODEL_SCANNER_MOBILENET_0_5_224_LITE:
                scannerLite = new HiScanLite(this);
                break;

            case MODEL_SSD_MOBILENET:
                objectScanner = new HiObjectScan(this);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initToolBar();
        
        isExternalStorageWritable();
        isExternalStorageReadable();
        
        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        Log.e(TAG, "Current api version: " + currentapiVersion);
        if (currentapiVersion >= Build.VERSION_CODES.M && verifyStoragePermissions(this)) {
            verifyStoragePermissions(this);
        }
        
        modelPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "deepvi_models"+ File.separator;

        //ivCaptured = (FaceView) findViewById(R.id.ivCaptured);
        //ivCaptured = (ImageView) findViewById(R.id.ivCaptured);
        ivCaptured = (RecognitionScoreView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) findViewById(R.id.tvLabel);

        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setMode(FaceDetector.FAST_MODE)
                .build();

        // This is a temporary workaround for a bug in the face detector with respect to operating
        // on very small images.  This will be fixed in a future release.  But in the near term, use
        // of the SafeFaceDetector class will patch the issue.
        safeFaceDetector = new SafeFaceDetector(faceDetector);
        
        switchModel();
    }    
    
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */    
    private static boolean verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (write_permission != PackageManager.PERMISSION_GRANTED || read_persmission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    RESULT_EXTERNAL_STORAGE
            );
            return false;
        } else {
            return true;
        }
    }

    /* Checks if external storage is available for read and write */    
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */    
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /* if the bitmap is too big, it will cause canvas to crash*/
    private void scaleBmp() {
        Bitmap resizedBmp;

        // scale down the bitmap to speed-up the face detection
        if (bmp.getWidth() * bmp.getHeight() > 10000000L) {
            resizedBmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 4, bmp.getHeight() / 4, true);

        } else if (bmp.getWidth() * bmp.getHeight() > 4000000L) {
            resizedBmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 2, bmp.getHeight() / 2, true);

        } else {
            resizedBmp = bmp;

        }

        //Bitmap resizedBmp = Bitmap.createScaledBitmap(bmp, 224, 224, true);

        bmp = resizedBmp;
    }

    public void detectFace(Bitmap bmp) {
        if (!safeFaceDetector.isOperational()) {
            Toast.makeText(this, "Face Detector dependencies are not yet available.", Toast.LENGTH_SHORT).show();

        } else {
            long startTime = SystemClock.uptimeMillis();
            Frame frame = new Frame.Builder().setBitmap(bmp).build();
            mFaces = safeFaceDetector.detect(frame);
            Log.i(TAG, String.format("Google face detection elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));

            // check if we detect any faces
            Log.i(TAG, "Faces detected: " + mFaces.size());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
            String imgPath;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imgPath = fileUri.getPath();

                // Add this picture to gallery
                addPicToGallery(getApplicationContext(), fileUri);
            } else {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }          

            bmp = BitmapFactory.decodeFile(imgPath);
            scaleBmp();
            Log.i(TAG, "Image path: " + imgPath);

            // Do face detection first
            /*
            long startTime = SystemClock.uptimeMillis();
            detectFace(bmp);
            Log.e(TAG, String.format("Face detection elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            */


            dialog = ProgressDialog.show(MainActivity.this, "Predicting...", "Wait for one sec...", true);            

            CNNTask cnnTask = new CNNTask(MainActivity.this);        
            cnnTask.execute(imgPath);
        } 

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initPrediction() {        
        tvLabel.setText("");
    }

    private class CNNTask extends AsyncTask<String, Void, int[]> {
        private CNNListener listener;
        private long startTime;
        private long executionTime;

        public CNNTask(CNNListener listener) {        
            this.listener = listener;
        }

        @Override
        protected int[] doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
                        
            int[] results;            
            
            switch (modelSelected) {
            case MODEL_SCANNER_MOBILENET_0_5_224:
                scannerResults = scanner.recognizeImage(bmp);
                Log.e(TAG, String.format("Model running time: %d ms", SystemClock.uptimeMillis() - startTime));
                tfResults = scanner.getTfResults();
                if (scannerResults != null) {
                    for (final HiScan.Recognition recog : scannerResults) {
                        Log.e(TAG, recog.getTitle() + ": " + recog.getConfidence());
                    }
                }
                results = new int[1];
	            break;

            case MODEL_SCANNER_MOBILENET_0_5_224_LITE:
                scannerLite.recognizeImage(bmp);
                Log.e(TAG, String.format("Model running time: %d ms", SystemClock.uptimeMillis() - startTime));
                results = new int[1];
                break;

            case MODEL_SSD_MOBILENET:
                objectScannerResults = objectScanner.recognizeImage(bmp);

                if (objectScannerResults != null) {
                    for (final HiObjectScan.Recognition recog : objectScannerResults) {
                        Log.e(TAG, recog.getTitle() + ": " + recog.getConfidence() + ", " + recog.getLocation().width() + ", " + recog.getLocation().height());
                    }
                }

                results = new int[1];
                break;

            default:
            	results = new int[1];
            	break;
            }
            
            return results;
        }

        @Override
        protected void onPostExecute(int[] results) {
        	executionTime = SystemClock.uptimeMillis() - startTime;
            Log.e(TAG, String.format("elapsed wall time: %d ms", executionTime));
            listener.onTaskCompleted(results, executionTime);
            super.onPostExecute(results);
        }
    }

    @Override
    public void onTaskCompleted(int[] results, long executionTime) {
        
        //ivCaptured.setImageBitmap(bmp);
        if (modelSelected == MODEL_SSD_MOBILENET) {
            ivCaptured.setContent(bmp, objectScannerResults);
        } else {
            ivCaptured.setContent(bmp);
        }
        
        StringBuilder sb = new StringBuilder();        
        sb.append("Time: " + executionTime + "ms");
        sb.append(System.getProperty("line.separator"));
        
        switch (modelSelected) {


        case MODEL_SCANNER_MOBILENET_0_5_224:
            if (scannerResults != null) {
                for (final HiScan.Recognition recog : scannerResults) {
                    //sb.append(scanner.getChineseLabel(recog.getTitle()) + ": " + String.format("%.02f", recog.getConfidence()));
                    sb.append(recog.getTitle() + ": " + String.format("%.02f", recog.getConfidence()));
                    sb.append(System.getProperty("line.separator"));
                }
            }

            if (tfResults != null) {
                int i = 0;
                sb.append(System.getProperty("line.separator"));
                for (final Classifier.Recognition rawRecog : tfResults) {
                    //sb.append(scanner.getChineseLabel(recog.getTitle()) + ": " + String.format("%.02f", recog.getConfidence()));
                    sb.append(rawRecog.getTitle() + ": " + String.format("%.03f", rawRecog.getConfidence()));
                    sb.append(System.getProperty("line.separator"));
                    i++;
                    if (i >= 5) {
                        break;
                    }
                }
            }

            break;
        }

        tvLabel.setText(sb.toString());
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        //Log.e(LOG_TAG, "Entering function getOutputMediaFile");
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "DeepVI");
        //File root = Environment.getExternalStorageDirectory();
        //String baseDir = root.getAbsolutePath()+ "/DCIM/Camera";
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
            //mediaFile = new File(baseDir + File.separator + "IMG_" + timeStamp + ".jpg");
            //Log.e(TAG, "Media File: " + baseDir + File.separator + "IMG_" + timeStamp + ".jpg");
        } else {
            //Log.e(TAG, "Failed to create media file");
            return null;
        }

        return mediaFile;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void showModelSelectionDialog() {
    	prevModelSelected = modelSelected;
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);    	
    	builder.setTitle("Please select one model to use")
    	.setSingleChoiceItems(modelList, modelSelected, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				modelSelected = which;
			}
		})
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();			
				
				switchModel();
				
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) { 
				modelSelected = prevModelSelected;
				dialog.dismiss();
			}
		});
    	builder.create();
    	builder.show();
    }       
}
