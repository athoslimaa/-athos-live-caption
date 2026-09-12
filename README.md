# Athos Live Caption — MVP 0.1 (Probe)

Experimento direcionado ao Fire TV Stick HD 2024 (`AFTSS`), Fire OS 7 / Android 9 (API 28).

## O que esta versão comprova

- abre pela interface da Fire TV e funciona com o controle remoto;
- pede permissão para exibir conteúdo sobre outros apps;
- mantém um serviço em primeiro plano enquanto YouTube, FloGrappling ou IPTV estão abertos;
- mostra uma faixa de diagnóstico por cima do vídeo;
- tenta abrir a fonte `VOICE_RECOGNITION` e exibe o nível do áudio recebido.

> Importante: no Fire OS 7 não existe `AudioPlaybackCapture` (API 29). O medidor desta versão identifica a entrada de áudio que o aparelho disponibilizar; ele ainda não transcreve nem promete capturar o áudio interno de outros apps.

## Gerar o APK no Android Studio

1. Abra a pasta `AthosLiveCaption` no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Acesse **Build → Build APK(s)**.
4. O arquivo será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Gerar o APK pelo GitHub

Cada envio para a branch `main` executa o workflow **Build APK**. Também é possível
iniciá-lo manualmente na aba **Actions**. Ao finalizar, abra a execução e baixe o
artefato `AthosLiveCaption-MVP-0.1`.

## Instalar pelo ADB

Ative **Opções do desenvolvedor → Depuração ADB** no Fire Stick e descubra o IP em **Minha Fire TV → Sobre → Rede**.

```bash
adb connect IP_DO_FIRESTICK:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Roteiro do teste

1. Abra **Athos Live Caption**.
2. Selecione **INICIAR TESTE** e conceda as permissões.
3. Pressione HOME e abra primeiro o YouTube.
4. Confirme se a faixa aparece sobre o vídeo.
5. Observe se o nível muda quando há fala e quando a TV está em silêncio.
6. Repita em FloGrappling e no app de IPTV.

Anote separadamente: `overlay apareceu?`, `nível reagiu ao som?`, `app testado` e `houve travamento?`.
