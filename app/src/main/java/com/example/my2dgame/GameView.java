package com.example.my2dgame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private Thread gameThread;
    private SurfaceHolder holder;
    private volatile boolean running = false;
    private Paint paint;
    private Bitmap originalBackground;
    private Bitmap scaledBackground;
    private Player player;
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private float backgroundOffsetX = 0;
    private float backgroundSpeed = 2;

    private Bitmap enemyBitmap;
    private Bitmap playerBitmap;
    private Bitmap scaledEnemyBitmap;
    private Bitmap bulletBitmap;

    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundMap;
    private int shootSoundId;
    private int enemyHitSoundId;
    private boolean soundEffectsEnabled = true;

    private MediaPlayer mediaPlayer;
    private boolean musicEnabled = false;

    private Bitmap musicTurnOnBitmap;
    private Bitmap musicTurnOffBitmap;
    private Bitmap soundOffBitmap;
    private Bitmap soundOnBitmap;

    private AssetManager assetManager; // Để đọc file từ assets

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(40);

        // Lấy AssetManager
        assetManager = context.getAssets();

        // Khởi tạo SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();
        soundMap = new HashMap<>();

        // Tải âm thanh từ assets bằng SoundPool
        try {
            AssetFileDescriptor shootDescriptor = assetManager.openFd("sword-slash.wav");
            shootSoundId = soundPool.load(shootDescriptor, 1);
            if (shootSoundId == 0) System.out.println("Shoot sound not loaded!");
            AssetFileDescriptor enemyHitDescriptor = assetManager.openFd("enemy_dead.mp3");
            enemyHitSoundId = soundPool.load(enemyHitDescriptor, 1);
            if (enemyHitSoundId == 0) System.out.println("Enemy hit sound not loaded!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading sound from assets: " + e.getMessage());
        }

        // Khởi tạo MediaPlayer với file từ assets
        try {
            AssetFileDescriptor musicDescriptor = assetManager.openFd("background_music.mp3");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(musicDescriptor.getFileDescriptor(), musicDescriptor.getStartOffset(), musicDescriptor.getLength());
            mediaPlayer.prepare();
            mediaPlayer.setVolume(0.6f, 0.6f);
            mediaPlayer.setLooping(true);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading background music: " + e.getMessage());
        }

        // Tải Bitmap cho nút điều khiển
        musicTurnOnBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.music_turnon);
        musicTurnOffBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.music_turnoff);
        soundOffBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sound_off);
        soundOnBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sound_on);

        originalBackground = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        playerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.knight);
        enemyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.enemy);
        bulletBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bullet);

        // Scale player
        int playerWidth = playerBitmap.getWidth() / 4;
        int playerHeight = playerBitmap.getHeight() / 4;
        Bitmap scaledPlayerBitmap = Bitmap.createScaledBitmap(playerBitmap, playerWidth, playerHeight, true);
        player = new Player(100, 100, scaledPlayerBitmap, 5);

        // Scale enemy
        int enemyWidth = enemyBitmap.getWidth() / 6;
        int enemyHeight = enemyBitmap.getHeight() / 4;
        scaledEnemyBitmap = Bitmap.createScaledBitmap(enemyBitmap, enemyWidth, enemyHeight, true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (originalBackground != null && width > 0 && height > 0) {
            scaledBackground = Bitmap.createScaledBitmap(originalBackground, width * 2, height, true);
        }
        if (playerBitmap != null && width > 0 && height > 0) {
            int playerWidth = playerBitmap.getWidth() / 4;
            int playerHeight = playerBitmap.getHeight() / 4;
            Bitmap scaledPlayerBitmap = Bitmap.createScaledBitmap(playerBitmap, playerWidth, playerHeight, true);
            player = new Player(100, 100, scaledPlayerBitmap, 5);
        }

        if (scaledEnemyBitmap != null && width > 0 && height > 0) {
            enemies.clear();
            for (int i = 0; i < 3; i++) {
                float startY = (float) (Math.random() * (height - scaledEnemyBitmap.getHeight()));
                enemies.add(new Enemy(width, startY, scaledEnemyBitmap, 5));
            }
        }

        // Scale và đặt vị trí nút điều khiển
        int buttonSize = 100;
        musicTurnOnBitmap = Bitmap.createScaledBitmap(musicTurnOnBitmap, buttonSize, buttonSize, true);
        musicTurnOffBitmap = Bitmap.createScaledBitmap(musicTurnOffBitmap, buttonSize, buttonSize, true);
        soundOffBitmap = Bitmap.createScaledBitmap(soundOffBitmap, buttonSize, buttonSize, true);
        soundOnBitmap = Bitmap.createScaledBitmap(soundOnBitmap, buttonSize, buttonSize, true);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void run() {
        while (running) {
            if (!holder.getSurface().isValid()) continue;

            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                update();
                draw(canvas);
                holder.unlockCanvasAndPost(canvas);
            }

            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        player.update(getWidth(), getHeight());
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(getWidth(), getHeight());
            for (int j = projectiles.size() - 1; j >= 0; j--) {
                Projectile p = projectiles.get(j);
                if (isCollision(enemy, p)) {
                    enemies.remove(i);
                    projectiles.remove(j);
                    if (soundEffectsEnabled && soundPool != null && enemyHitSoundId != 0) {
                        soundPool.play(enemyHitSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                    }
                    break;
                }
            }
        }
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update(getWidth(), getHeight());
            if (!p.isActive()) {
                projectiles.remove(i);
            }
        }
        backgroundOffsetX -= backgroundSpeed;
        if (backgroundOffsetX <= -getWidth()) {
            backgroundOffsetX = 0;
        }
    }

    private boolean isCollision(Enemy enemy, Projectile projectile) {
        float enemyX = enemy.getX();
        float enemyY = enemy.getY();
        float projX = projectile.getX();
        float projY = projectile.getY();
        return projX < enemyX + enemy.getWidth() &&
                projX + projectile.getWidth() > enemyX &&
                projY < enemyY + enemy.getHeight() &&
                projY + projectile.getHeight() > enemyY;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (scaledBackground != null) {
            canvas.drawBitmap(scaledBackground, backgroundOffsetX, 0, paint);
            canvas.drawBitmap(scaledBackground, backgroundOffsetX + getWidth(), 0, paint);
        } else if (originalBackground != null) {
            canvas.drawBitmap(originalBackground, 0, 0, paint);
        } else {
            canvas.drawColor(Color.BLACK);
        }

        player.draw(canvas, paint);
        for (Enemy enemy : enemies) {
            enemy.draw(canvas, paint);
        }
        for (Projectile p : projectiles) {
            p.draw(canvas, paint);
        }

        // Vẽ nút điều khiển âm thanh
        int buttonSize = 100;
        int centerX = getWidth() / 2 - buttonSize / 2;
        if (musicEnabled) {
            canvas.drawBitmap(musicTurnOffBitmap, centerX, 10, paint); // Nút tắt nhạc (biên trên)
        } else {
            canvas.drawBitmap(musicTurnOnBitmap, centerX, 10, paint); // Nút bật nhạc
        }
        if (soundEffectsEnabled) {
            canvas.drawBitmap(soundOffBitmap, centerX, getHeight() - buttonSize - 10, paint); // Nút tắt hiệu ứng (biên dưới)
        } else {
            canvas.drawBitmap(soundOnBitmap, centerX, getHeight() - buttonSize - 10, paint); // Nút bật hiệu ứng
        }

        paint.setColor(Color.GRAY);
        canvas.drawRect(50, getHeight() - 250, 150, getHeight() - 150, paint); // Lên
        canvas.drawRect(50, getHeight() - 150, 150, getHeight() - 50, paint); // Xuống
        canvas.drawRect(0, getHeight() - 200, 100, getHeight() - 100, paint); // Trái
        canvas.drawRect(100, getHeight() - 200, 200, getHeight() - 100, paint); // Phải
        canvas.drawRect(getWidth() - 150, getHeight() - 150, getWidth() - 50, getHeight() - 50, paint); // Nút bắn
    }

    public void resume() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void pause() {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                player.setMovingUp(x >= 50 && x <= 150 && y >= getHeight() - 250 && y <= getHeight() - 150);
                player.setMovingDown(x >= 50 && x <= 150 && y >= getHeight() - 150 && y <= getHeight() - 50);
                player.setMovingLeft(x >= 0 && x <= 100 && y >= getHeight() - 200 && y <= getHeight() - 100);
                player.setMovingRight(x >= 100 && x <= 200 && y >= getHeight() - 200 && y <= getHeight() - 100);
                if (x >= getWidth() - 150 && x <= getWidth() - 50 && y >= getHeight() - 150 && y <= getHeight() - 50) {
                    shoot();
                }
                // Điều khiển nhạc nền
                int buttonSize = 100;
                int centerX = getWidth() / 2 - buttonSize / 2;
                if (x >= centerX && x <= centerX + buttonSize && y >= 10 && y <= 10 + buttonSize) {
                    toggleMusic();
                }
                // Điều khiển hiệu ứng âm thanh
                if (x >= centerX && x <= centerX + buttonSize && y >= getHeight() - buttonSize - 10 && y <= getHeight() - 10) {
                    toggleSoundEffects();
                }
                break;
            case MotionEvent.ACTION_UP:
                player.setMovingUp(false);
                player.setMovingDown(false);
                player.setMovingLeft(false);
                player.setMovingRight(false);
                break;
        }
        return true;
    }

    private void shoot() {
        if (bulletBitmap != null) {
            float bulletX = player.getX() + player.getWidth();
            float bulletY = player.getY() + (player.getHeight() - bulletBitmap.getHeight()) / 2;
            projectiles.add(new Projectile(bulletX, bulletY, bulletBitmap, 10));
            if (soundEffectsEnabled && soundPool != null && shootSoundId != 0) {
                soundPool.play(shootSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            }
        }
    }

    private void toggleMusic() {
        if (mediaPlayer != null) {
            if (!musicEnabled) {
                mediaPlayer.start();
                musicEnabled = true;
            } else {
                mediaPlayer.pause();
                musicEnabled = false;
            }
        }
    }

    private void toggleSoundEffects() {
        soundEffectsEnabled = !soundEffectsEnabled;
    }
}