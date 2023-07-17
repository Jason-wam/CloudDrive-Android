package com.jason.cloud.drive.views.fragment

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.databinding.FragmentTasksBinding
import com.jason.cloud.drive.viewmodel.TasksViewModel

class TasksFragment : BaseBindFragment<FragmentTasksBinding>(R.layout.fragment_tasks) {
    companion object {
        fun newInstance() = TasksFragment()
    }

    private val viewModel: TasksViewModel by lazy {
        ViewModelProvider(this)[TasksViewModel::class.java]
    }

    override fun initView(context: Context) {

    }
}