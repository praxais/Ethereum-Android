package com.xais.ethdemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_home.*

/**
 * Created by prajwal on 3/25/19.
 */

class HomeActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnCreate?.setOnClickListener(this)
        btnImport?.setOnClickListener(this)

        checkWalletAvailable()
    }

    override fun onClick(view: View?) {
        when (view) {
            btnCreate -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            btnImport -> {
                startActivity(Intent(this, ImportActivity::class.java))
                finish()
            }
        }
    }

    private fun checkWalletAvailable() {
        val editor = getSharedPreferences("wallet", Context.MODE_PRIVATE)
        if (!editor.getString("filename", "").isNullOrEmpty()){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}