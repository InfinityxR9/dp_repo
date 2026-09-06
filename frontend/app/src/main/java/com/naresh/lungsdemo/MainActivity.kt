package com.naresh.lungsdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.compose.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.naresh.lungsdemo.ui.theme.LungsdemoTheme
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            LungsdemoTheme {
                Surface {
                  AppContent()
                }
            }
        }
    }
    @Composable
    fun AppContent() {
        var showSplash by remember { mutableStateOf(true) }

        if (showSplash) {
            SplashScreen {
                showSplash = false
            }
        } else {
            val currentUser: FirebaseUser? = auth.currentUser
            if (currentUser != null) {
                val intent=Intent(this,MainScreen::class.java)
                 startActivity(intent)// Replace with your main screen composable
            } else {
                LoginScreen(onGoogleSignInClick = { signInWithGoogle() })

            }
        }
    }

    @Composable
    fun SplashScreen(onSplashFinished: () -> Unit) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.aaloo))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = 1,
            speed = 8.0f// 
        )

        LaunchedEffect(progress) {
            if (progress == 1f) {
                onSplashFinished()
            }
        }

        Box(
            modifier = Modifier
                .background(Color.White), // Replace with your desired splash background color
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress }// Adjust size of the animation
            )
        }
    }
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Google sign-in failed", e)
            Toast.makeText(this, "Google Sign-In failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google Sign-In successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainScreen::class.java)
                    startActivity(intent)
                } else {
                    Log.w("GoogleSignIn", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
