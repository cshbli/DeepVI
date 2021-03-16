package com.huawei.vi.androidlib;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;

/**
 * Created by hongbing on 5/4/16.
 */
public class LuaManager {
    private final String TAG = "LuaManager";
    private static LuaManager mInstance;

    private long mTorchState;
    
    public static final int BITMAP_FORMAT_RGBA    = 0;
    public static final int BITMAP_FORMAT_BGR     = 1;
    
    static {
        System.loadLibrary("torch_openface");
    }
    
    private LuaManager(Context context) {
        ApplicationInfo info = context.getApplicationInfo();
        mTorchState = initTorch(context.getAssets(), info.nativeLibraryDir);
    }
    
    @Override
    protected void finalize() throws Throwable {
        destroyTorch(mTorchState);
        super.finalize();
    }
    
    public static LuaManager getLuaManager(Context context) {
        if (mInstance == null)
            mInstance = new LuaManager(context);
        return mInstance;
    }
    
    public float[] getFaceDescriptor(int width, int height, byte[] bitmapData, int bitmapDataFormat) {
        return getFaceDescriptor(mTorchState, width, height, bitmapData, bitmapDataFormat);        
    }
    
    private native float[] getFaceDescriptor(long stateLocation, int width, int height,	byte[] bitmapRGBData, int bitmapDataFormat);

    public native long initTorch(AssetManager manager, String libdir);

	public native void destroyTorch(long stateLocation);
}
