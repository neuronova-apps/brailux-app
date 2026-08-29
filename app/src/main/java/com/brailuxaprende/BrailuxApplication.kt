package com.brailuxaprende

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

class BrailuxApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BrailuxFeatures.ASSISTANT_ENABLED) {
            FirebaseApp.initializeApp(this)
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                appCheckProviderFactory(),
            )
        }
    }
}
