package com.jason.cloud.drive.views.fragment

import android.animation.AnimatorInflater
import android.content.Context
import android.view.View
import androidx.core.view.MenuCompat
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.base.BaseViewPager2Adapter
import com.jason.cloud.drive.databinding.FragmentTasksBinding
import com.jason.cloud.drive.views.fragment.tasks.DownloadDoneFragment
import com.jason.cloud.drive.views.fragment.tasks.DownloadFragment
import com.jason.cloud.drive.views.fragment.tasks.UploadDoneFragment
import com.jason.cloud.drive.views.fragment.tasks.UploadFragment
import com.jason.videocat.utils.extension.view.onTabSelected
import com.jason.videocat.utils.extension.view.setTitleFont

class TasksFragment : BaseBindFragment<FragmentTasksBinding>(R.layout.fragment_tasks) {
    companion object {
        fun newInstance() = TasksFragment()
    }

    override fun initView(context: Context) {
        MenuCompat.setGroupDividerEnabled(binding.toolbar.menu, true)

        binding.toolbar.setTitleFont("fonts/AaJianHaoTi.ttf")

        binding.viewPager2.offscreenPageLimit = 3
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.getChildAt(0)?.overScrollMode = View.OVER_SCROLL_NEVER
        binding.viewPager2.adapter = BaseViewPager2Adapter(this).apply {
            addFragment("0", UploadFragment.newInstance())
            addFragment("1", DownloadFragment.newInstance())
            addFragment("2", DownloadFragment.newInstance())
            addFragment("3", UploadDoneFragment.newInstance())
            addFragment("4", DownloadDoneFragment.newInstance())
        }

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("上传任务"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("取回任务"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("离线任务"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("上传完成任务"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("取回完成任务"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("离线完成任务"))

        binding.tabLayout.onTabSelected {
            binding.viewPager2.setCurrentItem(it.position, false)
        }

        binding.indicatorBar.addOnOffsetChangedListener { _, verticalOffset ->
            binding.appBarLayout.stateListAnimator = if (verticalOffset != 0) {
                AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_layout_elevation)
            } else {
                AnimatorInflater.loadStateListAnimator(
                    context, R.animator.appbar_layout_elevation_nil
                )
            }
        }
    }
}