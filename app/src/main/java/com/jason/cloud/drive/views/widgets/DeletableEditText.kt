package com.jason.cloud.drive.views.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import com.jason.cloud.drive.R


/**
 * @Author: 进阶的面条
 * @Date: 2021-12-08 15:56
 * @Description: TODO
 */
open class DeletableEditText(context: Context, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {
    private var mClearDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_round_edit_clear_15)?.mutate()

    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.DeletableEditText, 0, 0)
        val clearButtonTint = typedArray.getColor(R.styleable.DeletableEditText_clearButtonTint, Color.BLACK)
        mClearDrawable?.setTint(clearButtonTint)
        if (isInEditMode && hint.isNullOrBlank()) {
            hint = "请输入文本内容..."
            setClearIconVisible(true)
        }
    }

    fun onConfirmListener(call: (text: String) -> Unit) {
        this.setSingleLine()
        this.imeOptions = EditorInfo.IME_ACTION_DONE
        this.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                call.invoke(v.text.toString())
            }
            false
        }
    }

    fun onSearchListener(call: (text: String) -> Unit) {
        this.setSingleLine()
        this.imeOptions = EditorInfo.IME_ACTION_SEARCH
        this.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                call.invoke(v.text.toString())
            }
            false
        }
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        setClearIconVisible(isFocused && text.isNotEmpty())
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        setClearIconVisible(focused && length() > 0)
        postInvalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                mClearDrawable?.let {
                    if (event.x <= width - paddingRight && event.x >= width - paddingRight - it.bounds.width()) {
                        setText("")
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun setClearIconVisible(visible: Boolean) {
        if (visible) {
            setCompoundDrawablesWithIntrinsicBounds(null, null, mClearDrawable, null)
        } else {
            setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }
}