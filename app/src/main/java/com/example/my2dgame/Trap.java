package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Trap {
    private float x, y;
    private Bitmap bitmap;
    private int activeFrames = 0; // Thời gian hoạt động

    public Trap(float x, float y, Bitmap bitmap) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
    }

    public void update(int screenWidth, int screenHeight) {
        if (activeFrames > 0) activeFrames--;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (isActive()) canvas.drawBitmap(bitmap, x, y, paint);
    }

    public void deactivate(int frames) {
        activeFrames = frames;
    }

    public boolean isActive() {
        return activeFrames > 0;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return bitmap.getWidth(); }
    public float getHeight() { return bitmap.getHeight(); }
}