package customviews;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ZoomRecyclerView extends RecyclerView {

    private float scaleFactor = 1.0f;
    private final float minScale = 1.0f;
    private final float maxScale = 4.0f;

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;
    private final PointF lastTouch = new PointF();
    private final PointF translate = new PointF();
    private boolean isScaling = false;
    private float zoomFocusX = 0;
    private float zoomFocusY = 0;


    public ZoomRecyclerView(@NonNull Context context) {
        this(context, null);

    }




    public ZoomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (scaleFactor > 1.0f) {
                    // Zoom out her zaman zoomFocusX ve zoomFocusY kullanılarak yapılır
                    animateZoomToPoint(zoomFocusX, zoomFocusY, minScale);
                } else {
                    // Yeni zoom in noktası belirle ve animasyonu başlat
                    zoomFocusX = e.getX();
                    zoomFocusY = e.getY();
                    animateZoomToPoint(zoomFocusX, zoomFocusY, 2.0f);
                }
                return true;
            }


        });



        // RecyclerView'in kaydırma hassasiyeti
        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (scaleFactor > 1.0f) {
                    // Kaydırma hızını scaleFactor ile orantılı olarak azalt
                    float adjustedDx = dx / scaleFactor; // X ekseninde kaydırma
                    float adjustedDy = dy / scaleFactor; // Y ekseninde kaydırma

                    // Burada kaydırma işlemini yapıyoruz. adjustedDx ve adjustedDy, zoom seviyesine bağlı olarak hızın azalmasını sağlar.
                    scrollBy((int) adjustedDx, (int) adjustedDy);
                }
            }
        });


        setWillNotDraw(false); // Çizim yapabilmesi için false olarak ayarla
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        // Eğer zoom varsa ve pan işlemi yapılacaksa
        if (event.getPointerCount() == 1 && scaleFactor > 1.0f && !isScaling) {
            handlePan(event);
        }

        return super.onTouchEvent(event);
    }




    private void handlePan(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouch.x;
                float dy = event.getY() - lastTouch.y;

                // Yatay kaydırma işlemi
                translate.x += dx;
                int lastVisibleItemPosition = ((MayLinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
                if (lastVisibleItemPosition == 1 && scaleFactor>1) {
                    translate.y += dy; // Bu satırı kaldırıyoruz veya yorum satırı yapıyoruz
                }
                // Dikey kaydırma işlemini devre dışı bırakıyoruz

                applyTranslationLimits();
                lastTouch.set(event.getX(), event.getY());
                invalidate();
                break;
        }
    }



    private void animateZoomToPoint(float focusX, float focusY, float targetScale) {
        ValueAnimator animator = ValueAnimator.ofFloat(scaleFactor, targetScale);
        animator.setDuration(300); // Animasyon süresi
        animator.addUpdateListener(animation -> {
            scaleFactor = (float) animation.getAnimatedValue();
            if (getLayoutManager() instanceof MayLinearLayoutManager) {
                MayLinearLayoutManager layoutManager = (MayLinearLayoutManager) getLayoutManager();
                layoutManager.setZoomScale(scaleFactor);
            }
            float px = focusX - ((float) getWidth() / 2);
            float py = focusY - ((float) getHeight() / 2);

            translate.set(-px * (scaleFactor - 1), -py * (scaleFactor - 1));

            applyTranslationLimits();
            matrix.setScale(scaleFactor, scaleFactor, (float) getWidth() / 2, (float) getHeight() / 2);
            invalidate();

        });
        animator.start();

    }


//
//    private void zoomToPoint(float x, float y, float scale) {
//        scaleFactor = Math.max(minScale, Math.min(scale, maxScale));
//
//        float px = x - (getWidth() / 2);
//        float py = y - (getHeight() / 2);
//
//        translate.set(-px * (scaleFactor - 1), -py * (scaleFactor - 1));
//
//        applyTranslationLimits();
//        matrix.setScale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);
//        invalidate();
//    }

    public void resetZoom() {
        scaleFactor = 1.0f;
        translate.set(0, 0);
        matrix.reset();
        invalidate();
        if (getLayoutManager() instanceof MayLinearLayoutManager) {
            MayLinearLayoutManager layoutManager = (MayLinearLayoutManager) getLayoutManager();
            layoutManager.setZoomScale(scaleFactor);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(translate.x, translate.y);
        canvas.scale(scaleFactor, scaleFactor, (float) getWidth() / 2, (float) getHeight() / 2);
        super.dispatchDraw(canvas);
        canvas.restore();
    }


    private void applyTranslationLimits() {
        float maxDx = (getWidth() * (scaleFactor - 1)) / 2;
        float maxDy = (getHeight() * (scaleFactor - 1))/2 + 50;

        translate.x = Math.min(maxDx, Math.max(-maxDx, translate.x));
        translate.y = Math.min(maxDy, Math.max(-maxDy, translate.y));
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            isScaling = true;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = scaleFactor * detector.getScaleFactor();
            scaleFactor = Math.max(minScale, Math.min(scale, maxScale));
            zoomFocusX = detector.getFocusX();
            zoomFocusY = detector.getFocusY();

            if (getLayoutManager() instanceof MayLinearLayoutManager) {
                MayLinearLayoutManager layoutManager = (MayLinearLayoutManager) getLayoutManager();
                layoutManager.setZoomScale(scaleFactor);
            }
            applyTranslationLimits();
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            isScaling = false;
        }
    }


}