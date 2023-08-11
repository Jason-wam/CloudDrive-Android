package com.jason.cloud.drive.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.jason.cloud.drive.R

class StateLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val viewMap = HashMap<Any, View>()
    private var retryTag: String = "retry"
    private val textViewTag: String = "msg"

    private var previewState: Int = -1

    companion object State {
        var errorViewId: Int = -1
        var emptyViewId: Int = -1
        var loadingViewId: Int = -1
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.StateLayout)
        with(typedArray.getResourceId(R.styleable.StateLayout_state_error_view, errorViewId)) {
            if (this != -1) {
                viewMap["error"] = View.inflate(context, this, null).apply {
                    visibility = GONE
                    tag = "error"
                }
            }
        }

        with(typedArray.getResourceId(R.styleable.StateLayout_state_empty_view, emptyViewId)) {
            if (this != -1) {
                viewMap["empty"] = View.inflate(context, this, null).apply {
                    visibility = GONE
                    tag = "empty"
                }
            }
        }

        with(typedArray.getResourceId(R.styleable.StateLayout_state_load_view, loadingViewId)) {
            if (this != -1) {
                viewMap["loading"] = View.inflate(context, this, null).apply {
                    visibility = GONE
                    tag = "loading"
                }
            }
        }

        previewState = typedArray.getInt(R.styleable.StateLayout_preview_state, -1)
        typedArray.recycle()

        //如果用户未设置状态布局则使用内置默认布局
        if (viewMap["error"] == null) {
            viewMap["error"] = View.inflate(context, R.layout.layout_default_state_error, null).apply {
                visibility = GONE
                tag = "error"
            }
        }
        if (viewMap["empty"] == null) {
            viewMap["empty"] = View.inflate(context, R.layout.layout_default_state_empty, null).apply {
                visibility = GONE
                tag = "empty"
            }
        }
        if (viewMap["loading"] == null) {
            viewMap["loading"] =
                View.inflate(context, R.layout.layout_default_state_loading, null).apply {
                    visibility = GONE
                    tag = "loading"
                }
        }
    }

    fun setRetryButton(tag: String) {
        this.retryTag = tag
    }

    fun bindView(tag: Any, view: View) {
        view.tag = tag
        viewMap[tag] = view
    }

    fun bindView(tag: Any, @LayoutRes layoutId: Int): View {
        val view = View.inflate(context, layoutId, null).apply {
            this.tag = tag
        }
        viewMap[tag] = view
        return view
    }

    fun showContent() {
        switchView("content")
    }

    fun showError() = showError(-1, null)

    fun showError(retryCallBack: (() -> Unit)? = null) = showError(-1, retryCallBack)

    fun showError(@StringRes textId: Int = -1, retryCallBack: (() -> Unit)? = null) {
        if (textId != -1) {
            val text = context.getString(textId)
            showError(text, retryCallBack)
        } else {
            showError(null, retryCallBack)
        }
    }

    fun showError(text: CharSequence? = null, retryCallBack: (() -> Unit)? = null) {
        switchView("error") { layoutView ->
            layoutView.findViewWithTag<View>(textViewTag)?.let { textView ->
                if (textView is TextView) {
                    textView.text = text
                    textView.isVisible = text?.isNotBlank() == true
                }
            }

            val retryButton = layoutView.findViewWithTag<View>(retryTag)
            if (retryCallBack != null && retryButton != null) {
                retryButton.isVisible = true
                retryButton.setOnClickListener {
                    retryCallBack.invoke()
                }
            }
        }
    }

    fun showEmpty() = showEmpty("", null)

    fun showEmpty(layoutViewCallBack: ((view: View) -> Unit)? = null) = showEmpty("", layoutViewCallBack)

    fun showEmpty(@StringRes textId: Int = -1, layoutViewCallBack: ((view: View) -> Unit)? = null) {
        if (textId != -1) {
            val text = context.getString(textId)
            showEmpty(text, layoutViewCallBack)
        } else {
            showEmpty("", layoutViewCallBack)
        }
    }

    fun showEmpty(text: CharSequence? = null, layoutViewCallBack: ((view: View) -> Unit)? = null) {
        switchView("empty") { layoutView ->
            layoutViewCallBack?.invoke(layoutView)
            layoutView.findViewWithTag<View>(textViewTag)?.let { textView ->
                if (textView is TextView) {
                    textView.text = text
                    textView.isVisible = text?.isNotBlank() == true
                }
            }
        }
    }

    fun showLoading() = showLoading("", null)

    fun showLoading(layoutViewCallBack: ((view: View) -> Unit)? = null) = showLoading("", layoutViewCallBack)

    fun showLoading(@StringRes textId: Int = -1, layoutViewCallBack: ((view: View) -> Unit)? = null) {
        if (textId != -1) {
            val text = context.getString(textId)
            showLoading(text, layoutViewCallBack)
        } else {
            showLoading("", layoutViewCallBack)
        }
    }

    fun showLoading(text: CharSequence? = null, layoutViewCallBack: ((view: View) -> Unit)? = null) {
        switchView("loading") { layoutView ->
            layoutViewCallBack?.invoke(layoutView)
            layoutView.findViewWithTag<View>(textViewTag)?.let { textView ->
                if (textView is TextView) {
                    textView.text = text
                    textView.isVisible = text?.isNotBlank() == true
                }
            }
        }
    }

    fun switchView(tag: Any, block: ((view: View) -> Unit)? = null) {
        //toList防止并发修改异常
        if (viewMap[tag] == null) {
            throw ClassNotFoundException("No stateView found with the tag of $tag！")
        }
        viewMap.values.toList().forEach { view ->
            if (view.parent == null) {
                addView(view)
            }

            if (view.tag == tag) {
                view.visibility = View.VISIBLE
                block?.invoke(view)
            } else {
                view.visibility = View.INVISIBLE
            }
        }
    }

    fun hide() {
        viewMap.values.forEach { it.visibility = View.INVISIBLE }
    }

    override fun addView(child: View) {
        super.addView(child)
        checkIsContentView(child)
    }

    override fun addView(child: View, index: Int) {
        super.addView(child, index)
        checkIsContentView(child)
    }

    override fun addView(child: View, width: Int, height: Int) {
        super.addView(child, width, height)
        checkIsContentView(child)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams?) {
        super.addView(child, params)
        checkIsContentView(child)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        checkIsContentView(child)
    }

    override fun addViewInLayout(child: View, index: Int, params: ViewGroup.LayoutParams?): Boolean {
        checkIsContentView(child)
        return super.addViewInLayout(child, index, params)
    }

    override fun addViewInLayout(child: View, index: Int, params: ViewGroup.LayoutParams?, preventRequestLayout: Boolean): Boolean {
        checkIsContentView(child)
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }

    private fun checkIsContentView(view: View) {
        val exist = viewMap.values.any {
            it.tag == "content"
        }
        if (exist.not()) {
            viewMap["content"] = view.apply {
                tag = "content"
            }
        }
        if (isInEditMode) {
            when (previewState) {
                0 -> showError("加载失败啦，点击重试！")
                1 -> showEmpty("这里什么都没有哦...")
                2 -> showLoading("正在努力加载数据...")
                else -> showContent()
            }
        }
    }
}