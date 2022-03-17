package com.zchd.sdk.floatviewdemo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_show).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FloatView.getInstance().show();
            }
        });

        findViewById(R.id.btn_hide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FloatView.getInstance().hide();
            }
        });

        findViewById(R.id.btn_show_badge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FloatView.getInstance().showBadge();
            }
        });

        findViewById(R.id.btn_hide_badge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FloatView.getInstance().hideBadge();
            }
        });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FloatView.attach(this, FloatView.AttachEdgeMode.ALL);
        FloatView.getInstance().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("0", "----------------->>");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}