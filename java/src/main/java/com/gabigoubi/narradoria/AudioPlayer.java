package com.gabigoubi.narradoria;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer {

    public static void playWavBytes(byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length == 0) {
            return;
        }

        new Thread(() -> {
            try {
                InputStream inputStream = new ByteArrayInputStream(audioBytes);
                AudioInputStream originalStream = AudioSystem.getAudioInputStream(inputStream);
                AudioFormat originalFormat = originalStream.getFormat();

                byte[] originalBytes = originalStream.readAllBytes();

                // Converte de PCM_FLOAT 32-bit para PCM_SIGNED 16-bit
                byte[] convertedBytes = convertFloat32ToShort16(originalBytes);

                // Define o novo formato compatível com qualquer placa de som
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        originalFormat.getSampleRate(), // 24000 Hz
                        16,                             // 16 bits
                        originalFormat.getChannels(),   // Mono (1)
                        originalFormat.getChannels() * 2,
                        originalFormat.getSampleRate(),
                        false
                );

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

                line.open(targetFormat);
                line.start();

                line.write(convertedBytes, 0, convertedBytes.length);

                line.drain();
                line.close();
            } catch (Exception e) {
                NarradorIAMod.LOGGER.error("Erro ao reproduzir o áudio da IA: " + e.getMessage());
            }
        }).start();
    }

    private static byte[] convertFloat32ToShort16(byte[] inputBytes) {
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputBytes).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer outputBuffer = ByteBuffer.allocate(inputBytes.length / 2).order(ByteOrder.LITTLE_ENDIAN);

        while (inputBuffer.hasRemaining()) {
            float sample = inputBuffer.getFloat();

            // Força o valor a ficar entre -1.0 e 1.0 para evitar ruídos mecânicos
            if (sample > 1.0f) sample = 1.0f;
            if (sample < -1.0f) sample = -1.0f;

            // Converte a escala matemática para 16 bits (-32768 a 32767)
            short shortSample = (short) (sample * 32767);
            outputBuffer.putShort(shortSample);
        }

        return outputBuffer.array();
    }
}