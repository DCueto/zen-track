package me.dcueto.zentrackapp.network

// 10.0.2.2 = localhost del host desde el emulador Android.
// En producción/CI sobreescribir con BuildConfig.API_BASE_URL vía buildConfigField en androidApp.
actual val apiBaseUrl: String = "http://10.0.2.2:8080"
