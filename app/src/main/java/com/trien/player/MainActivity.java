package com.trien.player;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.trien.player.radio.PlaybackStatus;
import com.trien.player.radio.RadioManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MainActivity extends AppCompatActivity {

    ImageButton trigger;

    RadioManager radioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioManager = RadioManager.with(this);
        trigger = findViewById(R.id.radioPlayButton);
    }

    @Override
    public void onStart() {

        super.onStart();

        EventBus.getDefault().register(this);

    }

    @Override
    public void onStop() {

        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        radioManager.unbind();
    }

    @Override
    protected void onResume() {
        super.onResume();

        radioManager.bind();
    }

    @Override
    public void onBackPressed() {

        finish();

    }

    @Subscribe
    public void onEvent(String status){

        switch (status){

            case PlaybackStatus.LOADING:

                // loading

                break;

            case PlaybackStatus.ERROR:

                Toast.makeText(this, R.string.no_stream, Toast.LENGTH_SHORT).show();

                break;

        }

        trigger.setImageResource(status.equals(PlaybackStatus.PLAYING)
                ? R.drawable.pause_button
                : R.drawable.play_button);

    }

    public void playButtonPressed(View view){
        String streamURL = getString(R.string.radio_url);
        radioManager.playOrPause(streamURL);
    }

    public void NextActivityPressed(View view) {

        Intent intent = new Intent(this, TempActivity.class);
        startActivity(intent);

    }
}
