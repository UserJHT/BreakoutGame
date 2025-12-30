package com.example.project2;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundManager {

    private ToneGenerator toneGenerator;

    public SoundManager() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }

    public void playPaddleHit() {
        toneGenerator.startTone(ToneGenerator.TONE_DTMF_5, 50); 
    }

    public void playBrickHit() {
        toneGenerator.startTone(ToneGenerator.TONE_DTMF_8, 50);
    }

    public void playWallHit() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 50);
    }

    public void playGameOver() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 500);
    }

    public void playWin() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500);
    }

    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
