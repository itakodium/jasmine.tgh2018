package com.example.takumi.uimock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Thread.sleep(1000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setTheme(R.style.AppTheme_NoActionBar);
        setContentView(R.layout.activity_main);

        Intent start = new Intent(this, MainActivity.class);
        startActivity(start);
        finish();
    }
}
