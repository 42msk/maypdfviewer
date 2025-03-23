package com.mehmetkaya.pdfokuyucu;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MayPdfPrint {

    private final Context context;
    private final Uri pdfUri;

    public MayPdfPrint(Context context, Uri pdfUri) {
        this.context = context;
        this.pdfUri = pdfUri;
    }

    public void print(int pageCount) {
        if (pageCount == 0) {
            pageCount = PrintDocumentInfo.PAGE_COUNT_UNKNOWN;
        }
        if (pdfUri == null) {
            Toast.makeText(context, "PDF dosyası bulunamadı!", Toast.LENGTH_SHORT).show();
            return;
        }

        // PrintManager örneği al
        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

        // Yazdırma işlemi için bir isim belirle
        String jobName = context.getString(R.string.app_name) + " Document";

        // PrintDocumentAdapter oluştur
        int finalPageCount = pageCount;
        PrintDocumentAdapter printAdapter = new PrintDocumentAdapter() {
            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
                try {
                    InputStream input = context.getContentResolver().openInputStream(pdfUri);
                    OutputStream output = new FileOutputStream(destination.getFileDescriptor());

                    byte[] buf = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = input.read(buf)) > 0) {
                        output.write(buf, 0, bytesRead);
                    }

                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                    input.close();
                    output.close();
                } catch (IOException e) {
                    callback.onWriteFailed(e.getMessage());
                }
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                // PDF dosyasının yazdırılmaya hazır olduğunu belirt
                callback.onLayoutFinished(new PrintDocumentInfo.Builder("document.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(finalPageCount)
                        .build(), true);
            }
        };

        // Yazdırma işlemini başlat
        if (printManager != null) {
            printManager.print(jobName, printAdapter, null);
        } else {
            Toast.makeText(context, "Yazdırma servisi kullanılamıyor!", Toast.LENGTH_SHORT).show();
        }
    }
}