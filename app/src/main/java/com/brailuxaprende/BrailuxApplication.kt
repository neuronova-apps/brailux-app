package com.brailuxaprende

import android.app.Application
import com.brailuxaprende.ai.BrailuxDiagnosticLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

class BrailuxApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        BrailuxDiagnosticLogger.d("BrailuxDiagnostic", "FirebaseApp inicializado. Configurando AppCheckProviderFactory...")
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            appCheckProviderFactory(),
        )
        BrailuxDiagnosticLogger.d("BrailuxDiagnostic", "AppCheckProviderFactory configurado.")
    }
}
