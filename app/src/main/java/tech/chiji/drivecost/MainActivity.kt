package tech.chiji.drivecost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import tech.chiji.drivecost.ui.DriveCostRoute
import tech.chiji.drivecost.ui.theme.DriveCostTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriveCostTheme {
                DriveCostRoute()
            }
        }
    }
}
