package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BoosterDao {
    @Query("SELECT * FROM booster_sets ORDER BY createdAt DESC")
    fun getAllBoosterSets(): Flow<List<BoosterSetEntity>>

    @Query("SELECT * FROM booster_sets WHERE id = :id LIMIT 1")
    fun getBoosterSetById(id: String): Flow<BoosterSetEntity?>

    @Query("SELECT * FROM booster_sets WHERE id = :id LIMIT 1")
    suspend fun getBoosterSetByIdOneShot(id: String): BoosterSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoosterSet(set: BoosterSetEntity)

    @Update
    suspend fun updateBoosterSet(set: BoosterSetEntity)

    @Query("DELETE FROM booster_sets WHERE id = :id")
    suspend fun deleteBoosterSetById(id: String)

    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    fun getUserProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    suspend fun getUserProgressOneShot(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgress(progress: UserProgressEntity)

    @Update
    suspend fun updateUserProgress(progress: UserProgressEntity)
}
