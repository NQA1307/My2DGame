package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class GameObject {
    protected float x, y; // Vị trí
    protected Bitmap bitmap; // Hình ảnh

    public GameObject(float x, float y, Bitmap bitmap) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, x, y, paint);
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return bitmap != null ? bitmap.getWidth() : 0; }
    public float getHeight() { return bitmap != null ? bitmap.getHeight() : 0; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
}