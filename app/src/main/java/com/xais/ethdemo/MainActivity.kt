package com.xais.ethdemo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ChainId
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val password = "mypassword"
    private val chainId = ChainId.RINKEBY
    private val chainHost = "https://rinkeby.infura.io/v3/fc3b28083a234976a573818de16fd142"
    private lateinit var credentials: Credentials

    private lateinit var mnemonic: String
    private lateinit var address: String
    private lateinit var balance: String
    private lateinit var signature: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSend?.setOnClickListener(this)
        initWallet()
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(view: View?) {
        when (view) {
            btnSend -> {
                if (!edtAddress?.text?.toString().isNullOrEmpty()) {
                    prbLoadingHash?.visibility = View.VISIBLE
                    sendEth().subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            prbLoadingHash?.visibility = View.GONE
                            txvHash?.text = "Transaction Hash: $it"
                        }, {
                            prbLoadingHash?.visibility = View.GONE
                            Log.d("Xais-error", it.localizedMessage)
                            txvHash?.text = "Error: Transaction not successful"
                        })
                } else {
                    Toast.makeText(this, "Address is empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initWallet() {
        getWallet()
            .flatMap {
                Log.d("Mnemonic:", it.mnemonic)
                mnemonic = it.mnemonic
                loadCredentialsForWallet(it)
            }.flatMap {
                credentials = it
                address = it.address
                Log.d("Address:", it.address)
                getBalance(it)
            }.flatMap {
                Log.d("Balance:", Convert.fromWei(BigDecimal(it), Convert.Unit.ETHER).toPlainString() + "ETH")
                balance = Convert.fromWei(BigDecimal(it), Convert.Unit.ETHER).toPlainString() + "ETH"
                getSignIn(credentials, "test")
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val result = ByteArray(65)
                System.arraycopy(it.r, 0, result, 0, 32)
                System.arraycopy(it.s, 0, result, 32, 32)
                result[64] = it.v
                Log.d("Signature:", String.format("Msg = %s\nSig = %s", "test", Numeric.toHexString(result)))
                signature = String.format("Msg = %s\nSig = %s", "test", Numeric.toHexString(result))

                txvMnemonic?.text = "Mnemonic: $mnemonic"
                txvAddress?.text = "Address: $address"
                txvBalance?.text = "Balance: $balance"
                txvSignature?.text = "Signature: $signature"
                prbLoading?.visibility = View.GONE
            }, {
                Log.d("Xais-error", it.localizedMessage)
            })
    }

    private fun getWallet(): Single<Bip39Wallet> {
        return Single.create<Bip39Wallet> { e ->
            try {
                val prefs = getSharedPreferences("wallet", Context.MODE_PRIVATE)
                val filename = prefs.getString("filename", "")
                val mnemonic = prefs.getString("mnemonic", "")
                if (TextUtils.isEmpty(filename) || TextUtils.isEmpty(mnemonic)) {
                    e.onSuccess(generateWallet(application.filesDir))
                    return@create
                }
                // validate
                Bip44WalletUtils.loadBip44Credentials(password, mnemonic)
                e.onSuccess(Bip39Wallet(filename, mnemonic))
                return@create
            } catch (exception: CipherException) {
                e.onError(exception)
            } catch (exception: IOException) {
                e.onError(exception)
            }
            e.onError(Throwable("Error"))
        }
    }

    @Throws(CipherException::class, IOException::class)
    private fun generateWallet(dir: File): Bip39Wallet {
        val wallet = Bip44WalletUtils.generateBip44Wallet(password, dir)
        val file = File(dir, wallet.filename)
        if (!file.exists()) throw IOException("No file created")
        saveWallet(wallet)
        return wallet
    }

    private fun saveWallet(wallet: Bip39Wallet) {
        val editor = getSharedPreferences("wallet", Context.MODE_PRIVATE).edit()
        editor.putString("filename", wallet.filename)
        editor.putString("mnemonic", wallet.mnemonic)
        editor.apply()
    }

    private fun loadCredentialsForWallet(wallet: Bip39Wallet): Single<Credentials> {
        return Single.create<Credentials> { e ->
            val credentials = Bip44WalletUtils.loadBip44Credentials(password, wallet.mnemonic)
            // m/44'/60'/0'/0
            val keyPair = credentials.ecKeyPair as Bip32ECKeyPair
            // m/44'/60'/0'/0/0
            e.onSuccess(Credentials.create(Bip32ECKeyPair.deriveKeyPair(keyPair, intArrayOf(0))))
        }
    }

    private fun getBalance(credentials: Credentials): Single<BigInteger> {
        return Single.create { e ->
            val web3j = Web3j.build(HttpService(chainHost))
            try {
                e.onSuccess(web3j.ethGetBalance(credentials.address, DefaultBlockParameterName.LATEST).send().balance)
            } catch (exception: Exception) {
                e.onError(exception)
            }
        }
    }

    private fun getSignIn(credentials: Credentials, message: String): Single<Sign.SignatureData> {
        return Single.create { e ->
            val hash = Hash.sha3(Numeric.toHexString(message.toByteArray()))
            e.onSuccess(Sign.signPrefixedMessage(Numeric.hexStringToByteArray(hash), credentials.ecKeyPair))
        }
    }

    private fun sendEth(): Single<String> {
        return Single.create { e ->
            val web3j = Web3j.build(HttpService(chainHost))
            try {
                val transferReceipt = Transfer.sendFunds(
                    web3j, credentials,
                    edtAddress?.text.toString(), // you can put any address here
                    Convert.toWei("0.0004", Convert.Unit.ETHER), Convert.Unit.WEI
                ).send()
                e.onSuccess(transferReceipt.transactionHash ?: "N/A")
            } catch (exception: IOException) {
                e.onError(exception)
            } catch (exception: RuntimeException) {
                e.onError(exception)
            }
        }
    }
}
