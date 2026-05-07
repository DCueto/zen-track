package me.dcueto.zentrackapp.navigation

sealed class AppDestinations(val route: String) {
    data object Workspaces : AppDestinations("workspaces")

    data object Board : AppDestinations("board/{workspaceId}") {
        const val ARG = "workspaceId"
        fun route(workspaceId: String) = "board/$workspaceId"
    }

    data object TaskDetail : AppDestinations("task/{taskId}") {
        const val ARG = "taskId"
        fun route(taskId: String) = "task/$taskId"
    }
}
