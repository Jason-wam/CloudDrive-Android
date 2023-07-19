package com.jason.cloud.drive.utils.extension.view

import android.animation.AnimatorInflater
import android.animation.StateListAnimator
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.jason.cloud.drive.R


fun AppBarLayout.bindRvElevation(recyclerView: RecyclerView) {
    AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_layout_elevation).also {
        bindRvElevation(recyclerView, it)
    }
}

fun AppBarLayout.bindRvElevation(recyclerView: RecyclerView, animatorId: Int) {
    AnimatorInflater.loadStateListAnimator(context, animatorId).also {
        bindRvElevation(recyclerView, it)
    }
}

fun AppBarLayout.bindRvElevation(recyclerView: RecyclerView, animator: StateListAnimator) {
    if (recyclerView.adapter == null) {
        throw java.lang.Exception("RecyclerView adapter can't be null before bind Elevation!!!")
    }

    stateListAnimator = animator

    val animatorNil = AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_layout_elevation_nil)

    fun refreshShadow() {
        stateListAnimator = if (recyclerView.adapter?.itemCount == 0) {
            animator
        } else {
            if (recyclerView.canScrollVertically(-1)) {
                animator
            } else {
                animatorNil
            }
        }
    }

    recyclerView.adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            refreshShadow()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            refreshShadow()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            refreshShadow()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            refreshShadow()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            refreshShadow()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            refreshShadow()
        }
    })

    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            refreshShadow()
        }
    })
}

fun AppBarLayout.setElevationIsVisible(isVisible: Boolean) {
    val animator = AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_layout_elevation)
    val animatorNil = AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_layout_elevation_nil)
    stateListAnimator = if (isVisible) {
        animator
    } else {
        animatorNil
    }
}

fun AppBarLayout.bindNestedScrollViewElevation(scrollView: NestedScrollView) {
    AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_layout_elevation).also {
        bindNestedScrollViewElevation(scrollView, it)
    }
}

fun AppBarLayout.bindNestedScrollViewElevation(scrollView: NestedScrollView, animator: StateListAnimator) {
    val animatorNil = AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_layout_elevation_nil)

    fun showShadowIsVisible(show: Boolean) {
        stateListAnimator = if (show) {
            animator
        } else {
            animatorNil
        }
    }

    showShadowIsVisible(scrollView.scrollY != 0)

    scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, _, _, _ ->
        showShadowIsVisible(v.scrollY != 0)
    })
}
