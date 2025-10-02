package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class CollisionEffect {
    private float x, y;
    private Bitmap bitmap;
    private int frames = 30; // Hiệu ứng kéo dài 0.5 giây (60 FPS)

    public CollisionEffect(float x, float y, Bitmap bitmap) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
    }

    public void update() {
        if (frames > 0) frames--;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (frames > 0) canvas.drawBitmap(bitmap, x - bitmap.getWidth() / 2, y - bitmap.getHeight() / 2, paint);
    }

    public boolean isAlive() { return frames > 0; }
}