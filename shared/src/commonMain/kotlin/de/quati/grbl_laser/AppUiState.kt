package de.quati.grbl_laser

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File // TODO
import kotlin.collections.plus
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText


class AppViewModel : ViewModel() {
    val uiState: StateFlow<AppUiState>
        field = MutableStateFlow(AppUiState())

    fun onIntent(intent: AppIntent) {
        uiState.update { state ->
            when (intent) {
                is AppIntent.FontSizeChanged -> state.copy(fontSize = intent.value)
                is AppIntent.LaserPowerChanged -> state.copy(laserPower = intent.value)
                is AppIntent.LaserSpeedChanged -> state.copy(laserSpeed = intent.value)
                is AppIntent.InputDataChanged -> state.copy(inputData = intent.value)
                is AppIntent.FontNameChanged -> state.copy(fontName = intent.value)
                is AppIntent.FontStyleToggled -> {
                    val newStyles = if (intent.value in state.fontStyles) {
                        state.fontStyles - intent.value
                    } else {
                        state.fontStyles + intent.value
                    }
                    state.copy(fontStyles = newStyles)
                }

                is AppIntent.HAlignChanged -> state.copy(hAlign = intent.value)
                is AppIntent.VAlignChanged -> state.copy(vAlign = intent.value)
                AppIntent.SuccessMessageDismissed -> state.copy(successMessage = null)
                is AppIntent.GenerateClicked -> {
                    state.generateGCodeData?.let { data ->
                        println("Generating G-Code files...")
                        val nofDigits = (data.inputData.size + 1).toString().length.coerceAtLeast(2)
                        val outputDir = intent.outputDir.toPath().also { it.createDirectories() }
                        generate(data).forEachIndexed { index, (name, gcode) ->
                            val prefix = (index + 1).toString().padStart(nofDigits, '0')
                            val filename = "${prefix}_${name.toPathSafe()}.nc"
                            outputDir.resolve(filename).writeText(gcode.content)
                        }
                        println("Generated all files in $outputDir")
                        state.copy(successMessage = "Generated ${data.inputData.size} G-Code file(s) in $outputDir")
                    } ?: state
                }
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
    val plotThemeIsDark: Boolean = false,
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
