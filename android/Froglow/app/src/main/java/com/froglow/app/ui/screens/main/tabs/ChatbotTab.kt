package com.froglow.app.ui.screens.main.tabs

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.froglow.app.R
import com.froglow.app.network.ChatRequest
import com.froglow.app.network.RetrofitClient
import kotlinx.coroutines.launch

private data class ChatMessage(
    val fromBot: Boolean,
    val text: String
)

private val ChatScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

private val BotBubbleBg = Color(0xFFF6F1EA)
private val UserBubbleBg = Color(0xFFEAF4E2)

private val DisabledButtonBg = Color(0xFFE9E5DE)
private val DisabledButtonText = Color(0xFFAAA39A)

@Composable
fun ChatbotTab() {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current
    val api = remember { RetrofitClient.create(context) }
    val scope = rememberCoroutineScope()

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(true, "안녕! 나는 Froglow 챗봇 🐸"),
                ChatMessage(true, "피부 고민을 말해줘!")
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatScreenBg)
    ) {
        ChatHeader()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    fromBot = msg.fromBot,
                    text = msg.text
                )
            }
        }

        ChatInputBar(
            value = input,
            onValueChange = { input = it },
            onSend = {
                val userMessage = input.trim()
                if (userMessage.isBlank()) return@ChatInputBar

                input = ""
                messages = messages + ChatMessage(false, userMessage)

                scope.launch {
                    val loadingMessage = ChatMessage(true, "답변 작성 중...")
                    messages = messages + loadingMessage

                    try {
                        Log.d("CHATBOT", "요청: $userMessage")

                        val res = kotlinx.coroutines.withTimeout(20000) {
                            api.askChatbot(ChatRequest(userMessage))
                        }

                        Log.d("CHATBOT", "응답 전체: $res")
                        Log.d("CHATBOT", "reply: ${res.reply}")

                        messages = messages
                            .dropLast(1) + ChatMessage(true, cleanBotReply(res.reply))

                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Log.e("CHATBOT", "타임아웃", e)

                        messages = messages
                            .dropLast(1) + ChatMessage(true, "에러: 응답 시간이 너무 오래 걸려요")
                    } catch (e: Exception) {
                        Log.e("CHATBOT", "에러", e)

                        val errorMsg = e.message ?: "알 수 없는 오류 발생"

                        messages = messages
                            .dropLast(1) + ChatMessage(true, "에러: $errorMsg")
                    }
                }
            }
        )
    }
}

@Composable
private fun ChatHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            ),
        shape = RoundedCornerShape(28.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = GreenSoft,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.frog_record),
                        contentDescription = "챗봇 개구리",
                        modifier = Modifier.size(38.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "챗봇",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                Text(
                    text = "피부 고민을 도와줄개굴",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    fromBot: Boolean,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromBot) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.78f),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (fromBot) 6.dp else 20.dp,
                bottomEnd = if (fromBot) 20.dp else 6.dp
            ),
            color = if (fromBot) BotBubbleBg else UserBubbleBg,
            border = BorderStroke(
                width = 1.dp,
                color = if (fromBot) CardBorder else GreenSoft
            ),
            tonalElevation = 0.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryText,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = value.isNotBlank()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = "피부 고민을 입력해보세요",
                        color = SecondaryText
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp),
                singleLine = true,
                shape = RoundedCornerShape(999.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onSend,
                enabled = canSend,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = DisabledButtonBg,
                    disabledContentColor = DisabledButtonText
                ),
                modifier = Modifier.size(width = 64.dp, height = 54.dp)
            ) {
                Text(
                    text = "➤",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun cleanBotReply(raw: String): String {
    val cleaned = raw
        .replace(Regex("개\\s*굴"), "")
        .replace("🐸", "")
        .replace(Regex("\\s+([.!?])"), "$1")
        .replace(Regex("[.!?]+"), "\n")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .ifBlank {
            "답변을 준비했어"
        }

    return "$cleaned 개굴 🐸"
}