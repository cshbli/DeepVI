/*
*  
*/

package com.huawei.vi.androidlib;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import org.opencv.core.Mat;

/**
 *
 */
public class FaceAlignment {
    private static final String TAG = "FaceAlignment";
    private int FROM_ASSET = 0;
        
    static {
        try {
            System.loadLibrary("face_align");
            Log.i(TAG, "face_align library loaded success");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "face_align library not found!");
        }
    }
                   
    public int detFaceAndAlign(Context mContext, Mat srcImage, Mat faceImage) {
        String modelPath;
        if (FROM_ASSET == 1) {
            AssetManager am = mContext.getAssets();
            modelPath = ViUtils.createFileFromAsset(mContext, am, "shape_predictor_68_face_landmarks.dat").getPath();
        }
        else {            
            File sdcard = Environment.getExternalStorageDirectory();
            modelPath = sdcard.getAbsolutePath() + File.separator + "deepvi_models"+ File.separator + "shape_predictor_68_face_landmarks.dat";            
        }        
        
        return DLibFaceDetectAndAlign(srcImage.getNativeObjAddr(), modelPath, faceImage.getNativeObjAddr());
    }

    public void init() {
        jniInit();
    }

    public void deInit() {
        jniDeInit();
    }    

    private native int jniInit();

    private native int jniDeInit();
    
    private native int DLibFaceDetectAndAlign(long srcImage, String landmarkPath, long faceImage);
}
