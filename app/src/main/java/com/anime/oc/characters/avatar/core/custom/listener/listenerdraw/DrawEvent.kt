package com.anime.oc.characters.avatar.core.custom.listener.listenerdraw

import android.view.MotionEvent
import com.anime.oc.characters.avatar.core.custom.DrawView


interface DrawEvent {
    fun onActionDown(tattooView: DrawView?, event: MotionEvent?)
    fun onActionMove(tattooView: DrawView?, event: MotionEvent?)
    fun onActionUp(tattooView: DrawView?, event: MotionEvent?)
}