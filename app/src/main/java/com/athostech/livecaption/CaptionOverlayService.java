package com.athostech.livecaption;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class CaptionOverlayService extends Service {
    private static final String CHANNEL_ID = "athos_caption_probe";
    private static final int SAMPLE_RATE = 16000;
    private WindowManager windowManager;
    private View overlay;
    private AudioRecord recorder;
    private Thread audioThread;
    private volatile boolean running;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Athos Live Caption")
                .setContentText("Teste de overlay e áudio em execução")
                .setSmallIcon(R.drawable.app_icon)
                .setOngoing(true)
                .build();
        startForeground(1001, notification);
        showOverlay();
        startAudioProbe();
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Diagnóstico de legendas", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void showOverlay() {
        if (!Settings.canDrawOverlays(this)) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = LayoutInflater.from(this).inflate(R.layout.overlay_caption, null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 70;
        windowManager.addView(overlay, params);
    }

    private void startAudioProbe() {
        int min = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) {
            updateOverlay("Entrada indisponível", "AudioRecord não suportado");
            return;
        }
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, min * 2);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                updateOverlay("Entrada indisponível", "Fire OS recusou a fonte de áudio");
                return;
            }
            recorder.startRecording();
            running = true;
            audioThread = new Thread(() -> probeLoop(min), "athos-audio-probe");
            audioThread.start();
        } catch (SecurityException | IllegalStateException e) {
            updateOverlay("Falha ao abrir áudio", e.getClass().getSimpleName());
        }
    }

    private void probeLoop(int size) {
        short[] buffer = new short[size];
        while (running) {
            int count = recorder.read(buffer, 0, buffer.length);
            if (count <= 0) continue;
            long sum = 0;
            for (int i = 0; i < count; i++) sum += Math.abs(buffer[i]);
            int level = (int) Math.min(100, (sum / Math.max(1, count)) * 100L / 6000L);
            String bars = level > 65 ? "▮▮▮▮" : level > 35 ? "▮▮▮" : level > 10 ? "▮▮" : "▮";
            updateOverlay(level > 3 ? "Áudio detectado " + bars : "Aguardando áudio…",
                    "Nível da entrada: " + level + "%");
        }
    }

    private void updateOverlay(String headline, String detail) {
        if (overlay == null) return;
        overlay.post(() -> {
            ((TextView) overlay.findViewById(R.id.overlayStatus)).setText(headline);
            ((TextView) overlay.findViewById(R.id.overlayLevel)).setText(detail);
        });
    }

    @Override public void onDestroy() {
        running = false;
        if (recorder != null) {
            try { recorder.stop(); } catch (IllegalStateException ignored) { }
            recorder.release();
            recorder = null;
        }
        if (overlay != null && windowManager != null) {
            windowManager.removeView(overlay);
            overlay = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
