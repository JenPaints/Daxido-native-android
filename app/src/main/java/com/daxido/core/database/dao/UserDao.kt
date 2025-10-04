package com.daxido.core.database.dao

import androidx.room.*
import com.daxido.core.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isSynced = 1 WHERE id = :userId")
    suspend fun markAsSynced(userId: String)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
