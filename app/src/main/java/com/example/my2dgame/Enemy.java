package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Enemy {
    private float x, y;
    private Bitmap bitmap;
    private float speed;

    public Enemy(float x, float y, Bitmap bitmap, float speed) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
        this.speed = speed;
    }

    public void update(float playerX, float playerY, int screenWidth, int screenHeight) {
        x -= speed; // Di chuyển sang trái
        if (x < -bitmap.getWidth()) speed = 0; // Dừng khi ra khỏi màn hình
    }

    public void draw(Canvas canvas, Paint paint) {
        canvas.drawBitmap(bitmap, x, y, paint);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return bitmap.getWidth(); }
    public float getHeight() { return bitmap.getHeight(); }
    public void setSpeed(float speed) { this.speed = speed; }
}