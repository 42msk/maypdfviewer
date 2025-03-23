package com.mehmetkaya.pdfokuyucu;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetkaya.pdfokuyucu.databinding.ItemPdfPageBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder> {
    private RecyclerView recyclerView;
    private final MayPdfRenderer mayPdfRenderer;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    int recyclerViewWidth;

    public PdfPageAdapter(MayPdfRenderer mayPdfRenderer, RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.mayPdfRenderer = mayPdfRenderer;
        this.executorService = Executors.newCachedThreadPool(); // Yükleme için iş parçacığı havuzu
        this.mainHandler = new Handler(Looper.getMainLooper());
        recyclerViewWidth = recyclerView.getWidth();

    }



    @NonNull
    @Override
    public PdfPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
        ItemPdfPageBinding binding = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PdfPageViewHolder(binding);

    }

    @Override
    public void onBindViewHolder(@NonNull PdfPageViewHolder holder, int position) {
        if (mayPdfRenderer == null) {
            return;
        }
        if (position == mayPdfRenderer.getPageCount()) {
            // Boş sayfa
            holder.binding.pdfPageView.setImageDrawable(null); // ImageView'ı temizle
            holder.binding.pdfPageView.setBackgroundColor(Color.TRANSPARENT); // Beyaz arka plan

            holder.binding.pdfPageView.getLayoutParams().height = 500;
            holder.binding.pdfPageView.requestLayout();
        } else {
            loadPageAsync(holder, position);
        }

    }

    private void loadPageAsync(PdfPageViewHolder holder, int position) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = mayPdfRenderer.renderPage(position, recyclerViewWidth*2 );
                    if (bitmap != null) {
                        // UI thread'ine dönüş yap ve ImageView'ı güncelle
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                holder.binding.pdfPageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("PdfPageAdapter", "Error loading page: " + position, e);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mayPdfRenderer !=null ? mayPdfRenderer.getPageCount()+1 : 0;
    }

    static class PdfPageViewHolder extends RecyclerView.ViewHolder {
        ItemPdfPageBinding binding;

        public PdfPageViewHolder(ItemPdfPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }


}
