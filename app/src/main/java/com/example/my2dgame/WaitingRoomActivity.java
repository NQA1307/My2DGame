package com.example.my2dgame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WaitingRoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        Button startButton = findViewById(R.id.button_start);
        Button exitButton = findViewById(R.id.button_exit);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển sang MainActivity (màn hình game chính)
                Intent intent = new Intent(WaitingRoomActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Đóng màn hình chờ sau khi chuyển
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Thoát ứng dụng
                finishAffinity();
            }
        });
    }
}
