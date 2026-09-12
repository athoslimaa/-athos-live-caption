package com.athostech.livecaption;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 30;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.statusText);
        Button start = findViewById(R.id.startButton);
        Button stop = findViewById(R.id.stopButton);
        start.setOnClickListener(v -> beginTest());
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, CaptionOverlayService.class));
            status.setText("Teste parado.");
        });
        start.requestFocus();
    }

    private void beginTest() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Autorize o microfone. Se negar, testaremos somente a faixa sobre o vídeo.");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        startProbe();
    }

    private void startProbe() {
        Intent service = new Intent(this, CaptionOverlayService.class);
        startForegroundService(service);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            status.setText("Teste iniciado. Pressione HOME e abra um vídeo.");
        } else {
            status.setText("Teste visual iniciado sem áudio. Pressione HOME e abra um vídeo.");
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_AUDIO) startProbe();
    }
}
