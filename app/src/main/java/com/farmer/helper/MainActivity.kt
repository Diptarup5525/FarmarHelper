package com.farmer.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.farmer.helper.data.AppDatabase
import com.farmer.helper.data.User
import com.farmer.helper.data.UserDao
import com.farmer.helper.network.GeminiHelper
import com.farmer.helper.network.RetrofitClient
import com.farmer.helper.network.WeatherApiService
import com.farmer.helper.network.WeatherResponse
import com.farmer.helper.ui.theme.FarmerHelperTheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
            FarmerHelperTheme {
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
                        tts?.let { HomeScreen(tts = it) }
                    }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("KrishiBandhu Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary
            )
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("KrishiBandhu Signup", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary
            )
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

// keep WeatherScreen, MarketPricesScreen, SchemesScreen as before
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Weather Information", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            weather != null -> {
                val w = weather!!

                // Weather icon
                val iconUrl = "https://openweathermap.org/img/wn/${w.weather.firstOrNull()?.icon}@2x.png"
                AsyncImage(
                    model = iconUrl,
                    contentDescription = "Weather Icon",
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("${w.name}, ${w.sys.country}", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(16.dp))

                // Temperature card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Temperature", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Current: ${w.main.temp}°C")
                        Text("Feels like: ${w.main.feels_like}°C")
                        Text("Min: ${w.main.temp_min}°C | Max: ${w.main.temp_max}°C")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Humidity & Wind card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Humidity & Wind", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Humidity: ${w.main.humidity}%")
                        Text("Wind: ${w.wind.speed} m/s, ${w.wind.deg}°")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sunrise & Sunset card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sunrise & Sunset", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))

                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val sunriseTime = sdf.format(Date(w.sys.sunrise * 1000))
                        val sunsetTime = sdf.format(Date(w.sys.sunset * 1000))

                        Text("Sunrise: $sunriseTime")
                        Text("Sunset: $sunsetTime")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weather description card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Condition", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${w.weather.firstOrNull()?.main ?: "N/A"} - ${w.weather.firstOrNull()?.description ?: "N/A"}")
                    }
                }

            }
            errorMessage != null -> {
                Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketPricesScreen() {
    val crops = listOf(
        "Rice" to "₹50/kg",
        "Wheat" to "₹40/kg",
        "Corn" to "₹30/kg",
        "Sugarcane" to "₹35/kg",
        "Cotton" to "₹60/kg",
        "Potato" to "₹25/kg",
        "Onion" to "₹30/kg",
        "Tomato" to "₹28/kg",
        "Chili" to "₹120/kg",
        "Cabbage" to "₹20/kg",
        "Carrot" to "₹30/kg",
        "Maize" to "₹32/kg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Market Prices", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(crops) { crop ->
                val (name, price) = crop
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = price,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun SchemesScreen() {
    val context = LocalContext.current
    val schemes = listOf(
        Triple(
            "PM-Kisan Samman Nidhi",
            "Direct income support of ₹6,000/year to eligible farmers.",
            "https://pmkisan.gov.in/"
        ),
        Triple(
            "Pradhan Mantri Fasal Bima Yojana (PMFBY)",
            "Crop insurance scheme to protect farmers against losses due to natural calamities.",
            "https://pmfby.gov.in/"
        ),
        Triple(
            "Soil Health Card Scheme",
            "Helps farmers improve soil fertility by providing soil health reports.",
            "https://soilhealth.dac.gov.in/"
        ),
        Triple(
            "National Agriculture Market (eNAM)",
            "Online trading platform for agricultural commodities across India.",
            "https://enam.gov.in/"
        ),
        Triple(
            "Kisan Credit Card (KCC)",
            "Provides short-term credit support to farmers for crop cultivation needs.",
            "https://www.myscheme.gov.in/schemes/kcc"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Government Schemes", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(schemes) { (title, description, url) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Learn More",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}


// ✅ Data class for chat messages
data class ChatMessage(val text: String?, val imageUri: Uri?, val isUser: Boolean)

@Composable
fun ChatScreen(tts: TextToSpeech?) {
    val context = LocalContext.current
    var userMessage by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // ✅ store selected image
    val chatHistory = remember { mutableStateListOf<ChatMessage>() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var lastAiResponse by remember { mutableStateOf<String?>(null) }

    // Function to send text messages
    fun sendMessage(message: String) {
        if (message.isNotBlank()) {
            val trimmedMessage = message.trim()
            chatHistory.add(ChatMessage(text = trimmedMessage, imageUri = null, isUser = true))
            userMessage = ""
            coroutineScope.launch {
                val response = GeminiHelper.getResponse(trimmedMessage)
                chatHistory.add(ChatMessage(text = response, imageUri = null, isUser = false))
                lastAiResponse = response
            }
        }
    }

    // Function to send image + optional text
    fun sendImageQuery(context: Context, message: String, imageUri: Uri) {
        chatHistory.add(ChatMessage(text = message.ifBlank { null }, imageUri = imageUri, isUser = true))
        userMessage = ""
        coroutineScope.launch {
            val response = GeminiHelper.sendQueryWithImage(context, message, imageUri)
            chatHistory.add(ChatMessage(text = response, imageUri = null, isUser = false))
            lastAiResponse = response
        }
    }

    // Speech-to-text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                sendMessage(spokenText)
            }
        }
    }

    // Image picker launcher
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri // ✅ store image for preview
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("KrishiBandhu AI Chat", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Chat bubbles
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState
        ) {
            items(chatHistory) { chat ->
                ChatBubble(chatMessage = chat)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Auto-scroll
        LaunchedEffect(chatHistory.size) {
            if (chatHistory.isNotEmpty()) {
                listState.animateScrollToItem(chatHistory.size - 1)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ Preview selected image before sending
        selectedImageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Selected image",
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
            )
        }

        // Text input + send button
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("Ask about crops, weather, etc.") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (selectedImageUri != null) {
                    sendImageQuery(context, userMessage, selectedImageUri!!)
                    selectedImageUri = null // ✅ clear after sending
                } else {
                    sendMessage(userMessage)
                }
            }) {
                Text("Send")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Image select button
        Button(
            onClick = { imageLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📷 Select Image")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Speech input button
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

        Spacer(modifier = Modifier.height(8.dp))

        // TTS Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { lastAiResponse?.let { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, null) } }
            ) { Text("▶ Speak") }

            Button(onClick = { tts?.stop() }) { Text("⏸ Stop") }
        }
    }
}

@Composable
fun ChatBubble(chatMessage: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (chatMessage.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (chatMessage.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // ✅ Show text if available
                chatMessage.text?.let { text ->
                    if (chatMessage.isUser) {
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        MarkdownText(
                            markdown = text,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // ✅ Show image if available
                chatMessage.imageUri?.let { uri ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = uri,
                        contentDescription = "Chat image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }
    }
}