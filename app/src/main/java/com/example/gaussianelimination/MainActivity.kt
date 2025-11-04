package com.example.gaussianelimination

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gaussianelimination.screens.GaussianEliminationScreen
import com.example.gaussianelimination.ui.theme.GaussianEliminationTheme
import com.example.gaussianelimination.viewmodel.GaussianEliminationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaussianEliminationTheme {
                val viewModel: GaussianEliminationViewModel = viewModel()
                GaussianEliminationScreen(viewModel)
            }
        }
    }
}