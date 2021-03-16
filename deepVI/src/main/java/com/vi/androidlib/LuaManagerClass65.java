package com.huawei.vi.androidlib;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.util.Log;

/**
 * Created by hongbing on 5/4/16.
 */
public class LuaManagerClass65 {
    private final static String TAG = "LuaManagerClass65";
    private static LuaManagerClass65 mInstance;

    private long mTorchState;
    
    public static final int BITMAP_FORMAT_RGBA    = 0;
    public static final int BITMAP_FORMAT_BGR     = 1;
    
    static {
        System.loadLibrary("torch_class65");
    }
    
    private LuaManagerClass65(Context context) {
        ApplicationInfo info = context.getApplicationInfo();
        Log.e(TAG, "start initTorch");
        mTorchState = initTorch(context.getAssets(), info.nativeLibraryDir);
        Log.e(TAG, "Done initTorch");
    }
    
    @Override
    protected void finalize() throws Throwable {
        destroyTorch(mTorchState);
        super.finalize();
    }
    
    public static LuaManagerClass65 getLuaManager(Context context) {
        if (mInstance == null)
            mInstance = new LuaManagerClass65(context);
        return mInstance;
    }
    
    public float[] getClassificationResults(int width, int height, byte[] bitmapData, int bitmapDataFormat) {
        return getClassificationResults(mTorchState, width, height, bitmapData, bitmapDataFormat);        
    }
    
    private native float[] getClassificationResults(long stateLocation, int width, int height,	byte[] bitmapRGBData, int bitmapDataFormat);

    public native long initTorch(AssetManager manager, String libdir);

	public native void destroyTorch(long stateLocation);
}
