package de.thu.musicplayer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Application context
    private static Context appContext;

    private SeekBar progressBar;
    private SeekBar volumeBar;

    private Cursor cursor;
    private Handler handler;
    private Runnable runnable;

    private Song currentSong;

    private int oldIndex;
    private boolean userPressedPlay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();

        // Register the broadcast manager
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("Media player is prepared"));

        progressBar = findViewById(R.id.progressBar);
        volumeBar = findViewById(R.id.volumeBar);

        setOnProgressListener();
        setOnVolumeChangedListener();

        cursor = getSongsList();
        handler = new Handler();

        int index = randomSong();
        cursor.moveToPosition(index);
        currentSong = newSong(index);
        oldIndex = currentSong.getIndex();

        setTextView();
    }

    /**
     * Listener for the play button or pause button
     *
     * @param view view which has been pressed
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    public void onPlayPauseClicked(View view) {

        ImageView playPause = (ImageView) view;

        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_item));

        if (playPause.getDrawable().getConstantState() ==
                getResources().getDrawable(R.drawable.play).getConstantState()) {
            userPressedPlay = true;

            //  play the current song
            currentSong.play();

            updateUI();

            playPause.setImageResource(R.drawable.pause);
        } else {
            userPressedPlay = false;

            // pause the current song
            currentSong.pause();

            playPause.setImageResource(R.drawable.play);
        }
    }

    /**
     * Listener for previous button
     *
     * @param view view which has been pressed
     */
    public void onPreviousClicked(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_item));

        // if the song has been playing for more than 4 seconds, then replay the current song
        if (currentSong.getPlayedTime() / 1000 % 60 > 4) {
            currentSong.replay();
            return;
        }

        // play the previous song
        cursor.moveToPosition(oldIndex);

        handler.removeCallbacks(runnable);

        currentSong.stop();
        currentSong = newSong(oldIndex);

        currentSong.play();

        updateUI();
        setTextView();
    }

    /**
     *
     *
     * @param view
     */
    public void onNextClicked(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_item));

        // set this song as a new previous song
        oldIndex = currentSong.getIndex();

        //  choose a song randomly
        int index = randomSong();
        cursor.moveToPosition(index);
        handler.removeCallbacks(runnable);

        currentSong.stop();

        currentSong = newSong(index);

        setTextView();

        currentSong.play();

        updateUI();
    }

    /**
     * Gets the list of all available songs from the memory of the phone
     *
     * @return cursor object
     */
    private Cursor getSongsList() {
        // Obtain content resolver
        ContentResolver contentResolver = getContentResolver();


        cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Audio.Media.IS_MUSIC, null,
                MediaStore.Audio.Media.DATE_ADDED);


        return cursor;
    }

    /**
     * Randomly chooses song from index 0 to number of available songs in the phone
     *
     * @return randomly chosen index
     */
    private int randomSong() {
        Random random = new Random();
        int index;
        do {
            index = random.nextInt(cursor.getCount());
        } while (index == cursor.getPosition());

        return index;
    }

    /**
     * Creates a new song object based on the randomly chosen index
     *
     * @param index index of the randomly chosen song
     * @return new song object
     */
    private Song newSong(int index) {
        String title = cursor.getString(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.AudioColumns.TITLE
        ));

        String artist = cursor.getString(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.AudioColumns.ARTIST
        ));

        String path = cursor.getString(cursor.getColumnIndexOrThrow(
                MediaStore.MediaColumns.DATA
        ));

        return new Song(title, artist, path, index);
    }

    /**
     * Sets the text view, which corresponds to  the details of the song
     */
    private void setTextView() {
        TextView songDetails = findViewById(R.id.songDetails);
        songDetails.setText(currentSong.toString());
        songDetails.setSelected(true);
    }

    /**
     * Sets the time labels
     */
    private void setTimeLabel() {
        int currentMin = currentSong.getPlayedTime() / 1000 / 60;
        int currentSec = currentSong.getPlayedTime() / 1000 % 60;

        int remainingMin = currentSong.getLeftTime() / 1000 / 60;
        int remainingSec = currentSong.getLeftTime() / 1000 % 60;

        TextView playedTime = findViewById(R.id.played);
        TextView remainingTime = findViewById(R.id.remaining);
        String playedText = currentMin + ":" + (currentSec / 10 == 0 ? "0" : "") + currentSec;
        String remainingText = "-" + remainingMin + ":" + (remainingSec / 10 == 0 ? "0" : "") +
                remainingSec;

        playedTime.setText(playedText);
        remainingTime.setText(remainingText);
    }

    /**
     * Updates the UI every second while the song is being played
     */
    private void updateUI() {
        runOnUiThread(runnable = new Runnable() {
            @Override
            public void run() {
                currentSong.update();

                setTimeLabel();

                progressBar.setProgress(currentSong.getMediaPlayer().getCurrentPosition());

                handler.postDelayed(this, 1000);
            }
        });
    }

    /**
     * Sets the listener for the volume seek bar
     */
    private void setOnVolumeChangedListener() {
        volumeBar.setProgress(volumeBar.getMax());
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float log1 = (float) (Math.log(volumeBar.getMax() - progress) /
                            Math.log(volumeBar.getMax()));
                    currentSong.getMediaPlayer().setVolume(1 - log1, 1 - log1);

                    ImageView volumeDownOff = findViewById(R.id.volumeDownOff);
                    if (progress == 0) {
                        volumeDownOff.setImageResource(R.drawable.volume_off);
                    } else {
                        if (volumeDownOff.getDrawable().getConstantState() ==
                                getResources().getDrawable(R.drawable.volume_off).getConstantState()) {
                            volumeDownOff.setImageResource(R.drawable.volume_down);
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /**
     * Sets the listener for the song progress seek bar
     */
    private void setOnProgressListener() {
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentSong.getMediaPlayer().seekTo(progress);
                    setTimeLabel();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                currentSong.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                currentSong.play();
            }
        });
    }

    /**
     * Receives the message from the song class once it is ready to play
     */
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentSong.update();
            setTimeLabel();
            progressBar.setMax(currentSong.getMediaPlayer().getDuration());
            float log1 = (float) (Math.log(volumeBar.getMax() - volumeBar.getProgress()) /
                    Math.log(volumeBar.getMax()));
            currentSong.getMediaPlayer().setVolume(1 - log1,
                    1 - log1);

            if (userPressedPlay) {
                currentSong.play();
            }
        }
    };

    public static Context getAppContext() {
        return appContext;
    }
}