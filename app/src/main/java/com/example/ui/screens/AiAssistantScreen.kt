package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Assignment
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.ElevatedCardBg
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AiStreamState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    assignments: List<Assignment>,
    aiStreamState: AiStreamState,
    onGenerateDailyPlan: () -> Unit,
    onAskQuestion: (String) -> Unit
) {
    var customPrompt by remember { mutableStateOf("") }
    val activeCount = assignments.count { !it.isSubmitted }
    val revisionCount = assignments.count { !it.isSubmitted && it.needsRevision }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero AI Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElectricViolet.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.22f),
                                NeonTeal.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElectricViolet.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Gemini AI Наставник",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Активных заданий: $activeCount • На доработке: $revisionCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "ИИ знает ваш список предметов, на каком этапе вы остановились и какие замечания нужно исправить. Составьте приоритетный план фокуса или попросите помочь со структурой работы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Button(
                    onClick = onGenerateDailyPlan,
                    enabled = aiStreamState !is AiStreamState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_study_plan_button")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Составить план закрытия заданий и доработок")
                }
            }
        }

        // Quick prompt chips
        Text(
            text = "Быстрые запросы к Gemini",
            style = MaterialTheme.typography.labelLarge,
            color = SoftViolet
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "С чего начать доработки прямо сейчас?",
                "Как быстро оформить вывод к лабораторной?",
                "Составь план защиты курсовой на 3 минуты",
                "Как распределить 2 часа учёбы сегодня вечером?"
            ).forEach { suggestion ->
                AssistChip(
                    onClick = { onAskQuestion(suggestion) },
                    label = { Text(suggestion, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        // Custom question input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                placeholder = { Text("Спросите Gemini по любому заданию или предмету...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_question_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (customPrompt.isNotBlank()) {
                        onAskQuestion(customPrompt)
                        customPrompt = ""
                    }
                },
                enabled = customPrompt.isNotBlank() && aiStreamState !is AiStreamState.Loading,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricViolet)
                    .testTag("ai_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить запрос Gemini",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Streaming / Response output card
        when (aiStreamState) {
            is AiStreamState.Idle -> {
                Surface(
                    color = MidnightSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                ) {
                    Text(
                        text = "Нажмите «Составить план закрытия заданий» выше или выберите быстрый вопрос, чтобы получить персональный разбор от Gemini 3.8 Flash.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
            is AiStreamState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = NeonTeal, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Gemini анализирует ваши задания и дедлайны...")
                }
            }
            is AiStreamState.Streaming -> {
                AiResponseCard(text = aiStreamState.text, isStreaming = true)
            }
            is AiStreamState.Success -> {
                AiResponseCard(text = aiStreamState.text, isStreaming = false)
            }
            is AiStreamState.Error -> {
                Surface(
                    color = CoralDanger.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CoralDanger.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = aiStreamState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoralDanger,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun AiResponseCard(text: String, isStreaming: Boolean) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonTeal.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isStreaming) "Gemini печатает ответ..." else "Ответ Gemini AI",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonTeal
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
