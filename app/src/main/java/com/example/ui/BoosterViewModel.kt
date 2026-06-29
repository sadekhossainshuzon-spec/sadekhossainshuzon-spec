package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BoosterRepository
import com.example.data.BoosterSetEntity
import com.example.data.UserProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoosterViewModel(private val repository: BoosterRepository) : ViewModel() {

    val allBoosterSets: StateFlow<List<BoosterSetEntity>> = repository.allBoosterSets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userProgress: StateFlow<UserProgressEntity?> = repository.userProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _selectedSetId = MutableStateFlow<String>("daily")
    val selectedSetId: StateFlow<String> = _selectedSetId.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow<Int>(-1) // -1 means not playing
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    // Selected options map for current quiz: Question Index -> Option ("A", "B", "C")
    private val _selectedAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, String>> = _selectedAnswers.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    private val _showRewardDialog = MutableStateFlow<Int?>(null) // Contains coins rewarded if just finished successfully
    val showRewardDialog: StateFlow<Int?> = _showRewardDialog.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initDatabaseIfEmpty()
        }
    }

    fun selectSet(id: String) {
        _selectedSetId.value = id
        exitQuiz()
    }

    fun startQuiz() {
        _currentQuestionIndex.value = 0
        _selectedAnswers.value = emptyMap()
        _showRewardDialog.value = null
    }

    fun answerQuestion(index: Int, option: String) {
        val current = _selectedAnswers.value.toMutableMap()
        current[index] = option
        _selectedAnswers.value = current
    }

    fun nextQuestion(totalQuestions: Int) {
        val next = _currentQuestionIndex.value + 1
        if (next < totalQuestions) {
            _currentQuestionIndex.value = next
        } else {
            // End of quiz, submit!
            submitQuiz()
        }
    }

    private fun submitQuiz() {
        viewModelScope.launch {
            val setId = _selectedSetId.value
            val sets = allBoosterSets.value
            val currentSet = sets.find { it.id == setId } ?: return@launch

            var correctCount = 0
            currentSet.quiz.forEachIndexed { index, question ->
                val userAns = _selectedAnswers.value[index]
                if (userAns == question.correctOption) {
                    correctCount++
                }
            }

            // Reward is 10 coins per correct answer, and an extra bonus of 50 if they get ALL correct!
            val baseCoins = correctCount * 10
            val bonusCoins = if (correctCount == currentSet.quiz.size) 50 else 0
            val totalReward = baseCoins + bonusCoins

            // Update in repository
            repository.updateSetCompletion(setId, correctCount, totalReward)

            // Trigger reward UI if they won coins
            if (totalReward > 0) {
                _showRewardDialog.value = totalReward
            }

            // Increment facts read count if they did well
            repository.incrementFactsReadCount(currentSet.facts.size)

            _currentQuestionIndex.value = -1 // Exit playing state
        }
    }

    fun dismissRewardDialog() {
        _showRewardDialog.value = null
    }

    fun exitQuiz() {
        _currentQuestionIndex.value = -1
        _selectedAnswers.value = emptyMap()
    }

    fun generateCustomSet(topic: String) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            val result = repository.generateCustomBoosterSet(topic.trim())
            _isGenerating.value = false
            if (result.isSuccess) {
                val newSet = result.getOrThrow()
                _selectedSetId.value = newSet.id
            } else {
                val exception = result.exceptionOrNull()
                if (exception?.message == "API_KEY_MISSING") {
                    _generationError.value = "Gemini API Key is missing! Please configure GEMINI_API_KEY in the Secrets panel in AI Studio."
                } else {
                    _generationError.value = "Error generating set: ${exception?.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    fun clearGenerationError() {
        _generationError.value = null
    }

    class Factory(private val repository: BoosterRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BoosterViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BoosterViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
