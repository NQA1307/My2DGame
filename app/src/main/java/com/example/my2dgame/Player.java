package com.example.my2dgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Player extends GameObject {
    private float speed;
    private boolean movingUp, movingDown, movingLeft, movingRight;

    public Player(float x, float y, Bitmap bitmap, float speed) {
        super(x, y, bitmap);
        this.speed = speed;
        this.movingUp = this.movingDown = this.movingLeft = this.movingRight = false;
    }

    public void update(float screenWidth, float screenHeight) {
        if (movingUp && y > 0) y -= speed;
        if (movingDown && y < screenHeight - getHeight()) y += speed;
        if (movingLeft && x > 0) x -= speed;
        if (movingRight && x < screenWidth - getWidth()) x += speed;
    }

    public void setMovingUp(boolean movingUp) { this.movingUp = movingUp; }
    public void setMovingDown(boolean movingDown) { this.movingDown = movingDown; }
    public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
}