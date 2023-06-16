@file:OptIn(ExperimentalMaterial3Api::class, BetaOpenAI::class)

package com.example.clientchatgpt

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.BetaOpenAI
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import coil.compose.AsyncImage
import com.example.clientchatgpt.ui.theme.ClientChatGPTTheme

const val YOU_NAME = "You"
const val AI_NAME = "AI"
const val TEXT_AI_NAME = "gpt-3.5-turbo"
const val IMAGE_AI_NAME = "generateImage"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClientChatGPTTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                    StartChat()
                    ChatApp(this.applicationContext)
                }
            }
        }
    }
}

@Composable
fun ChoiceAIModel(
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
):String {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(items.indexOf(selectedItem)) }
    var currentItem by remember { mutableStateOf(selectedItem) }

    Box(modifier = modifier) {
        Text(
            text = currentItem ?: "Select an item",
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(16.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { 200.dp })
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    modifier = modifier,
                    onClick = {
                        onItemSelected(item)
                        currentItem = item
                        selectedIndex = items.indexOf(currentItem)
                        expanded = false
                    },
                    text = {
                        Text(
                            text = item,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
    return currentItem.toString()
}

fun onItemSelected(item: String) {
    // Handle the selected item here
    // You can perform any necessary operations or update the state accordingly
    println("Selected item: $item")
}

@Composable
fun StartChat() {
    Column() {
        ChoiceAIModel(listOf(TEXT_AI_NAME, IMAGE_AI_NAME),
            TEXT_AI_NAME,
            onItemSelected = { item ->
                onItemSelected(item)
            })
        OneMessage(ChatMessage(YOU_NAME, AnnotatedString("Text message!!\nSecond line of the message.")))
        OneMessage(ChatMessage(AI_NAME, AnnotatedString("Text message!!\nSecond line of the message.")))
    }
}

data class ChatMessage(
    val name: String,
    val text: AnnotatedString
)

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ChatApp(applicationContext: Context) {
    val chatAI = ChatAI()
    val lifecycleOwner = LocalLifecycleOwner.current
    var messagesTextChat by remember { mutableStateOf(mutableListOf<ChatMessage>()) }
    var messagesImageChat by remember { mutableStateOf(mutableListOf<ChatMessage>()) }
    var currentChat by remember { mutableStateOf(TEXT_AI_NAME) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier,
        topBar = {
            currentChat = ChoiceAIModel(listOf(TEXT_AI_NAME, IMAGE_AI_NAME),
                currentChat,
                onItemSelected = { item ->
                    onItemSelected(item)
                }
            )
        },
        bottomBar = {
            val request = InputText(modifier = Modifier)
            if (request.isNotEmpty()) {
                var message = ChatMessage(YOU_NAME, request)
                if (currentChat == TEXT_AI_NAME) {
                    messagesTextChat = (messagesTextChat.toMutableList() + message).toMutableList()
                } else {
                    messagesImageChat = (messagesImageChat.toMutableList() + message).toMutableList()
                }

                isLoading = true
                lifecycleOwner.lifecycleScope.launch {
                    val response =
                        GetResponseFromAI(chatAI, currentChat, request, applicationContext)
                    message = ChatMessage(AI_NAME, response)
                    if (currentChat == TEXT_AI_NAME) {
                        messagesTextChat =
                            (messagesTextChat.toMutableList() + message).toMutableList()
                    } else {
                        messagesImageChat =
                            (messagesImageChat.toMutableList() + message).toMutableList()
                    }
                    isLoading = false
                }
            }
        }
    ) { it ->
        Box(modifier = Modifier.padding(it)) {
            if (currentChat == TEXT_AI_NAME) {
                val textChatState = rememberLazyListState()
                LaunchedEffect(messagesTextChat.size){
                    if (messagesTextChat.size > 0) {
                        textChatState.animateScrollToItem(
                            messagesTextChat.size - 1,
                            0
                        )
                    }
                }
                LazyColumn(contentPadding = it, state = textChatState) {
                    items(messagesTextChat.size) { index ->
                        val message = messagesTextChat[index]
                        OneMessage(message, modifier = Modifier)
                    }
                }
            } else {
                val imageChatState = rememberLazyListState()
                LaunchedEffect(messagesImageChat.size){
                    if (messagesImageChat.size > 0) {
                        imageChatState.animateScrollToItem(
                            messagesImageChat.size - 1,
                            0
                        )
                    }
                }
                LazyColumn(contentPadding = it, state = imageChatState) {
                    items(messagesImageChat.size) { index ->
                        val message = messagesImageChat[index]
                        OneMessage(message, modifier = Modifier)
                    }
                }
            }
        }
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }
    }
}

suspend fun GetResponseFromAI(
    chatAI: ChatAI,
    currentChat: String,
    request: AnnotatedString,
    applicationContext: Context
): AnnotatedString {
    var str: AnnotatedString = AnnotatedString("")

    if (currentChat == TEXT_AI_NAME) {
        str = AnnotatedString(chatAI.doChat(request.toString()))
        println("response from Text AI: $str")
    } else {
        val strList = chatAI.doImage(request.toString())
        println("response from Image AI: $strList")
        for (url in strList) {
            val annotatedString = buildAnnotatedString {
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(style = SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                    append("Picture_link\n")
                }
                pop()
                append("\n")
            }
            str += annotatedString
        }
        str += AnnotatedString("\n")
    }

    return str
}

@Composable
fun InputText(modifier: Modifier = Modifier): AnnotatedString {
    // bottom components
    var requestText by remember { mutableStateOf("") }
    var buttonClicked by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // text input
        OutlinedTextField(
            value = requestText,
            onValueChange = { requestText = it },
            label = { Text("Enter your text here") },
            modifier = modifier.weight(1f),
            maxLines = 2
        )
        // button
        Button(
            onClick = {
                if (requestText.isEmpty()) {
                    return@Button
                }
                buttonClicked = true
            }
        ) {
            Text(text = "Click me")
        }
    }

    if (buttonClicked) {
        buttonClicked = false
        val ret = AnnotatedString(requestText)
        requestText = ""
        return ret
    }

    return AnnotatedString("")
}

@Composable
fun DrawIcon(color: Color) {
    Canvas(
        modifier = Modifier.size(52.dp),
        onDraw = {
            val rectangleSize = Size(64f, 64f)
            val rectangleBounds = Rect(0f, 0f, rectangleSize.width, rectangleSize.height)
            drawRect(
                color = color,
                topLeft = rectangleBounds.center,
                size = rectangleSize,
                style = Stroke(width = 16f)
            )
        }
    )
}

@Composable
fun OneMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    val colorIcon: Color = when (message.name) {
        YOU_NAME -> Color.Green
        AI_NAME -> Color.Blue
        else -> Color.Red
    }

    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val currentTime = LocalTime.now()
    val formattedTime = currentTime.format(formatter)
    val intro = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)){
            append("${message.name} ")
        }
        append("($formattedTime):\n")
    }
    val uriHandler = LocalUriHandler.current
    val str: AnnotatedString = intro + message.text

    Row() {
        DrawIcon(colorIcon)
        Column(modifier = modifier.wrapContentSize()) {
            SelectionContainer() {
                ClickableText(
                    text = str,
                    modifier = modifier,
                    style = TextStyle(color = MaterialTheme.colorScheme.primary),
                    onClick = { offset ->
                        str.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    }
                )
            }
            val urls = mutableListOf<String>()
            message.text.getStringAnnotations(tag = "URL", start = 0, end = str.length).forEach { annotation ->
                if (annotation.tag == "URL") {
                    urls.add(annotation.item as String)
                }
            }
            if (urls.size == 2) {
                AsyncImage(
                    model = urls[0],
                    contentDescription = null
                )
                AsyncImage(
                    model = urls[1],
                    contentDescription = null
                )
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ClientChatGPTTheme {
        OneMessage(ChatMessage("Android", AnnotatedString("My message!")))
    }
}
