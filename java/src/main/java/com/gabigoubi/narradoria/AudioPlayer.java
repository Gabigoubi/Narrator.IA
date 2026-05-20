package com.gabigoubi.narradoria;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioPlayer {

    public static void play(byte[] inputBytes) {
        new Thread(() -> {
            try {
                byte[] pcmBytes = convertFloat32ToShort16(inputBytes);

                AudioFormat format = new AudioFormat(24000f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                if (line == null) {
                    throw new Exception("SourceDataLine is null!");
                }

                line.open(format);
                line.start();
                line.write(pcmBytes, 0, pcmBytes.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                NarradorIAMod.LOGGER.error("Erro critico no AudioPlayer: ", e);
            }
        }).start();
    }

    private static byte[] convertFloat32ToShort16(byte[] inputBytes) {
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputBytes).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer outputBuffer = ByteBuffer.allocate(inputBytes.length / 2).order(ByteOrder.LITTLE_ENDIAN);

        float volumeMultiplier = 1.78f;

        while (inputBuffer.hasRemaining()) {
            float sample = inputBuffer.getFloat() * volumeMultiplier;

            if (sample > 1.0f) sample = 1.0f;
            if (sample < -1.0f) sample = -1.0f;

            short shortSample = (short) (sample * 32767);
            outputBuffer.putShort(shortSample);
        }

        return outputBuffer.array();
    }
}