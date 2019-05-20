package com.trien.player;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.trien.player.radio.RadioManager;

public class TempActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp);

        RadioManager radioManager;
        radioManager = RadioManager.with(this);
        String streamURL = getString(R.string.radio_url);
        radioManager.playOrPause(streamURL);
    }
}
