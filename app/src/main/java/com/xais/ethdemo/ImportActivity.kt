package com.xais.ethdemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_import.*
import org.web3j.crypto.Bip39Wallet
import org.web3j.crypto.MnemonicUtils
import java.util.*

/**
 * Created by prajwal on 3/25/19.
 */

class ImportActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        btnImport?.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view) {
            btnImport -> importWallet()
        }
    }

    private fun importWallet() {
        val one = edtOne?.text.toString()
        val two = edtTwo?.text.toString()
        val three = edtThree?.text.toString()
        val four = edtFour?.text.toString()
        val five = edtFive?.text.toString()
        val six = edtSix?.text.toString()
        val seven = edtSeven?.text.toString()
        val eight = edtEight?.text.toString()
        val nine = edtNine?.text.toString()
        val ten = edtTen?.text.toString()
        val eleven = edtEleven?.text.toString()
        val twelve = edtTwelve?.text.toString()

        if (one.isNotEmpty() && two.isNotEmpty() && three.isNotEmpty() && four.isNotEmpty() && five.isNotEmpty() &&
            six.isNotEmpty() && seven.isNotEmpty() && eight.isNotEmpty() && nine.isNotEmpty() && ten.isNotEmpty() &&
            eleven.isNotEmpty() && twelve.isNotEmpty()
        ) {
            val mnemonic =
                one.addString(two).addString(three).addString(four).addString(five).addString(six).addString(seven)
                    .addString(eight).addString(nine).addString(ten).addString(eleven).addString(twelve)
                    .toLowerCase(Locale.US)
            try {
                MnemonicUtils.generateEntropy(mnemonic)
                val wallet = Bip39Wallet("empty", mnemonic)
                saveWallet(wallet)
                navigateToMainActivity()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveWallet(wallet: Bip39Wallet) {
        val editor = getSharedPreferences("wallet", Context.MODE_PRIVATE).edit()
        editor.putString("filename", wallet.filename)
        editor.putString("mnemonic", wallet.mnemonic)
        editor.apply()
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}