package com.mehmetkaya.pdfokuyucu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.mehmetkaya.pdfokuyucu.databinding.ActivityMainBinding;

import java.io.FileNotFoundException;
import java.io.IOException;

import customviews.ZoomRecyclerView;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    Uri pdfUri;
    int pageCount;
    MayPdfPrint mayPdfPrint;

    private ZoomRecyclerView recyclerView;
    private TextView txtPageNumber;
    MayPdfRecyclerView mayPdfRecyclerView;
    MayPdfRenderer mayPdfRenderer;
    EditText editTextPageNumber;
    TextView txtPageCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        initLauncher();
        mayPdfRenderer = new MayPdfRenderer(this);
        recyclerView = binding.recyclerView;
        txtPageNumber = binding.txtPageNumber;
        editTextPageNumber = binding.editTextNumberPageNumber;
        txtPageCount = binding.txtPageCount;
        mayPdfRecyclerView = new MayPdfRecyclerView(recyclerView, txtPageNumber, mayPdfRenderer, editTextPageNumber, txtPageCount);
        setAllOverride();

        // onCreate'de handleIntent'i çağır
        try {
            handleIntent(getIntent());
        } catch (IOException e) {
            Log.e("MainActivity", "Error handling intent", e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Yeni bir intent geldiğinde, setIntent ve handleIntent çağırarak işleme yapıyoruz
        setIntent(intent);
        try {
            handleIntent(intent);
        } catch (IOException e) {
            Log.e("MainActivity", "Error handling intent", e);
        }
    }

    private void handleIntent(Intent intent) throws IOException {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                this.pdfUri = uri;
                Log.d("MainActivity", "PDF URI: " + uri.toString());
                showPdf(uri);  // PDF dosyasını göster
            } else {
                Toast.makeText(MainActivity.this, "PDF dosyası bulunamadı!", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "URI is null");
            }
        } else {
            Log.e("MainActivity", "Intent is null or action is not ACTION_VIEW");
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View pageNumber = editTextPageNumber;
        MaterialButton goPage = binding.btnGoPage;
        // Eğer dokunulan view EditText değilse
        if (pageNumber instanceof EditText) {
            // Ekranın herhangi bir yerine dokunulduğunda klavye kapatılır
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                Rect outRectPageNumber = new Rect();
                Rect outRectGoPage = new Rect();
                pageNumber.getGlobalVisibleRect(outRectPageNumber); // EditText'in ekran üzerindeki konumunu al
                goPage.getGlobalVisibleRect(outRectGoPage); // Butonun ekran üzerindeki konumunu al

                if (!outRectPageNumber.contains((int) ev.getX(), (int) ev.getY()) && !outRectGoPage.contains((int) ev.getX(), (int) ev.getY())) {
                    // Eğer dokunulan yer EditText dışındaysa, klavyeyi kapat ve focusu kaldır
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    hideKeyboardAndClearFocus((EditText) pageNumber);
                }
            }
        }

        return super.dispatchTouchEvent(ev);
    }




    @SuppressLint("ClickableViewAccessibility")
    private void setAllOverride() {





        editTextPageNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    editTextPageNumber.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            editTextPageNumber.selectAll();
                        }
                    }, 300); //klavye açılmasını bekle
                }
            }
        });



        editTextPageNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    goPageFunc();
                    hideKeyboardAndClearFocus(editTextPageNumber);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return true;
            }
        });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // PdfRendererHelper'ı kapat
        if (mayPdfRenderer != null) {
            try {
                mayPdfRenderer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void showPdf(Uri uri) throws IOException {
        binding.imageViewMainScreenLogo.setVisibility(ViewGroup.INVISIBLE);
        mayPdfRecyclerView.showPdf(uri);

    }


    public void goPageFunc(){
        try {
            int page = Integer.parseInt(String.valueOf(editTextPageNumber.getText()));
            mayPdfRecyclerView.scrollToPage(page);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçerli bir sayı giriniz!", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboardAndClearFocus(EditText editText) {
        editText.clearFocus(); // Odak kaybettir
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0); // Klavyeyi kapat
    }
    public void goPage(View view) {
        hideKeyboardAndClearFocus(editTextPageNumber);
        goPageFunc();
    }

    public void printPdf(View view){
        mayPdfPrint = new MayPdfPrint(MainActivity.this,pdfUri);
        mayPdfPrint.print(pageCount);
    }

    private void initLauncher() {
        pdfPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            pdfUri = result.getData().getData();
                            try {
                                showPdf(pdfUri);
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
    }

    public void selectPdf(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        pdfPickerLauncher.launch(intent);
    }
}