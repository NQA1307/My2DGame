package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.Random;

public class Enemy extends GameObject {
    private float speed;
    private Random random = new Random();

    public Enemy(float x, float y, Bitmap bitmap, float speed) {
        super(x, y, bitmap);
        this.speed = speed;
    }

    public void update(float screenWidth, float screenHeight) {
        x -= speed; // Di chuyển sang trái
        if (x + getWidth() < 0) { // Khi ra biên trái
            x = screenWidth; // Reset về biên phải
            y = random.nextFloat() * (screenHeight - getHeight()); // Vị trí Y ngẫu nhiên
        }
    }
}