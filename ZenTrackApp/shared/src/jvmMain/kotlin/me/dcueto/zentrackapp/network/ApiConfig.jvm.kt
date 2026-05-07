package me.dcueto.zentrackapp.network

actual val apiBaseUrl: String
    get() = System.getenv("ZENTRACK_API_URL") ?: "http://localhost:8080"
