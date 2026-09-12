package com.athostech.livecaption;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 20;
    private static final int REQ_OVERLAY = 21;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.statusText);
        Button start = findViewById(R.id.startButton);
        Button stop = findViewById(R.id.stopButton);
        start.setOnClickListener(v -> beginPermissionFlow());
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, CaptionOverlayService.class));
            status.setText("Teste encerrado.");
        });
        start.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (Settings.canDrawOverlays(this)
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            status.setText("Permissões liberadas. Selecione INICIAR TESTE.");
        }
    }

    private void beginPermissionFlow() {
        if (!Settings.canDrawOverlays(this)) {
            status.setText("Autorize 'exibir sobre outros apps' e volte para cá.");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Autorize o microfone para medirmos a entrada disponível.");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        startProbe();
    }

    private void startProbe() {
        Intent service = new Intent(this, CaptionOverlayService.class);
        startForegroundService(service);
        status.setText("Teste ativo. Pressione HOME e abra um vídeo.");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_AUDIO && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startProbe();
        } else {
            status.setText("Sem permissão de áudio: o overlay pode funcionar, mas não mediremos som.");
        }
    }
}
