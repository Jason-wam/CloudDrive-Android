package com.jason.cloud.drive.views.activity

import android.view.View
import androidx.activity.OnBackPressedCallback
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.base.BaseViewPager2Adapter
import com.jason.cloud.drive.databinding.ActivityMainBinding
import com.jason.cloud.drive.views.fragment.FilesFragment
import com.jason.cloud.drive.views.fragment.HomeFragment
import com.jason.cloud.drive.views.fragment.MineFragment
import com.jason.cloud.drive.extension.toast
import com.jason.cloud.drive.interfaces.CallActivityInterface
import com.jason.videocat.utils.extension.view.bindBottomNavigationView
import kotlin.system.exitProcess

class MainActivity : BaseBindActivity<ActivityMainBinding>(R.layout.activity_main),
    CallActivityInterface {
    override fun initView() {
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.getChildAt(0)?.overScrollMode = View.OVER_SCROLL_NEVER
        binding.viewPager2.bindBottomNavigationView(binding.bottomNavigationView)
        binding.viewPager2.adapter = BaseViewPager2Adapter(this).apply {
            addFragment("0", HomeFragment.newInstance())
            addFragment("1", FilesFragment.newInstance())
            addFragment("2", MineFragment.newInstance())
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    binding.viewPager2.setCurrentItem(0, false)
                    return@setOnItemSelectedListener true
                }

                R.id.file -> {
                    binding.viewPager2.setCurrentItem(1, false)
                    return@setOnItemSelectedListener true
                }

                R.id.mine -> {
                    binding.viewPager2.setCurrentItem(2, false)
                    return@setOnItemSelectedListener true
                }

                else -> return@setOnItemSelectedListener true
            }
        }
    }

    private var exitTime: Long = 0

    override fun callOnBackPressed() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            toast("再按一次退出程序")
            exitTime = System.currentTimeMillis()
        } else {
            try {
                finishAndRemoveTask()
                exitProcess(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}