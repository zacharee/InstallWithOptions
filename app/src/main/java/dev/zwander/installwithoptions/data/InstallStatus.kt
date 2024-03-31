package dev.zwander.installwithoptions.data

enum class InstallStatus {
    SUCCESS,
    FAILURE,
}

data class InstallResult(
    val status: InstallStatus,
    val packageName: String,
    val message: String,
)
