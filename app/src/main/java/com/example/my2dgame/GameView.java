package com.example.my2dgame;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private static final String TAG = "GameView";
    private Thread gameThread;
    private final SurfaceHolder holder;
    private volatile boolean running = false;
    private final Paint paint;
    private Bitmap originalBackground;
    private Bitmap scaledBackground;
    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<CollisionEffect> collisionEffects = new ArrayList<>();
    private final ArrayList<Item> items = new ArrayList<>();
    private float backgroundOffsetX = 0;
    private static final float BACKGROUND_SPEED = 2;

    // Bitmaps and sounds
    private Bitmap enemyBitmap, playerBitmap, scaledEnemyBitmap, bulletBitmap, playerMeleeBitmap, playerBombItemBitmap, explosionBitmap;
    private Bitmap heartItemBitmap, shieldItemBitmap, invincibleItemBitmap;
    private SoundPool soundPool;
    private int shootSoundId, itemPickupSoundId;
    private boolean soundEffectsEnabled = true;
    private MediaPlayer mediaPlayer;
    private boolean musicEnabled = false;
    private Bitmap musicTurnOnBitmap, musicTurnOffBitmap, soundOffBitmap, soundOnBitmap;
    private final AssetManager assetManager;
    private final Random random = new Random();

    // Timing
    private long lastEnemySpawnTime = 0, lastItemSpawnTime = 0, gameStartTime;
    private long remainingTimeSeconds;
    private static final long ENEMY_SPAWN_INTERVAL = 5000;
    private static final long ITEM_SPAWN_INTERVAL = 8000;
    private static final long GAME_DURATION_SECONDS = 60;
    private static final int MELEE_ATTACK_DURATION_FRAMES = 5;

    // Game State
    private boolean isGameOver = false, isGameWon = false;

    // End Screen Assets
    private Bitmap gameOverImageBitmap, congratulationsBitmap, replayButtonBitmap, settingsButtonBitmap, menuButtonBitmap;
    private Rect replayButtonRect, settingsButtonRect, menuButtonRect;

    // High Scores
    private List<Integer> highScores;
    private static final String PREFS_NAME = "My2DGamePrefs";
    private static final String HIGH_SCORES_KEY = "highScores";

    // Initial player state
    private Bitmap initialScaledPlayerBitmap;
    private float initialPlayerX, initialPlayerY, initialPlayerSpeed;


    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(40);
        assetManager = context.getAssets();

        loadSoundAssets();
        loadMusicAssets();
        loadGameImageAssets();
        loadHighScores();
        initializePlayerState();
        scaleBitmaps();
    }

    private void loadHighScores() {
        highScores = new ArrayList<>();
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String scoresString = prefs.getString(HIGH_SCORES_KEY, "");
        if (scoresString.isEmpty()) {
            highScores.add(500); highScores.add(400); highScores.add(300);
            highScores.add(200); highScores.add(100); highScores.add(50);
            saveHighScores();
        } else {
            String[] scores = scoresString.split(",");
            for (String score : scores) {
                if (!score.isEmpty()) highScores.add(Integer.parseInt(score));
            }
        }
    }

    private void saveHighScores() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < highScores.size(); i++) {
            sb.append(highScores.get(i)).append(",");
        }
        editor.putString(HIGH_SCORES_KEY, sb.toString());
        editor.apply();
    }

    private void checkAndSaveHighScore(int newScore) {
        highScores.add(newScore);
        Collections.sort(highScores, Collections.reverseOrder());
        while (highScores.size() > 6) {
            highScores.remove(highScores.size() - 1);
        }
        saveHighScores();
    }

    private void loadSoundAssets() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        soundPool = new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(audioAttributes).build();
        try {
            AssetFileDescriptor shootDescriptor = assetManager.openFd("sword_slash.wav");
            shootSoundId = soundPool.load(shootDescriptor, 1);
            AssetFileDescriptor itemPickupDescriptor = assetManager.openFd("item_effect_music.mp3");
            itemPickupSoundId = soundPool.load(itemPickupDescriptor, 1);
        } catch (IOException e) { Log.e(TAG, "Error loading sound assets", e); }
    }

    private void loadMusicAssets() {
        try {
            AssetFileDescriptor musicDescriptor = assetManager.openFd("background_music.mp3");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(musicDescriptor.getFileDescriptor(), musicDescriptor.getStartOffset(), musicDescriptor.getLength());
            mediaPlayer.prepare();
            mediaPlayer.setVolume(0.6f, 0.6f);
            mediaPlayer.setLooping(true);
        } catch (IOException e) { Log.e(TAG, "Error preparing MediaPlayer", e); }
    }

    private void loadGameImageAssets() {
        explosionBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.explosion_effect);
        musicTurnOnBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.music_turnon);
        musicTurnOffBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.music_turnoff);
        soundOffBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sound_off);
        soundOnBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sound_on);
        originalBackground = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        playerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.knight);
        enemyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.enemy);
        bulletBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fireball);
        playerMeleeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.swordslash);
        playerBombItemBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bomb);
        heartItemBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.heart_item);
        shieldItemBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.shield_item);
        invincibleItemBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.invincible_item);
        gameOverImageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.you_lose);
        congratulationsBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.congratulations);
        replayButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.replay);
        settingsButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.setting);
        menuButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.menu);
    }

    private void initializePlayerState() {
        int playerWidth = playerBitmap != null ? playerBitmap.getWidth() / 4 : 50;
        int playerHeight = playerBitmap != null ? playerBitmap.getHeight() / 4 : 50;
        initialScaledPlayerBitmap = playerBitmap != null ? Bitmap.createScaledBitmap(playerBitmap, playerWidth, playerHeight, true) : Bitmap.createBitmap(playerWidth, playerHeight, Bitmap.Config.ARGB_8888);
        initialPlayerX = 200; initialPlayerY = 300; initialPlayerSpeed = 5;
        player = new Player(initialPlayerX, initialPlayerY, initialScaledPlayerBitmap, initialPlayerSpeed);
    }

    private void scaleBitmaps() {
        int enemyW = enemyBitmap != null ? enemyBitmap.getWidth() / 6 : 50;
        int enemyH = enemyBitmap != null ? enemyBitmap.getHeight() / 4 : 50;
        scaledEnemyBitmap = enemyBitmap != null ? Bitmap.createScaledBitmap(enemyBitmap, enemyW, enemyH, true) : Bitmap.createBitmap(enemyW, enemyH, Bitmap.Config.ARGB_8888);
        int itemSize = (initialScaledPlayerBitmap != null) ? initialScaledPlayerBitmap.getWidth() / 2 : 25;
        if (heartItemBitmap != null) heartItemBitmap = Bitmap.createScaledBitmap(heartItemBitmap, itemSize, itemSize, true);
        if (shieldItemBitmap != null) shieldItemBitmap = Bitmap.createScaledBitmap(shieldItemBitmap, itemSize, itemSize, true);
        if (invincibleItemBitmap != null) invincibleItemBitmap = Bitmap.createScaledBitmap(invincibleItemBitmap, itemSize, itemSize, true);
        if (explosionBitmap != null && scaledEnemyBitmap != null) explosionBitmap = Bitmap.createScaledBitmap(explosionBitmap, (int) (scaledEnemyBitmap.getWidth() * 1.5), (int) (scaledEnemyBitmap.getHeight() * 1.5), true);
        if (bulletBitmap != null && initialScaledPlayerBitmap != null) bulletBitmap = Bitmap.createScaledBitmap(bulletBitmap, initialScaledPlayerBitmap.getWidth() / 2, initialScaledPlayerBitmap.getHeight() / 2, true);
        if (playerMeleeBitmap != null && initialScaledPlayerBitmap != null) playerMeleeBitmap = Bitmap.createScaledBitmap(playerMeleeBitmap, initialScaledPlayerBitmap.getWidth(), initialScaledPlayerBitmap.getHeight(), true);
        if (playerBombItemBitmap != null && initialScaledPlayerBitmap != null) playerBombItemBitmap = Bitmap.createScaledBitmap(playerBombItemBitmap, initialScaledPlayerBitmap.getWidth() / 3, initialScaledPlayerBitmap.getHeight() / 3, true);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) { resume(); }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder sh, int format, int width, int height) {
        if (originalBackground != null && width > 0 && height > 0) scaledBackground = Bitmap.createScaledBitmap(originalBackground, width * 2, height, true);
        int buttonSize = 100;
        if (musicTurnOnBitmap != null) musicTurnOnBitmap = Bitmap.createScaledBitmap(musicTurnOnBitmap, buttonSize, buttonSize, true);
        if (musicTurnOffBitmap != null) musicTurnOffBitmap = Bitmap.createScaledBitmap(musicTurnOffBitmap, buttonSize, buttonSize, true);
        if (soundOffBitmap != null) soundOffBitmap = Bitmap.createScaledBitmap(soundOffBitmap, buttonSize, buttonSize, true);
        if (soundOnBitmap != null) soundOnBitmap = Bitmap.createScaledBitmap(soundOnBitmap, buttonSize, buttonSize, true);

        if (gameOverImageBitmap != null && width > 0 && height > 0) {
            float aspectRatio = (float) gameOverImageBitmap.getWidth() / gameOverImageBitmap.getHeight();
            int endScreenImgHeight = height / 4;
            int endScreenImgWidth = (int) (endScreenImgHeight * aspectRatio);
            if (endScreenImgWidth > width * 0.8) {
                endScreenImgWidth = (int) (width * 0.8);
                endScreenImgHeight = (int) (endScreenImgWidth / aspectRatio);
            }
            gameOverImageBitmap = Bitmap.createScaledBitmap(gameOverImageBitmap, endScreenImgWidth, endScreenImgHeight, true);
            if (congratulationsBitmap != null) congratulationsBitmap = Bitmap.createScaledBitmap(congratulationsBitmap, endScreenImgWidth, endScreenImgHeight, true);
        }

        int gameOverButtonWidth = width / 6; int gameOverButtonHeight = height / 12;
        if (replayButtonBitmap != null) replayButtonBitmap = Bitmap.createScaledBitmap(replayButtonBitmap, gameOverButtonWidth, gameOverButtonHeight, true);
        if (settingsButtonBitmap != null) settingsButtonBitmap = Bitmap.createScaledBitmap(settingsButtonBitmap, gameOverButtonWidth, gameOverButtonHeight, true);
        if (menuButtonBitmap != null) menuButtonBitmap = Bitmap.createScaledBitmap(menuButtonBitmap, gameOverButtonWidth, gameOverButtonHeight, true);

        if (getWidth() > 0 && getHeight() > 0 && gameOverImageBitmap != null && replayButtonBitmap != null) {
            int endScreenImgY = getHeight() / 5;
            int buttonsY = endScreenImgY + (congratulationsBitmap != null ? congratulationsBitmap.getHeight() : gameOverImageBitmap.getHeight()) + 220;
            int buttonSpacing = 20;

            int totalButtonRowWidth = (3 * gameOverButtonWidth) + (2 * buttonSpacing);
            int startX = (getWidth() - totalButtonRowWidth) / 2;

            replayButtonRect = new Rect(startX, buttonsY, startX + gameOverButtonWidth, buttonsY + gameOverButtonHeight);
            menuButtonRect = new Rect(startX + gameOverButtonWidth + buttonSpacing, buttonsY, startX + 2 * gameOverButtonWidth + buttonSpacing, buttonsY + gameOverButtonHeight);
            settingsButtonRect = new Rect(startX + 2 * gameOverButtonWidth + 2 * buttonSpacing, buttonsY, startX + 3 * gameOverButtonWidth + 2 * buttonSpacing, buttonsY + gameOverButtonHeight);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) { pause(); }

    @Override
    public void run() {
        restartGame();
        while (running) {
            if (!holder.getSurface().isValid()) continue;
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        update();
                        draw(canvas);
                    }
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
            try { Thread.sleep(16); } catch (InterruptedException e) { Log.w(TAG, "Game thread interrupted", e); }
        }
    }

    private void update() {
        if (isGameOver || isGameWon) return;

        long currentTime = System.currentTimeMillis();
        remainingTimeSeconds = GAME_DURATION_SECONDS - ((currentTime - gameStartTime) / 1000);

        if (remainingTimeSeconds <= 0) {
            checkAndSaveHighScore(player != null ? player.getGold() : 0);
            isGameWon = true;
            if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
            return;
        }

        if (currentTime - lastEnemySpawnTime >= ENEMY_SPAWN_INTERVAL) { spawnEnemies(); lastEnemySpawnTime = currentTime; }
        if (currentTime - lastItemSpawnTime >= ITEM_SPAWN_INTERVAL) { spawnRandomItem(); lastItemSpawnTime = currentTime; }

        if (player != null) {
            player.update(getWidth(), getHeight());
            if (player.getHealth() <= 0) {
                isGameOver = true;
                if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
            }
        }

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(player != null ? player.getX() : 0, player != null ? player.getY() : 0, getWidth(), getHeight());
            if (isCollision(player, enemy)) {
                enemies.remove(i);
                if (player != null && !player.isInvincible()) {
                    if (!player.isShieldActive()) player.applyGenericEffect("health", -10);
                    else player.applyGenericEffect("health", -5);
                }
                if (explosionBitmap != null) collisionEffects.add(new CollisionEffect(enemy.getX(), enemy.getY(), explosionBitmap));
            }
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update(getWidth(), getHeight());
            if (!p.isActive()) { projectiles.remove(i); continue; }
            if (p.getType() == ProjectileType.PLAYER_SWORD_WAVE) {
                for (int j = enemies.size() - 1; j >= 0; j--) {
                    if (isCollision(enemies.get(j), p)) {
                        if (explosionBitmap != null) collisionEffects.add(new CollisionEffect(enemies.get(j).getX(), enemies.get(j).getY(), explosionBitmap));
                        enemies.remove(j);
                        if (player != null) player.applyGenericEffect("gold", 15);
                    }
                }
                if (p.getLifespan() <= 0) p.setActive(false); else p.decrementLifespan();
            } else {
                for (int j = enemies.size() - 1; j >= 0; j--) {
                    if (isCollision(enemies.get(j), p)) {
                        if (explosionBitmap != null) collisionEffects.add(new CollisionEffect(enemies.get(j).getX(), enemies.get(j).getY(), explosionBitmap));
                        enemies.remove(j);
                        projectiles.remove(i);
                        if (player != null) player.applyGenericEffect("gold", 10);
                        break;
                    }
                }
            }
        }

        for (int i = collisionEffects.size() - 1; i >= 0; i--) {
            if (!collisionEffects.get(i).isAlive()) collisionEffects.remove(i);
            else collisionEffects.get(i).update();
        }

        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            item.update(getWidth(), getHeight());
            if (!item.isActive()) { items.remove(i); continue; }
            if (isCollision(player, item)) {
                items.remove(i);
                if (soundEffectsEnabled && soundPool != null) soundPool.play(itemPickupSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                if (player != null) player.applyItemEffect(item.getType());
                if (explosionBitmap != null) collisionEffects.add(new CollisionEffect(item.getX(), item.getY(), explosionBitmap));
            }
        }

        backgroundOffsetX -= BACKGROUND_SPEED;
        if (getWidth() > 0 && backgroundOffsetX <= -getWidth()) backgroundOffsetX = 0;
    }
    
    private void spawnRandomItem() {
        if (getHeight() <= 0 || items.size() > 0) return;
        ItemType type = ItemType.values()[random.nextInt(ItemType.values().length)];
        Bitmap itemBitmap = null;
        switch (type) {
            case HEART: itemBitmap = heartItemBitmap; break;
            case SHIELD: itemBitmap = shieldItemBitmap; break;
            case INVINCIBILITY: itemBitmap = invincibleItemBitmap; break;
        }
        if (itemBitmap != null) {
            items.add(new Item(getWidth(), random.nextFloat() * (getHeight() - itemBitmap.getHeight()), itemBitmap, type));
            Log.d(TAG, "Spawned item: " + type);
        }
    }

    private void spawnEnemies() {
        if (scaledEnemyBitmap == null || getHeight() <= 0) return;
        for (int i = 0; i < random.nextInt(3) + 1; i++) {
            enemies.add(new Enemy(getWidth(), random.nextFloat() * (getHeight() - scaledEnemyBitmap.getHeight()), scaledEnemyBitmap, 5));
        }
    }

    private Rect createHitbox(float x, float y, int width, int height, float scale) {
        int newWidth = (int) (width * scale); int newHeight = (int) (height * scale);
        return new Rect((int)(x + (width - newWidth) / 2), (int)(y + (height - newHeight) / 2), (int)(x + (width + newWidth) / 2), (int)(y + (height + newHeight) / 2));
    }

    private boolean isCollision(Player player, Enemy enemy) {
        if (player == null || enemy == null) return false;
        return Rect.intersects(createHitbox(player.getX(), player.getY(), (int)player.getWidth(), (int)player.getHeight(), 0.8f), createHitbox(enemy.getX(), enemy.getY(), (int)enemy.getWidth(), (int)enemy.getHeight(), 0.85f));
    }

    private boolean isCollision(Player player, Item item) {
        if (player == null || item == null) return false;
        return Rect.intersects(new Rect((int)player.getX(), (int)player.getY(), (int)(player.getX() + player.getWidth()), (int)(player.getY() + player.getHeight())), new Rect((int)item.getX(), (int)item.getY(), (int)(item.getX() + item.getWidth()), (int)(item.getY() + item.getHeight())));
    }

    private boolean isCollision(Enemy enemy, Projectile projectile) {
        if (enemy == null || projectile == null) return false;
        return Rect.intersects(createHitbox(enemy.getX(), enemy.getY(), (int)enemy.getWidth(), (int)enemy.getHeight(), 0.85f), new Rect((int)projectile.getX(), (int)projectile.getY(), (int)(projectile.getX() + projectile.getWidth()), (int)(projectile.getY() + projectile.getHeight())));
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        if (scaledBackground != null) {
            canvas.drawBitmap(scaledBackground, backgroundOffsetX, 0, paint);
            if (getWidth() > 0) canvas.drawBitmap(scaledBackground, backgroundOffsetX + getWidth(), 0, paint);
        } else { canvas.drawColor(Color.BLACK); }

        if (isGameOver) {
            drawEndScreen(canvas, gameOverImageBitmap);
        } else if (isGameWon) {
            drawEndScreen(canvas, congratulationsBitmap);
        } else {
            drawGame(canvas);
        }
    }
    
    private void drawEndScreen(Canvas canvas, Bitmap endImage) {
        if (endImage != null) canvas.drawBitmap(endImage, (getWidth() - endImage.getWidth()) / 2f, getHeight() / 5f, paint);
        
        if (isGameWon) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(40);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("High Scores:", getWidth() / 2f, getHeight() / 2f, paint);
            float yPos = getHeight() / 2f + 60;
            for (int i = 0; i < highScores.size(); i++) {
                canvas.drawText((i + 1) + ". " + highScores.get(i), getWidth() / 2f, yPos, paint);
                yPos += 50;
            }
        }

        paint.setColor(Color.YELLOW);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Your Gold: " + (player != null ? player.getGold() : 0), getWidth() / 2f, getHeight() * 0.8f, paint);
        
        // Only draw buttons on Game Over screen
        if(isGameOver) {
            if (replayButtonRect != null && replayButtonBitmap != null) canvas.drawBitmap(replayButtonBitmap, replayButtonRect.left, replayButtonRect.top, paint);
            if (menuButtonRect != null && menuButtonBitmap != null) canvas.drawBitmap(menuButtonBitmap, menuButtonRect.left, menuButtonRect.top, paint);
            if (settingsButtonRect != null && settingsButtonBitmap != null) canvas.drawBitmap(settingsButtonBitmap, settingsButtonRect.left, settingsButtonRect.top, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGame(Canvas canvas) {
        if (player != null) player.draw(canvas, paint);
        for (Enemy enemy : enemies) enemy.draw(canvas, paint);
        for (Projectile p : projectiles) p.draw(canvas, paint);
        for (Item item : items) item.draw(canvas, paint);
        for (CollisionEffect effect : collisionEffects) effect.draw(canvas, paint);

        int topButtonSize = 100;
        int topCenterX = getWidth() > 0 ? getWidth() / 2 - topButtonSize / 2 : 0;
        if (musicEnabled && musicTurnOffBitmap != null) canvas.drawBitmap(musicTurnOffBitmap, topCenterX, 10, paint);
        else if (!musicEnabled && musicTurnOnBitmap != null) canvas.drawBitmap(musicTurnOnBitmap, topCenterX, 10, paint);
        if (soundOffBitmap != null && soundOnBitmap != null) {
            if (soundEffectsEnabled) canvas.drawBitmap(soundOffBitmap, topCenterX + topButtonSize + 10, 10, paint);
            else canvas.drawBitmap(soundOnBitmap, topCenterX + topButtonSize + 10, 10, paint);
        }

        paint.setColor(Color.GRAY);
        paint.setAlpha(128);
        if (getHeight() > 250 && getWidth() > 200) {
            canvas.drawRect(50, getHeight() - 250, 150, getHeight() - 150, paint);
            canvas.drawRect(50, getHeight() - 150, 150, getHeight() - 50, paint);
            canvas.drawRect(0, getHeight() - 200, 100, getHeight() - 100, paint);
            canvas.drawRect(100, getHeight() - 200, 200, getHeight() - 100, paint);

            int actionButtonY = getHeight() - 150, h = 100, w = 100, s = 10;
            Rect slashR = new Rect(getWidth() - w - s, actionButtonY, getWidth() - s, actionButtonY + h);
            canvas.drawRect(slashR, paint);
            Rect bombR = new Rect(slashR.left - w - s, actionButtonY, slashR.left - s, actionButtonY + h);
            canvas.drawRect(bombR, paint);
            Rect fireR = new Rect(bombR.left - w - s, actionButtonY, bombR.left - s, actionButtonY + h);
            canvas.drawRect(fireR, paint);

            paint.setAlpha(255); paint.setColor(Color.WHITE); paint.setTextSize(30); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("S", slashR.centerX(), slashR.centerY() + 10, paint);
            canvas.drawText("B", bombR.centerX(), bombR.centerY() + 10, paint);
            canvas.drawText("F", fireR.centerX(), fireR.centerY() + 10, paint);
        }

        paint.setTextAlign(Paint.Align.LEFT); paint.setTextSize(40);
        if (player != null) {
            paint.setColor(Color.WHITE);
            canvas.drawText("Health: " + player.getHealth(), 10, 50, paint);
            canvas.drawText("Armor: " + player.getArmor(), 10, 90, paint);
            canvas.drawText("Gold: " + player.getGold(), 10, 130, paint);
            if (player.getCurrentWeapon() != null) canvas.drawText("Weapon: " + player.getCurrentWeapon().toString(), 10, 170, paint);
            if (player.isInvincible()) {
                paint.setColor(Color.YELLOW);
                canvas.drawText("INVINCIBLE!", getWidth() / 2f - paint.measureText("INVINCIBLE!") / 2, 50, paint);
            }
        }
        paint.setColor(Color.YELLOW); paint.setTextSize(50); paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.getDefault(), "%02d:%02d", remainingTimeSeconds / 60, remainingTimeSeconds % 60), getWidth() - 20, 60, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    public void resume() { running = true; gameThread = new Thread(this); gameThread.start(); }
    public void pause() { running = false; boolean retry = true; while (retry) { try { if (gameThread != null) gameThread.join(); retry = false; } catch (InterruptedException e) { Log.w(TAG, "Game thread join interrupted", e); } } }

    private void restartGame() {
        isGameOver = false; isGameWon = false;
        player = new Player(initialPlayerX, initialPlayerY, initialScaledPlayerBitmap, initialPlayerSpeed);
        enemies.clear(); projectiles.clear(); items.clear(); collisionEffects.clear();
        backgroundOffsetX = 0; gameStartTime = System.currentTimeMillis();
        lastEnemySpawnTime = System.currentTimeMillis(); lastItemSpawnTime = System.currentTimeMillis();
        if (musicEnabled && mediaPlayer != null && !mediaPlayer.isPlaying()) { mediaPlayer.seekTo(0); mediaPlayer.start(); }
        Log.d(TAG, "Game Restarted");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getHeight() <= 0 || getWidth() <= 0) return super.onTouchEvent(event);
        float touchX = event.getX(); float touchY = event.getY(); int action = event.getAction();

        if (isGameOver) { // Only handle touches on Game Over screen
            if (action == MotionEvent.ACTION_DOWN) {
                if (replayButtonRect != null && replayButtonRect.contains((int) touchX, (int) touchY)) { restartGame(); performClick(); return true; }
                if (menuButtonRect != null && menuButtonRect.contains((int) touchX, (int) touchY)) { Log.d(TAG, "Menu button clicked."); performClick(); return true; }
                if (settingsButtonRect != null && settingsButtonRect.contains((int) touchX, (int) touchY)) { Log.d(TAG, "Settings button clicked."); performClick(); return true; }
            }
            return true;
        }
        
        if(isGameWon) return true; // Consume touches on win screen

        if (player != null) {
             if (touchX >= 50 && touchX <= 150 && touchY >= getHeight() - 250 && touchY <= getHeight() - 150) player.setMovingUp(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
             else if (touchX >= 50 && touchX <= 150 && touchY >= getHeight() - 150 && touchY <= getHeight() - 50) player.setMovingDown(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
             else if (touchX >= 0 && touchX <= 100 && touchY >= getHeight() - 200 && touchY <= getHeight() - 100) player.setMovingLeft(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
             else if (touchX >= 100 && touchX <= 200 && touchY >= getHeight() - 200 && touchY <= getHeight() - 100) player.setMovingRight(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            int actionButtonY = getHeight() - 150, h = 100, w = 100, s = 10;
            Rect slashR = new Rect(getWidth() - w - s, actionButtonY, getWidth() - s, actionButtonY + h);
            Rect bombR = new Rect(slashR.left - w - s, actionButtonY, slashR.left - s, actionButtonY + h);
            Rect fireR = new Rect(bombR.left - w - s, actionButtonY, bombR.left - s, actionButtonY + h);
            if (player != null) {
                if (fireR.contains((int)touchX, (int)touchY)) { player.switchAttack(WeaponType.FIREBALL); shoot(); performClick(); }
                else if (bombR.contains((int)touchX, (int)touchY)) { player.switchAttack(WeaponType.BOMB_DROP); shoot(); performClick(); }
                else if (slashR.contains((int)touchX, (int)touchY)) { player.switchAttack(WeaponType.SWORD_SLASH); shoot(); performClick(); }
            }

            int topButtonSize = 100; int topCenterX = getWidth() > 0 ? getWidth() / 2 - topButtonSize / 2 : 0;
            if (touchX >= topCenterX && touchX <= topCenterX + topButtonSize && touchY >= 10 && touchY <= 10 + topButtonSize) { toggleMusic(); performClick(); }
            if (touchX >= topCenterX + topButtonSize + 10 && touchX <= topCenterX + topButtonSize * 2 + 10 && touchY >= 10 && touchY <= 10 + topButtonSize) { toggleSoundEffects(); performClick(); }

        } else if (action == MotionEvent.ACTION_UP && player != null) {
            player.setMovingUp(false); player.setMovingDown(false); player.setMovingLeft(false); player.setMovingRight(false);
        }
        return true;
    }

    @Override
    public boolean performClick() { super.performClick(); return true; }

    private void triggerSuperBombEffect() {
        if (explosionBitmap == null || enemies.isEmpty()) return;
        int enemiesCleared = enemies.size();
        for (Enemy enemy : new ArrayList<>(enemies)) collisionEffects.add(new CollisionEffect(enemy.getX(), enemy.getY(), explosionBitmap));
        enemies.clear();
        Log.d(TAG, "Super Bomb triggered, " + enemiesCleared + " enemies cleared.");
        if (player != null) player.applyGenericEffect("gold", enemiesCleared * 5);
    }

    private void shoot() {
        if (player == null) return;
        WeaponType currentWeapon = player.getCurrentWeapon();
        if (currentWeapon == null) return;
        if ((currentWeapon == WeaponType.FIREBALL && bulletBitmap == null) || (currentWeapon == WeaponType.BOMB_DROP && playerBombItemBitmap == null) || (currentWeapon == WeaponType.SWORD_SLASH && playerMeleeBitmap == null)) return;

        Projectile p = player.createAttack(bulletBitmap, playerMeleeBitmap, playerBombItemBitmap, 15f);
        if (p != null) {
            if (p.getType() == ProjectileType.PLAYER_SUPER_BOMB) {
                triggerSuperBombEffect();
                if (soundEffectsEnabled && soundPool != null) soundPool.play(shootSoundId, 1.2f, 1.2f, 1, 0, 1.1f);
            } else {
                if (p.getType() == ProjectileType.PLAYER_SWORD_WAVE) p.setLifespan(MELEE_ATTACK_DURATION_FRAMES);
                projectiles.add(p);
                if (soundEffectsEnabled && soundPool != null) soundPool.play(shootSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            }
        } else Log.w(TAG, "player.createAttack() returned null for weapon: " + currentWeapon);
    }

    private void toggleMusic() {
        if (mediaPlayer == null) return;
        if (!musicEnabled) {
            if (!mediaPlayer.isPlaying()) mediaPlayer.seekTo(0);
            mediaPlayer.start();
            musicEnabled = true;
        } else {
            mediaPlayer.pause();
            musicEnabled = false;
        }
    }

    private void toggleSoundEffects() {
        soundEffectsEnabled = !soundEffectsEnabled;
        Log.d(TAG, "Sound effects " + (soundEffectsEnabled ? "enabled" : "disabled"));
    }
}
