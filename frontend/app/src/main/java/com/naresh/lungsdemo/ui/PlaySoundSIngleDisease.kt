package com.naresh.lungsdemo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.naresh.lungsdemo.R
import com.naresh.lungsdemo.ui.ui.theme.LungsdemoTheme

class PlaySoundSIngleDisease : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LungsdemoTheme {
               LottieExample()
            }
        }
    }
}
@Composable
fun LottieExample() {
    // Load Lottie composition from the raw folder
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.aaloo))

    // Control animation progress (you can modify this as needed)
    val progress by animateLottieCompositionAsState(composition)

    // Display the Lottie animation
    Box(modifier = Modifier.fillMaxSize()) {
        LottieAnimation(
            composition = composition,
            progress = progress
        )
    }
}

@Preview()
@Composable
fun animationPreview(){
    LungsdemoTheme {
        LottieExample()
    }
}