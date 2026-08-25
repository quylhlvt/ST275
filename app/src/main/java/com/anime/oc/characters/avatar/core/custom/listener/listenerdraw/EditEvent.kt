package com.anime.oc.characters.avatar.core.custom.listener.listenerdraw

import android.view.MotionEvent
import com.anime.oc.characters.avatar.core.custom.DrawView

class EditEvent : DrawEvent {
    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (!tattooView!!.isLocking()) {
            tattooView.editText()
        }
    }
}
