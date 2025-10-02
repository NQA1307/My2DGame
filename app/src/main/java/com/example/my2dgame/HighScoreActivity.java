package com.example.my2dgame;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class HighScoreActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "My2DGamePrefs";
    private static final String HIGH_SCORES_KEY = "highScores_v2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_score);

        ListView highScoreListView = findViewById(R.id.high_score_list);

        List<String> formattedScores = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String scoresString = prefs.getString(HIGH_SCORES_KEY, "");

        if (!scoresString.isEmpty()) {
            try {
                String[] scores = scoresString.split(";");
                for (String score : scores) {
                    if (!score.isEmpty()) {
                        String[] parts = score.split(":");
                        if (parts.length == 2) {
                            formattedScores.add(parts[0] + " - " + parts[1]);
                        }
                    }
                }
            } catch (Exception e) {
                // Handle potential parsing errors, maybe clear or show an error
                formattedScores.add("Error loading scores");
            }
        }

        if (formattedScores.isEmpty()) {
            formattedScores.add("No high scores yet!");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, formattedScores);
        highScoreListView.setAdapter(adapter);
    }
}
