package me.dcueto.zentrackapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import me.dcueto.zentrackapp.navigation.ZenTrackNavGraph
import me.dcueto.zentrackapp.ui.theme.ZenTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZenTrackTheme {
                Surface {
                    val navController = rememberNavController()
                    ZenTrackNavGraph(navController = navController)
                }
            }
        }
    }
}
