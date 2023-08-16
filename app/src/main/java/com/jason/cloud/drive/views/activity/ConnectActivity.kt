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
        binding.editIp.setText(Configure.host)
        binding.editPort.setText(Configure.port.toString())
        binding.editPassword.setText(Configure.password)

        binding.btnLogin.setOnClickListener {
            connect()
        }
    }

    private fun connect() {
        binding.btnLogin.text = "正在登录..."
        binding.btnLogin.alpha = 0.5f
        binding.btnLogin.isEnabled = false

        val ip = binding.editIp.text?.toString() ?: ""
        val port = binding.editPort.text?.toString() ?: "8820"
        val password = binding.editPassword.text?.toString() ?: ""

        scopeNetLife {
            val respond = Get<String>("http://$ip:$port/connect") {
                setHeader("password", password)
            }.await().asJSONObject()

            val code = respond.getInt("code")
            if (code == 200) {
                Configure.host = ip
                Configure.port = port.toInt()
                Configure.password = password
                toast("芝麻开门！")
                startActivity(MainActivity::class)
                finish()
            } else {
                toast(respond.getString("message"))
                binding.btnLogin.text = "登录"
                binding.btnLogin.alpha = 1f
                binding.btnLogin.isEnabled = true
            }
        }.catch {
            toast(it.toMessage())
            binding.btnLogin.text = "登录"
            binding.btnLogin.alpha = 1f
            binding.btnLogin.isEnabled = true
        }
    }
}