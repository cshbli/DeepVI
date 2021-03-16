package com.huawei.vi.androidlib;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by hongbing on 5/4/16.
 */
public class ViUtils {
    private static String TAG = "ViUtils";
    
    public static double L2_Distance(float[] array1, float[] array2)
    {
        double Sum = 0.0;
        for(int i=0;i<array1.length;i++) {
           Sum = Sum + Math.pow((array1[i]-array2[i]),2.0);
        }
        return Math.sqrt(Sum);
    }
    
    public static File createFileFromAsset(Context mContext, AssetManager am, String fileName) {
        try{
            File f = new File(mContext.getCacheDir()+"/"+fileName);
            if (!f.exists()) {
                InputStream inputStream = am.open(fileName);
                OutputStream outputStream = new FileOutputStream(f);

                byte buffer[] = new byte[1024];
                int length;
                while((length=inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer,0,length);
                }

                outputStream.close();
                inputStream.close();
            }

            return f;
        }catch (IOException e) {
            //Logging exception
            e.printStackTrace();
        }

        return null;
    }
    
    public static Bitmap getBitmapFromAsset(AssetManager assetManager, String fileName)
    {    
        InputStream istr = null;
        try {
            istr = assetManager.open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        return bitmap;
    }
    
    /**
     * 
     * @return Resource's RGBA byte array
     */
    public static byte[] getImageRGBA(Resources res, int resouceId, int width, int height) {
        Bitmap bitmap = BitmapFactory.decodeResource(res, resouceId);
        if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
        }
        return getImageRGBA(bitmap, width, height);
    }

    /**
     * 
     * @return Bitmap's RGBA byte array
     */
    public static byte[] getImageRGBA(Bitmap inputBitmap, int width, int height) {
        Config config = inputBitmap.getConfig();
        ByteBuffer buffer;

        Bitmap bitmap;
        /**
         * if bitmap size is not width*height create scaled bitmap
         */
        
        if (inputBitmap.getWidth() != width || inputBitmap.getHeight() != height) {          
            Log.i(TAG, "bitmap resized: " + inputBitmap.getWidth() + "x" + inputBitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(inputBitmap, width, height, false);
        } else {
            bitmap = inputBitmap;
        }
        
        /**
         * if bitmap is not ARGB_8888 format, copy bitmap with ARGB_8888 format
         */
        if (!config.equals(Bitmap.Config.ARGB_8888)) {
            Bitmap bitmapARBG = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            buffer = ByteBuffer.allocate(bitmapARBG.getByteCount());
            bitmapARBG.copyPixelsToBuffer(buffer);
            bitmapARBG.recycle();
        } else {
            buffer = ByteBuffer.allocate(bitmap.getByteCount());
            bitmap.copyPixelsToBuffer(buffer);
        }
        return buffer.array();
    }
}
