package com.naresh.lungsdemo

import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naresh.lungsdemo.MainScreen.Companion.BASE_URL
import com.naresh.lungsdemo.model.PlayRequest
import com.naresh.lungsdemo.network.RetrofitClient
import com.naresh.lungsdemo.network.SpeakerApiService
import com.naresh.lungsdemo.ui.theme.LungsdemoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Quiz : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            LungsdemoTheme {
                Surface {
                    QuizScreen()
                }
            }
        }
    }
}

val diseases = listOf(
    1 to "bronchial",
    2 to "vesicular",
    3 to "wheeze",
    4 to "crackle lung",
    5 to "stridor",
    6 to "pleural rub lung"
)

private fun generateQuestion(): Pair<
        Pair<Int, String>,
        List<Pair<Int, String>>
        > {
    val correctDisease = diseases.random()

    val options = diseases
        .filter { it != correctDisease }
        .shuffled()
        .take(3)
        .toMutableList()
        .apply {
            add(correctDisease)
        }
        .shuffled()

    return correctDisease to options
}

@Composable
fun QuizScreen() {
    val apiService: SpeakerApiService =
        RetrofitClient.getRetrofitInstance(BASE_URL)

    val initialQuestion = remember {
        generateQuestion()
    }

    var correctDisease by remember {
        mutableStateOf(initialQuestion.first)
    }

    var options by remember {
        mutableStateOf(initialQuestion.second)
    }

    var showResult by remember {
        mutableStateOf(false)
    }

    var resultMessage by remember {
        mutableStateOf("")
    }

    var score by remember {
        mutableIntStateOf(0)
    }

    var passedTime by remember {
        mutableIntStateOf(0)
    }

    var selectedOption by remember {
        mutableStateOf<Pair<Int, String>?>(null)
    }

    var questionCount by remember {
        mutableIntStateOf(0)
    }

    var showFinalScore by remember {
        mutableStateOf(false)
    }

    val coroutineScope = rememberCoroutineScope()

    fun playCurrentSound() {
        coroutineScope.launch {
            try {
                val response = apiService.playSound(
                    PlayRequest(
                        disease_number = correctDisease.first
                    )
                )

                if (response.isSuccessful) {
                    Log.d(
                        "QuizScreen",
                        "Play request successful for disease number: ${correctDisease.first}"
                    )
                } else {
                    Log.e(
                        "QuizScreen",
                        "Error sending play request: ${response.errorBody()?.string()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "QuizScreen",
                    "Exception in playSound: ${e.message}"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        playCurrentSound()
    }

    fun nextQuestion() {

        if (!showResult) return

        if (questionCount >= 4) {
            showFinalScore = true
            return
        }

        val nextQuestion = generateQuestion()

        correctDisease = nextQuestion.first
        options = nextQuestion.second

        selectedOption = null
        showResult = false
        passedTime = 0
        resultMessage = ""

        questionCount++

        playCurrentSound()
    }

    fun checkAnswer() {

        if (showResult) return

        if (selectedOption == null) {
            resultMessage = "Please select an answer first."
            return
        }

        showResult = true

        if (selectedOption == correctDisease) {
            score++
            resultMessage = "Correct!"
        } else {
            resultMessage =
                "Incorrect. The correct answer was: ${correctDisease.second}"
        }
    }


    LaunchedEffect(questionCount) {
        passedTime = 0

        while (passedTime < 30 && !showResult) {
            delay(1000L)

            passedTime++

            if (passedTime == 30 && !showResult) {
                resultMessage =
                    "Time's up! The correct answer was: ${correctDisease.second}"

                showResult = true
            }
        }
    }

    if (showFinalScore) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Quiz Finished! Your score: $score / 5",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    score = 0
                    questionCount = 0
                    showFinalScore = false

                    val newQuestion = generateQuestion()

                    correctDisease = newQuestion.first
                    options = newQuestion.second

                    selectedOption = null
                    showResult = false
                    passedTime = 0
                    resultMessage = ""

                    playCurrentSound()
                }
            ) {
                Text("Restart Quiz")
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier.height(40.dp)
            )

            Text(
                text = "Time: $passedTime s",
                fontSize = 24.sp,
                color = Color.Black
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text = "Identify the Disease Sound (${questionCount + 1}/5)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Surface(
                modifier = Modifier
                    .height(400.dp)
                    .fillMaxWidth(),
                color = colorResource(
                    R.color.option_container
                )
            ) {
                QuizWithSelectableOptions(
                    options = options,
                    selectedOption = selectedOption,
                    correctOption = correctDisease,
                    showResult = showResult,
                    onOptionSelected = {
                        selectedOption = it
                    }
                )
            }

            if (showResult) {
                Text(
                    text = resultMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        vertical = 16.dp
                    ),
                    color =
                        if (resultMessage == "Correct!") {
                            Color.Green
                        } else {
                            Color.Red
                        }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        nextQuestion()
                    },
                    enabled = showResult,
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .padding(5.dp),
                    shape =
                        RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        colorResource(R.color.black)
                    ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color.White,
                            contentColor =
                                colorResource(
                                    R.color.answer_select
                                )
                        )
                ) {
                    Text(
                        text = "Next",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        checkAnswer()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .padding(5.dp),
                    shape =
                        RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                colorResource(
                                    R.color.answer_select
                                ),
                            contentColor =
                                colorResource(
                                    R.color.white
                                )
                        )
                ) {
                    Text(
                        text = "Check Answer",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QuizWithSelectableOptions(
    options: List<Pair<Int, String>>,
    selectedOption: Pair<Int, String>?,
    correctOption: Pair<Int, String>,
    showResult: Boolean,
    onOptionSelected: (Pair<Int, String>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        options.forEach { option ->
            val isSelected =
                selectedOption == option

            val isCorrect =
                option == correctOption

            OutlinedButton(
                onClick = {
                    if (!showResult) {
                        onOptionSelected(option)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(65.dp)
                    .padding(vertical = 4.dp),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor =
                            when {
                                showResult &&
                                        isSelected &&
                                        isCorrect ->
                                    Color.Green

                                showResult &&
                                        isSelected ->
                                    Color.Red

                                else ->
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                            }
                    ),
                shape =
                    RoundedCornerShape(8.dp),
                border =
                    BorderStroke(
                        width = 2.dp,
                        color =
                            when {
                                showResult &&
                                        isSelected &&
                                        isCorrect ->
                                    Color.Green

                                showResult &&
                                        isSelected ->
                                    Color.Red

                                isSelected ->
                                    colorResource(
                                        R.color.answer_select
                                    )

                                else ->
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                                        .copy(alpha = 0.5f)
                            }
                    )
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = option.second,
                        modifier =
                            Modifier.weight(1f)
                    )

                    if (showResult && isSelected) {
                        Text(
                            text =
                                if (isCorrect) {
                                    "Correct"
                                } else {
                                    "Incorrect"
                                },
                            color =
                                if (isCorrect) {
                                    Color.Green
                                } else {
                                    Color.Red
                                },
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuizScreenPreview() {
    LungsdemoTheme {
        QuizScreen()
    }
}