package com.mehmetkaya.pdfokuyucu;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;

import customviews.MayLinearLayoutManager;
import customviews.ZoomRecyclerView;

public class MayPdfRecyclerView {
    int currentPage;
    int firstVisiblePosition;
    int lastVisiblePosition;
    ZoomRecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    TextView txtPageNumber;
    int pageCount;
    MayPdfRenderer mayPdfRenderer;
    EditText editTextPageNumber;
    TextView txtPageCount;
    int scrollTryCounter=0;
    int maxScrollTry=60;


    public MayPdfRecyclerView(ZoomRecyclerView recyclerView,TextView txtPageNumber,MayPdfRenderer mayPdfRenderer,EditText editTextPageNumber,TextView txtPageCount) {
        this.recyclerView = recyclerView;
        this.txtPageNumber = txtPageNumber ;
        this.mayPdfRenderer = mayPdfRenderer;
        this.editTextPageNumber = editTextPageNumber;
        this.txtPageCount = txtPageCount;
        layoutManager = new MayLinearLayoutManager(recyclerView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        setRecyclerViewScroolListener();
    }

    public boolean isPageLoaded(int position) {
        PdfPageAdapter.PdfPageViewHolder holder = (PdfPageAdapter.PdfPageViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null && holder.binding.pdfPageView.getDrawable() != null) {
            return true; // Sayfa yüklenmiş
        }
        return false; // Sayfa yüklenmemiş
    }



    public void scrollToPage(int pageNumber) {
        int position = pageNumber - 1;

        if (position >= 0 && position < pageCount) {
//            recyclerView.scrollToPosition(position);
             // Ekranın ortasını hesapla
            layoutManager.scrollToPositionWithOffset(position, 0);
            // Sayfa yüklenmemişse, belirli aralıklarla tekrar deneyelim
            new Thread(new Runnable() {
                @Override
                public void run() {
                        try {
                            Thread.sleep(1000); // Sayfanın yüklenmesini bekle
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    if (position > currentPage && scrollTryCounter<maxScrollTry) {
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("possition: " + position);
                                System.out.println("current page: " + currentPage);
                                System.out.println("page number:" + pageNumber);
                                scrollToPage(pageNumber);
                            }
                        });
                    } else {
                        scrollTryCounter=0;
                    }

                }
            }).start();
        } else {
            Toast.makeText(recyclerView.getContext(), "Geçersiz sayfa numarası", Toast.LENGTH_SHORT).show();
        }
    }


    protected void closePdfRenderer() {
        if (mayPdfRenderer != null) {
            try {
                mayPdfRenderer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



    @SuppressLint("DefaultLocale")
    public void showPdf(Uri uri) throws IOException {
        recyclerView.resetZoom();
        closePdfRenderer();
        System.out.println("Uri 2: "+uri);
        mayPdfRenderer.openPdf(uri);
        PdfPageAdapter pdfPageAdapter = new PdfPageAdapter(mayPdfRenderer,recyclerView);
        pageCount = mayPdfRenderer.getPageCount();
        txtPageCount.setText(String.format("/%d",pageCount));
        recyclerView.setAdapter(pdfPageAdapter);

    }

    private void setRecyclerViewScroolListener(){
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Görüntülenen sayfanın numarasını almak için visible item'ların pozisyonlarına bakıyoruz.
                firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                lastVisiblePosition = layoutManager.findLastVisibleItemPosition();

                // Görüntülenen ilk sayfanın numarasını alıyoruz.
                // (Pdf sayfa numarası sıfırdan başladığı için, pozisyon + 1)
                currentPage = firstVisiblePosition + 1;
//                System.out.println("first: "+ firstVisiblePosition);
//                System.out.println("last:"+lastVisiblePosition);
//                System.out.println("current:"+currentPage);

                editTextPageNumber.setText(String.valueOf(currentPage));
            }
        });
    }
}
