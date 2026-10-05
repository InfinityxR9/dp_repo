package com.naresh.lungsdemo

import android.content.Intent
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.naresh.lungsdemo.data.repository.TrainerRepository
import com.naresh.lungsdemo.network.RetrofitClient
import com.naresh.lungsdemo.network.SpeakerApiService
import com.naresh.lungsdemo.ui.theme.LungsdemoTheme
import com.naresh.lungsdemo.viewmodel.TrainerViewModel
import com.naresh.lungsdemo.viewmodel.TrainerViewModelFactory
import com.naresh.lungsdemo.model.TrainerMode
import com.naresh.lungsdemo.model.LungSound


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

    val repository = TrainerRepository(apiService)

    val factory = TrainerViewModelFactory(repository)

    val viewModel: TrainerViewModel = viewModel(
        factory = factory
    )

    val selectedSound by viewModel.selectedSound.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    val currentStatus by viewModel.currentStatus.collectAsState()
    val isPiConnected by viewModel.isPiConnected.collectAsState()
    val trainerMode by viewModel.trainerMode.collectAsState()

    var masterVolume by remember {
        mutableFloatStateOf(1f)
    }

    val sounds = listOf(
        LungSound("bronchial", "Bronchial"),
        LungSound("vesicular", "Vesicular"),
        LungSound("wheeze", "Wheeze"),
        LungSound("crackle", "Crackle"),
        LungSound("stridor", "Stridor"),
        LungSound("pleural_rub", "Pleural Rub"),
        LungSound("ronchi", "Ronchi")
    )


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(R.color.background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 60.dp,
                    start = 16.dp,
                    end = 16.dp
                ),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ---------------------------------------------------------
            // HEADER
            // ---------------------------------------------------------

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "LUNG LAB",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "Auscultation Trainer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "Train. Listen. Diagnose.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                if (isPiConnected != null) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "●",
                            color = if (isPiConnected == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },

                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        Text(
                            text = if (isPiConnected == true) {
                                "Raspberry Pi Connected"
                            } else {
                                "Raspberry Pi Not Connected"
                            },

                            style = MaterialTheme.typography.bodySmall,

                            fontWeight = FontWeight.Medium,

                            color = if (isPiConnected == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Training Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (trainerMode == TrainerMode.TRAINING) {
                            "Location is shown while you train."
                        } else {
                            "Location is hidden. Identify where the sound is playing."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeButton(
                            text = "Training",
                            selected = trainerMode == TrainerMode.TRAINING,
                            onClick = {
                                viewModel.setTrainerMode(TrainerMode.TRAINING)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ModeButton(
                            text = "Test",
                            selected = trainerMode == TrainerMode.TEST,
                            onClick = {
                                viewModel.setTrainerMode(TrainerMode.TEST)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }


            // ---------------------------------------------------------
            // NOW PLAYING
            // ---------------------------------------------------------

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(18.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = selectedSound?.name ?: "No sound selected",

                        style = MaterialTheme.typography.titleLarge,

                        fontWeight = FontWeight.Bold,

                        color = if (selectedSound != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = if (selectedSound != null) {
                            "Currently playing"
                        } else {
                            "Select a lung sound to begin"
                        },

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedSound != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (trainerMode == TrainerMode.TEST) {
                                "Identify the location where this sound is playing."
                            } else if (selectedSound?.location != null) {
                                "Location: ${selectedSound?.location?.displayName}"
                            } else {
                                "Location not configured"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (trainerMode == TrainerMode.TEST) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }


            // ---------------------------------------------------------
            // LUNG SOUNDS
            // ---------------------------------------------------------

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "LUNG SOUNDS",

                    style = MaterialTheme.typography.labelLarge,

                    fontWeight = FontWeight.SemiBold,

                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                sounds.chunked(2).forEach { rowSounds ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),

                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        rowSounds.forEach { sound ->

                            DiseaseButton(
                                diseaseName = sound.name,

                                isSelected = selectedSound?.id == sound.id,

                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.playSound(sound.id, sound.name)
                            }
                        }

                        if (rowSounds.size == 1) {

                            Spacer(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // AUDIO
            // ---------------------------------------------------------

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "AUDIO",

                    style = MaterialTheme.typography.labelLarge,

                    fontWeight = FontWeight.SemiBold,

                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(18.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),

                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        )
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "Master Volume",

                                style =
                                    MaterialTheme.typography.titleMedium,

                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Text(
                                text =
                                    "${(masterVolume * 100).toInt()}%",

                                style =
                                    MaterialTheme.typography.titleMedium,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = masterVolume,

                            onValueChange = { volume ->
                                masterVolume = volume
                            },

                            onValueChangeFinished = {
                                viewModel.setVolume(masterVolume)
                            },

                            valueRange = 0f..1f,

                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }


            // ---------------------------------------------------------
            // STOP SOUND
            // ---------------------------------------------------------

            OutlinedButton(
                onClick = {
                    viewModel.stopSound()
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),

                shape = RoundedCornerShape(16.dp),

                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error
                ),

                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {

                Text(
                    text = "Stop Sound",
                    fontWeight = FontWeight.SemiBold
                )
            }


            // ---------------------------------------------------------
            // CHECK RASPBERRY PI CONNECTION
            // ---------------------------------------------------------

            OutlinedButton(
                onClick = {
                    viewModel.checkConnection()
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),

                shape = RoundedCornerShape(16.dp),

                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                ),

                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {

                Text(
                    text = "Check Raspberry Pi Connection",
                    fontWeight = FontWeight.SemiBold
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

                shape = RoundedCornerShape(16.dp),

                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,

                    contentColor =
                        MaterialTheme.colorScheme.onPrimary
                )
            ) {

                Text(
                    text = "Take Quiz",
                    fontWeight = FontWeight.SemiBold
                )
            }


            // ---------------------------------------------------------
            // STATUS
            // ---------------------------------------------------------

            currentStatus?.let {

                Text(
                    text = it,

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            // ---------------------------------------------------------
            // LOADING
            // ---------------------------------------------------------

            if (isLoading) {

                CircularProgressIndicator()
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Text(
            text = text,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            }
        )
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

    Card(
        modifier = modifier.height(62.dp),

        onClick = onClick,

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {

                MaterialTheme.colorScheme.primaryContainer

            } else {

                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.55f)
            }
        ),

        border = BorderStroke(
            width = if (isSelected) {
                2.dp
            } else {
                1.dp
            },

            color = if (isSelected) {

                MaterialTheme.colorScheme.primary

            } else {

                MaterialTheme.colorScheme.outline
            }
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = if (isSelected) {
                    "●  $diseaseName"
                } else {
                    diseaseName
                },

                modifier = Modifier.fillMaxWidth(),

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.SemiBold,

                color = if (isSelected) {

                    MaterialTheme.colorScheme.primary

                } else {

                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
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