package com.jason.cloud.drive.views.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.jason.cloud.drive.R

class DownTipWindow(context: Context) : PopupWindow() {
    private var onShowListener: OnShowListener? = null

    interface OnShowListener {
        fun onShow()
    }

    init {
        this.contentView = View.inflate(context, R.layout.layout_down_tip, null)
        this.width = ViewGroup.LayoutParams.WRAP_CONTENT
        this.height = ViewGroup.LayoutParams.WRAP_CONTENT
        this.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        this.isOutsideTouchable = true //点击外部消失
        this.isFocusable = true //点击一下外部才消失
    }

    fun setTitle(title: CharSequence): DownTipWindow {
        contentView.findViewById<TextView>(R.id.tv_title)?.text = title
        return this
    }

    fun setMessage(message: CharSequence): DownTipWindow {
        contentView.findViewById<TextView>(R.id.tv_message)?.text = message
        return this
    }

    fun setButton(text: CharSequence, block: (() -> Unit)? = null): DownTipWindow {
        contentView.findViewById<TextView>(R.id.button)?.let {
            it.text = text
            it.setOnClickListener {
                dismiss()
                block?.invoke()
            }
        }
        return this
    }

    fun setOnShowListener(listener: OnShowListener): DownTipWindow {
        this.onShowListener = listener
        return this
    }

    fun show(anchor: View) {
        anchor.post {
            showAtLocation(anchor, Gravity.TOP, 0, 0)
            contentView.post {
                val width = contentView.width
                val height = contentView.height
                val location = IntArray(2)
                anchor.getLocationOnScreen(location)
                //该坐标值为View左上角的坐标

                val x = location[0] + anchor.width / 2 - width / 2
                val y = location[1] + anchor.height

                dismiss()
                //showAsDropDown(anchor)
                showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
                contentView.animate().alphaBy(0f).alpha(1f).setDuration(200).withEndAction {
                    onShowListener?.onShow()
                    moveIndicator(anchor)
                }
            }
        }
    }

    private fun moveIndicator(anchor: View) {
        val locationAnchor = IntArray(2)
        anchor.getLocationOnScreen(locationAnchor)
        val locationAnchorX = locationAnchor[0] + anchor.width / 2
        Log.e("DownTipWindow","locationAnchorX = $locationAnchorX")
        contentView.findViewById<View>(R.id.indicator)?.let {
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            val locationX = location[0] + it.width / 2
            val difference = locationAnchorX - locationX
            it.animate().translationXBy(it.x).translationX(difference.toFloat()).start()
        }
    }
}