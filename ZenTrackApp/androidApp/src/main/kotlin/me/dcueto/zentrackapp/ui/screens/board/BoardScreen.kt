package me.dcueto.zentrackapp.ui.screens.board

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
fun BoardScreen(
    workspaceId: String,
    onTaskSelected: (taskId: String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Board — $workspaceId", style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun BoardScreenPreview() {
    ZenTrackTheme { BoardScreen(workspaceId = "ws-preview", onTaskSelected = {}) }
}
