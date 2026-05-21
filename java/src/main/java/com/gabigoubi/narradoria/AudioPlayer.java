package com.gabigoubi.narradoria;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class AudioPlayer {

    public static void play(byte[] inputBytes) {
        new Thread(() -> {
            try {
                System.out.println("[AudioPlayer] Recebeu payload de " + inputBytes.length + " bytes. Desempacotando WAV...");

                // 1. Abre o WAV original que veio do Python (32-bit Float)
                ByteArrayInputStream bais = new ByteArrayInputStream(inputBytes);
                AudioInputStream originalStream = AudioSystem.getAudioInputStream(bais);
                AudioFormat originalFormat = originalStream.getFormat();

                System.out.println("[AudioPlayer] Formato original recebido: " + originalFormat.toString());

                // 2. Cria a "Fôrma" do formato que o Java aceita (16-bit PCM)
                // Mantemos o sample rate (24000.0 Hz) e os canais (1 - Mono), mas forçamos 16 bits.
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        originalFormat.getSampleRate(),
                        16, // <- O pulo do gato! Rebaixando de 32 para 16 bits
                        originalFormat.getChannels(),
                        originalFormat.getChannels() * 2, // 2 bytes por frame (16 bits)
                        originalFormat.getSampleRate(),
                        false // Little Endian
                );

                // 3. Pede para o Java converter o áudio em tempo real
                AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

                line.open(targetFormat);

                // Tentativa de aumentar o volume nativamente (Master Gain)
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl volume = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    volume.setValue(6.0f);
                }

                System.out.println("[AudioPlayer] Conversao para 16-bit concluida. Reproduzindo som...");
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = convertedStream.read(buffer)) != -1) {
                    line.write(buffer, 0, bytesRead);
                }

                line.drain();
                line.close();
                convertedStream.close();
                originalStream.close();

                System.out.println("[AudioPlayer] Reproducao finalizada com sucesso.");

            } catch (IllegalArgumentException iae) {
                NarradorIAMod.LOGGER.error("[AudioPlayer] O Hardware de audio não suporta a conversão: ", iae);
            } catch (Exception e) {
                NarradorIAMod.LOGGER.error("[AudioPlayer] Erro critico ao reproduzir WAV: ", e);
            } finally {
                HttpAssistant.isNarrating = false;
                System.out.println("[AudioPlayer] [UNLOCK] isNarrating = false. Mod liberado para novos eventos!");
            }
        }).start();
    }
}