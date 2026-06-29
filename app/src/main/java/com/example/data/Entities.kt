package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "booster_sets")
data class BoosterSetEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val facts: List<FunFact>,
    val quiz: List<QuizQuestion>,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val score: Int = 0
)

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 0,
    val totalFactsReadCount: Int = 0,
    val totalQuizzesCompleted: Int = 0
)
