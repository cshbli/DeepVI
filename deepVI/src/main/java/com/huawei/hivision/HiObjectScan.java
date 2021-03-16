package com.huawei.hivision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowObjectDetectionAPIModel;
import org.tensorflow.demo.env.ImageUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by hongbing on 1/30/18.
 */

public class HiObjectScan {
    private static final String TAG = "HiObjectScan";

    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    public class Recognition {
        /**
         * Display name for the recognition.
         */
        private final String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private final Float confidence;

        /**
         * Optional location within the source image for the location of the recognized object.
         */
        private RectF location;

        public Recognition(final String title, final Float confidence, final RectF location) {
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }
    }

    private Classifier tfClassifier;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final float TF_OD_MINIMUM_CONFIDENCE = 0.25f;

    /**
     * Initializes a hiscan session for classifying images.
     *
     * @param myContext The application context to be used to load assets.
     * @throws IOException
     */
    public HiObjectScan(Context myContext) {
        try {
            tfClassifier = TensorFlowObjectDetectionAPIModel.create(
                    myContext.getAssets(),
                    "file:///android_asset/object_scanner_ssd_mobilenet.pb",
                    "file:///android_asset/object_scanner_labels_list.txt",
                    TF_OD_API_INPUT_SIZE);
            //cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            Log.e(TAG, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            myContext, "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Classify an input image
     *
     * @param bmp the input image bitmap
     * @return A list of label and confidence pair, can be null if the image not belongs to one the 11 top classes
     */
    public List<HiObjectScan.Recognition> recognizeImage(final Bitmap bmp) {
        Bitmap ODCroppedBitmap = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);
        Matrix ODFrameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        bmp.getWidth(), bmp.getHeight(),
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        0, true);

        Matrix ODCropToFrameTransform = new Matrix();
        ODFrameToCropTransform.invert(ODCropToFrameTransform);

        final Canvas ODCanvas = new Canvas(ODCroppedBitmap);
        ODCanvas.drawBitmap(bmp, ODFrameToCropTransform, null);

        List<Classifier.Recognition> ODResults = tfClassifier.recognizeImage(ODCroppedBitmap);
        List<HiObjectScan.Recognition> results = new LinkedList<HiObjectScan.Recognition>();

        for (final Classifier.Recognition recog : ODResults) {
            final RectF location = recog.getLocation();
            if (location != null && recog.getConfidence() >= TF_OD_MINIMUM_CONFIDENCE) {
                ODCropToFrameTransform.mapRect(location);
                results.add(new HiObjectScan.Recognition(recog.getTitle(), recog.getConfidence(), location));
            }
        }

        if (results.size() < 1) {
            return null;
        } else {
            return results;
        }
    }
}
