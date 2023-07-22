package com.jason.exo.extension;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.analytics.DefaultAnalyticsCollector;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.CueGroup;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.video.VideoSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.text.StringsKt;
import xyz.doikki.videoplayer.model.TimedText;
import xyz.doikki.videoplayer.model.Track;
import xyz.doikki.videoplayer.player.AbstractPlayer;
import xyz.doikki.videoplayer.player.VideoViewManager;


public class ExoMediaPlayer extends AbstractPlayer implements Player.Listener {

    protected Context mAppContext;
    protected ExoPlayer mInternalPlayer;
    protected MediaSource mMediaSource;
    protected ExoMediaSourceHelper mMediaSourceHelper;

    private PlaybackParameters mSpeedPlaybackParameters;

    private boolean mIsPreparing;

    private LoadControl mLoadControl;
    private RenderersFactory mRenderersFactory;
    private TrackSelector mTrackSelector;
    private boolean cacheEnabled = false;

    public ExoMediaPlayer(Context context) {
        mAppContext = context.getApplicationContext();
        mMediaSourceHelper = ExoMediaSourceHelper.getInstance(context);
        mRenderersFactory = new FfmpegRenderersFactory(mAppContext);
        Log.i("ExoMediaPlayer:", "create");
    }

    @Override
    public void initPlayer() {
        mInternalPlayer = new ExoPlayer.Builder(
                mAppContext,
                mRenderersFactory == null ? mRenderersFactory = new DefaultRenderersFactory(mAppContext) : mRenderersFactory,
                new DefaultMediaSourceFactory(mAppContext),
                mTrackSelector == null ? mTrackSelector = new DefaultTrackSelector(mAppContext) : mTrackSelector,
                mLoadControl == null ? mLoadControl = new DefaultLoadControl() : mLoadControl,
                DefaultBandwidthMeter.getSingletonInstance(mAppContext),
                new DefaultAnalyticsCollector(Clock.DEFAULT))
                .build();
        setOptions();
        //播放器日志
        if (VideoViewManager.getConfig().mIsEnableLog && mTrackSelector instanceof MappingTrackSelector) {
            mInternalPlayer.addAnalyticsListener(new EventLogger((MappingTrackSelector) mTrackSelector, "ExoPlayer"));
        }

        mInternalPlayer.setWakeMode(C.WAKE_MODE_LOCAL);
        mInternalPlayer.addListener(this);
    }

