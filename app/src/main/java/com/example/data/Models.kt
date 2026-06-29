package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FunFact(
    val english: String,
    val bangla: String
)

@JsonClass(generateAdapter = true)
data class QuizOption(
    val option: String, // "A", "B", or "C"
    val textEn: String,
    val textBn: String
)

@JsonClass(generateAdapter = true)
data class QuizQuestion(
    val questionEn: String,
    val questionBn: String,
    val options: List<QuizOption>,
    val correctOption: String // "A", "B", or "C"
)

@JsonClass(generateAdapter = true)
data class BoosterSet(
    val facts: List<FunFact>,
    val quiz: List<QuizQuestion>
)
