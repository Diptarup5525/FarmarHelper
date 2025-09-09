package com.farmer.helper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.farmer.helper.network.RetrofitClient
import com.farmer.helper.network.WeatherApiService
import com.farmer.helper.network.WeatherResponse
import com.farmer.helper.network.GeminiHelper
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(
                        onSignupClick = { navController.navigate("signup") },
                        onLoginClick = { navController.navigate("home") }
                    )
                }
                composable("signup") {
                    SignupScreen(
                        onSignupComplete = { navController.navigate("home") }
                    )
                }
                composable("home") {
                    HomeScreen(tts = tts)
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun LoginScreen(onSignupClick: () -> Unit, onLoginClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Farmer Helper Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSignupClick, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? Signup")
        }
    }
}

@Composable
fun SignupScreen(onSignupComplete: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Farmer Helper Signup", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onSignupComplete, modifier = Modifier.fillMaxWidth()) {
            Text("Signup")
        }
    }
}

@Composable
fun HomeScreen(tts: TextToSpeech?) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Weather", "Market Prices", "Schemes", "AI Chat")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> WeatherScreen()
            1 -> MarketPricesScreen()
            2 -> SchemesScreen()
            3 -> ChatScreen(tts = tts)
        }
    }
}

@Composable
fun WeatherScreen() {
    var weather by remember { mutableStateOf<WeatherResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val service = RetrofitClient.retrofit.create(WeatherApiService::class.java)
                weather = service.getWeather(
                    city = "Kolkata",
                    apiKey = "9bf9ac380eddc10a828be70e348cb015"
                )
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("Weather Info", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        when {
            weather != null -> {
                Text("Temperature: ${weather!!.main.temp}°C")
                Text("Condition: ${weather!!.weather.firstOrNull()?.description ?: "N/A"}")
            }
            errorMessage != null -> {
                Text("Error: $errorMessage")
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun MarketPricesScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Market Prices", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Rice: ₹50/kg\nWheat: ₹40/kg\nCorn: ₹30/kg")
    }
}

@Composable
fun SchemesScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Government Schemes", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("PM-Kisan Yojana\nCrop Insurance\nSoil Health Card")
    }
}

@Composable
fun ChatScreen(tts: TextToSpeech?) {
    var userMessage by remember { mutableStateOf("") }
    val chatHistory = remember { mutableStateListOf<String>() }
    val coroutineScope = rememberCoroutineScope()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                chatHistory.add("You: $spokenText")
                coroutineScope.launch {
                    val response = GeminiHelper.getResponse(spokenText)
                    chatHistory.add("AI: $response")
                    tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Farmer Helper AI Chat", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp)) {
            chatHistory.forEach { message ->
                Text(message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("Ask about crops, weather, etc.") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (userMessage.isNotBlank()) {
                    chatHistory.add("You: $userMessage")
                    coroutineScope.launch {
                        val response = GeminiHelper.getResponse(userMessage)
                        chatHistory.add("AI: $response")
                        tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                    userMessage = ""
                }
            }) {
                Text("Send")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechLauncher.launch(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎤 Speak")
        }
    }
}

