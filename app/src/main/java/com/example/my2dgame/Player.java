package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class Player {
    private float x, y;
    private final Bitmap bitmap;
    private float speed;
    private float dx, dy;
    private boolean movingUp, movingDown, movingLeft, movingRight;
    private int health = 100;
    private int armor = 50;
    private int shield = 0;
    private int gold = 0;
    private WeaponType currentWeapon = WeaponType.FIREBALL;

    private boolean isInvincible = false;
    private long invincibilityEndTime = 0;

    public Player(float x, float y, Bitmap bitmap, float speed) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
        this.speed = speed;
        this.dx = 0;
        this.dy = 0;
    }

    public void update(int screenWidth, int screenHeight) {
        dx = 0;
        dy = 0;
        if (movingUp) dy = -speed;
        if (movingDown) dy = speed;
        if (movingLeft) dx = -speed;
        if (movingRight) dx = speed;

        x += dx;
        y += dy;
        if (x < 0) x = 0;
        if (bitmap != null && x > screenWidth - bitmap.getWidth()) x = screenWidth - bitmap.getWidth();
        if (y < 0) y = 0;
        if (bitmap != null && y > screenHeight - bitmap.getHeight()) y = screenHeight - bitmap.getHeight();

        if (shield > 0) shield--;

        if (isInvincible && System.currentTimeMillis() > invincibilityEndTime) {
            isInvincible = false;
            Log.d("Player", "Invincibility ended");
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (bitmap != null) canvas.drawBitmap(bitmap, x, y, paint);
    }

    public void setMovingUp(boolean movingUp) { this.movingUp = movingUp; }
    public void setMovingDown(boolean movingDown) { this.movingDown = movingDown; }
    public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }

    public void applyItemEffect(ItemType itemType) {
        switch (itemType) {
            case HEART:
                applyGenericEffect("health", 25); // Use generic effect for consistency
                Log.d("Player", "Heart item picked up. Health +25");
                break;
            case SHIELD:
                this.shield = 300; // 5 seconds at 60 FPS
                Log.d("Player", "Shield item picked up. Shield activated for 300 frames.");
                break;
            case INVINCIBILITY:
                activateInvincibility(5); // 5 seconds
                Log.d("Player", "Invincibility item picked up.");
                break;
        }
    }

    public void applyGenericEffect(String effectType, int value) {
        switch (effectType) {
            case "health":
                health = Math.max(0, Math.min(100, health + value));
                Log.d("Player", "Health changed by " + value + ". New health: " + health);
                break;
            case "armor":
                armor = Math.max(0, Math.min(100, armor + value));
                Log.d("Player", "Armor changed by " + value + ". New armor: " + armor);
                break;
            case "shield":
                shield = Math.max(0, shield + value);
                Log.d("Player", "Shield changed by " + value + ". New shield duration: " + shield);
                break;
            case "gold":
                gold += value;
                Log.d("Player", "Gold changed by " + value + ". New gold: " + gold);
                break;
            case "speed":
                speed += value;
                Log.d("Player", "Speed changed by " + value + ". New speed: " + speed);
                break;
        }
    }

    public Projectile createAttack(Bitmap projectileBitmap, Bitmap meleeBitmap, Bitmap bombBitmap, float projectileSpeed) {
        if (currentWeapon == null) return null;

        float attackX, attackY;

        switch (currentWeapon) {
            case FIREBALL:
                if (projectileBitmap == null) {
                    Log.w("Player", "Cannot create FIREBALL, projectileBitmap is null");
                    return null;
                }
                attackX = x + getWidth();
                attackY = y + (getHeight() / 2.0f) - (projectileBitmap.getHeight() / 2.0f);
                return new Projectile(attackX, attackY, projectileBitmap, projectileSpeed, ProjectileType.PLAYER_FIREBALL);
            case BOMB_DROP:
                if (bombBitmap == null) {
                     Log.w("Player", "Cannot create BOMB_DROP, bombBitmap is null");
                    return null;
                }
                attackX = x + getWidth()/2.0f - bombBitmap.getWidth() / 2.0f;
                attackY = y + getHeight();
                return new Bomb(attackX, attackY, bombBitmap, 0, ProjectileType.PLAYER_SUPER_BOMB);
            case SWORD_SLASH:
                if (meleeBitmap == null) {
                    Log.w("Player", "Cannot create SWORD_SLASH, meleeBitmap is null");
                    return null;
                }
                attackX = x + getWidth() - meleeBitmap.getWidth()/2;
                attackY = y + (getHeight() / 2.0f) - (meleeBitmap.getHeight() / 2.0f);
                Projectile melee = new Projectile(attackX, attackY, meleeBitmap, 0, ProjectileType.PLAYER_SWORD_WAVE);
                return melee;
            default:
                return null;
        }
    }

    public void switchAttack(WeaponType type) {
        this.currentWeapon = type;
        Log.d("Player", "Player switched to weapon: " + type);
    }

    public void activateInvincibility(int durationSeconds) {
        this.isInvincible = true;
        this.invincibilityEndTime = System.currentTimeMillis() + durationSeconds * 1000L;
        Log.d("Player", "Invincibility activated for " + durationSeconds + " seconds.");
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return bitmap != null ? bitmap.getWidth() : 0; }
    public float getHeight() { return bitmap != null ? bitmap.getHeight() : 0; }
    public int getHealth() { return health; }
    public int getArmor() { return armor; }
    public int getShield() { return shield; }
    public int getGold() { return gold; }
    public float getSpeed() { return speed; }
    public boolean isShieldActive() { return shield > 0; }
    public WeaponType getCurrentWeapon() { return currentWeapon; }
    public boolean isInvincible() { return isInvincible; }
}
