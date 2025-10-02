package com.example.my2dgame;

import android.content.Context;
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
import java.util.Random;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private static final String TAG = "GameView";
    private Thread gameThread;
    private final SurfaceHolder holder;
    private volatile boolean running = false;
    private final Paint paint;
    private Bitmap originalBackground; // Removed final
    private Bitmap scaledBackground;
    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<CollisionEffect> collisionEffects = new ArrayList<>();
    private final ArrayList<Item> items = new ArrayList<>();
    private float backgroundOffsetX = 0;
    private static final float BACKGROUND_SPEED = 2; // Changed to static final

    private Bitmap enemyBitmap;
    private Bitmap playerBitmap;
    private Bitmap scaledEnemyBitmap;
    private Bitmap bulletBitmap;
    private Bitmap playerMeleeBitmap;
    private Bitmap playerBombItemBitmap;
    private Bitmap itemBitmap;
    private Bitmap explosionBitmap;

    private SoundPool soundPool;
    private int shootSoundId;
    private int itemPickupSoundId;
    private boolean soundEffectsEnabled = true;

    private MediaPlayer mediaPlayer;
    private boolean musicEnabled = false;

    private Bitmap musicTurnOnBitmap;
    private Bitmap musicTurnOffBitmap;
    private Bitmap soundOffBitmap;
    private Bitmap soundOnBitmap;

    private final AssetManager assetManager;
    private final Random random = new Random();
    private long lastEnemySpawnTime = 0;
    private static final long SPAWN_INTERVAL = 5000;
    private static final int MELEE_ATTACK_DURATION_FRAMES = 5;

    // Game Over state
    private boolean isGameOver = false;
    private Bitmap gameOverImageBitmap;
    private Bitmap replayButtonBitmap;
    private Bitmap settingsButtonBitmap;
    private Bitmap menuButtonBitmap; // Added Menu button
    private Rect replayButtonRect;
    private Rect settingsButtonRect;
    private Rect menuButtonRect; // Added Menu button rect

    // Initial player state for restart
    private Bitmap initialScaledPlayerBitmap;
    private float initialPlayerX;
    private float initialPlayerY;
    private float initialPlayerSpeed;


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
        initializePlayerState();
        scaleBitmaps();
    }

    private void loadSoundAssets() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build();
        try {
            AssetFileDescriptor shootDescriptor = assetManager.openFd("sword_slash.wav");
            shootSoundId = soundPool.load(shootDescriptor, 1);
            AssetFileDescriptor itemPickupDescriptor = assetManager.openFd("item_effect_music.mp3");
            itemPickupSoundId = soundPool.load(itemPickupDescriptor, 1);
        } catch (IOException e) {
            Log.e(TAG, "Error loading sound assets", e);
        }
    }

    private void loadMusicAssets() {
        try {
            AssetFileDescriptor musicDescriptor = assetManager.openFd("background_music.mp3");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(musicDescriptor.getFileDescriptor(), musicDescriptor.getStartOffset(), musicDescriptor.getLength());
            mediaPlayer.prepare();
            mediaPlayer.setVolume(0.6f, 0.6f);
            mediaPlayer.setLooping(true);
        } catch (IOException e) {
            Log.e(TAG, "Error preparing MediaPlayer", e);
        }
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
        itemBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.item);
        playerMeleeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.swordslash);
        playerBombItemBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bomb);

        gameOverImageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.you_lose);
        replayButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.replay);
        settingsButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.setting);
        menuButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.menu); // Load menu button image
    }

    private void initializePlayerState() {
        int playerWidth = playerBitmap != null ? playerBitmap.getWidth() / 4 : 50;
        int playerHeight = playerBitmap != null ? playerBitmap.getHeight() / 4 : 50;
        initialScaledPlayerBitmap = playerBitmap != null ? Bitmap.createScaledBitmap(playerBitmap, playerWidth, playerHeight, true) : Bitmap.createBitmap(playerWidth, playerHeight, Bitmap.Config.ARGB_8888);
        initialPlayerX = 200;
        initialPlayerY = 300;
        initialPlayerSpeed = 5;
        player = new Player(initialPlayerX, initialPlayerY, initialScaledPlayerBitmap, initialPlayerSpeed);
    }

    private void scaleBitmaps() {
        int enemyW = enemyBitmap != null ? enemyBitmap.getWidth() / 6 : 50;
        int enemyH = enemyBitmap != null ? enemyBitmap.getHeight() / 4 : 50;
        scaledEnemyBitmap = enemyBitmap != null ? Bitmap.createScaledBitmap(enemyBitmap, enemyW, enemyH, true) : Bitmap.createBitmap(enemyW, enemyH, Bitmap.Config.ARGB_8888);

        if (itemBitmap != null && initialScaledPlayerBitmap != null && initialScaledPlayerBitmap.getWidth() > 0 && initialScaledPlayerBitmap.getHeight() > 0) {
            int itemW = initialScaledPlayerBitmap.getWidth() / 2;
            int itemH = initialScaledPlayerBitmap.getHeight() / 2;
            itemBitmap = Bitmap.createScaledBitmap(itemBitmap, itemW, itemH, true);
        }

        if (explosionBitmap != null && scaledEnemyBitmap != null && scaledEnemyBitmap.getWidth() > 0 && scaledEnemyBitmap.getHeight() > 0) {
            int expWidth = (int) (scaledEnemyBitmap.getWidth() * 1.5);
            int expHeight = (int) (scaledEnemyBitmap.getHeight() * 1.5);
            explosionBitmap = Bitmap.createScaledBitmap(explosionBitmap, expWidth, expHeight, true);
        }

        if (bulletBitmap != null && initialScaledPlayerBitmap != null && initialScaledPlayerBitmap.getWidth() > 0 && initialScaledPlayerBitmap.getHeight() > 0) {
            int bulletW = initialScaledPlayerBitmap.getWidth() / 2;
            int bulletH = initialScaledPlayerBitmap.getHeight() / 2;
            bulletBitmap = Bitmap.createScaledBitmap(bulletBitmap, bulletW, bulletH, true);
        }

        if (playerMeleeBitmap != null && initialScaledPlayerBitmap != null && initialScaledPlayerBitmap.getWidth() > 0 && initialScaledPlayerBitmap.getHeight() > 0) {
            playerMeleeBitmap = Bitmap.createScaledBitmap(playerMeleeBitmap, initialScaledPlayerBitmap.getWidth(), initialScaledPlayerBitmap.getHeight(), true);
        }
        if (playerBombItemBitmap != null && initialScaledPlayerBitmap != null && initialScaledPlayerBitmap.getWidth() > 0 && initialScaledPlayerBitmap.getHeight() > 0) {
            playerBombItemBitmap = Bitmap.createScaledBitmap(playerBombItemBitmap, initialScaledPlayerBitmap.getWidth() / 3, initialScaledPlayerBitmap.getHeight() / 3, true);
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        resume();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder sh, int format, int width, int height) {
        if (originalBackground != null && width > 0 && height > 0) {
            scaledBackground = Bitmap.createScaledBitmap(originalBackground, width * 2, height, true);
        }
        int buttonSize = 100;
        if (musicTurnOnBitmap != null)
            musicTurnOnBitmap = Bitmap.createScaledBitmap(musicTurnOnBitmap, buttonSize, buttonSize, true);
        if (musicTurnOffBitmap != null)
            musicTurnOffBitmap = Bitmap.createScaledBitmap(musicTurnOffBitmap, buttonSize, buttonSize, true);
        if (soundOffBitmap != null)
            soundOffBitmap = Bitmap.createScaledBitmap(soundOffBitmap, buttonSize, buttonSize, true);
        if (soundOnBitmap != null)
            soundOnBitmap = Bitmap.createScaledBitmap(soundOnBitmap, buttonSize, buttonSize, true);

        if (gameOverImageBitmap != null && width > 0 && height > 0) {
            float aspectRatio = (float) gameOverImageBitmap.getWidth() / gameOverImageBitmap.getHeight();
            int gameOverHeight = height / 3;
            int gameOverWidth = (int) (gameOverHeight * aspectRatio);
            if (gameOverWidth > width * 0.8) {
                gameOverWidth = (int) (width * 0.8);
                gameOverHeight = (int) (gameOverWidth / aspectRatio);
            }
            gameOverImageBitmap = Bitmap.createScaledBitmap(gameOverImageBitmap, gameOverWidth, gameOverHeight, true);
        }

        int gameOverButtonWidth = width / 5;
        int gameOverButtonHeight = height / 10;
        if (replayButtonBitmap != null) {
            replayButtonBitmap = Bitmap.createScaledBitmap(replayButtonBitmap, gameOverButtonWidth, gameOverButtonHeight, true);
        }
        if (settingsButtonBitmap != null) {
            settingsButtonBitmap = Bitmap.createScaledBitmap(settingsButtonBitmap, gameOverButtonWidth, gameOverButtonHeight, true);
        }
        if (menuButtonBitmap != null) { // Scale menu button
            menuButtonBitmap = Bitmap.createScaledBitmap(menuButtonBitmap, gameOverButtonWidth, gameOverButtonHeight, true);
        }

        if (getWidth() > 0 && getHeight() > 0 && gameOverImageBitmap != null && replayButtonBitmap != null && settingsButtonBitmap != null && menuButtonBitmap != null) {
            int gameOverImageY = getHeight() / 4;
            int buttonsY = gameOverImageY + gameOverImageBitmap.getHeight() + 50;
            int buttonSpacing = 20;

            // Calculate total width of all buttons and spacing
            int totalButtonRowWidth = (3 * gameOverButtonWidth) + (2 * buttonSpacing);
            int startX = (getWidth() - totalButtonRowWidth) / 2;

            int replayButtonX = startX;
            replayButtonRect = new Rect(replayButtonX, buttonsY, replayButtonX + replayButtonBitmap.getWidth(), buttonsY + replayButtonBitmap.getHeight());

            int menuButtonX = replayButtonX + gameOverButtonWidth + buttonSpacing;
            menuButtonRect = new Rect(menuButtonX, buttonsY, menuButtonX + menuButtonBitmap.getWidth(), buttonsY + menuButtonBitmap.getHeight());

            int settingsButtonX = menuButtonX + gameOverButtonWidth + buttonSpacing;
            settingsButtonRect = new Rect(settingsButtonX, buttonsY, settingsButtonX + settingsButtonBitmap.getWidth(), buttonsY + settingsButtonBitmap.getHeight());
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
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
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Log.w(TAG, "Game thread interrupted", e);
            }
        }
    }

    private void update() {
        if (isGameOver) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnemySpawnTime >= SPAWN_INTERVAL) {
            spawnEnemies();
            lastEnemySpawnTime = currentTime;
            if (itemBitmap != null && random.nextInt(100) < 40 && getHeight() > 0 && itemBitmap.getHeight() < getHeight()) {
                float itemHeight = itemBitmap.getHeight();
                float startX = getWidth();
                float startY = random.nextFloat() * (getHeight() - itemHeight);
                items.add(new Item(startX, startY, itemBitmap, 3.0f));
            }
        }

        if (player != null) {
            player.update(getWidth(), getHeight());
            if (player.getHealth() <= 0 && !isGameOver) {
                isGameOver = true;
                Log.d(TAG, "Game Over!");
                if (musicEnabled && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }
        }

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy == null) continue;
            enemy.update(player != null ? player.getX() : 0, player != null ? player.getY() : 0, getWidth(), getHeight());
            if (enemy.getX() < -enemy.getWidth()) {
                enemies.remove(i);
                continue;
            }
            if (isCollision(player, enemy)) {
                enemies.remove(i);
                if (player != null && !player.isInvincible()) {
                    if (!player.isShieldActive()) {
                        player.applyGenericEffect("health", -10);
                    } else {
                        player.applyGenericEffect("health", -5);
                    }
                } else if (player != null && player.isInvincible()) {
                    Log.d(TAG, "Player invincible, no damage from enemy collision.");
                }
                if (explosionBitmap != null) collisionEffects.add(new CollisionEffect(enemy.getX(), enemy.getY(), explosionBitmap));
            }
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (p == null) {
                projectiles.remove(i);
                continue;
            }
            p.update(getWidth(), getHeight());
            if (!p.isActive()) {
                projectiles.remove(i);
                continue;
            }
            if (p.getType() == ProjectileType.PLAYER_SWORD_WAVE) {
                for (int j = enemies.size() - 1; j >= 0; j--) {
                    Enemy enemy = enemies.get(j);
                    if (isCollision(enemy, p)) {
                        enemies.remove(j);
                        if (player != null) player.applyGenericEffect("gold", 15);
                        if (explosionBitmap != null) collisionEffects.add(new CollisionEffect(enemy.getX(), enemy.getY(), explosionBitmap));
                    }
                }
                if (p.getLifespan() <= 0) p.setActive(false);
                else p.decrementLifespan();
            } else {
                for (int j = enemies.size() - 1; j >= 0; j--) {
                    Enemy enemy = enemies.get(j);
                    if (isCollision(enemy, p)) {
                        enemies.remove(j);
                        projectiles.remove(i);
                        if (player != null) player.applyGenericEffect("gold", 10);
                        if (explosionBitmap != null) collisionEffects.add(new CollisionEffect(enemy.getX(), enemy.getY(), explosionBitmap));
                        break;
                    }
                }
            }
        }

        for (int i = collisionEffects.size() - 1; i >= 0; i--) {
            CollisionEffect effect = collisionEffects.get(i);
            effect.update();
            if (!effect.isAlive()) collisionEffects.remove(i);
        }

        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            item.update(getWidth(), getHeight());
            if (isCollision(player, item)) {
                items.remove(i);
                if (soundEffectsEnabled && soundPool != null && itemPickupSoundId != 0) {
                    soundPool.play(itemPickupSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }
                if (player != null) { 
                    int effectChoice = random.nextInt(4);
                    switch (effectChoice) {
                        case 0: player.applyItemEffect(ItemType.ARMOR_UP); Log.d(TAG, "Item spawned: Armor Up"); break;
                        case 1: player.applyItemEffect(ItemType.SHIELD_PICKUP); Log.d(TAG, "Item spawned: Shield Pickup"); break;
                        case 2: player.applyGenericEffect("health", 20); Log.d(TAG, "Item spawned: Health +20"); break;
                        case 3: player.activateInvincibility(15); Log.d(TAG, "Item spawned: Invincibility 15s"); break;
                    }
                }
                if (explosionBitmap != null) collisionEffects.add(new CollisionEffect(item.getX(), item.getY(), explosionBitmap));
                break;
            }
            if (!item.isActive()) items.remove(i);
        }

        backgroundOffsetX -= BACKGROUND_SPEED;
        if (getWidth() > 0 && backgroundOffsetX <= -getWidth()) {
            backgroundOffsetX = 0;
        }
    }

    private void spawnEnemies() {
        if (scaledEnemyBitmap == null) return;
        int enemyCount = random.nextInt(3) + 1;
        int height = getHeight();
        if (height <= 0 || scaledEnemyBitmap.getHeight() <= 0 || height < scaledEnemyBitmap.getHeight()) return;
        for (int i = 0; i < enemyCount; i++) {
            float startY = random.nextFloat() * (height - scaledEnemyBitmap.getHeight());
            enemies.add(new Enemy(getWidth(), startY, scaledEnemyBitmap, 5));
        }
    }

    private Rect createHitbox(float x, float y, int width, int height, float scale) {
        if (width <= 0 || height <= 0) return new Rect();
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        int offsetX = (width - newWidth) / 2;
        int offsetY = (height - newHeight) / 2;
        return new Rect((int)(x + offsetX), (int)(y + offsetY), (int)(x + offsetX + newWidth), (int)(y + offsetY + newHeight));
    }

    private boolean isCollision(Player player, Enemy enemy) {
        if (player == null || enemy == null || player.getWidth() <= 0 || player.getHeight() <= 0 || enemy.getWidth() <= 0 || enemy.getHeight() <= 0) return false;
        Rect playerRect = createHitbox(player.getX(), player.getY(), (int)player.getWidth(), (int)player.getHeight(), 0.8f);
        Rect enemyRect  = createHitbox(enemy.getX(), enemy.getY(), (int)enemy.getWidth(), (int)enemy.getHeight(), 0.85f);
        return Rect.intersects(playerRect, enemyRect);
    }

    private boolean isCollision(Player player, Item item) {
        if (player == null || item == null || player.getWidth() <= 0 || player.getHeight() <= 0 || item.getWidth() <= 0 || item.getHeight() <= 0) return false;
        Rect playerRect = new Rect((int)player.getX(), (int)player.getY(), (int)(player.getX() + player.getWidth()), (int)(player.getY() + player.getHeight()));
        Rect itemRect = new Rect((int)item.getX(), (int)item.getY(), (int)(item.getX() + item.getWidth()), (int)(item.getY() + item.getHeight()));
        return Rect.intersects(playerRect, itemRect);
    }

    private boolean isCollision(Enemy enemy, Projectile projectile) {
        if (enemy == null || projectile == null || enemy.getWidth() <= 0 || enemy.getHeight() <= 0 || projectile.getWidth() <= 0 || projectile.getHeight() <= 0) return false;
        Rect enemyRect = createHitbox(enemy.getX(), enemy.getY(), (int)enemy.getWidth(), (int)enemy.getHeight(), 0.85f);
        Rect projRect = new Rect((int)projectile.getX(), (int)projectile.getY(), (int)(projectile.getX() + projectile.getWidth()), (int)(projectile.getY() + projectile.getHeight()));
        return Rect.intersects(enemyRect, projRect);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        if (scaledBackground != null) {
            canvas.drawBitmap(scaledBackground, backgroundOffsetX, 0, paint);
            if (getWidth() > 0) {
                 canvas.drawBitmap(scaledBackground, backgroundOffsetX + getWidth(), 0, paint);
            }
        } else if (originalBackground != null) {
            canvas.drawBitmap(originalBackground, 0, 0, paint);
        } else {
            canvas.drawColor(Color.BLACK);
        }

        if (isGameOver) {
            if (gameOverImageBitmap != null) {
                int x = (getWidth() - gameOverImageBitmap.getWidth()) / 2;
                int y = getHeight() / 4;
                canvas.drawBitmap(gameOverImageBitmap, x, y, paint);
            }
            if (replayButtonBitmap != null && replayButtonRect != null) {
                canvas.drawBitmap(replayButtonBitmap, replayButtonRect.left, replayButtonRect.top, paint);
            }
            if (menuButtonBitmap != null && menuButtonRect != null) { // Draw menu button
                canvas.drawBitmap(menuButtonBitmap, menuButtonRect.left, menuButtonRect.top, paint);
            }
            if (settingsButtonBitmap != null && settingsButtonRect != null) {
                canvas.drawBitmap(settingsButtonBitmap, settingsButtonRect.left, settingsButtonRect.top, paint);
            }
        } else {
            if (player != null) player.draw(canvas, paint);
            for (Enemy enemy : enemies) enemy.draw(canvas, paint);
            for (Projectile p : projectiles) p.draw(canvas, paint);
            for (Item item : items) {
                if (itemBitmap != null) item.draw(canvas, paint);
            }
            for (CollisionEffect effect : collisionEffects) effect.draw(canvas, paint);

            int topButtonSize = 100;
            int topCenterX = getWidth() > 0 ? getWidth() / 2 - topButtonSize / 2 : 0;
            if (musicEnabled && musicTurnOffBitmap != null) {
                canvas.drawBitmap(musicTurnOffBitmap, topCenterX, 10, paint);
            } else if (!musicEnabled && musicTurnOnBitmap != null) {
                canvas.drawBitmap(musicTurnOnBitmap, topCenterX, 10, paint);
            }
            if (soundOffBitmap != null && soundOnBitmap != null) {
                if (soundEffectsEnabled) {
                    canvas.drawBitmap(soundOffBitmap, topCenterX + topButtonSize + 10, 10, paint);
                } else {
                    canvas.drawBitmap(soundOnBitmap, topCenterX + topButtonSize + 10, 10, paint);
                }
            }

            paint.setColor(Color.GRAY);
            paint.setAlpha(128);
            if (getHeight() > 250 && getWidth() > 200) {
                canvas.drawRect(50, getHeight() - 250, 150, getHeight() - 150, paint); // Up
                canvas.drawRect(50, getHeight() - 150, 150, getHeight() - 50, paint); // Down
                canvas.drawRect(0, getHeight() - 200, 100, getHeight() - 100, paint); // Left
                canvas.drawRect(100, getHeight() - 200, 200, getHeight() - 100, paint); // Right

                int actionButtonY = getHeight() - 150;
                int actionButtonHeight = 100;
                int actionButtonWidth = 100;
                int spacing = 10;

                Rect slashButtonRect = new Rect(getWidth() - actionButtonWidth - spacing, actionButtonY, getWidth() - spacing, actionButtonY + actionButtonHeight);
                canvas.drawRect(slashButtonRect, paint);
                Rect bombButtonRect = new Rect(slashButtonRect.left - actionButtonWidth - spacing, actionButtonY, slashButtonRect.left - spacing, actionButtonY + actionButtonHeight);
                canvas.drawRect(bombButtonRect, paint);
                Rect fireballButtonRect = new Rect(bombButtonRect.left - actionButtonWidth - spacing, actionButtonY, bombButtonRect.left - spacing, actionButtonY + actionButtonHeight);
                canvas.drawRect(fireballButtonRect, paint);

                paint.setAlpha(255);
                paint.setColor(Color.WHITE);
                paint.setTextSize(30);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("S", slashButtonRect.centerX(), slashButtonRect.centerY() + 10, paint);
                canvas.drawText("B", bombButtonRect.centerX(), bombButtonRect.centerY() + 10, paint);
                canvas.drawText("F", fireballButtonRect.centerX(), fireballButtonRect.centerY() + 10, paint);
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTextSize(40);
            }

            if (player != null) {
                paint.setColor(Color.WHITE);
                canvas.drawText("Health: " + player.getHealth(), 10, 50, paint);
                canvas.drawText("Armor: " + player.getArmor(), 10, 90, paint);
                canvas.drawText("Gold: " + player.getGold(), 10, 130, paint);
                if (player.getCurrentWeapon() != null) {
                     canvas.drawText("Weapon: " + player.getCurrentWeapon().toString(), 10, 170, paint);
                }
                if (player.isInvincible()) {
                    paint.setColor(Color.YELLOW);
                    canvas.drawText("INVINCIBLE!", getWidth() / 2f - paint.measureText("INVINCIBLE!") / 2, 50, paint);
                    paint.setColor(Color.WHITE);
                }
            }
        }
    }

    public void resume() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void pause() {
        running = false;
        boolean retry = true;
        while (retry) {
            try {
                if (gameThread != null) gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Log.w(TAG, "Game thread join interrupted", e);
            }
        }
    }

    private void restartGame() {
        isGameOver = false;
        player = new Player(initialPlayerX, initialPlayerY, initialScaledPlayerBitmap, initialPlayerSpeed);
        enemies.clear();
        projectiles.clear();
        items.clear();
        collisionEffects.clear();
        backgroundOffsetX = 0;
        lastEnemySpawnTime = System.currentTimeMillis();

        if (musicEnabled && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
        Log.d(TAG, "Game Restarted");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getHeight() <= 0 || getWidth() <= 0) return super.onTouchEvent(event);

        float touchX = event.getX();
        float touchY = event.getY();
        int action = event.getAction();

        if (isGameOver) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (replayButtonRect != null && replayButtonRect.contains((int) touchX, (int) touchY)) {
                    restartGame();
                    performClick();
                    return true;
                }
                if (menuButtonRect != null && menuButtonRect.contains((int) touchX, (int) touchY)) { // Handle menu button touch
                    Log.d(TAG, "Menu button clicked on Game Over screen.");
                    // Future: Implement navigation to Menu screen
                    performClick();
                    return true;
                }
                if (settingsButtonRect != null && settingsButtonRect.contains((int) touchX, (int) touchY)) {
                    Log.d(TAG, "Settings button clicked on Game Over screen.");
                    performClick();
                    return true;
                }
            }
            return true;
        }

        if (player != null) {
             if (touchX >= 50 && touchX <= 150 && touchY >= getHeight() - 250 && touchY <= getHeight() - 150) player.setMovingUp(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
             else if (touchX >= 50 && touchX <= 150 && touchY >= getHeight() - 150 && touchY <= getHeight() - 50) player.setMovingDown(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
             else if (touchX >= 0 && touchX <= 100 && touchY >= getHeight() - 200 && touchY <= getHeight() - 100) player.setMovingLeft(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
             else if (touchX >= 100 && touchX <= 200 && touchY >= getHeight() - 200 && touchY <= getHeight() - 100) player.setMovingRight(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            int actionButtonY = getHeight() - 150;
            int actionButtonHeight = 100;
            int actionButtonWidth = 100;
            int spacing = 10;

            Rect slashButtonRect = new Rect(getWidth() - actionButtonWidth - spacing, actionButtonY, getWidth() - spacing, actionButtonY + actionButtonHeight);
            Rect bombButtonRect = new Rect(slashButtonRect.left - actionButtonWidth - spacing, actionButtonY, slashButtonRect.left - spacing, actionButtonY + actionButtonHeight);
            Rect fireballButtonRect = new Rect(bombButtonRect.left - actionButtonWidth - spacing, actionButtonY, bombButtonRect.left - spacing, actionButtonY + actionButtonHeight);

            if (player != null) {
                if (fireballButtonRect.contains((int)touchX, (int)touchY)) {
                    player.switchAttack(WeaponType.FIREBALL);
                    shoot();
                    performClick();
                } else if (bombButtonRect.contains((int)touchX, (int)touchY)) {
                    player.switchAttack(WeaponType.BOMB_DROP);
                    shoot();
                    performClick();
                } else if (slashButtonRect.contains((int)touchX, (int)touchY)) {
                    player.switchAttack(WeaponType.SWORD_SLASH);
                    shoot();
                    performClick();
                }
            }

            int topButtonSize = 100;
            int topCenterX = getWidth() > 0 ? getWidth() / 2 - topButtonSize / 2 : 0;
            if (touchX >= topCenterX && touchX <= topCenterX + topButtonSize && touchY >= 10 && touchY <= 10 + topButtonSize) {
                toggleMusic();
                performClick();
            }
            if (touchX >= topCenterX + topButtonSize + 10 && touchX <= topCenterX + topButtonSize * 2 + 10 && touchY >= 10 && touchY <= 10 + topButtonSize) {
                toggleSoundEffects();
                performClick();
            }

        } else if (action == MotionEvent.ACTION_UP && player != null) {
            player.setMovingUp(false);
            player.setMovingDown(false);
            player.setMovingLeft(false);
            player.setMovingRight(false);
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void triggerSuperBombEffect() {
        if (explosionBitmap == null) {
            Log.w(TAG, "Cannot trigger super bomb effect, explosionBitmap is null.");
            return;
        }
        if (enemies.isEmpty()) {
             Log.d(TAG, "Super Bomb triggered, but no enemies to clear.");
             return;
        }
        int enemiesCleared = enemies.size();
        for (Enemy enemy : new ArrayList<>(enemies)) {
            collisionEffects.add(new CollisionEffect(enemy.getX(), enemy.getY(), explosionBitmap));
        }
        enemies.clear();
        Log.d(TAG, "Super Bomb triggered, " + enemiesCleared + " enemies cleared.");
        if (player != null) {
            player.applyGenericEffect("gold", enemiesCleared * 5);
        }
    }

    private void shoot() {
        if (player == null) {
            Log.w(TAG, "Player is null, cannot shoot.");
            return;
        }

        WeaponType currentWeapon = player.getCurrentWeapon();
        if (currentWeapon == null) {
            Log.w(TAG, "Current weapon is null, cannot shoot.");
            return;
        }

        if (currentWeapon == WeaponType.FIREBALL && bulletBitmap == null) {
            Log.w(TAG, "bulletBitmap is null, cannot shoot fireball.");
            return;
        }
        if (currentWeapon == WeaponType.BOMB_DROP && playerBombItemBitmap == null) {
            Log.w(TAG, "playerBombItemBitmap is null, cannot shoot bomb.");
            return;
        }
        if (currentWeapon == WeaponType.SWORD_SLASH && playerMeleeBitmap == null) {
            Log.w(TAG, "playerMeleeBitmap is null, cannot perform slash.");
            return;
        }

        Projectile p = player.createAttack(bulletBitmap, playerMeleeBitmap, playerBombItemBitmap, 15f);
        if (p != null) {
            if (p.getType() == ProjectileType.PLAYER_SUPER_BOMB) {
                triggerSuperBombEffect();
                if (soundEffectsEnabled && soundPool != null && shootSoundId != 0) {
                    soundPool.play(shootSoundId, 1.2f, 1.2f, 1, 0, 1.1f);
                }
            } else {
                if (p.getType() == ProjectileType.PLAYER_SWORD_WAVE) {
                    p.setLifespan(MELEE_ATTACK_DURATION_FRAMES);
                }
                projectiles.add(p);
                if (soundEffectsEnabled && soundPool != null && shootSoundId != 0) {
                    soundPool.play(shootSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }
            }
        } else {
            Log.w(TAG, "player.createAttack() returned null for weapon: " + player.getCurrentWeapon());
        }
    }

    private void toggleMusic() {
        if (mediaPlayer != null) {
            if (!musicEnabled) {
                if (!mediaPlayer.isPlaying()) mediaPlayer.seekTo(0);
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
        Log.d(TAG, "Sound effects " + (soundEffectsEnabled ? "enabled" : "disabled"));
    }
}
