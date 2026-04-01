package me.dcueto.zentrackapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform