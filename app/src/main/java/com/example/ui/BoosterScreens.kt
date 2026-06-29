package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BoosterSetEntity
import com.example.data.FunFact
import com.example.data.QuizQuestion
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoosterMainScreen(
    viewModel: BoosterViewModel,
    modifier: Modifier = Modifier
) {
    val boosterSets by viewModel.allBoosterSets.collectAsState()
    val userProgress by viewModel.userProgress.collectAsState()
    val selectedSetId by viewModel.selectedSetId.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationError by viewModel.generationError.collectAsState()
    val showRewardDialog by viewModel.showRewardDialog.collectAsState()

    val currentSet = boosterSets.find { it.id == selectedSetId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CharcoalDark, Color(0xFF14151B))
                )
            )
    ) {
        if (currentQuestionIndex >= 0 && currentSet != null) {
            // Quiz Mode
            QuizPlayView(
                currentSet = currentSet,
                currentIndex = currentQuestionIndex,
                selectedAnswers = selectedAnswers,
                onAnswerSelected = { index, option ->
                    viewModel.answerQuestion(index, option)
                },
                onNextClicked = {
                    viewModel.nextQuestion(currentSet.quiz.size)
                },
                onQuitClicked = {
                    viewModel.exitQuiz()
                }
            )
        } else {
            // Dashboard Mode
            DashboardView(
                boosterSets = boosterSets,
                selectedSetId = selectedSetId,
                currentSet = currentSet,
                userCoins = userProgress?.coins ?: 0,
                factsCount = userProgress?.totalFactsReadCount ?: 0,
                quizzesCompleted = userProgress?.totalQuizzesCompleted ?: 0,
                isGenerating = isGenerating,
                onSetSelected = { viewModel.selectSet(it) },
                onStartQuiz = { viewModel.startQuiz() },
                onGenerateTopic = { viewModel.generateCustomSet(it) }
            )
        }

        // Gemini Error Message
        generationError?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { viewModel.clearGenerationError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = "AI Generation Error", fontWeight = FontWeight.Bold)
                    }
                },
                text = { Text(text = errorMsg) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearGenerationError() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "Dismiss")
                    }
                },
                containerColor = SurfaceDark,
                titleContentColor = Color.White,
                textContentColor = TextGray
            )
        }

        // Reward Dialog / Coin Shower
        showRewardDialog?.let { coins ->
            RewardDialogView(
                coinsRewarded = coins,
                onDismiss = { viewModel.dismissRewardDialog() }
            )
        }
    }
}

