package com.example.project2;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public class SoundManager {

    private static final int SAMPLE_RATE = 44100;

    private byte[] paddleHitSound;
    private byte[] brickHitSound;
    private byte[] wallHitSound;
    private byte[] gameOverSound;
    private byte[] winSound;

    public SoundManager() {
        paddleHitSound = generateSineWave(440, 0.1f);
        brickHitSound = generateSineWave(880, 0.1f);
        wallHitSound = generateSineWave(220, 0.05f);
        gameOverSound = generateSineWave(110, 0.5f);
        winSound = generateArpeggio();
    }

    private byte[] generateSineWave(int frequency, float duration) {
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] soundData = new byte[2 * numSamples];
        double phase = 0;
        
        for (int i = 0; i < numSamples; i++) {
            short sample = (short) (Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE) * 32767);
            soundData[2 * i] = (byte) (sample & 0xFF);
            soundData[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);

            if (i > numSamples - 1000) {
            }
        }
        return soundData;
    }
    
    private byte[] generateArpeggio() {
         int[] freqs = {523, 659, 784, 1046};
         float noteDuration = 0.1f;
         int totalSamples = (int)(freqs.length * noteDuration * SAMPLE_RATE);
         byte[] data = new byte[2 * totalSamples];
         
         int offset = 0;
         for(int freq : freqs) {
             byte[] note = generateSineWave(freq, noteDuration);
             System.arraycopy(note, 0, data, offset, note.length);
             offset += note.length;
         }
         return data;
    }

    public void playPaddleHit() {
        playSound(paddleHitSound);
    }

    public void playBrickHit() {
        playSound(brickHitSound);
    }

    public void playWallHit() {
        playSound(wallHitSound);
    }

    public void playGameOver() {
        playSound(gameOverSound);
    }

    public void playWin() {
        playSound(winSound);
    }

    private void playSound(final byte[] soundData) {
        new Thread(() -> {
            try {
                AudioTrack audioTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(soundData.length)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();

                audioTrack.write(soundData, 0, soundData.length);
                audioTrack.play();
                Thread.sleep((long)(soundData.length / (SAMPLE_RATE * 2.0) * 1000) + 100);
                audioTrack.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
