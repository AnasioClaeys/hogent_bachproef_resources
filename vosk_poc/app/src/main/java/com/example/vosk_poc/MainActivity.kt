package com.example.vosk_poc

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vosk_poc.ui.theme.Vosk_pocTheme
import kotlinx.serialization.json.Json
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException

class MainActivity : ComponentActivity(), RecognitionListener {

    private var model: Model? = null
    private lateinit var speechService: SpeechService
    private var speechStreamService: SpeechStreamService? = null
    private var stringSpeech by mutableStateOf("")
    private var finalSpeechResult = mutableStateListOf<String>()
    private var checkedStates = mutableStateListOf<Boolean>()
    private var selectedModel by mutableStateOf("vosk-model-small-en-us-0.15")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initModel(selectedModel)

        setContent {
            Vosk_pocTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(
                        stringSpeech,
                        finalSpeechResult,
                        checkedStates,
                        startSpeechRecognition = { startSpeechRecognition() },
                        stopRecognition = { onDestroy() },
                        addToList = { addToList() },
                        removeFromList = { index -> removeFromList(index) },
                        setSelectedModel = { model ->
                            selectedModel = model
                            initModel(model)
                        },
                        onTextChange = { text -> stringSpeech = text }
                    )
                }
            }
        }
    }


    private fun parseJsonText(a: String): String {
        return Json.decodeFromString<DecodeJsonText>(a).partial
    }

    private fun parseJsonResult(a: String): String {
        return Json.decodeFromString<DecodeJsonResult>(a).text
    }

    private fun initModel(modelName: String) {
        StorageService.unpack(this, modelName, "model",
            { model: Model? ->
                this.model = model
            }
        ) { exception: IOException ->
            exception.printStackTrace()
        }
    }

    override fun onPartialResult(hypothesis: String?) {
    }

    override fun onResult(hypothesis: String?) {
        val result = parseJsonResult(hypothesis!!)
        stringSpeech += " $result"
    }

    override fun onFinalResult(hypothesis: String?) {
        if (speechStreamService != null) {
            speechStreamService = null
        }
        speechService.stop()
    }

    override fun onError(exception: Exception?) {
        exception?.printStackTrace()
    }

    override fun onTimeout() {
        TODO("Not yet implemented")
    }

    private fun startSpeechRecognition() {
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService.startListening(this)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (speechService != null) {
            speechService.stop()
            speechService.shutdown()
        }
        if (speechStreamService != null) {
            speechStreamService!!.stop()
        }
    }

    private fun addToList() {
        if (stringSpeech.isEmpty()) return
        finalSpeechResult.add(stringSpeech.trim())
        checkedStates.add(false)
        stringSpeech = ""
        onDestroy()
    }

    private fun removeFromList(index: Int) {
        if (index >= 0 && index < finalSpeechResult.size) {
            finalSpeechResult.removeAt(index)
            checkedStates.removeAt(index)
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(
    recognizedText: String,
    final: List<String>,
    checkedStates: MutableList<Boolean>,
    startSpeechRecognition: () -> Unit,
    modifier: Modifier = Modifier,
    stopRecognition: () -> Unit,
    addToList: () -> Unit,
    removeFromList: (Int) -> Unit,
    setSelectedModel: (String) -> Unit,
    onTextChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column {

        // Taalkeuze
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { setSelectedModel("vosk-model-small-en-us-0.15") },
                modifier = Modifier
                    .padding(horizontal = 2.dp)  // Verminderde horizontale padding
                    .weight(1f),  // Zorgt voor gelijke breedte
                colors = ButtonDefaults.buttonColors(Color(0xFF8fbbdb))
            ) {
                Text(
                    text = "Engels",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp,
                    color = Color(0xFF000000)
                )
            }

            Button(
                onClick = { setSelectedModel("vosk-model-small-nl-0.22") },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(Color(0xFFC0D7E8))
            ) {
                Text(
                    text = "Nederlands",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp,
                    color = Color(0xFF000000)
                )
            }

            Button(
                onClick = { setSelectedModel("vosk-model-small-fr-0.22") },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(Color(0xFFC0D7E8))
            ) {
                Text(
                    text = "Frans",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp,
                    color = Color(0xFF000000)
                )
            }
        }


        // speech recognition buttons

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = startSpeechRecognition,
                modifier = Modifier
                    .padding(8.dp)
                    .size(80.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.mic_24dp_fill0_wght400_grad0_opsz24__1_),
                    contentDescription = "Start spraakherkenning",
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(60.dp))

            Button(
                onClick = stopRecognition,
                modifier = Modifier
                    .padding(8.dp)
                    .size(80.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.mic_off_24dp_fill0_wght400_grad0_opsz24),
                    contentDescription = "Stop spraakherkenning",
                    modifier = Modifier.size(48.dp)
                )
            }
        }


        // Herkende spraak


        Row(
            modifier = Modifier.padding(top = 16.dp),
        ) {
            TextField(
                value = recognizedText, onValueChange = onTextChange,
                label = {
                    Text("Herkende spraak")
                },
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            )


            Button(
                onClick = {
                    addToList()
                    focusManager.clearFocus()  // Clear focus from the TextField
                },
                modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(13.dp)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Divider()

        LazyColumn(
            modifier= Modifier.fillMaxSize().padding(top = 8.dp, bottom = 8.dp),
            content = {
            final.forEachIndexed { index, item ->
                item {
                    var isChecked by remember { mutableStateOf(checkedStates[index]) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp) // Add padding to avoid clipping at the edges
                            .clip(RoundedCornerShape(8.dp)) // Apply border radius
                            .background(Color(0xFFC0D7E8)) // Set background color
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = Color(0xFFC0D7E8),
                            ),
                            leadingContent = {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        isChecked = it
                                        checkedStates[index] = it
                                    }
                                )
                            },
                            headlineText = {
                                Text(
                                    text = item,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else null
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { removeFromList(index) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        })
    }
}
