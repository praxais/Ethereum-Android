package com.xais.ethdemo

import android.content.Context
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import android.util.Log
import io.reactivex.plugins.RxJavaPlugins
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

/**
 * Created by prajwal on 3/7/19.
 */

class EthApplication : MultiDexApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        //to handle rxJava crashes
        RxJavaPlugins.setErrorHandler { throwable ->
            Log.d("Xais-error", throwable.localizedMessage)
        }
        setupBouncyCastle()
    }

    //setup bouncy castle for security purposes
    private fun setupBouncyCastle() {
        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
            ?: // Web3j will set up the provider lazily when it's first used.
            return
        if (provider.javaClass == BouncyCastleProvider::class.java) {
            // BC with same package name, shouldn't happen in real life.
            return
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}