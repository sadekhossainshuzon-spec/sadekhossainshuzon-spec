package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class DatabaseTypeConverters {
    private val moshi = Moshi.Builder().build()

    @TypeConverter
    fun fromFunFactList(value: List<FunFact>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, FunFact::class.java)
        val adapter = moshi.adapter<List<FunFact>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toFunFactList(value: String?): List<FunFact>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, FunFact::class.java)
        val adapter = moshi.adapter<List<FunFact>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun fromQuizQuestionList(value: List<QuizQuestion>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, QuizQuestion::class.java)
        val adapter = moshi.adapter<List<QuizQuestion>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toQuizQuestionList(value: String?): List<QuizQuestion>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, QuizQuestion::class.java)
        val adapter = moshi.adapter<List<QuizQuestion>>(type)
        return adapter.fromJson(value)
    }
}
