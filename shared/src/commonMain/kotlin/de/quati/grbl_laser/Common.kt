package de.quati.grbl_laser


sealed interface GenerateStatus {
    object NotStarted : GenerateStatus
    object InProgress : GenerateStatus
    data class Error(val message: String) : GenerateStatus
    data class Success(val message: String) : GenerateStatus
}
