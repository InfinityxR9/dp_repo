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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.naresh.lungsdemo.model.VolumeRequest
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

    var isLoading by remember {
        mutableStateOf(false)
    }

    var currentStatus by remember {
        mutableStateOf<String?>(null)
    }

    var masterMultiplier by remember {
        mutableFloatStateOf(1f)
    }

    var selectedSoundId by remember {
        mutableStateOf<String?>(null)
    }

    var selectedSoundName by remember {
        mutableStateOf<String?>(null)
    }


    val sounds = listOf(
        "bronchial" to "Bronchial",
        "vesicular" to "Vesicular",
        "wheeze" to "Wheeze",
        "crackle" to "Crackle",
        "stridor" to "Stridor",
        "pleural_rub" to "Pleural Rub",
        "ronchi" to "Ronchi"
    )


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(R.color.background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Spacer(
                modifier = Modifier.height(50.dp)
            )


            // ---------------------------------------------------------
            // TITLE
            // ---------------------------------------------------------

            Text(
                text = "Disease Sound Controller",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal
            )


            // ---------------------------------------------------------
            // DISEASE SOUND BUTTONS
            // ---------------------------------------------------------

            sounds.chunked(2).forEach { rowSounds ->

                if (rowSounds.size == 1) {

                    val (id, name) = rowSounds.first()

                    DiseaseButton(
                        diseaseName = name,
                        isSelected = selectedSoundId == id,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        coroutineScope.launch {

                            isLoading = true

                            try {

                                val response =
                                    apiService.playSound(
                                        PlayRequest(
                                            soundId = id
                                        )
                                    )

                                if (response.isSuccessful) {

                                    selectedSoundId = id
                                    selectedSoundName = name

                                    Toast.makeText(
                                        context,
                                        response.body()?.message
                                            ?: "$name sound started",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {

                                    Toast.makeText(
                                        context,
                                        "Failed to play $name: ${response.code()}",
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
                    }

                } else {

                    Row(
                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        rowSounds.forEach { (id, name) ->

                            DiseaseButton(
                                diseaseName = name,
                                isSelected = selectedSoundId == id,
                                modifier = Modifier.weight(1f)
                            ) {

                                coroutineScope.launch {

                                    isLoading = true

                                    try {

                                        val response =
                                            apiService.playSound(
                                                PlayRequest(
                                                    soundId = id
                                                )
                                            )

                                        if (response.isSuccessful) {

                                            selectedSoundId = id
                                            selectedSoundName = name

                                            Toast.makeText(
                                                context,
                                                response.body()?.message
                                                    ?: "$name sound started",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        } else {

                                            Toast.makeText(
                                                context,
                                                "Failed to play $name: ${response.code()}",
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
                            }
                        }
                    }
                }
            }


            // ---------------------------------------------------------
            // CURRENTLY PLAYING
            // ---------------------------------------------------------

            Text(
                text = selectedSoundName?.let {
                    "Currently Playing: $it"
                } ?: "Currently Playing: None",

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // ---------------------------------------------------------
            // MASTER VOLUME
            // ---------------------------------------------------------

            Text(
                text =
                    "Master Volume: ${(masterMultiplier * 100).toInt()}%",

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold
            )


            Slider(
                value = masterMultiplier,

                onValueChange = { volume ->
                    masterMultiplier = volume
                },

                onValueChangeFinished = {

                    coroutineScope.launch {

                        try {

                            val response =
                                apiService.setVolume(
                                    VolumeRequest(
                                        volume = masterMultiplier
                                    )
                                )

                            if (response.isSuccessful) {

                                currentStatus =
                                    "Master volume set to " +
                                            "${(masterMultiplier * 100).toInt()}%"

                            } else {

                                Toast.makeText(
                                    context,
                                    "Failed to set volume: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                context,
                                "Volume connection error: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },

                valueRange = 0f..1f,

                modifier =
                    Modifier.fillMaxWidth()
            )


            // ---------------------------------------------------------
            // STOP SOUND
            // ---------------------------------------------------------

            Button(
                onClick = {

                    coroutineScope.launch {

                        isLoading = true

                        try {

                            val response =
                                apiService.stopSound()

                            if (response.isSuccessful) {

                                selectedSoundId = null
                                selectedSoundName = null

                                Toast.makeText(
                                    context,
                                    response.body()?.message
                                        ?: "Sound stopped",
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


            // ---------------------------------------------------------
            // CHECK STATUS
            // ---------------------------------------------------------

            Button(
                onClick = {

                    coroutineScope.launch {

                        isLoading = true

                        try {

                            val response =
                                apiService.getStatus()

                            if (response.isSuccessful) {

                                val status =
                                    response.body()

                                val message = """
                                    Running: ${status?.running}
                                    Active Sensor: ${status?.activeSensor ?: "None"}
                                    Pressure: ${status?.pressure ?: 0}%
                                    Sound: ${status?.soundId ?: "None"}
                                    Audio Volume: ${status?.audioVolume ?: 0f}
                                    Master Volume: ${((status?.masterMultiplier ?: 0f) * 100).toInt()}%
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


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // ---------------------------------------------------------
            // CHECK RASPBERRY PI CONNECTION
            // ---------------------------------------------------------

            Button(
                onClick = {

                    coroutineScope.launch {

                        isLoading = true

                        try {

                            val response =
                                apiService.health()

                            if (response.isSuccessful) {

                                val health =
                                    response.body()

                                Toast.makeText(
                                    context,
                                    health?.message
                                        ?: "Raspberry Pi Connected",
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
                    text =
                        "Check Raspberry Pi Connection",

                    fontWeight =
                        FontWeight.Bold
                )
            }


            // ---------------------------------------------------------
            // QUIZ
            // ---------------------------------------------------------

            Button(
                onClick = {

                    context.startActivity(
                        Intent(
                            context,
                            Quiz::class.java
                        )
                    )
                },

                shape =
                    RoundedCornerShape(12.dp),

                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            colorResource(
                                R.color.buttonColor
                            )
                    )
            ) {

                Text(
                    text = "Take Quiz",
                    fontWeight = FontWeight.Bold
                )
            }


            currentStatus?.let {

                Text(
                    text = it
                )
            }


            if (isLoading) {

                CircularProgressIndicator()
            }


            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}


// -----------------------------------------------------------------------------
// DISEASE BUTTON
// -----------------------------------------------------------------------------

@Composable
fun DiseaseButton(
    diseaseName: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        shape =
            RoundedCornerShape(12.dp),

        modifier =
            modifier.height(50.dp),

        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (isSelected) {
                        Color(0xFF1E3A8A)
                    } else {
                        colorResource(
                            R.color.buttonColor
                        )
                    }
            )
    ) {

        Text(
            text = diseaseName,
            fontWeight = FontWeight.Bold
        )
    }
}


// -----------------------------------------------------------------------------
// PREVIEW
// -----------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun PreviewUI() {

    LungsdemoTheme {

        SpeakerControlScreen()
    }
}