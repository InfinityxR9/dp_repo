
package com.naresh.lungsdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.naresh.lungsdemo.model.TrainerMode
import com.naresh.lungsdemo.ui.theme.LungsdemoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LungsdemoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent()
                }
            }
        }
    }

    @Composable
    fun AppContent() {
        var showHome by remember { mutableStateOf(false) }

        if (showHome) {
            HomeScreen(
                onTrainingClick = {
                    openTrainer(TrainerMode.TRAINING)
                },
                onTestClick = {
                    openTrainer(TrainerMode.TEST)
                },
                onQuizClick = {
                    startActivity(Intent(this, Quiz::class.java))
                }
            )
        } else {
            SplashScreen {
                showHome = true
            }
        }
    }

    private fun openTrainer(mode: TrainerMode) {
        val intent = Intent(this, MainScreen::class.java).apply {
            putExtra(MainScreen.EXTRA_TRAINER_MODE, mode.name)
        }
        startActivity(intent)
    }

    @Composable
    fun SplashScreen(onSplashFinished: () -> Unit) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.aaloo)
        )

        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = 1,
            speed = 8.0f
        )

        LaunchedEffect(progress) {
            if (progress >= 1f) {
                onSplashFinished()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress }
            )
        }
    }
}
