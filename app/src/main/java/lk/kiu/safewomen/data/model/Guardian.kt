package lk.kiu.safewomen.data.model

data class Guardian(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String = "Guardian",
    val isPrimary: Boolean = false
)
