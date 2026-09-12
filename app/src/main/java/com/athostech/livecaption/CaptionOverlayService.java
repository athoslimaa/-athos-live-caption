package com.athostech.livecaption;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CaptionOverlayService extends Service {
    private static final String CHANNEL_ID = "athos_caption_probe";
    private static final int SAMPLE_RATE = 16000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Toast captionToast;
    private View toastView;
    private AudioRecord recorder;
    private volatile boolean running;

    private final Runnable keepCaptionVisible = new Runnable() {
        @Override public void run() {
            if (!running || captionToast == null) return;
            captionToast.show();
            handler.postDelayed(this, 2800);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Athos Live Caption")
                .setContentText("Teste visual em execução")
                .setSmallIcon(R.drawable.app_icon)
                .setOngoing(true)
                .build();
        startForeground(1001, notification);
        running = true;
        showToastOverlay();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAudioProbe();
        } else {
            updateCaption("FAIXA VISUAL ATIVA", "Microfone não autorizado");
        }
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Diagnóstico de legendas", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @SuppressWarnings("deprecation")
    private void showToastOverlay() {
        toastView = LayoutInflater.from(this).inflate(R.layout.overlay_caption, null);
        captionToast = new Toast(getApplicationContext());
        captionToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 70);
        captionToast.setDuration(Toast.LENGTH_LONG);
        captionToast.setView(toastView);
        updateCaption("FAIXA VISUAL ATIVA", "Abra YouTube, FloGrappling ou IPTV");
        keepCaptionVisible.run();
    }

    private void startAudioProbe() {
        int min = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) {
            updateCaption("FAIXA VISUAL ATIVA", "Entrada de áudio indisponível");
            return;
        }
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, min * 2);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                updateCaption("FAIXA VISUAL ATIVA", "Fire OS recusou a entrada de áudio");
                recorder.release();
                recorder = null;
                return;
            }
            recorder.startRecording();
            new Thread(() -> probeLoop(min), "athos-audio-probe").start();
        } catch (SecurityException | IllegalStateException e) {
            updateCaption("FAIXA VISUAL ATIVA", "Falha ao abrir áudio: " + e.getClass().getSimpleName());
        }
    }

    private void probeLoop(int size) {
        short[] buffer = new short[size];
        while (running && recorder != null) {
            int count = recorder.read(buffer, 0, buffer.length);
            if (count <= 0) continue;
            long sum = 0;
            for (int i = 0; i < count; i++) sum += Math.abs(buffer[i]);
            int level = (int) Math.min(100, (sum / Math.max(1, count)) * 100L / 6000L);
            String bars = level > 65 ? "▮▮▮▮" : level > 35 ? "▮▮▮" : level > 10 ? "▮▮" : "▮";
            updateCaption(level > 3 ? "ÁUDIO DETECTADO " + bars : "AGUARDANDO ÁUDIO…",
                    "Nível da entrada: " + level + "%");
        }
    }

    private void updateCaption(String headline, String detail) {
        handler.post(() -> {
            if (toastView == null) return;
            ((TextView) toastView.findViewById(R.id.overlayStatus)).setText(headline);
            ((TextView) toastView.findViewById(R.id.overlayLevel)).setText(detail);
        });
    }

    @Override public void onDestroy() {
        running = false;
        handler.removeCallbacks(keepCaptionVisible);
        if (captionToast != null) captionToast.cancel();
        if (recorder != null) {
            try { recorder.stop(); } catch (IllegalStateException ignored) { }
            recorder.release();
            recorder = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
