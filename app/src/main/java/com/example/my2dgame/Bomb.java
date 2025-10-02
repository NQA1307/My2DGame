package com.example.my2dgame;

import android.graphics.Bitmap;
// Canvas and Paint are not strictly needed here if draw is fully handled by Projectile/GameObject
// import android.graphics.Canvas;
// import android.graphics.Paint;

public class Bomb extends Projectile { // Bomb vẫn kế thừa từ Projectile là hợp lý

    // Có thể thêm các thuộc tính riêng cho Bomb, ví dụ: thời gian nổ (fuseTime)
    // private int fuseTime;
    // private boolean exploded;

    public Bomb(float x, float y, Bitmap bitmap, float speed, ProjectileType type) {
        // Gọi constructor của lớp cha (Projectile) với đầy đủ 5 tham số
        super(x, y, bitmap, speed, type);
        // Khởi tạo các thuộc tính riêng của Bomb nếu có
        // this.fuseTime = 180; // Ví dụ: 3 giây ở 60 FPS
        // this.exploded = false;
    }

    @Override
    public void update(int screenWidth, int screenHeight) {
        // Gọi update của Projectile để xử lý active state (nếu ra khỏi màn hình)
        // và di chuyển cơ bản nếu speed != 0 (mặc dù bomb thường có speed = 0 khi thả)
        super.update(screenWidth, screenHeight);

        // Logic riêng cho Bomb:
        // Ví dụ: đếm ngược thời gian nổ
        // if (!exploded && active) {
        //     if (fuseTime > 0) {
        //         fuseTime--;
        //     } else {
        //         explode();
        //     }
        // }
    }

    // public void explode() {
    //     this.exploded = true;
    //     this.active = false; // Bomb không còn active sau khi nổ
    //     // Ở đây có thể tạo ra một đối tượng ExplosionEffect hoặc thay đổi bitmap
    //     // Logic va chạm và gây sát thương từ vụ nổ sẽ được xử lý trong GameView/GameManager
    //     System.out.println("Bomb exploded at (" + x + ", " + y + ")");
    // }

    // public boolean hasExploded() {
    //     return exploded;
    // }

    // Phương thức draw() được kế thừa từ Projectile (và GameObject).
    // Nếu Bomb có trạng thái hình ảnh khác (ví dụ: đang nổ), có thể override draw().
}
