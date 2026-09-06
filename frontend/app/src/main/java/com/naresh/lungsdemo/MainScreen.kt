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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class MainScreen : ComponentActivity() {

    companion object {
        const val BASE_URL = "http://10.127.96.226:5000/"
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

fun setVolume(baseUrl: String, volume: Float) {
    val client = OkHttpClient()

    val json = """{ "volume": $volume }"""

    val body = json.toRequestBody(
        "application/json".toMediaType()
    )

    val request = Request.Builder()
        .url("$baseUrl/volume")
        .post(body)
        .build()

    client.newCall(request).enqueue(
        object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {
                e.printStackTrace()
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {
                response.close()
            }
        }
    )
}

@Composable
fun SpeakerControlScreen() {
    val context = LocalContext.current

    val apiService: SpeakerApiService =
        RetrofitClient.getRetrofitInstance(MainScreen.BASE_URL)

    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var currentStatus by remember { mutableStateOf<String?>(null) }
    var volume by remember { mutableStateOf(0.15f) }

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

            val diseases = listOf(
                1 to "Bronchial",
                2 to "Vesicular",
                3 to "Wheeze",
                4 to "Crackle lung",
                5 to "Stridor",
                6 to "Pleural rub lung"
            )

            diseases.forEach { (id, name) ->
                DiseaseButton(
                    diseaseName = name
                ) {
                    coroutineScope.launch {
                        isLoading = true

                        try {
                            apiService.playSound(
                                PlayRequest(id)
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

            Text(
                text = "Volume Control (Stethoscope)",
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (volume > 0.05f) {
                            volume -= 0.05f
                            setVolume(
                                MainScreen.BASE_URL,
                                volume
                            )
                        }
                    }
                ) {
                    Text("➖")
                }

                Text(
                    text = String.format(
                        "%.2f",
                        volume
                    ),
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        if (volume < 1.0f) {
                            volume += 0.05f
                            setVolume(
                                MainScreen.BASE_URL,
                                volume
                            )
                        }
                    }
                ) {
                    Text("➕")
                }
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