package me.dcueto.zentrackapp.cli

data class ReplSession(
    var token: String? = null,
    var refreshToken: String? = null,
    var userEmail: String? = null,
    var activeWorkspaceName: String? = null,
    var activeProjectKey: String? = null,
    val apiUrl: String = System.getenv("ZENTRACK_API_URL") ?: "http://localhost:8080"
) {
    val isAuthenticated: Boolean get() = token != null

    fun prompt(): String {
        val ctx = buildString {
            if (activeWorkspaceName != null) {
                append(activeWorkspaceName)
                if (activeProjectKey != null) append("/").append(activeProjectKey)
            }
        }
        return if (ctx.isEmpty()) "ZenTrack > " else "ZenTrack [$ctx] > "
    }

    fun clear() {
        token = null; refreshToken = null; userEmail = null
        activeWorkspaceName = null; activeProjectKey = null
    }
}
