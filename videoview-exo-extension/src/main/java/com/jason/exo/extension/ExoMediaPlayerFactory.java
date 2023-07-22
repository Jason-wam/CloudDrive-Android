package com.jason.exo.extension;

import android.content.Context;

import xyz.doikki.videoplayer.player.PlayerFactory;

public class ExoMediaPlayerFactory extends PlayerFactory<ExoMediaPlayer> {
    private boolean cacheEnabled = false;

    public static ExoMediaPlayerFactory create(boolean cacheEnabled) {
        ExoMediaPlayerFactory factory = new ExoMediaPlayerFactory();
        factory.cacheEnabled = cacheEnabled;
        return factory;
    }

    @Override
    public ExoMediaPlayer createPlayer(Context context) {
        ExoMediaPlayer player = new ExoMediaPlayer(context);
        player.setCacheEnabled(cacheEnabled);
        return player;
    }
}
