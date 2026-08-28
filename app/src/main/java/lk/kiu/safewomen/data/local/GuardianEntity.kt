package lk.kiu.safewomen.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import lk.kiu.safewomen.data.model.Guardian

@Entity(tableName = "guardians")
data class GuardianEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean
) {
    fun toDomain(): Guardian = Guardian(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        relationship = relationship,
        isPrimary = isPrimary
    )

    companion object {
        fun fromDomain(guardian: Guardian): GuardianEntity = GuardianEntity(
            id = guardian.id,
            name = guardian.name,
            phoneNumber = guardian.phoneNumber,
            relationship = guardian.relationship,
            isPrimary = guardian.isPrimary
        )
    }
}
