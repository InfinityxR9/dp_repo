package com.naresh.lungsdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.naresh.lungsdemo.ui.theme.LungsdemoTheme

class RegisterUser : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            LungsdemoTheme {
                Surface {
                    RegisterScreen()
                }
            }
        }
    }
}

@Composable
fun RegisterScreen() {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }

    val auth = Firebase.auth

    Image(
        painter = painterResource(id = R.drawable.background),
        contentDescription = "Background",
        modifier = Modifier.fillMaxSize()
    )

    Column {
        Spacer(modifier = Modifier.fillMaxHeight(0.1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Create account",
                fontSize = 36.sp,
                color = colorResource(id = R.color.Login),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create an account to continue using the lung auscultation app",
                modifier = Modifier.fillMaxWidth(0.6f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(
                        width = 2.dp,
                        color = colorResource(id = R.color.Login),
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.Login),
                    unfocusedBorderColor = colorResource(id = R.color.Login),
                    focusedContainerColor = colorResource(id = R.color.email),
                    unfocusedContainerColor = colorResource(id = R.color.email)
                ),
                placeholder = {
                    Text(text = "Email")
                }
            )
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.06f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(
                        width = 2.dp,
                        color = colorResource(id = R.color.Login),
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.Login),
                    unfocusedBorderColor = colorResource(id = R.color.Login),
                    focusedContainerColor = colorResource(id = R.color.email),
                    unfocusedContainerColor = colorResource(id = R.color.email)
                ),
                placeholder = {
                    Text(text = "Password")
                },
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.06f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(
                        width = 2.dp,
                        color = colorResource(id = R.color.Login),
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.Login),
                    unfocusedBorderColor = colorResource(id = R.color.Login),
                    focusedContainerColor = colorResource(id = R.color.email),
                    unfocusedContainerColor = colorResource(id = R.color.email)
                ),
                placeholder = {
                    Text(text = "Confirm Password")
                },
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.08f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {

                    // Clean the email before validation and sending it to Firebase
                    val cleanEmail = email.trim().lowercase()

                    // Validate email
                    if (
                        !android.util.Patterns.EMAIL_ADDRESS
                            .matcher(cleanEmail)
                            .matches()
                    ) {
                        Toast.makeText(
                            context,
                            "Please enter a valid email address",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    // Validate password
                    if (password.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Please enter a password",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    // Validate password length
                    if (password.length < 6) {
                        Toast.makeText(
                            context,
                            "Password must be at least 6 characters",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    // Validate matching passwords
                    if (password != confirmPassword) {
                        Toast.makeText(
                            context,
                            "Passwords do not match",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    // Start loading
                    loading = true

                    // Temporary debugging
                    Log.d(
                        "RegisterDebug",
                        "Email being sent to Firebase: '$cleanEmail', length=${cleanEmail.length}"
                    )

                    // Create Firebase account
                    auth.createUserWithEmailAndPassword(
                        cleanEmail,
                        password
                    ).addOnCompleteListener { task ->

                        loading = false

                        if (task.isSuccessful) {

                            Toast.makeText(
                                context,
                                "Login successful",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(
                                context,
                                MainActivity::class.java
                            )

                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK

                            context.startActivity(intent)

                        } else {

                            Log.d(
                                "Login failed",
                                task.exception?.message.toString()
                            )

                            Toast.makeText(
                                context,
                                "Login failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },

                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.15f),

                shape = RoundedCornerShape(8.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.Login)
                )
            ) {
                if (loading) {

                    CircularProgressIndicator(
                        color = colorResource(id = R.color.white)
                    )

                } else {

                    Text(
                        text = "Sign up",
                        fontSize = 28.sp,
                        color = colorResource(id = R.color.white),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.08f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Already have an account")

                    addStyle(
                        style = SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = colorResource(id = R.color.Login)
                        ),
                        start = 0,
                        end = length
                    )
                },
                modifier = Modifier
                    .clickable {
                        val intent = Intent(context, LoginUser::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    }
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.11f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Or continue with",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "google",
                modifier = Modifier.size(25.dp)
            )

            Spacer(modifier = Modifier.fillMaxWidth(0.08f))

            Image(
                painter = painterResource(id = R.drawable.facebook),
                contentDescription = "facebook",
                modifier = Modifier.size(25.dp)
            )

            Spacer(modifier = Modifier.fillMaxWidth(0.08f))

            Image(
                painter = painterResource(id = R.drawable.microsoft),
                contentDescription = "microsoft",
                modifier = Modifier.size(25.dp)
            )
        }
    }
}