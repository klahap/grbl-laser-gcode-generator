package de.quati.grbl_laser

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlin.collections.plus
import kotlinx.io.writeString


class AppViewModel : ViewModel() {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("AppViewModel"))
    val uiState: StateFlow<AppUiState>
        field = MutableStateFlow(AppUiState())

    fun onIntent(intent: AppIntent): Unit = when (intent) {
        is AppIntent.FontSizeChanged -> uiState.update { it.copy(fontSize = intent.value) }
        is AppIntent.LaserPowerChanged -> uiState.update { it.copy(laserPower = intent.value) }
        is AppIntent.LaserSpeedChanged -> uiState.update { it.copy(laserSpeed = intent.value) }
        is AppIntent.InputDataChanged -> uiState.update { it.copy(inputData = intent.value) }
        is AppIntent.FontNameChanged -> uiState.update { it.copy(fontName = intent.value) }
        is AppIntent.FontStyleToggled -> uiState.update { state ->
            val newStyles = if (intent.value in state.fontStyles) {
                state.fontStyles - intent.value
            } else {
                state.fontStyles + intent.value
            }
            state.copy(fontStyles = newStyles)
        }

        is AppIntent.HAlignChanged -> uiState.update { it.copy(hAlign = intent.value) }
        is AppIntent.VAlignChanged -> uiState.update { it.copy(vAlign = intent.value) }
        AppIntent.GeneratorStatusMessageDismissed -> uiState.update { it.copy(generateStatus = GenerateStatus.NotStarted) }
        is AppIntent.GenerateClicked -> runAsync(::generate)
    }

    private fun runAsync(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    private suspend fun generate() {
        uiState.update { it.copy(generateStatus = GenerateStatus.InProgress) }
        val generator = uiState.value.generator
        val inputData = uiState.value.inputDataValue
        if (inputData == null || generator == null)
            return uiState.update { it.copy(generateStatus = GenerateStatus.Error("Input data or generator settings not valid")) }

        val outputDir = FileKit.openDirectoryPicker(
            directory = null,
            dialogSettings = FileKitDialogSettings(
                title = "Select Output Directory",
            ),
        )?.path?.let(::Path)?.also {
            appFileSystem.createDirectories(path = it)
        } ?: return uiState.update {
            it.copy(generateStatus = GenerateStatus.Error("No output directory selected"))
        }

        val nofDigits = (inputData.size + 1).toString().length.coerceAtLeast(2)
        inputData.forEachIndexed { index, text ->
            val prefix = (index + 1).toString().padStart(nofDigits, '0')
            val filePath = Path(base = outputDir, "${prefix}_${text.toPathSafe()}.nc")
            val gcode = generator.generateGCode(text)

            appFileSystem.sink(path = filePath, append = false).use { sink ->
                val buffer = Buffer().apply {
                    writeString(gcode.content)
                }
                sink.write(buffer, buffer.size)
            }
        }
        uiState.update {
            it.copy(generateStatus = GenerateStatus.Success("Generated ${inputData.size} G-Code file(s) in:\n$outputDir"))
        }
    }
}

data class AppUiState(
    val fontSize: String = "13.0",
    val laserPower: String = "400",
    val laserSpeed: String = "400",
    val inputData: String = "",
    val fontName: String? = getDefaultFont() ?: getFonts().firstOrNull(),
    val fontStyles: Set<FontStyle> = emptySet(),
    val hAlign: Align = Align.CENTER,
    val vAlign: Align = Align.CENTER,
    val generateStatus: GenerateStatus = GenerateStatus.NotStarted,
    val plotThemeIsDark: Boolean = false,
) {

    val fontSizeValue = fontSize.toFloatOrNull()?.takeIf { it > 0f }
    val laserPowerValue = laserPower.toUIntOrNull()?.takeIf { it in 0u..1000u }
    val laserSpeedValue = laserSpeed.toUIntOrNull()?.takeIf { it > 0u }
    val inputDataValue = inputData.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        .takeIf { it.isNotEmpty() }
    val generator = generateGCodeData()?.toGenerator()
    val canGenerate = generator != null && inputDataValue != null

    fun generateGCodeData(): GeneratorSettings? = GeneratorSettings(
        fontName = fontName ?: return null,
        fontSize = fontSizeValue ?: return null,
        fontStyles = fontStyles,
        laserPower = laserPowerValue ?: return null,
        laserSpeed = laserSpeedValue ?: return null,
        hAlign = hAlign,
        vAlign = vAlign,
    )
}

sealed interface AppIntent {
    data class FontSizeChanged(val value: String) : AppIntent
    data class LaserPowerChanged(val value: String) : AppIntent
    data class LaserSpeedChanged(val value: String) : AppIntent
    data class InputDataChanged(val value: String) : AppIntent
    data class FontNameChanged(val value: String?) : AppIntent
    data class FontStyleToggled(val value: FontStyle) : AppIntent
    data class HAlignChanged(val value: Align) : AppIntent
    data class VAlignChanged(val value: Align) : AppIntent
    data object GenerateClicked : AppIntent
    data object GeneratorStatusMessageDismissed : AppIntent
}
