package me.dcueto.zentrackapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import me.dcueto.zentrackapp.ui.screens.board.BoardScreen
import me.dcueto.zentrackapp.ui.screens.workspaces.WorkspacesScreen

@Composable
fun ZenTrackNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.Workspaces.route
    ) {
        composable(AppDestinations.Workspaces.route) {
            WorkspacesScreen(
                onWorkspaceSelected = { workspaceId ->
                    navController.navigate(AppDestinations.Board.route(workspaceId))
                }
            )
        }

        composable(
            route = AppDestinations.Board.route,
            arguments = listOf(navArgument(AppDestinations.Board.ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString(AppDestinations.Board.ARG)
                ?: return@composable
            BoardScreen(
                workspaceId = workspaceId,
                onTaskSelected = { taskId ->
                    navController.navigate(AppDestinations.TaskDetail.route(taskId))
                }
            )
        }

        composable(
            route = AppDestinations.TaskDetail.route,
            arguments = listOf(navArgument(AppDestinations.TaskDetail.ARG) { type = NavType.StringType })
        ) {
            // TaskDetailScreen — Fase 2
        }
    }
}
