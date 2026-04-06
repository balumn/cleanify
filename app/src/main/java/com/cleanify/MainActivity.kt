package com.cleanify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.cleanify.navigation.NavGraph
import com.cleanify.ui.theme.CleanifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            CleanifyAppRoot()
        }
    }
}

@Composable
private fun CleanifyAppRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    CleanifyTheme {
        NavGraph(
            navController = navController,
            modifier = modifier,
        )
    }
}

