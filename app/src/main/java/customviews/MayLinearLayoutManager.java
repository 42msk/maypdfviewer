package customviews;

import android.content.Context;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class MayLinearLayoutManager extends LinearLayoutManager {
    private static final float DEFAULT_SPEED = 100f; // normal kaydırma hızı (inç başına milisaniye)
    private float zoomScale = 1.0f; // zoom miktarı

    public MayLinearLayoutManager(Context context) {
        super(context);
    }

    public float getZoomScale() {
        return zoomScale;
    }

    public void setZoomScale(float zoomScale) {
        this.zoomScale = zoomScale;
    }




    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // Kaydırma hızını zoomScale ile ayarlıyoruz
        int adjustedDy = (int) (dy / zoomScale); // zoomScale arttıkça kaydırma hızı azalır
        return super.scrollVerticallyBy(adjustedDy, recycler, state);
    }




    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        final LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                // Burada speed değerini zoomScale ile çarpıyoruz, zoom arttıkça hız azalmalı
                float speed = DEFAULT_SPEED / displayMetrics.densityDpi;
                return speed * zoomScale; // zoom arttıkça hız daha da yavaşlayacak
            }

            @Nullable
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return MayLinearLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }
        };

        // Hedef pozisyona kaydırma başlatılıyor
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }
}