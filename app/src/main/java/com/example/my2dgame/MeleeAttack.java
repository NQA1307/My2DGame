package com.example.my2dgame;

import android.graphics.Bitmap;

public class MeleeAttack extends Projectile {
    private float initialX; // Để lưu tọa độ x ban đầu cho việc giới hạn tầm đánh

    public MeleeAttack(float x, float y, Bitmap bitmap, float speed) {
        // Giả sử ProjectileType.PLAYER_SWORD là một enum hợp lệ trong ProjectileType.java
        super(x, y, bitmap, speed, ProjectileType.PLAYER_SWORD_WAVE); // Sử dụng PLAYER_SWORD_WAVE cho nhất quán
        this.initialX = x; // Lưu lại tọa độ x ban đầu
    }

    @Override
    public void update(int screenWidth, int screenHeight) {
        super.update(screenWidth, screenHeight);
        // Giới hạn tầm đánh: nếu di chuyển quá 100 pixel theo chiều dương x so với vị trí ban đầu
        // Hoặc nếu đi ra ngoài màn hình bên trái (trường hợp speed âm hoặc đối tượng quay ngược lại)
        if (type == ProjectileType.PLAYER_SWORD_WAVE) { // Chỉ áp dụng logic này cho kiếm
            if (getX() > initialX + 100 || getX() < initialX - 100) { // Giới hạn tầm hoạt động 100px về cả hai phía từ initialX
                active = false;
            }
            // Đảm bảo MeleeAttack cũng bị vô hiệu hóa nếu ra khỏi màn hình, giống Projectile
            if (x > screenWidth || x < -getWidth()) {
                active = false;
            }
        } 
        // Đối với các loại MeleeAttack khác (nếu có), bạn có thể thêm logic tương tự hoặc khác biệt
    }
}
