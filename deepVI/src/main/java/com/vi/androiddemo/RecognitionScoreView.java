package com.vi.androiddemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.vi.hivision.HiObjectScan;

import org.tensorflow.demo.env.BorderedText;


import java.util.List;

/**
 * Created by hongbing on 5/3/17.
 */

public class RecognitionScoreView extends View {
    private Bitmap mBitmap;
    private List<HiObjectScan.Recognition> results;
    private int xOffset = 0;
    private int yOffset = 0;

    private final Paint boxPaint = new Paint();

    private final float textSizePx;
    private final BorderedText borderedText;

    private static final float TEXT_SIZE_DIP = 18;
    private static final int[] COLORS = {
            Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE,
            Color.parseColor("#55FF55"), Color.parseColor("#FFA500"), Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"), Color.parseColor("#FFFFAA"), Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"), Color.parseColor("#0D0068")
    };

    public RecognitionScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);

        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(12.0f);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);
        boxPaint.setStrokeMiter(100);

        textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
    }

    /**
     * Sets the bitmap background
     */
    public void setContent(Bitmap bitmap) {
        mBitmap = bitmap;
        invalidate();
    }

    /**
     * Sets the bitmap background and the detected objects
     */
    public void setContent(Bitmap bitmap, final List<HiObjectScan.Recognition> results) {
        mBitmap = bitmap;
        this.results = results;
        invalidate();
    }

    /**
     * Draws the bitmap background, scaled to the device size.  Returns the scale for future use in
     * positioning the detected objects.
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

        /*
        Matrix frameToCanvasMatrix =
            ImageUtils.getTransformationMatrix(
                mBitmap.getWidth(),
                mBitmap.getHeight(),
                (int)(scale * mBitmap.getWidth()),
                (int)(scale * mBitmap.getHeight()),
                0,
                false);

        canvas.drawBitmap(mBitmap, frameToCanvasMatrix, null);
        */

        return scale;
    }

    /**
     * Draws a rectangle around each detected face
     */
    private void drawObjectsBox(Canvas canvas, double scale) {
        /*
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            canvas.drawRect((float)(Math.max(location.left, 0) * scale + xOffset),
                    (float)(Math.max(location.top, 0) * scale + yOffset),
                    (float)(location.right * scale + xOffset),
                    (float)(location.bottom * scale + yOffset),
                    paint);
        }
        */
        int i = 0;
        for (final HiObjectScan.Recognition result : results) {
            final RectF location = result.getLocation();
            boxPaint.setColor(COLORS[i]);
            i = (i + 1) % COLORS.length;
            canvas.drawRect((float)(Math.max(location.left, 0) * scale + xOffset),
                    (float)(Math.max(location.top, 0) * scale + yOffset),
                    (float)(location.right * scale + xOffset),
                    (float)(location.bottom * scale + yOffset),
                    boxPaint);

            final String labelString =
                    !TextUtils.isEmpty(result.getTitle())
                            ? String.format("%s %.2f", result.getTitle(), result.getConfidence())
                            : String.format("%.2f", result.getConfidence());
            borderedText.drawText(canvas, (float)(Math.max(location.left, 0) * scale + xOffset),
                                          (float)(location.bottom * scale + yOffset),
                                          labelString);
        }
    }

    /**
     * Draws the bitmap background and the associated detected objects.
     */
    @Override
    public void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        double scale = 1.0;
        if (mBitmap != null) {
            scale = drawBitmap(canvas);
        }

        if (results != null && results.size() > 0) {
            drawObjectsBox(canvas, scale);
        }
    }
}