    @Override
    public void onCues(@NonNull CueGroup cueGroup) {
        Player.Listener.super.onCues(cueGroup);
        for (Cue cue : cueGroup.cues) {
            TimedText timedText = new TimedText();
            timedText.text = cue.text;
            timedText.bitmap = cue.bitmap;
            timedText.bitmapHeight = cue.bitmapHeight;
            mPlayerEventListener.onTimedText(timedText);
        }
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public void setTrackSelector(TrackSelector trackSelector) {
        mTrackSelector = trackSelector;
    }

    public void setRenderersFactory(RenderersFactory renderersFactory) {
        mRenderersFactory = renderersFactory;
    }

    public void setLoadControl(LoadControl loadControl) {
        mLoadControl = loadControl;
    }

    @Override
    public void setDataSource(String path, Map<String, String> headers) {
        mMediaSource = mMediaSourceHelper.getMediaSource(path, headers, cacheEnabled);
    }

    @Override
    public void setDataSource(AssetFileDescriptor fd) {
        //no support
    }

    @Override
    public void start() {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.setPlayWhenReady(false);
    }

    @Override
    public void stop() {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.stop();
    }

    @Override
    public void prepareAsync() {
        if (mInternalPlayer == null)
            return;
        if (mMediaSource == null) return;
        if (mSpeedPlaybackParameters != null) {
            mInternalPlayer.setPlaybackParameters(mSpeedPlaybackParameters);
        }
        mIsPreparing = true;
        mInternalPlayer.setMediaSource(mMediaSource);
        mInternalPlayer.prepare();
    }

    @Override
    public void reset() {
        if (mInternalPlayer != null) {
            mInternalPlayer.stop();
            mInternalPlayer.clearMediaItems();
            mInternalPlayer.setVideoSurface(null);
            mIsPreparing = false;
        }
    }

    @Override
    public boolean isPlaying() {
        if (mInternalPlayer == null)
            return false;
        int state = mInternalPlayer.getPlaybackState();
        switch (state) {
            case Player.STATE_BUFFERING:
            case Player.STATE_READY:
                return mInternalPlayer.getPlayWhenReady();
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
            default:
                return false;
        }
    }

    @Override
    public void seekTo(long time) {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.seekTo(time);
    }

    @Override
    public void release() {
        if (mInternalPlayer != null) {
            mInternalPlayer.removeListener(this);
            mInternalPlayer.release();
            mInternalPlayer = null;
        }

        mIsPreparing = false;
        mSpeedPlaybackParameters = null;
    }

    @Override
    public long getCurrentPosition() {
        if (mInternalPlayer == null)
            return 0;
        return mInternalPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        if (mInternalPlayer == null)
            return 0;
        return mInternalPlayer.getDuration();
    }

    @Override
    public int getBufferedPercentage() {
        return mInternalPlayer == null ? 0 : mInternalPlayer.getBufferedPercentage();
    }

    @Override
    public void setSurface(Surface surface) {
        if (mInternalPlayer != null) {
            mInternalPlayer.setVideoSurface(surface);
        }
    }

    @Override
    public void setDisplay(SurfaceHolder holder) {
        if (holder == null)
            setSurface(null);
        else
            setSurface(holder.getSurface());
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if (mInternalPlayer != null)
            mInternalPlayer.setVolume((leftVolume + rightVolume) / 2);
    }

    @Override
    public void setLooping(boolean isLooping) {
        if (mInternalPlayer != null)
            mInternalPlayer.setRepeatMode(isLooping ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
    }

    @Override
    public void setOptions() {
        //准备好就开始播放
        mInternalPlayer.setPlayWhenReady(true);
    }

    @Override
    public void setSpeed(float speed) {
        PlaybackParameters playbackParameters = new PlaybackParameters(speed);
        mSpeedPlaybackParameters = playbackParameters;
        if (mInternalPlayer != null) {
            mInternalPlayer.setPlaybackParameters(playbackParameters);
        }
    }

    @Override
    public float getSpeed() {
        if (mSpeedPlaybackParameters != null) {
            return mSpeedPlaybackParameters.speed;
        }
        return 1f;
    }

    @Override
    public long getTcpSpeed() {
        // no support
        return 0;
    }

    @Override
    public List<Track> getSubtitles() {
        ArrayList<Track> trackArrayList = new ArrayList<>();
        try {
            for (Tracks.Group group : mInternalPlayer.getCurrentTracks().getGroups()) {
                if (group.getType() == C.TRACK_TYPE_TEXT) {
                    for (int i = 0; i < group.getMediaTrackGroup().length; i++) {
                        String id = group.getTrackFormat(i).id;
                        if (id != null) {
                            Format format = group.getTrackFormat(i);
                            trackArrayList.add(new Track(Integer.parseInt(id), format.language + "," + format.label));
                        }
                    }
                }
            }
            if (trackArrayList.isEmpty()) {
                Log.i("TrackSelector", "trackArrayList isEmpty !");
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return trackArrayList;
    }


    private int selectedSubtitleIndex = -1;

    @Override
    public void selectSubtitle(Track track) {
        try {
            selectedSubtitleIndex = track.index;
            for (Tracks.Group group : mInternalPlayer.getCurrentTracks().getGroups()) {
                if (group.getType() == C.TRACK_TYPE_TEXT) {
                    for (int i = 0; i < group.getMediaTrackGroup().length; i++) {
                        String id = group.getTrackFormat(i).id;
                        if (id != null) {
                            if (Integer.parseInt(id) == track.index) {
                                if (mInternalPlayer.getTrackSelector() != null) {
                                    String language = group.getTrackFormat(i).language;
                                    Log.i("TrackSelector", "select >>> " + language);

                                    TrackSelectionParameters parameters = mInternalPlayer.getTrackSelectionParameters();

                                    mInternalPlayer.setTrackSelectionParameters(
                                            parameters
                                                    .buildUpon()
                                                    .setOverrideForType(
                                                            new TrackSelectionOverride(
                                                                    group.getMediaTrackGroup(),
                                                                    /* trackIndex= */ 0))
                                                    .build());
                                } else {
                                    Log.i("TrackSelector", "not support !");
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getSelectedSubtitleIndex() {
        try {
            for (Tracks.Group group : mInternalPlayer.getCurrentTracks().getGroups()) {
                if (group.getType() == C.TRACK_TYPE_TEXT) {
                    for (int i = 0; i < group.getMediaTrackGroup().length; i++) {
                        if (group.isTrackSelected(i)) {
                            return i;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return selectedSubtitleIndex;
    }

    @Override
    public List<Track> getTracks() {
        ArrayList<Track> trackArrayList = new ArrayList<>();
        try {
            for (Tracks.Group group : mInternalPlayer.getCurrentTracks().getGroups()) {
                if (group.getType() == C.TRACK_TYPE_AUDIO) {
                    for (int i = 0; i < group.getMediaTrackGroup().length; i++) {
                        String id = group.getTrackFormat(i).id;
                        if (id != null) {
                            Format format = group.getTrackFormat(i);
                            trackArrayList.add(new Track(Integer.parseInt(id), format.language + "," + format.label));
                        }
                        Log.i("TrackSelector", "id: " + id);
                        Log.i("TrackSelector", "format: " + group.getTrackFormat(i));
                    }
                }
            }
            if (trackArrayList.isEmpty()) {
                Log.i("TrackSelector", "trackArrayList isEmpty !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackArrayList;
    }

    @Override
    public void selectTrack(Track track) {
        try {
            selectedTrackerIndex = track.index;
            for (Tracks.Group group : mInternalPlayer.getCurrentTracks().getGroups()) {
                if (group.getType() == C.TRACK_TYPE_AUDIO) {
                    for (int i = 0; i < group.getMediaTrackGroup().length; i++) {
                        String id = group.getTrackFormat(i).id;
                        if (id != null) {
                            if (Integer.parseInt(id) == track.index) {
                                if (mInternalPlayer.getTrackSelector() != null) {
                                    String language = group.getTrackFormat(i).language;
                                    Log.i("TrackSelector", "select >>> " + language);

                                    TrackSelectionParameters parameters = mInternalPlayer.getTrackSelectionParameters();
                                    mInternalPlayer.setTrackSelectionParameters(
                                            parameters
                                                    .buildUpon()
                                                    .setOverrideForType(
                                                            new TrackSelectionOverride(
                                                                    group.getMediaTrackGroup(),
                                                                    0))
                                                    .build());
                                } else {
                                    Log.i("TrackSelector", "not support !");
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private int selectedTrackerIndex = -1;

    @Override
    public int getSelectedTrackIndex() {
        try {
            for (Tracks.Group group : mInternalPlayer.getCurrentTracks().getGroups()) {
                if (group.getType() == C.TRACK_TYPE_AUDIO) {
                    for (int i = 0; i < group.getMediaTrackGroup().length; i++) {
                        if (group.isTrackSelected(i)) {
                            return i;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return selectedTrackerIndex;
    }


    @Override
    public boolean isVideo() {
        try {
            Tracks tracks = mInternalPlayer.getCurrentTracks();
            for (Tracks.Group group : tracks.getGroups()) {
                if (group.getType() == C.TRACK_TYPE_VIDEO) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (mPlayerEventListener == null) return;
        if (mIsPreparing) {
            if (playbackState == Player.STATE_READY) {
                mPlayerEventListener.onPrepared();
                mPlayerEventListener.onInfo(MEDIA_INFO_RENDERING_START, 0);
                mIsPreparing = false;
                selectDefaultSubtitle();
            }
            return;
        }
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                mPlayerEventListener.onInfo(MEDIA_INFO_BUFFERING_START, getBufferedPercentage());
                break;
            case Player.STATE_READY:
                mPlayerEventListener.onInfo(MEDIA_INFO_BUFFERING_END, getBufferedPercentage());
                break;
            case Player.STATE_ENDED:
                mPlayerEventListener.onCompletion();
                break;
            case Player.STATE_IDLE:
                break;
        }
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        if (mPlayerEventListener != null) {
            mPlayerEventListener.onError();
        }
    }

    @Override
    public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
        if (mPlayerEventListener != null) {
            mPlayerEventListener.onVideoSizeChanged(videoSize.width, videoSize.height);
            if (videoSize.unappliedRotationDegrees > 0) {
                mPlayerEventListener.onInfo(MEDIA_INFO_VIDEO_ROTATION_CHANGED, videoSize.unappliedRotationDegrees);
            }
        }
    }

    private void selectDefaultSubtitle() {
        String language = Locale.getDefault().getLanguage();
        Log.i("ExoPlayer", "language = " + language);
        List<Track> trackList = getSubtitles();
        if (trackList.size() == 1) {
            selectSubtitle(trackList.get(0));
            TimedText timedText = new TimedText();
            timedText.text = "已加载默认字幕: " + trackList.get(0).name;
            mPlayerEventListener.onTimedText(timedText);
        } else {
            for (int i = 0; i < trackList.size(); i++) {
                Track track = trackList.get(i);
                Log.i("ExoPlayer", "subtitle = " + track.name);
                if (StringsKt.contains(track.name, language, true)) {
                    selectSubtitle(track);
                    TimedText timedText = new TimedText();
                    timedText.text = "已加载系统语言字幕: " + track.name;
                    mPlayerEventListener.onTimedText(timedText);
                    break;
                }
            }
        }
    }
}
