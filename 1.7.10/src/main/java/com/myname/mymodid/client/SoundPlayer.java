package com.myname.mymodid.client;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;

public class SoundPlayer {
    public static final File SOUND_DIR = new File("config/mymodid_sounds");

    public static void play(final String name) {
        if (name == null || name.isEmpty()) return;
        new Thread(() -> {
            try {
                SOUND_DIR.mkdirs();
                File wav = new File(SOUND_DIR, name + ".wav");
                System.out.println("[SoundPlayer] 尝试播放: " + wav.getAbsolutePath() + " 存在=" + wav.exists());
                if (!wav.exists()) {
                    System.out.println("[SoundPlayer] 文件不存在: " + wav.getAbsolutePath());
                    return;
                }
                AudioInputStream ais = AudioSystem.getAudioInputStream(wav);
                AudioFormat format = ais.getFormat();
                System.out.println("[SoundPlayer] 格式: " + format);
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
                ais.close();
                System.out.println("[SoundPlayer] 播放完成: " + name);
            } catch (Exception e) {
                System.out.println("[SoundPlayer] 播放失败: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public static byte[] readVoiceData(String name) {
        try {
            File wav = new File(SOUND_DIR, name + ".wav");
            if (!wav.exists()) {
                System.out.println("[SoundPlayer] readVoiceData 文件不存在: " + wav.getAbsolutePath());
                return null;
            }
            if (wav.length() > 1024 * 1024) {
                System.out.println("[SoundPlayer] 文件超过1MB: " + wav.length());
                return null;
            }
            java.io.FileInputStream fis = new java.io.FileInputStream(wav);
            byte[] data = new byte[(int) wav.length()];
            fis.read(data);
            fis.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void playFromBytes(final String name, final byte[] data) {
        new Thread(() -> {
            try {
                java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bais);
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
                ais.close();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}
