package com.fxyan.demo.rulerview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RulerView rulerView = findViewById(R.id.ruler_view);
        final TextView valueTv = findViewById(R.id.tv_value);
        rulerView.setListener(new RulerView.OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                valueTv.setText(String.valueOf(value));
            }
        });
        rulerView.setBorderValue(100_000, 0, 10_000);
    }
}
