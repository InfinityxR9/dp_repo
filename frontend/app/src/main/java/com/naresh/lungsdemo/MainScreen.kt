//package com.naresh.lungsdemo
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.colorResource
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.Font
//import androidx.compose.ui.text.font.FontStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.core.app.ActivityCompat.startActivityForResult
//import androidx.core.content.ContextCompat.startActivity
//import com.naresh.lungsdemo.MainScreen.Companion.BASE_URL
//import com.naresh.lungsdemo.model.PlayRequest
//import com.naresh.lungsdemo.network.RetrofitClient
//import com.naresh.lungsdemo.network.SpeakerApiService
//import com.naresh.lungsdemo.ui.theme.LungsdemoTheme
//import kotlinx.coroutines.launch
//import retrofit2.HttpException
//import java.io.IOException
//
//class MainScreen : ComponentActivity() {
//    companion object {
//        const val BASE_URL = "http://10.127.96.226:5000" // Replace with your Raspberry Pi's IP
//    }
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            LungsdemoTheme {
//               Surface {
//                   SpeakerControlScreen()
//               }
//            }
//        }
//    }
//}
//
//@Composable
//fun SpeakerControlScreen() {
//    val context = LocalContext.current
//    val apiService: SpeakerApiService = RetrofitClient.getRetrofitInstance(BASE_URL)
//    val coroutineScope = rememberCoroutineScope()
//
//    var isLoading by remember { mutableStateOf(false) }
//    var currentStatus by remember { mutableStateOf<String?>(null) }
//
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = colorResource(R.color.background)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Spacer(modifier=Modifier.height(60.dp))
//            Text(
//                text = "Disease Sound Controller",
//                style = MaterialTheme.typography.headlineSmall,
//                fontStyle = FontStyle.Normal,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(bottom = 24.dp)
//            )
//
//            // Disease buttons
//            val diseases = listOf(
//                1 to "bronchial",
//                2 to "vesicular",
//                3 to "wheeze",
//                4 to "crackle lung",
//                5 to "stridor",
//                6 to "pleaural rub lung"
//            )
//            diseases.forEach { (id, name) ->
//                DiseaseButton(diseaseName = name.capitalize()) {
//                    coroutineScope.launch {
//                        isLoading = true
//                        try {
//                            val response = apiService.playSound(PlayRequest(id))
//                            Log.d("response",response.toString())
//                            if (response.isSuccessful) {
//                                val body = response.body()
//                                if (body != null && body.error == null) {
//                                    Log.d("success","connection ho gyaa bhosdika")
//                                    Toast.makeText(
//                                        context,
//                                        body.status ?: "Playing sound.",
//                                        Toast.LENGTH_LONG
//                                    ).show()
//                                } else {
//                                    Toast.makeText(
//                                        context,
//                                        "Error: ${body?.error}",
//                                        Toast.LENGTH_LONG
//                                    ).show()
//                                    Log.e("SpeakerControl", "Error: ${body?.error}")
//                                }
//                            } else {
//                                Toast.makeText(
//                                    context,
//                                    "Failed to play sound. Code: ${response.code()}",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                                Log.e("SpeakerControl", "Failed to play sound. Code: ${response.code()}")
//                            }
//                        } catch (e: IOException) {
//                            Toast.makeText(
//                                context,
//                                "Network Error: ${e.localizedMessage}",
//                                Toast.LENGTH_LONG
//                            ).show()
//                            Log.e("SpeakerControl", "Network Error: ${e.localizedMessage}", e)
//                        } catch (e: HttpException) {
//                            Toast.makeText(
//                                context,
//                                "HTTP Error: ${e.localizedMessage}",
//                                Toast.LENGTH_LONG
//                            ).show()
//                            Log.e("SpeakerControl", "HTTP Error: ${e.localizedMessage}", e)
//                        } catch (e: Exception) {
//                            Toast.makeText(
//                                context,
//                                "Unexpected Error: ${e.localizedMessage}",
//                                Toast.LENGTH_LONG
//                            ).show()
//                            Log.e("SpeakerControl", "Unexpected Error: ${e.localizedMessage}", e)
//                        } finally {
//                            isLoading = false
//                        }
//                    }
//                }
//            }
//
//            // Status Button
//            Button(
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = colorResource(R.color.checkStatus)
//                ),
//                onClick = {
//                    coroutineScope.launch {
//                        isLoading = true
//                        try {
//                            val response = apiService.getStatus()
//                            if (response.isSuccessful) {
//                                val body = response.body()
//                                currentStatus =
//                                    "Playing: ${if (body?.playing == true) "Yes" else "No"}\n" +
//                                            "Current Speaker: ${body?.current_speaker ?: "None"}"
//                            } else {
//                                currentStatus = "Failed to fetch status. Code: ${response.code()}"
//                                Log.e("SpeakerControl", "Failed to fetch status. Code: ${response.code()}")
//                            }
//                        } catch (e: IOException) {
//                            currentStatus = "Network Error: ${e.localizedMessage}"
//                            Log.e("SpeakerControl", "Network Error: ${e.localizedMessage}", e)
//                        } catch (e: HttpException) {
//                            currentStatus = "HTTP Error: ${e.localizedMessage}"
//                            Log.e("SpeakerControl", "HTTP Error: ${e.localizedMessage}", e)
//                        } catch (e: Exception) {
//                            currentStatus = "Unexpected Error: ${e.localizedMessage}"
//                            Log.e("SpeakerControl", "Unexpected Error: ${e.localizedMessage}", e)
//                        } finally {
//                            isLoading = false
//                        }
//                    }
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(50.dp)
//            ) {
//                Text(text = "Check Status",
//                    fontWeight = FontWeight.Bold,
//                    fontStyle = FontStyle.Normal,
//                    color = Color.Black,
//                    style= MaterialTheme.typography.headlineSmall)
//            }
//            Button(onClick = {
//                val intent=Intent(context,Quiz::class.java)
//                context.startActivity(intent)
//            }, shape = RoundedCornerShape(12.dp),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(50.dp),
//                colors= ButtonDefaults.buttonColors(
//                    containerColor = colorResource(R.color.buttonColor)
//                )) {
//                Text("Take Quiz",
//                    style=MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//
//            // Display current status
//            currentStatus?.let {
//                Text(text = it, modifier = Modifier.padding(top = 8.dp))
//            }
//
//            // Loading Indicator
//            if (isLoading) {
//                CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
//            }
//        }
//    }
//}
//
//
//@Composable
//fun DiseaseButton(diseaseName: String, onClick: () -> Unit) {
//    Button(
//        onClick = onClick,
//        shape = RoundedCornerShape(12.dp),
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(50.dp),
//        colors= ButtonDefaults.buttonColors(
//            containerColor = colorResource(R.color.buttonColor)
//        )
//    ) {
//        Text(text = diseaseName,
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.Bold)
//    }
//}
//
//@Preview
//@Composable
//fun PreviewOf(){
//    LungsdemoTheme {
//        SpeakerControlScreen()
//    }
//}












