package com.ledway;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.widget.Toast;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import java.io.File;

/**
 * Created by togb on 2017/1/7.
 */

public class PDFViewerActivity extends AppCompatActivity {
  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_pdf_viewer);
    initView();
  }

  private void initView() {
    PDFView pdfView = (PDFView) findViewById(R.id.pdfview);
    String fileName = getIntent().getStringExtra("pdf_file_uri");
    pdfView.fromFile(new File(fileName)).onError(new OnErrorListener() {
      public void onError(Throwable t) {
        Toast.makeText(PDFViewerActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
      }
    }).load();
  }
}
