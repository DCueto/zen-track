package me.dcueto.zentrackapp

actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
