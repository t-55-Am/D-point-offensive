package com.myname.mymodid.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class SoundPlayer {
    public static final File SOUND_DIR = new File("config/objective_d_sounds");

    public static void play(final String name) {
        if (name == null || name.isEmpty()) return;
        new Thread(() -> {
            try {
                SOUND_DIR.mkdirs();
                File wav = new File(SOUND_DIR, name + ".wav");
                if (!wav.exists()) return;
                AudioInputStream ais = AudioSystem.getAudioInputStream(wav);
                playStream(ais);
                ais.close();
            } catch (Exception e) {
                System.out.println("[SoundPlayer] play fail: " + e.getMessage());
            }
        }).start();
    }

    public static byte[] readVoiceData(String name) {
        try {
            File wav = new File(SOUND_DIR, name + ".wav");
            if (!wav.exists()) return null;
            if (wav.length() > 1024 * 1024) return null;
            FileInputStream fis = new FileInputStream(wav);
            byte[] data = new byte[(int) wav.length()];
            fis.read(data);
            fis.close();
            return data;
        } catch (Exception e) { return null; }
    }

    public static void playFromBytes(final String name, final byte[] data) {
        new Thread(() -> {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bais);
                playStream(ais);
                ais.close();
            } catch (Exception e) {
                System.out.println("[SoundPlayer] bytes play fail: " + e.getMessage());
            }
        }).start();
    }

    private static void playStream(AudioInputStream ais) throws Exception {
        AudioFormat format = ais.getFormat();
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = ais.read(buffer)) != -1) {
            line.write(buffer, 0, bytesRead);
        }
        line.drain();
        line.close();
    }
}
