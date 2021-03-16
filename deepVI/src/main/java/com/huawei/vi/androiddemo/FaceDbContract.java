/**
 * 
 */
package com.huawei.vi.androiddemo;

import android.provider.BaseColumns;

/**
 * @author hongbing
 *
 */
public class FaceDbContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public FaceDbContract() {}
    
    /* Inner class that defines the table contents */
    public static abstract class FaceEntry implements BaseColumns {
        public static final String TABLE_NAME = "face";
        public static final String FACE_COLUMN_NAME = "name";
        public static final String FACE_COLUMN_FEATURES = "features";        
    }
}
