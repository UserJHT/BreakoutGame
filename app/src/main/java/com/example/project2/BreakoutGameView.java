package com.example.project2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.Random;

public class BreakoutGameView extends View {

    private Paint paint;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;
    private boolean isGameOver = false;
    private boolean isWin = false;

    // Game Objects
    private RectF paddle;
    private float paddleX;
    private float paddleWidth;
    private float paddleHeight = 40f;
    
    private float ballX, ballY;
    private float ballRadius = 20f;
    private float ballSpeedX = 15f;
    private float ballSpeedY = -15f;

    private ArrayList<RectF> bricks;
    private ArrayList<Particle> particles;
    private int brickRows = 5;
    private int brickCols = 8;
    private float brickWidth;
    private float brickHeight = 60f;
    private float brickPadding = 10f;
    
    private int screenWidth, screenHeight;
    private int score = 0;
    private int highScore = 0;
    
    private SoundManager soundManager;
    private GameDatabaseHelper dbHelper;
    private Random random = new Random();

    private static final long UPDATE_MILLIS = 16; // ~60 FPS
    
    private class Particle {
        float x, y;
        float vx, vy;
        int color;
        int life;
        
        Particle(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.life = 30 + random.nextInt(20);
            float speed = 5f + random.nextFloat() * 10f;
            double angle = random.nextDouble() * 2 * Math.PI;
            this.vx = (float)(Math.cos(angle) * speed);
            this.vy = (float)(Math.sin(angle) * speed);
        }
        
        boolean update() {
            x += vx;
            y += vy;
            life--;
            return life > 0;
        }
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (isPlaying && !isGameOver && !isWin) {
                update();
                invalidate();
                handler.postDelayed(this, UPDATE_MILLIS);
            }
        }
    };

    public BreakoutGameView(Context context) {
        super(context);
        init();
    }

    public BreakoutGameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paddle = new RectF();
        bricks = new ArrayList<>();
        particles = new ArrayList<>();
        soundManager = new SoundManager();
        dbHelper = new GameDatabaseHelper(getContext());
        highScore = dbHelper.getHighScore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        
        paddleWidth = w / 5f;
        brickWidth = (w - (brickCols + 1) * brickPadding) / brickCols;
        
        startGame();
    }

    private void startGame() {
        isPlaying = true;
        isGameOver = false;
        isWin = false;
        score = 0;

        paddleX = screenWidth / 2f - paddleWidth / 2f;
        updatePaddleRect();

        ballX = screenWidth / 2f;
        ballY = screenHeight - paddleHeight - 100f;
        ballSpeedX = 10f;
        ballSpeedY = -15f;

        bricks.clear();
        for (int row = 0; row < brickRows; row++) {
            for (int col = 0; col < brickCols; col++) {
                float left = brickPadding + col * (brickWidth + brickPadding);
                float top = brickPadding + row * (brickHeight + brickPadding) + 100f; // Offset from top
                bricks.add(new RectF(left, top, left + brickWidth, top + brickHeight));
            }
        }

        handler.removeCallbacks(gameLoop);
        handler.post(gameLoop);
    }

    private void updatePaddleRect() {
        paddle.set(paddleX, screenHeight - paddleHeight - 50f, paddleX + paddleWidth, screenHeight - 50f);
    }

    private void update() {
        Iterator<Particle> it = particles.iterator();
        while(it.hasNext()){
            Particle p = it.next();
            if(!p.update()){
                it.remove();
            }
        }

        ballX += ballSpeedX;
        ballY += ballSpeedY;

        if (ballX - ballRadius < 0) {
            ballX = ballRadius;
            ballSpeedX = -ballSpeedX;
            soundManager.playWallHit();
        } else if (ballX + ballRadius > screenWidth) {
            ballX = screenWidth - ballRadius;
            ballSpeedX = -ballSpeedX;
            soundManager.playWallHit();
        }

        if (ballY - ballRadius < 0) {
            ballY = ballRadius;
            ballSpeedY = -ballSpeedY;
            soundManager.playWallHit();
        }

        if (RectF.intersects(paddle, new RectF(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius))) {
             // Simple bounce
             ballSpeedY = -Math.abs(ballSpeedY); 
             // Add some english based on where it hit the paddle
             float hitPoint = ballX - (paddleX + paddleWidth / 2);
             ballSpeedX = hitPoint * 0.15f; 
             soundManager.playPaddleHit();
        }

        for (int i = 0; i < bricks.size(); i++) {
            RectF brick = bricks.get(i);
            if (RectF.intersects(brick, new RectF(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius))) {
                spawnParticles(brick.centerX(), brick.centerY(), Color.RED);
                bricks.remove(i);
                ballSpeedY = -ballSpeedY;
                score += 10;
                soundManager.playBrickHit();
                if (Math.abs(ballSpeedX) < 25) ballSpeedX *= 1.02f;
                if (Math.abs(ballSpeedY) < 25) ballSpeedY *= 1.02f;
                break; 
            }
        }

        if (bricks.isEmpty()) {
            isWin = true;
            isPlaying = false;
            soundManager.playWin();
            checkHighScore();
        }

        if (ballY > screenHeight) {
            isGameOver = true;
            isPlaying = false;
            soundManager.playGameOver();
            checkHighScore();
            Toast.makeText(getContext(), "Game Over! Tap to restart.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void spawnParticles(float x, float y, int color) {
        for(int i=0; i<20; i++) {
            particles.add(new Particle(x, y, color));
        }
    }
    
    private void checkHighScore() {
        if (score > highScore) {
            highScore = score;
            dbHelper.addScore(score);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);

        paint.setColor(Color.BLUE);
        canvas.drawRect(paddle, paint);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(ballX, ballY, ballRadius, paint);

        paint.setColor(Color.RED);
        for (RectF brick : bricks) {
            canvas.drawRect(brick, paint);
        }

        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        canvas.drawText("Score: " + score, 50, 80, paint);
        canvas.drawText("High Score: " + highScore, screenWidth - 400, 80, paint);

        for(Particle p : particles) {
            paint.setColor(p.color);
            paint.setAlpha(p.life * 255 / 50);
            canvas.drawCircle(p.x, p.y, 5, paint);
        }
        paint.setAlpha(255);

        if (isWin) {
            paint.setTextSize(100);
            paint.setColor(Color.GREEN);
            String text = "YOU WIN!";
            float textWidth = paint.measureText(text);
            canvas.drawText(text, (screenWidth - textWidth) / 2, screenHeight / 2f, paint);
            canvas.drawText("Tap to restart", (screenWidth - paint.measureText("Tap to restart")) / 2, screenHeight / 2f + 120, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isGameOver || isWin) {
                    startGame();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isPlaying) {
                    paddleX = event.getX() - paddleWidth / 2;
                    if (paddleX < 0) paddleX = 0;
                    if (paddleX + paddleWidth > screenWidth) paddleX = screenWidth - paddleWidth;
                    updatePaddleRect();
                    invalidate();
                }
                break;
        }
        return true;
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(gameLoop);
    }
}
