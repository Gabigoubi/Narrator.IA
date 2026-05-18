package com.gabigoubi.narradoria;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.annotation.processing.Generated; 
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
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputStream);
                AudioFormat format = audioStream.getFormat();
                
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                
                line.open(format);
                line.start();
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    line.write(buffer, 0, bytesRead);
                }
                
                line.drain();
                line.close();
            } catch (Exception e) {
                NarradorIAMod.LOGGER.error("Erro ao reproduzir o áudio da IA: " + e.getMessage());
            }
        }).start();
    }
}