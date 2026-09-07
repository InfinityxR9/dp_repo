package com.naresh.lungsdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naresh.lungsdemo.model.PlayRequest
import com.naresh.lungsdemo.network.RetrofitClient
import com.naresh.lungsdemo.network.SpeakerApiService
import com.naresh.lungsdemo.ui.theme.LungsdemoTheme
import kotlinx.coroutines.launch

class MainScreen : ComponentActivity() {

    companion object {
        const val BASE_URL = "http://10.230.233.226:8000/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            LungsdemoTheme {
                Surface {
                    SpeakerControlScreen()
                }
            }
        }
    }
}

@Composable
fun SpeakerControlScreen() {
    val context = LocalContext.current

    val apiService: SpeakerApiService =
        RetrofitClient.getRetrofitInstance(MainScreen.BASE_URL)

    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var currentStatus by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(R.color.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            Text(
                text = "Disease Sound Controller",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal
            )

            val sounds = listOf(
                "bronchial" to "Bronchial",
                "vesicular" to "Vesicular",
                "wheeze" to "Wheeze",
                "crackle" to "Crackle",
                "stridor" to "Stridor",
                "pleural_rub" to "Pleural Rub",
                "ronchi" to "Ronchi"
            )

            sounds.forEach { (id, name) ->
                DiseaseButton(
                    diseaseName = name
                ) {
                    coroutineScope.launch {
                        isLoading = true

                        try {
                            apiService.playSound(
                                PlayRequest(soundId = id)
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Error: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true

                        try {
                            val response = apiService.stopSound()

                            if (response.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    response.body()?.message ?: "Sound stopped",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to stop sound: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                context,
                                "Connection error: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()

                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Stop Sound",
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true

                        try {
                            val response = apiService.getStatus()

                            if (response.isSuccessful) {

                                val status = response.body()

                                val message = """
                        Running: ${status?.running}
                        Active Sensor: ${status?.activeSensor ?: "None"}
                        Pressure: ${status?.pressure ?: 0}%
                        Sound: ${status?.soundId ?: "None"}
                        Audio Volume: ${status?.audioVolume ?: 0f}
                    """.trimIndent()

                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()

                            } else {

                                Toast.makeText(
                                    context,
                                    "Failed to get status: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                context,
                                "Connection error: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()

                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Check Status",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true

                        try {
                            val response = apiService.health()

                            if (response.isSuccessful) {

                                val health = response.body()

                                Toast.makeText(
                                    context,
                                    health?.message ?: "Raspberry Pi Connected",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                Toast.makeText(
                                    context,
                                    "Pi responded with error: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                context,
                                "Cannot connect to Raspberry Pi: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()

                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Check Raspberry Pi Connection",
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            context,
                            Quiz::class.java
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        colorResource(R.color.buttonColor)
                )
            ) {
                Text(
                    text = "Take Quiz",
                    fontWeight = FontWeight.Bold
                )
            }

            currentStatus?.let {
                Text(text = it)
            }

            if (isLoading) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun DiseaseButton(
    diseaseName: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor =
                colorResource(R.color.buttonColor)
        )
    ) {
        Text(
            text = diseaseName,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUI() {
    LungsdemoTheme {
        SpeakerControlScreen()
    }
}