package de.thu.musicplayer;
import android.content.Intent;
import android.media.MediaPlayer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.io.IOException;


public class Song {

    private MediaPlayer player;

    private String title;
    private String artist;

    private int index;
    private int playedTime;
    private int leftTime;

    public Song(String title, String artist, String dataSource, int index) {
        this.title = title;
        this.artist = artist;
        this.index = index;

        player = new MediaPlayer();

        try {
            player.setDataSource(dataSource);
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.prepareAsync();
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                player.setLooping(true);
                sendMessage();
            }
        });
    }

    /**
     * Sends message to the main activity indicating that the media is ready to play.
     */
    private static void sendMessage() {
        Intent intent = new Intent("Media player is prepared");
        LocalBroadcastManager.getInstance(MainActivity.getAppContext()).sendBroadcast(intent);
    }

    /**
     * Plays the song
     */
    public void play() {
        player.start();
    }

    /**
     * Pauses the song
     */
    public void pause() {
        player.pause();
    }

    /**
     * Stops the song
     */
    public void stop() {
        player.stop();
        player.release();
    }

    /**
     * Replays the song
     */
    public void replay() {
        player.seekTo(0);
    }

    /**
     * Updates the played and remaining time of the song
     */
    public void update() {
        playedTime = player.getCurrentPosition();
        leftTime = player.getDuration() - playedTime;
    }

    public int getPlayedTime() {
        return playedTime;
    }

    public int getLeftTime() {
        return leftTime;
    }

    public int getIndex() {
        return index;
    }

    public MediaPlayer getMediaPlayer() {
        return player;
    }

    /**
     * Represents a song as sequence of strings
     * @return string representation of a song
     */
    @Override
    public String toString() {
        return artist + " - " + title;
    }
}
