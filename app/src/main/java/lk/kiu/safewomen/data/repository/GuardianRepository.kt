package lk.kiu.safewomen.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lk.kiu.safewomen.data.local.GuardianDao
import lk.kiu.safewomen.data.local.GuardianEntity
import lk.kiu.safewomen.data.model.Guardian

class GuardianRepository(private val guardianDao: GuardianDao) {

    val allGuardians: Flow<List<Guardian>> = guardianDao.getAllGuardians().map { list ->
        list.map { it.toDomain() }
    }

    val guardianCount: Flow<Int> = guardianDao.getGuardianCount()

    suspend fun getAllGuardiansSync(): List<Guardian> {
        return guardianDao.getAllGuardiansSync().map { it.toDomain() }
    }

    suspend fun insertGuardian(guardian: Guardian): Long {
        return guardianDao.insertGuardian(GuardianEntity.fromDomain(guardian))
    }

    suspend fun updateGuardian(guardian: Guardian) {
        guardianDao.updateGuardian(GuardianEntity.fromDomain(guardian))
    }

    suspend fun deleteGuardian(guardian: Guardian) {
        guardianDao.deleteGuardian(GuardianEntity.fromDomain(guardian))
    }

    suspend fun deleteGuardianById(id: Long) {
        guardianDao.deleteGuardianById(id)
    }
}
