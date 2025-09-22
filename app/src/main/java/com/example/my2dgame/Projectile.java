package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Projectile {
    private float x, y; // Vị trí
    private float speed; // Tốc độ di chuyển
    private Bitmap bitmap; // Hình ảnh viên đạn
    private boolean isActive; // Trạng thái viên đạn (còn tồn tại không)

    public Projectile(float x, float y, Bitmap bitmap, float speed) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
        this.speed = speed;
        this.isActive = true;
    }

    public void update(float screenWidth, float screenHeight) {
        // Di chuyển sang phải
        x += speed;
        // Kiểm tra nếu ra khỏi màn hình
        if (x > screenWidth) {
            isActive = false;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (bitmap != null && isActive) {
            canvas.drawBitmap(bitmap, x, y, paint);
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return bitmap != null ? bitmap.getWidth() : 0; }
    public float getHeight() { return bitmap != null ? bitmap.getHeight() : 0; }
    public boolean isActive() { return isActive; }
}