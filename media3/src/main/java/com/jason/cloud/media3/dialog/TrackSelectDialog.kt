package com.jason.cloud.media3.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.jason.cloud.media3.R
import com.jason.cloud.media3.adapter.TrackSelectAdapter
import com.jason.cloud.media3.model.TrackSelectEntity

class TrackSelectDialog(context: Context) : Dialog(context, R.style.Media3DialogStyle) {
    private val adapter = TrackSelectAdapter()
    private val tvTitle by lazy { findViewById<TextView>(R.id.tv_title) }
    private val rvSelection by lazy { findViewById<RecyclerView>(R.id.rv_selection) }
    private val btnNeutral by lazy { findViewById<MaterialButton>(R.id.btn_neutral) }
    private val btnNegative by lazy { findViewById<MaterialButton>(R.id.btn_negative) }
    private val btnPositive by lazy { findViewById<MaterialButton>(R.id.btn_positive) }
    private var lastSelection = 0

    init {
        setContentView(R.layout.layout_track_select_dialog)
        rvSelection.adapter = adapter
        adapter.setOnSelectionChangedListener { i, _ ->
            doSelection(i)
        }
    }

    private fun doSelection(position: Int) {
        Log.e("TrackSelectDialog", "doSelection: $position")
        for (i in 0 until adapter.itemCount) {
            val holder = rvSelection.findViewHolderForAdapterPosition(i)
            if (holder is TrackSelectAdapter.ViewHolder) {
                holder.checkbox.isChecked = i == position
            }
        }
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        tvTitle.text = title
    }

    override fun setTitle(titleId: Int) {
        super.setTitle(titleId)
        tvTitle.setText(titleId)
    }

    fun setTitle(title: CharSequence): TrackSelectDialog {
        tvTitle.text = title
        return this
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionData(list: List<TrackSelectEntity>): TrackSelectDialog {
        adapter.setData(list)
        adapter.notifyDataSetChanged()
        return this
    }

    fun setSelectedPosition(position: Int): TrackSelectDialog {
        lastSelection = position
        adapter.setSelectedPosition(position)
        rvSelection.scrollToPosition(position)
        return this
    }

    fun onPositive(
        text: CharSequence? = null,
        block: ((selection: TrackSelectEntity) -> Unit)? = null
    ): TrackSelectDialog {
        if (text != null) {
            btnPositive.text = text
        }
        btnPositive.setOnClickListener {
            if (lastSelection != adapter.getSelectionPosition()) {
                block?.invoke(adapter.getSelectedItem())
            }
            dismiss()
        }
        return this
    }

    fun onNegative(text: CharSequence? = null, block: (() -> Unit)? = null): TrackSelectDialog {
        if (text != null) {
            btnNegative.text = text
        }
        btnNegative.isVisible = true
        btnNegative.setOnClickListener {
            block?.invoke()
            dismiss()
        }
        return this
    }

    fun onNeutral(text: CharSequence? = null, block: (() -> Unit)? = null): TrackSelectDialog {
        if (text != null) {
            btnNeutral.text = text
        }
        btnNeutral.isVisible = true
        btnNeutral.setOnClickListener {
            block?.invoke()
            dismiss()
        }
        return this
    }
}