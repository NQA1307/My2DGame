package com.example.my2dgame;

import android.graphics.Bitmap;

public class Item extends GameObject {
    private final ItemType type;
    private boolean active = true;

    public Item(float x, float y, Bitmap bitmap, ItemType type) {
        super(x, y, bitmap);
        this.type = type;
    }

    public void update(int screenWidth, int screenHeight) {
        // Items might move across the screen (e.g., from right to left)
        x -= 3; // Example speed, you can make this variable
        if (x < -getWidth()) {
            active = false; // Deactivate when off-screen
        }
    }

    public ItemType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }
}