package com.naresh.lungsdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

class MainScreen : ComponentActivity() {

    companion object {
        // 🔴 YAHI PI KA IP RAHEGA
        const val BASE_URL = "http://10.127.96.226:5000"
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

/* -------------------- VOLUME API CALL -------------------- */
fun setVolume(baseUrl: String, volume: Float) {
    val client = OkHttpClient()

    val json = """{ "volume": $volume }"""
    val body = json.toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url("$baseUrl/volume")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.close()
        }
    })
}

/* -------------------- UI -------------------- */
@Composable
fun SpeakerControlScreen() {

    val context = LocalContext.current
    val apiService: SpeakerApiService =
        RetrofitClient.getRetrofitInstance(MainScreen.BASE_URL)

    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var currentStatus by remember { mutableStateOf<String?>(null) }

    // 🔊 Default LOW volume (stethoscope ke liye perfect)
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

            /* -------- DISEASE BUTTONS -------- */
            val diseases = listOf(
                1 to "Bronchial",
                2 to "Vesicular",
                3 to "Wheeze",
                4 to "Crackle lung",
                5 to "Stridor",
                6 to "Pleural rub lung"
            )

            diseases.forEach { (id, name) ->
                DiseaseButton(diseaseName = name) {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            apiService.playSound(PlayRequest(id))
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

            /* -------- VOLUME CONTROL -------- */
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
                            setVolume(MainScreen.BASE_URL, volume)
                        }
                    }
                ) {
                    Text("➖")
                }

                Text(
                    text = String.format("%.2f", volume),
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        if (volume < 1.0f) {
                            volume += 0.05f
                            setVolume(MainScreen.BASE_URL, volume)
                        }
                    }
                ) {
                    Text("➕")
                }
            }

            /* -------- STATUS BUTTON -------- */
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.checkStatus)
                ),
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val response = apiService.getStatus()
                            if (response.isSuccessful) {
                                val body = response.body()
                                currentStatus =
                                    "Playing: ${body?.playing}\nSpeaker: ${body?.current_speaker}"
                            }
                        } catch (e: Exception) {
                            currentStatus = "Error: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    "Check Status",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            /* -------- QUIZ -------- */
            Button(
                onClick = {
                    context.startActivity(
                        Intent(context, Quiz::class.java)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.buttonColor)
                )
            ) {
                Text(
                    "Take Quiz",
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

/* -------------------- BUTTON COMPONENT -------------------- */
@Composable
fun DiseaseButton(diseaseName: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.buttonColor)
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
