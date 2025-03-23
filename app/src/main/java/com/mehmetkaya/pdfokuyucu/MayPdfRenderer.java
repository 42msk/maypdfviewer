package com.mehmetkaya.pdfokuyucu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MayPdfRenderer {
    private final Object renderLock = new Object();
    private Context context;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;

    public MayPdfRenderer(Context context) {
        this.context = context;
    }

    // Uri ile PDF açma
    public void openPdf(Uri pdfUri) throws IOException {
        System.out.println("URI CALISTI: "+ pdfUri);
        parcelFileDescriptor = getParcelFileDescriptor(context, pdfUri);
        pdfRenderer = new PdfRenderer(parcelFileDescriptor);
    }



    public Bitmap renderPage(int pageIndex, int recyclerViewWidth) {
        synchronized (renderLock) {
            if (pdfRenderer == null || pageIndex < 0 || pageIndex >= getPageCount()) {
                return null;
            }

            PdfRenderer.Page page = null;
            try {
                page = pdfRenderer.openPage(pageIndex);

                // Sayfa genişliği ve yüksekliğini al
                int pageWidth = page.getWidth();
                int pageHeight = page.getHeight();

                if (pageWidth <= 0 || pageHeight <= 0) {
                    throw new IllegalStateException("Page width or height is invalid: " + pageWidth + "x" + pageHeight);
                }

                // Ölçek faktörünü hesapla (RecyclerView genişliğine göre ayarla)
                float scaleFactor = (float) recyclerViewWidth / pageWidth;

                // Yeni genişlik ve yükseklik
                int bitmapWidth = (int) (pageWidth * 2);
                int bitmapHeight = (int) (pageHeight * 2);

                // Bitmap oluştur
                Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                return bitmap;
            } finally {
                if (page != null) {
                    page.close(); // Sayfayı kapat
                }
            }
        }
    }

    public int getPageCount() {
        return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
    }

    public ParcelFileDescriptor getParcelFileDescriptor(Context context, Uri uri) {
        try {
            return context.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() throws IOException {
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
        if (parcelFileDescriptor != null) {
            parcelFileDescriptor.close();
        }
    }
}
