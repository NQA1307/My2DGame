package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Enemy {
    private float x, y;
    private final Bitmap bitmap; // Made final
    private float speed;

    public Enemy(float x, float y, Bitmap bitmap, float speed) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
        this.speed = speed;
    }

    public void update(float playerX, float playerY, int screenWidth, int screenHeight) {
        // Calculate the direction vector from enemy to player
        float dx = playerX - x;
        float dy = playerY - y;

        // Normalize the direction vector
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance > 0) {
            dx = dx / distance;
            dy = dy / distance;
        }

        // Move the enemy towards the player
        x += dx * speed;
        y += dy * speed;

        // Optional: Keep the enemy from going off the left side of the screen
        // (though with player tracking, this is less likely to be the primary exit method)
        if (x < -getWidth()) {
            // You might want to handle this differently, e.g., mark as inactive
            // For now, let's just stop it to prevent it going too far off-screen.
            speed = 0; 
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, x, y, paint);
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return bitmap.getWidth(); }
    public float getHeight() { return bitmap.getHeight(); }
    public void setSpeed(float speed) { this.speed = speed; } // This can still be useful for effects (e.g., traps)
}
