package xyz.doikki.videoplayer.controller;

import android.graphics.Bitmap;

import java.util.List;

import xyz.doikki.videoplayer.model.Track;

public interface MediaPlayerControl {

    void start();

    void pause();

    long getDuration();

    long getCurrentPosition();

    void seekTo(long pos);

    boolean isPlaying();

    int getBufferedPercentage();

    void startFullScreen();

    void stopFullScreen();

    boolean isFullScreen();

    void setMute(boolean isMute);

    boolean isMute();

    void setScreenScaleType(int screenScaleType);

    void setSpeed(float speed);

    float getSpeed();

    long getTcpSpeed();

    void replay(boolean resetPosition);

    void setMirrorRotation(boolean enable);

    Bitmap doScreenShot();

    int[] getVideoSize();

    void setRotation(float rotation);

    void startTinyScreen();

    void stopTinyScreen();

    boolean isTinyScreen();

    /**
     * 获取字幕列表
     */
    List<Track> getSubtitles();

    void selectSubtitle(Track track);

    int getSelectedSubtitleIndex();

    List<Track> getTracks();

    void selectTrack(Track track);

    int getSelectedTrackIndex();

    boolean isVideo();
}