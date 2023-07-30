package com.jason.aplaye.extension

import android.content.Context
import xyz.doikki.videoplayer.player.PlayerFactory

open class APlayerFactory(private val enableToast: Boolean) : PlayerFactory<APlayer>() {
    companion object {
        fun create(enableToast: Boolean = false): APlayerFactory {
            return APlayerFactory(enableToast)
        }
    }

    override fun createPlayer(context: Context): APlayer {
        return APlayer(context, enableToast)
    }
}