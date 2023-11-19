package com.jason.cloud.drive.views.activity

import com.drake.net.Get
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.databinding.ActivityConnectBinding
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.startActivity
import com.jason.cloud.extension.toast

class ConnectActivity : BaseBindActivity<ActivityConnectBinding>(R.layout.activity_connect) {
    override fun initView() {
        binding.editHost.setText(Configure.host)
        binding.editPassword.setText(Configure.password)

        binding.btnLogin.setOnClickListener {
            connect()
        }
    }

    private fun connect() {
        binding.btnLogin.text = "正在连接服务器..."
        binding.btnLogin.alpha = 0.5f
        binding.btnLogin.isEnabled = false

        val host = binding.editHost.text?.toString() ?: ""
        val password = binding.editPassword.text?.toString() ?: ""

        scopeNetLife {
            val respond = Get<String>("$host/connect") {
                setHeader("password", password)
            }.await().asJSONObject()

            val code = respond.getInt("code")
            if (code == 200) {
                Configure.host = host
                Configure.password = password
                toast("芝麻开门！")
                startActivity(MainActivity::class)
                finish()
            } else {
                toast(respond.getString("message"))
                binding.btnLogin.setText(R.string.connect_server)
                binding.btnLogin.alpha = 1f
                binding.btnLogin.isEnabled = true
            }
        }.catch {
            toast(it.toMessage())
            binding.btnLogin.setText(R.string.connect_server)
            binding.btnLogin.alpha = 1f
            binding.btnLogin.isEnabled = true
        }
    }
}