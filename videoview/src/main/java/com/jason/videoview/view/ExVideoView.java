package com.jason.videoview.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.doikki.videoplayer.player.VideoView;

public class ExVideoView extends VideoView {
    private OnSizeChangedListener onSizeChangedListener;
    private OnStateChangeListener onStateChangeListener;

    public ExVideoView(@NonNull Context context) {
        super(context);
    }

    public ExVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onVideoSizeChanged(int videoWidth, int videoHeight) {
        super.onVideoSizeChanged(videoWidth, videoHeight);
        if (onSizeChangedListener != null) {
            onSizeChangedListener.onSizeChanged(videoWidth, videoHeight);
        }
    }

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        this.onSizeChangedListener = listener;
    }

    public void setOnStateChangedListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }

    public interface OnSizeChangedListener {
        void onSizeChanged(int width, int height);
    }
}
