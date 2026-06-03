package de.quati.grbl_laser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File
import kotlin.collections.plus
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText


class AppViewModel : ViewModel() {
    var uiState by mutableStateOf(AppUiState())
        private set

    fun onIntent(intent: AppIntent) {
        uiState = when (intent) {
            is AppIntent.FontSizeChanged -> uiState.copy(fontSize = intent.value)
            is AppIntent.LaserPowerChanged -> uiState.copy(laserPower = intent.value)
            is AppIntent.LaserSpeedChanged -> uiState.copy(laserSpeed = intent.value)
            is AppIntent.InputDataChanged -> uiState.copy(inputData = intent.value)
            is AppIntent.FontNameChanged -> uiState.copy(fontName = intent.value)
            is AppIntent.FontStyleToggled -> {
                val newStyles = if (intent.value in uiState.fontStyles) {
                    uiState.fontStyles - intent.value
                } else {
                    uiState.fontStyles + intent.value
                }
                uiState.copy(fontStyles = newStyles)
            }

            is AppIntent.HAlignChanged -> uiState.copy(hAlign = intent.value)
            is AppIntent.VAlignChanged -> uiState.copy(vAlign = intent.value)
            AppIntent.SuccessMessageDismissed -> uiState.copy(successMessage = null)
            is AppIntent.GenerateClicked -> {
                uiState.generateGCodeData?.let { data ->
                    println("Generating G-Code files...")
                    val nofDigits = (data.inputData.size + 1).toString().length.coerceAtLeast(2)
                    val outputDir = intent.outputDir.toPath().also { it.createDirectories() }
                    generate(data).forEachIndexed { index, (name, gcode) ->
                        val prefix = (index + 1).toString().padStart(nofDigits, '0')
                        val filename = "${prefix}_${name.toPathSafe()}.nc"
                        outputDir.resolve(filename).writeText(gcode.content)
                    }
                    println("Generated all files in $outputDir")
                    uiState.copy(successMessage = "Generated ${data.inputData.size} G-Code file(s) in $outputDir")
                } ?: uiState
            }
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
    val successMessage: String? = null,
) {
    val fontSizeValue = fontSize.toFloatOrNull()?.takeIf { it > 0f }
    val laserPowerValue = laserPower.toUIntOrNull()?.takeIf { it in 0u..1000u }
    val laserSpeedValue = laserSpeed.toUIntOrNull()?.takeIf { it > 0u }
    val inputDataValue = inputData.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        .takeIf { it.isNotEmpty() }
    val generateGCodeData: GenerateGCodeData? = run {
        GenerateGCodeData(
            fontName = fontName ?: return@run null,
            fontSize = fontSizeValue ?: return@run null,
            fontStyles = fontStyles,
            inputData = inputDataValue ?: return@run null,
            laserPower = laserPowerValue ?: return@run null,
            laserSpeed = laserSpeedValue ?: return@run null,
            hAlign = hAlign,
            vAlign = vAlign,
        )
    }
    val canGenerate = generateGCodeData != null
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
    data class GenerateClicked(val outputDir: File) : AppIntent
    data object SuccessMessageDismissed : AppIntent
}
