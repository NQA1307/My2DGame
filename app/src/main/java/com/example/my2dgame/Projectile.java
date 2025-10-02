package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Projectile extends GameObject { 
    protected float speed;
    protected boolean active = true;
    protected ProjectileType type; 
    private int lifespan = -1; // -1 means no lifespan limit by default

    public Projectile(float x, float y, Bitmap bitmap, float speed, ProjectileType type) {
        super(x, y, bitmap); 
        this.speed = speed;
        this.type = type;
    }

    // Removed @Override as GameObject.update() might have a different signature or not exist
    public void update(int screenWidth, int screenHeight) {
        // For projectiles with a lifespan, GameView will handle decrementing and deactivation.
        // This update mainly handles movement for projectiles that move.
        if (type == ProjectileType.PLAYER_FIREBALL) {
            x += speed;
            if (x > screenWidth) {
                active = false;
            }
        } else if (type == ProjectileType.ENEMY_BULLET) {
            x -= speed; 
            if (x < -getWidth()) { 
                active = false;
            }
        }
        // PLAYER_SWORD_WAVE has speed = 0, its lifespan and deactivation are handled in GameView
        // PLAYER_BOMB (super bomb) is an instant effect, also handled in GameView
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (active && bitmap != null) {
            super.draw(canvas, paint);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ProjectileType getType() {
        return type;
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    public int getLifespan() {
        return lifespan;
    }

    public void decrementLifespan() {
        if (this.lifespan > 0) { // Only decrement if it's a positive value
            this.lifespan--;
        }
    }
}
