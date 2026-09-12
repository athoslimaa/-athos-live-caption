package com.athostech.livecaption;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 20;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.statusText);
        Button start = findViewById(R.id.startButton);
        Button stop = findViewById(R.id.stopButton);
        start.setOnClickListener(v -> beginPermissionFlow());
        stop.setOnClickListener(v -> {
            status.setText("Desative Athos Live Caption na tela de acessibilidade.");
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });
        start.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (isAccessibilityServiceEnabled()
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            status.setText("Teste ativo. Pressione HOME e abra um vídeo.");
        }
    }

    private void beginPermissionFlow() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Autorize o microfone para medirmos a entrada disponível.");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (!isAccessibilityServiceEnabled()) {
            status.setText("Ative Athos Live Caption em Acessibilidade e volte para cá.");
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        status.setText("Teste ativo. Pressione HOME e abra um vídeo.");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_AUDIO && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            beginPermissionFlow();
        } else {
            status.setText("Sem permissão de áudio: o overlay pode funcionar, mas não mediremos som.");
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expected = new ComponentName(this, CaptionAccessibilityService.class);
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            ComponentName current = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(current)) return true;
        }
        return false;
    }
}
