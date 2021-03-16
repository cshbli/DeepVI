package com.huawei.vi.androiddemo;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

public class FaceView extends View {

    private Bitmap mBitmap;
    private SparseArray<Face> mFaces;
    private int faceScale = 1;
    private int xOffset = 0;
    private int yOffset = 0;
    private ArrayList<String> faceNames;

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the bitmap background and the associated face detections.
     */
    void setContent(Bitmap bitmap, SparseArray<Face> faces, int faceScale, ArrayList<String> faceNames) {
        mBitmap = bitmap;
        mFaces = faces;
        this.faceScale = faceScale;
        this.faceNames = faceNames;
        invalidate();
    }

    /**
     * Draws the bitmap background and the associated face landmarks.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if ((mBitmap != null) && (mFaces != null)) {
            double scale = drawBitmap(canvas);
            
            if (mFaces.size() > 0) {
	            drawFaceRectangle(canvas, scale);
	            //  drawFaceAnnotations(canvas, scale);
	            //detectFaceCharacteristics(canvas, scale);
	            drawFaceNames(canvas, scale, faceNames);
            }
        }
    }

    /**
     * Draws the bitmap background, scaled to the device size.  Returns the scale for future use in
     * positioning the facial landmark graphics.
     */
    private double drawBitmap(Canvas canvas) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        
        xOffset = (int)(viewWidth - imageWidth * scale) / 2;
        yOffset = (int)(viewHeight - imageHeight * scale) / 2;
        
        Rect destBounds = new Rect(xOffset, yOffset, (int)(imageWidth * scale) + xOffset, (int)(imageHeight * scale) + yOffset);
        canvas.drawBitmap(mBitmap, null, destBounds, null);
        return scale;
    }

    /**
     * Draws a rectangle around each detected face
     */
    private void drawFaceRectangle(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            canvas.drawRect((float)(Math.max(face.getPosition().x, 0) * faceScale * scale + xOffset),
                    (float)(Math.max(face.getPosition().y, 0) * faceScale * scale + yOffset),
                    (float)((face.getPosition().x + face.getWidth()) * faceScale * scale + xOffset),
                    (float)((face.getPosition().y + face.getHeight()) * faceScale * scale + yOffset),
                    paint);
        }
    }

    /**
     * Draws a small circle for each detected landmark, centered at the detected landmark position.
     * <p>
     *
     * Note that eye landmarks are defined to be the midpoint between the detected eye corner
     * positions, which tends to place the eye landmarks at the lower eyelid rather than at the
     * pupil position.
     */
    private void drawFaceAnnotations(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                canvas.drawCircle(cx, cy, 10, paint);
            }
        }

    }

    /**
     * Detects characteristics of a face
     */
    private void detectFaceCharacteristics(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextSize(25.0f);

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            float cx = (float)(face.getPosition().x * faceScale * scale + xOffset);
            float cy = (float) (face.getPosition().y * faceScale * scale + yOffset);
            canvas.drawText(String.valueOf(face.getIsSmilingProbability()), cx, cy + 10.0f, paint);
        }
    }

    private void drawFaceNames(Canvas canvas, double scale, ArrayList<String> faceNames) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextSize(50.0f);
        
        for (int i = 0; i < mFaces.size(); ++i) {        
	        Face face = mFaces.valueAt(i);
	        float cx = (float)(Math.max(face.getPosition().x, 0) * faceScale * scale + xOffset);
	        float cy = (float)(Math.max(face.getPosition().y, 0) * faceScale * scale + yOffset);
	        canvas.drawText(faceNames.get(i), cx, cy + 10.0f, paint);
        }        
    }
}
