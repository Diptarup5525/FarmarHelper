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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.farmer.helper.data.AppDatabase
import com.farmer.helper.data.User
import com.farmer.helper.data.UserDao
import com.farmer.helper.network.RetrofitClient
import com.farmer.helper.network.WeatherApiService
import com.farmer.helper.network.WeatherResponse
import com.farmer.helper.network.GeminiHelper
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        // Initialize Room database
        val db = AppDatabase.getDatabase(this)
        userDao = db.userDao()

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(
                        userDao = userDao,
                        onSignupClick = { navController.navigate("signup") },
                        onLoginSuccess = { navController.navigate("home") }
                    )
                }
                composable("signup") {
                    SignupScreen(
                        userDao = userDao,
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
fun LoginScreen(userDao: UserDao, onSignupClick: () -> Unit, onLoginSuccess: () -> Unit) {
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("KrishiBandhu Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    val user = userDao.login(mobile, password)
                    if (user != null) {
                        errorMessage = null
                        onLoginSuccess()
                    } else {
                        errorMessage = "Invalid mobile number or password"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSignupClick, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? Signup")
        }
    }
}

@Composable
fun SignupScreen(userDao: UserDao, onSignupComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Farmer Helper Signup", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                when {
                    name.isBlank() -> errorMessage = "Name cannot be empty"
                    mobile.length != 10 -> errorMessage = "Enter a valid 10-digit mobile number"
                    address.isBlank() -> errorMessage = "Address cannot be empty"
                    password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                    password != confirmPassword -> errorMessage = "Passwords do not match"
                    else -> {
                        scope.launch {
                            val user = User(name = name, mobile = mobile, address = address, password = password)
                            userDao.insertUser(user)
                            errorMessage = null
                            onSignupComplete()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
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
                weather = service.getWeather(city = "Kolkata", apiKey = "9bf9ac380eddc10a828be70e348cb015")
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
    val chatHistory = remember { mutableStateListOf<Pair<String, Boolean>>() } // Pair(message, isUser)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                chatHistory.add("You: $spokenText" to true)
                coroutineScope.launch {
                    val response = GeminiHelper.getResponse(spokenText)
                    chatHistory.add(response to false)
                    tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isNotBlank()) {
            val messageToSend = message.trim()
            chatHistory.add("You: $messageToSend" to true)
            userMessage = ""
            coroutineScope.launch {
                val response = GeminiHelper.getResponse(messageToSend)
                chatHistory.add(response to false)
                tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("KrishiBandhu AI Chat", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState
        ) {
            items(chatHistory) { (message, isUser) ->
                ChatBubble(message = message, isUser = isUser)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Auto-scroll to bottom
        LaunchedEffect(chatHistory.size) {
            if (chatHistory.isNotEmpty()) {
                listState.animateScrollToItem(chatHistory.size - 1)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("Ask about crops, weather, etc.") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { sendMessage(userMessage) }) {
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

@Composable
fun ChatBubble(message: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (isUser) {
                // User message → plain text
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                // AI message → render Markdown
                MarkdownText(
                    markdown = message,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
