package com.example.my2dgame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private Thread gameThread;
    private SurfaceHolder holder;
    private volatile boolean running = false;
    private Paint paint;
    private Bitmap originalBackground; // Bitmap gốc cho nền
    private Bitmap scaledBackground; // Bitmap đã scale cho nền
    private Bitmap playerBitmap; // Bitmap gốc cho nhân vật
    private Bitmap scaledPlayerBitmap; // Bitmap đã scale cho nhân vật
    private float playerX = 100; // Vị trí X nhân vật
    private float playerY = 100; // Vị trí Y nhân vật
    private float playerSpeed = 5; // Tốc độ di chuyển nhân vật
    private float backgroundSpeed = 2; // Tốc độ scroll nền
    private float backgroundOffsetX = 0; // Offset X cho scroll nền
    private boolean movingUp = false, movingDown = false, movingLeft = false, movingRight = false; // Trạng thái di chuyển

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(40);

        // Tải ảnh nền
        originalBackground = BitmapFactory.decodeResource(getResources(), R.drawable.background);

        // Tải ảnh nhân vật
        playerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (originalBackground != null && width > 0 && height > 0) {
            scaledBackground = Bitmap.createScaledBitmap(originalBackground, width * 2, height, true);
        }
        // Scale nhân vật khi kích thước thay đổi
        if (playerBitmap != null && width > 0 && height > 0) {
            int newWidth = playerBitmap.getWidth() / 10;
            int newHeight = playerBitmap.getHeight() / 10;
            scaledPlayerBitmap = Bitmap.createScaledBitmap(playerBitmap, newWidth, newHeight, true);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @Override
    public void run() {
        while (running) {
            if (!holder.getSurface().isValid()) continue;

            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                update();
                draw(canvas);
                holder.unlockCanvasAndPost(canvas);
            }

            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        // Di chuyển nhân vật dựa trên trạng thái nút
        if (movingUp && playerY > 0) playerY -= playerSpeed;
        if (movingDown && playerY < getHeight() - (scaledPlayerBitmap != null ? scaledPlayerBitmap.getHeight() : playerBitmap.getHeight())) playerY += playerSpeed;
        if (movingLeft && playerX > 0) playerX -= playerSpeed;
        if (movingRight && playerX < getWidth() - (scaledPlayerBitmap != null ? scaledPlayerBitmap.getWidth() : playerBitmap.getWidth())) playerX += playerSpeed;

        // Scroll background từ trái sang phải
        backgroundOffsetX -= backgroundSpeed;
        if (backgroundOffsetX <= -getWidth()) {
            backgroundOffsetX = 0;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Vẽ nền scroll
        if (scaledBackground != null) {
            canvas.drawBitmap(scaledBackground, backgroundOffsetX, 0, paint);
            canvas.drawBitmap(scaledBackground, backgroundOffsetX + getWidth(), 0, paint);
        } else if (originalBackground != null) {
            canvas.drawBitmap(originalBackground, 0, 0, paint);
        } else {
            canvas.drawColor(Color.BLACK);
        }

        // Vẽ nhân vật (ảnh đã scale)
        if (scaledPlayerBitmap != null) {
            canvas.drawBitmap(scaledPlayerBitmap, playerX, playerY, paint);
        } else if (playerBitmap != null) {
            canvas.drawBitmap(playerBitmap, playerX, playerY, paint); // Fallback nếu chưa scale
        }

        // Vẽ nút điều khiển ảo
        paint.setColor(Color.GRAY);
        canvas.drawRect(50, getHeight() - 250, 150, getHeight() - 150, paint); // Lên
        canvas.drawRect(50, getHeight() - 150, 150, getHeight() - 50, paint); // Xuống
        canvas.drawRect(0, getHeight() - 200, 100, getHeight() - 100, paint); // Trái
        canvas.drawRect(100, getHeight() - 200, 200, getHeight() - 100, paint); // Phải

        paint.setColor(Color.WHITE);
        canvas.drawText("↑", 90, getHeight() - 180, paint);
        canvas.drawText("↓", 90, getHeight() - 80, paint);
        canvas.drawText("←", 30, getHeight() - 130, paint);
        canvas.drawText("→", 130, getHeight() - 130, paint);
    }

    public void resume() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void pause() {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                movingUp = (x >= 50 && x <= 150 && y >= getHeight() - 250 && y <= getHeight() - 150);
                movingDown = (x >= 50 && x <= 150 && y >= getHeight() - 150 && y <= getHeight() - 50);
                movingLeft = (x >= 0 && x <= 100 && y >= getHeight() - 200 && y <= getHeight() - 100);
                movingRight = (x >= 100 && x <= 200 && y >= getHeight() - 200 && y <= getHeight() - 100);
                break;

            case MotionEvent.ACTION_UP:
                movingUp = movingDown = movingLeft = movingRight = false;
                break;
        }
        return true;
    }
}