@Composable
fun DashboardView(
    boosterSets: List<BoosterSetEntity>,
    selectedSetId: String,
    currentSet: BoosterSetEntity?,
    userCoins: Int,
    factsCount: Int,
    quizzesCompleted: Int,
    isGenerating: Boolean,
    onSetSelected: (String) -> Unit,
    onStartQuiz: () -> Unit,
    onGenerateTopic: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var customTopicText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Status Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Brain Booster",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = AmberPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Learn daily, play quiz, earn coins!",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                // Coins Chip
                Row(
                    modifier = Modifier
                        .testTag("coins_indicator")
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFE5A93B), AmberPrimary)))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Coins",
                        tint = CharcoalDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$userCoins Coins",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                }
            }
        }

        // Stats Row Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, Color(0xFF2C2D35)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🧠 Facts Read",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$factsCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(Color(0xFF2C2D35))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🏆 Quizzes Play",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$quizzesCompleted",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = VioletAccent
                        )
                    }
                }
            }
        }

        // AI Custom Generator Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, Color(0xFF322A45)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "AI Booster",
                            tint = AmberPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "AI custom topic generator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Enter any topic (e.g., Space, Dinosaurs, Sports, Oceans) to instantly generate a bilingual fun-fact booster & reward quiz!",
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = customTopicText,
                        onValueChange = { customTopicText = it },
                        placeholder = { Text(text = "Type any topic...", color = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_topic_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (customTopicText.isNotBlank()) {
                                onGenerateTopic(customTopicText)
                                customTopicText = ""
                                focusManager.clearFocus()
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = Color(0xFF2C2D35),
                            focusedContainerColor = CharcoalDark,
                            unfocusedContainerColor = CharcoalDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (customTopicText.isNotBlank() && !isGenerating) {
                                onGenerateTopic(customTopicText)
                                customTopicText = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_generate_button"),
                        enabled = !isGenerating && customTopicText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = CharcoalDark,
                            disabledContainerColor = Color(0xFF2C2D35),
                            disabledContentColor = TextGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CharcoalDark,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Booster engine working...", fontSize = 14.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Spark",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Generate Bilingual Booster Set",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Available Booster Set Selector Tabs
        item {
            Text(
                text = "📚 Saved Boosters (আপনার বুস্টার সমূহ)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // List of saved booster sets
                boosterSets.forEach { set ->
                    val isSelected = set.id == selectedSetId
                    val hasCompleted = set.isCompleted

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) AmberPrimary else SurfaceDark)
                            .clickable { onSetSelected(set.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("booster_tab_${set.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (hasCompleted) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = "Status",
                                tint = if (isSelected) CharcoalDark else if (hasCompleted) EmeraldSuccess else TextGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = set.topic,
                                color = if (isSelected) CharcoalDark else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Selected Booster Set Content
        if (currentSet != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today's 10 Fun Facts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "১০টি মজার তথ্য (নিচের তথ্যগুলো মনোযোগ দিয়ে পড়ুন)",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }

                    if (currentSet.isCompleted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E3A24))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Score: ${currentSet.score}/5 ⭐",
                                color = EmeraldSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // The 10 bilingual facts
            itemsIndexed(currentSet.facts) { index, fact ->
                FactItemCard(index = index + 1, fact = fact)
            }

            // Quiz CTA card
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentSet.isCompleted) Color(0xFF161C18) else Color(0xFF1B1B3A)
                    ),
                    border = BorderStroke(1.dp, if (currentSet.isCompleted) Color(0xFF223528) else Color(0xFF2A2A5E)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Reward",
                            tint = AmberPrimary,
                            modifier = Modifier.size(44.dp)
                        )

                        Text(
                            text = if (currentSet.isCompleted) "Booster Set Mastered!" else "🎁 Daily Reward Quiz (৫টি কুইজ)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (currentSet.isCompleted) {
                                "You completed this quiz already! You can replay anytime to practice your mind and review these amazing fun facts."
                            } else {
                                "Answer all 5 questions correctly based on the facts above to unlock your reward of 50 Brain Coins!"
                            },
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "(৫০ ব্রেন কয়েন পুরস্কার জিততে ৫টি প্রশ্নের সঠিক উত্তর দিন!)",
                            fontSize = 11.sp,
                            color = AmberPrimary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )

                        Button(
                            onClick = onStartQuiz,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_quiz_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentSet.isCompleted) EmeraldSuccess else AmberPrimary,
                                contentColor = CharcoalDark
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (currentSet.isCompleted) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                contentDescription = "Play"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentSet.isCompleted) "Replay Quiz Practice" else "Unbox Daily Quiz Challenge",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FactItemCard(index: Int, fact: FunFact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fact_card_$index"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, Color(0xFF25262E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fact Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2D35)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        color = AmberPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "Fact $index (মজার তথ্য $index)",
                    color = AmberPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // English Text
            Text(
                text = fact.english,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 22.sp
            )

            // Bangla translation box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A22), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(24.dp)
                        .background(VioletAccent)
                )
                Text(
                    text = fact.bangla,
                    fontSize = 13.sp,
                    color = Color(0xFFDCDCE5),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun QuizPlayView(
    currentSet: BoosterSetEntity,
    currentIndex: Int,
    selectedAnswers: Map<Int, String>,
    onAnswerSelected: (Int, String) -> Unit,
    onNextClicked: () -> Unit,
    onQuitClicked: () -> Unit
) {
    val question = currentSet.quiz[currentIndex]
    val selectedOption = selectedAnswers[currentIndex]
    val isAnswered = selectedOption != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quiz Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onQuitClicked,
                modifier = Modifier
                    .background(Color(0xFF25262E), CircleShape)
                    .size(36.dp)
                    .testTag("quiz_quit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quit Quiz",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Reward Quiz challenge",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary
                )
                Text(
                    text = "Topic: ${currentSet.topic}",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }

            Box(modifier = Modifier.size(36.dp)) // Spacer to keep title centered
        }

        // Linear Progress Bar
        val progress = (currentIndex + 1).toFloat() / currentSet.quiz.size
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AmberPrimary,
            trackColor = Color(0xFF25262E)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question ${currentIndex + 1} of ${currentSet.quiz.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray
            )
            Text(
                text = "${((progress) * 100).toInt()}% Done",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AmberPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Question Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF25262E)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // English Question
                Text(
                    text = "Q${currentIndex + 1}. ${question.questionEn}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 26.sp
                )

                // Bangla Question
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16161C), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Translate",
                        tint = VioletAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = question.questionBn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDCDCE5),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Options List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            question.options.forEach { option ->
                val optionLetter = option.option
                val isSelected = selectedOption == optionLetter
                val isCorrect = optionLetter == question.correctOption

                // Define coloring based on states
                val (borderStroke, background) = when {
                    isAnswered && isCorrect -> {
                        // Correct option (highlighted in Green)
                        Pair(
                            BorderStroke(2.dp, EmeraldSuccess),
                            Color(0xFF113220)
                        )
                    }
                    isAnswered && isSelected && !isCorrect -> {
                        // User chose this option, but it is incorrect (highlighted in Red)
                        Pair(
                            BorderStroke(2.dp, CrimsonError),
                            Color(0xFF33141E)
                        )
                    }
                    else -> {
                        // Idle state or unselected incorrect options
                        Pair(
                            BorderStroke(1.dp, Color(0xFF25262E)),
                            SurfaceDark
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(background)
                        .clickable(enabled = !isAnswered) {
                            onAnswerSelected(currentIndex, optionLetter)
                        }
                        .border(borderStroke, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("option_$optionLetter"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${optionLetter})  ${option.textEn}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = option.textBn,
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }

                    // Status Icons at the right of option
                    if (isAnswered) {
                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Correct",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Incorrect",
                                tint = CrimsonError,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Actions Bottom CTA
        AnimatedVisibility(
            visible = isAnswered,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut()
        ) {
            Button(
                onClick = onNextClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("quiz_next_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    contentColor = CharcoalDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (currentIndex == currentSet.quiz.size - 1) "Submit Quiz Results" else "Next Question",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun RewardDialogView(
    coinsRewarded: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .testTag("reward_dialog_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(2.dp, AmberPrimary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🎉 CONGRATULATIONS!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = AmberPrimary,
                        letterSpacing = 1.sp
                    )

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Trophy",
                        tint = AmberPrimary,
                        modifier = Modifier.size(72.dp)
                    )

                    Text(
                        text = "Daily Reward Unlocked!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "You successfully answered the quiz and boosted your brain capacity!",
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    // Grand Coin Showcase
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2E2413))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Coins Earned",
                            tint = AmberPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+$coinsRewarded Coins",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberPrimary
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("claim_reward_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = CharcoalDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Claim Reward Coins",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
