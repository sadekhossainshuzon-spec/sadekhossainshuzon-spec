package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class BoosterRepository(private val boosterDao: BoosterDao) {

    val allBoosterSets: Flow<List<BoosterSetEntity>> = boosterDao.getAllBoosterSets()
    val userProgress: Flow<UserProgressEntity?> = boosterDao.getUserProgress()

    suspend fun initDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        // Ensure user progress row exists
        val currentProgress = boosterDao.getUserProgressOneShot()
        if (currentProgress == null) {
            boosterDao.insertUserProgress(UserProgressEntity(id = 1, coins = 0))
        }

        // Ensure default booster set exists
        val defaultSet = boosterDao.getBoosterSetByIdOneShot("daily")
        if (defaultSet == null) {
            boosterDao.insertBoosterSet(getDefaultBoosterSet())
        }
    }

    fun getBoosterSetById(id: String): Flow<BoosterSetEntity?> {
        return boosterDao.getBoosterSetById(id)
    }

    suspend fun updateSetCompletion(setId: String, score: Int, coinsReward: Int) = withContext(Dispatchers.IO) {
        val currentSet = boosterDao.getBoosterSetByIdOneShot(setId)
        if (currentSet != null && !currentSet.isCompleted) {
            // Mark set as completed with score
            boosterDao.updateBoosterSet(currentSet.copy(isCompleted = true, score = score))

            // Update user progress
            val currentProgress = boosterDao.getUserProgressOneShot() ?: UserProgressEntity(id = 1)
            boosterDao.insertUserProgress(
                currentProgress.copy(
                    coins = currentProgress.coins + coinsReward,
                    totalQuizzesCompleted = currentProgress.totalQuizzesCompleted + 1
                )
            )
        }
    }

    suspend fun incrementFactsReadCount(amount: Int) = withContext(Dispatchers.IO) {
        val currentProgress = boosterDao.getUserProgressOneShot() ?: UserProgressEntity(id = 1)
        boosterDao.insertUserProgress(
            currentProgress.copy(
                totalFactsReadCount = currentProgress.totalFactsReadCount + amount
            )
        )
    }

    suspend fun addCoins(amount: Int) = withContext(Dispatchers.IO) {
        val currentProgress = boosterDao.getUserProgressOneShot() ?: UserProgressEntity(id = 1)
        boosterDao.insertUserProgress(
            currentProgress.copy(
                coins = currentProgress.coins + amount
            )
        )
    }

    suspend fun generateCustomBoosterSet(topic: String): Result<BoosterSetEntity> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("API_KEY_MISSING"))
        }

        val promptText = """
            Generate a Brain Booster set on the topic: "$topic".
            You must return a JSON object with two fields: "facts" and "quiz".
            "facts" must be an array of exactly 10 interesting fun facts.
            Each fact must contain "english" (the fact in English) and "bangla" (the translation in Bangla).
            "quiz" must be an array of exactly 5 multiple choice questions (MCQs) based on the generated facts.
            Each quiz question must contain:
            - "questionEn": English question
            - "questionBn": Bangla translation of the question
            - "options": An array of exactly 3 options. Each option must contain:
              - "option": "A", "B", or "C"
              - "textEn": English option text
              - "textBn": Bangla translation of option text
            - "correctOption": The correct option identifier ("A", "B", or "C")

            JSON schema reference:
            {
              "facts": [
                {
                  "english": "Honey never spoils. You can eat 3,000-year-old honey!",
                  "bangla": "মধু কখনো নষ্ট হয় না। আপনি চাইলে ৩,০০০ বছর আগের মধুও খেতে পারবেন!"
                }
              ],
              "quiz": [
                {
                  "questionEn": "Which food item never spoils?",
                  "questionBn": "কোন খাবারটি কখনো নষ্ট হয় না?",
                  "options": [
                    {"option": "A", "textEn": "Milk", "textBn": "দুধ"},
                    {"option": "B", "textEn": "Honey", "textBn": "মধু"},
                    {"option": "C", "textEn": "Rice", "textBn": "ভাত"}
                  ],
                  "correctOption": "B"
                }
              ]
            }
            Ensure the content is completely valid JSON and translates both facts and questions beautifully into Bangla. Maintain a highly enthusiastic and engaging tone. Avoid markdown fences other than raw JSON.
        """.trimIndent()

        val request = GenerateContentRequestDto(
            contents = listOf(ContentDto(parts = listOf(PartDto(text = promptText)))),
            systemInstruction = ContentDto(
                parts = listOf(
                    PartDto(
                        text = "You are the core AI engine of the 'Brain Booster Quiz App'. You specialize in generating extremely interesting, accurate, and educational bilingual quiz booster sets in JSON format."
                    )
                )
            ),
            generationConfig = GenerationConfigDto(
                responseMimeType = "application/json",
                temperature = 0.8f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response from AI engine")

            val cleanedJsonText = cleanJson(jsonText)
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(BoosterSet::class.java)
            val parsedBoosterSet = adapter.fromJson(cleanedJsonText)
                ?: throw Exception("Failed to parse AI response")

            if (parsedBoosterSet.facts.isEmpty() || parsedBoosterSet.quiz.isEmpty()) {
                throw Exception("AI response contains empty facts or quiz list")
            }

            val entity = BoosterSetEntity(
                id = UUID.randomUUID().toString(),
                topic = topic,
                facts = parsedBoosterSet.facts,
                quiz = parsedBoosterSet.quiz
            )

            boosterDao.insertBoosterSet(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanJson(rawText: String): String {
        var cleaned = rawText.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }

    private fun getDefaultBoosterSet(): BoosterSetEntity {
        return BoosterSetEntity(
            id = "daily",
            topic = "Daily Facts (আজকের কুইজ)",
            facts = listOf(
                FunFact(
                    english = "Honey never spoils. You can eat 3,000-year-old honey!",
                    bangla = "মধু কখনো নষ্ট হয় না। আপনি চাইলে ৩,০০০ বছর আগের মধুও খেতে পারবেন!"
                ),
                FunFact(
                    english = "Bananas are curved because they grow towards the sun.",
                    bangla = "কলা বাঁকা হয় কারণ এটি সূর্যের আলোর দিকে বৃদ্ধি পায়।"
                ),
                FunFact(
                    english = "Wombat poop is cube-shaped, so it doesn't roll away.",
                    bangla = "ওমব্যাট (এক ধরণের প্রাণী) এর মল চারকোনা বা কিউব আকৃতির হয়, যাতে এটি গড়িয়ে চলে না যায়।"
                ),
                FunFact(
                    english = "Water makes up about 60% of the human body.",
                    bangla = "মানবদেহের প্রায় ৬০% অংশই পানি দিয়ে তৈরি।"
                ),
                FunFact(
                    english = "An octopus has three hearts and blue blood.",
                    bangla = "একটি অক্টোপাসের তিনটি হার্ট এবং নীল রক্ত থাকে।"
                ),
                FunFact(
                    english = "Cows have best friends and get stressed when they are separated.",
                    bangla = "গরুদেরও বেস্ট ফ্রেন্ড বা পরম বন্ধু থাকে এবং তারা আলাদা হয়ে গেলে মানসিক চাপে ভোগে।"
                ),
                FunFact(
                    english = "Cats can't taste sweetness.",
                    bangla = "বিড়াল মিষ্টি স্বাদের অনুভূতি পায় না।"
                ),
                FunFact(
                    english = "It rains diamonds on Saturn and Jupiter.",
                    bangla = "শনি এবং বৃহস্পতি গ্রহে হীরার বৃষ্টি হয়।"
                ),
                FunFact(
                    english = "Sound travels about 4 times faster in water than in air.",
                    bangla = "শব্দ বাতাসের চেয়ে পানিতে প্রায় ৪ গুণ দ্রুত ভ্রমণ করে।"
                ),
                FunFact(
                    english = "Sloths can hold their breath longer than dolphins.",
                    bangla = "স্লথ ডলফিনের চেয়েও বেশি সময় ধরে তাদের শ্বাস আটকে রাখতে পারে।"
                )
            ),
            quiz = listOf(
                QuizQuestion(
                    questionEn = "Which food item never spoils?",
                    questionBn = "কোন খাবারটি কখনো নষ্ট হয় না?",
                    options = listOf(
                        QuizOption("A", "Milk", "দুধ"),
                        QuizOption("B", "Honey", "মধু"),
                        QuizOption("C", "Rice", "ভাত")
                    ),
                    correctOption = "B"
                ),
                QuizQuestion(
                    questionEn = "Why are bananas curved?",
                    questionBn = "কলা কেন বাঁকা হয়?",
                    options = listOf(
                        QuizOption("A", "Because of heavy rain", "ভারী বৃষ্টির কারণে"),
                        QuizOption("B", "Because they grow towards the sun", "সূর্যের দিকে বৃদ্ধি পাওয়ার কারণে"),
                        QuizOption("C", "Because of the wind", "বাতাসের কারণে")
                    ),
                    correctOption = "B"
                ),
                QuizQuestion(
                    questionEn = "How many hearts does an octopus have?",
                    questionBn = "অক্টোপাসের কয়টি হার্ট থাকে?",
                    options = listOf(
                        QuizOption("A", "1", "১"),
                        QuizOption("B", "2", "২"),
                        QuizOption("C", "3", "৩")
                    ),
                    correctOption = "C"
                ),
                QuizQuestion(
                    questionEn = "On which planets does it rain diamonds?",
                    questionBn = "কোন গ্রহগুলোতে হীরার বৃষ্টি হয়?",
                    options = listOf(
                        QuizOption("A", "Mars and Earth", "মঙ্গল ও পৃথিবী"),
                        QuizOption("B", "Saturn and Jupiter", "শনি ও বৃহস্পতি"),
                        QuizOption("C", "Venus and Mercury", "শুক্র ও বুধ")
                    ),
                    correctOption = "B"
                ),
                QuizQuestion(
                    questionEn = "Which animal's poop is cube-shaped?",
                    questionBn = "কোন প্রাণীর মল চারকোনা আকৃতির হয়?",
                    options = listOf(
                        QuizOption("A", "Wombat", "ওমব্যাট"),
                        QuizOption("B", "Sloth", "স্লথ"),
                        QuizOption("C", "Cat", "বিড়াল")
                    ),
                    correctOption = "A"
                )
            )
        )
    }
}
