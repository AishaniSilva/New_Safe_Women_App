package lk.kiu.safewomen.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GuardianDao {
    @Query("SELECT * FROM guardians ORDER BY isPrimary DESC, id ASC")
    fun getAllGuardians(): Flow<List<GuardianEntity>>

    @Query("SELECT * FROM guardians ORDER BY isPrimary DESC, id ASC")
    suspend fun getAllGuardiansSync(): List<GuardianEntity>

    @Query("SELECT * FROM guardians WHERE id = :id LIMIT 1")
    suspend fun getGuardianById(id: Long): GuardianEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuardian(guardian: GuardianEntity): Long

    @Update
    suspend fun updateGuardian(guardian: GuardianEntity)

    @Delete
    suspend fun deleteGuardian(guardian: GuardianEntity)

    @Query("DELETE FROM guardians WHERE id = :id")
    suspend fun deleteGuardianById(id: Long)

    @Query("SELECT COUNT(*) FROM guardians")
    fun getGuardianCount(): Flow<Int>
}
