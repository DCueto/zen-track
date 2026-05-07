package me.dcueto.zentrackapp.ui.screens.workspaces

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.dcueto.zentrackapp.ui.theme.ZenTrackTheme

@Composable
fun WorkspacesScreen(onWorkspaceSelected: (workspaceId: String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Workspaces — Fase 2", style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkspacesScreenPreview() {
    ZenTrackTheme { WorkspacesScreen(onWorkspaceSelected = {}) }
}
